/*
 * MIT License
 *
 * Stream-rec  https://github.com/hua0512/stream-rec
 *
 * Copyright (c) 2024 hua0512 (https://github.com/hua0512)
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package github.hua0512.plugins.base

import github.hua0512.app.App
import github.hua0512.data.config.Action
import github.hua0512.data.config.DownloadConfig
import github.hua0512.data.dto.GlobalPlatformConfig
import github.hua0512.data.event.StreamerEvent.*
import github.hua0512.data.stream.StreamData
import github.hua0512.data.stream.Streamer
import github.hua0512.data.stream.StreamingPlatform
import github.hua0512.plugins.event.EventCenter
import github.hua0512.utils.deleteFile
import github.hua0512.utils.withIOContext
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.datetime.Clock
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import kotlin.io.path.Path
import kotlin.time.Duration
import kotlin.time.DurationUnit
import kotlin.time.toDuration

/**
 * Class responsible for downloading streamer streams
 * @author hua0512
 * @date : 2024/4/21 20:15
 */
class StreamerDownloadManager(
  private val app: App,
  private val streamer: Streamer,
  private val plugin: Download<DownloadConfig>,
  private val downloadSemaphore: Semaphore,
) {

  companion object {
    @JvmStatic
    private val logger: Logger = LoggerFactory.getLogger(StreamerDownloadManager::class.java)
  }

  /**
   * List to store the downloaded stream data
   */
  private val dataList = mutableListOf<StreamData>()

  /**
   * Returns the platform config for the streamer
   */
  private val StreamingPlatform.platformConfig: GlobalPlatformConfig
    get() {
      return when (this) {
        StreamingPlatform.HUYA -> app.config.huyaConfig
        StreamingPlatform.DOUYIN -> app.config.douyinConfig
        StreamingPlatform.DOUYU -> app.config.douyuConfig
        else -> throw UnsupportedOperationException("Platform not supported")
      }
    }


  // download retry count
  private var retryCount = 0

  // delay to wait before retrying the download, used when streams goes from live to offline
  private val retryDelay = app.config.downloadRetryDelay.toDuration(DurationUnit.SECONDS)

  // delay between download checks
  private val downloadInterval = app.config.downloadCheckInterval.toDuration(DurationUnit.SECONDS)

  // retry delay for parted downloads
  private val platformRetryDelay =
    (streamer.platform.platformConfig.partedDownloadRetry ?: 0).toDuration(DurationUnit.SECONDS)

  // max download retries
  private val maxRetry = app.config.maxDownloadRetries

  /**
   * Flag to check if the download is cancelled
   */
  private var isCancelled = MutableStateFlow(false)

  /**
   * Flag to check if the download is in progress
   */
  private var isDownloading = false

  private var updateLiveStatusCallback: suspend (id: Long, isLive: Boolean) -> Unit = { _, _ -> }
  private var updateStreamerLastLiveTime: suspend (id: Long, lastLiveTime: Long) -> Unit = { _, _ -> }
  private var checkShouldUpdateStreamerLastLiveTime: suspend (id: Long, lastLiveTime: Long, now: Long) -> Boolean =
    { _, _, _ -> false }
  private var onSavedToDb: suspend (stream: StreamData) -> Unit = {}
  private var avatarUpdateCallback: (id: Long, avatarUrl: String) -> Unit = { _, _ -> }
  private var onDescriptionUpdateCallback: (id: Long, description: String) -> Unit = { _, _ -> }
  private var onRunningActions: suspend (data: List<StreamData>, actions: List<Action>) -> Unit = { _, _ -> }


  suspend fun init() = supervisorScope {
    plugin.apply {
      avatarUrlUpdateCallback {
        streamer.avatar = it
        logger.info("avatar updated : $it")
        avatarUpdateCallback(streamer.id, it)
      }
      descriptionUpdateCallback {
        streamer.streamTitle = it
        logger.info("description updated : $it")
        onDescriptionUpdateCallback(streamer.id, it)
      }
      init(streamer)
    }
  }

  private suspend fun handleMaxRetry() = supervisorScope {
    // reset retry count
    retryCount = 0
    // update db with the new isLive value
    if (streamer.isLive) updateLiveStatusCallback(streamer.id, false)
    streamer.isLive = false
    // stream is not live or without data
    if (dataList.isEmpty()) {
      return@supervisorScope
    }
    // stream finished with data
    logger.info("${streamer.name} stream finished")
    EventCenter.sendEvent(
      StreamerOffline(
        streamer.name,
        streamer.url,
        streamer.platform,
        Clock.System.now(),
        dataList.toList()
      )
    )
    // call onStreamingFinished callback with the copy of the list
    launch {
      bindOnStreamingEndActions(streamer, dataList.toList())
    }
    dataList.clear()
    delay(downloadInterval)
  }

  private suspend fun checkStreamerLiveStatus(): Boolean {
    return try {
      // check if streamer is live
      plugin.shouldDownload()
    } catch (e: Exception) {
      when (e) {
        is IllegalArgumentException -> throw e // rethrow the exception
        else -> {
          logger.error("${streamer.name} error while checking if streamer is live : ${e.message}")
          false
        }
      }
    }
  }

  private suspend fun handleLiveStreamer() {
    // save streamer to the database with the new isLive value
    if (!streamer.isLive) {
      EventCenter.sendEvent(
        StreamerOnline(
          streamer.name,
          streamer.url,
          streamer.platform,
          streamer.streamTitle ?: "",
          Clock.System.now()
        )
      )
      updateLiveStatusCallback(streamer.id, true)
    }
    streamer.isLive = true
    updateLastLiveTime()
    // while loop for parted download
    while (true) {
      val stream = downloadStream()
      if (stream == null) {
        logger.error("${streamer.name} unable to get stream data (${retryCount + 1}/$maxRetry)")
        break
      }
      // save the stream data to the database
      saveStreamData(stream)
      if (!isCancelled.value) delay(platformRetryDelay)
      else break
    }
  }

  private suspend fun downloadStream(): StreamData? {
    // stream is live, start downloading
    // while loop for parting the download
    return downloadSemaphore.withPermit {
      isDownloading = true
      try {
        plugin.download()
      } catch (e: Exception) {
        EventCenter.sendEvent(StreamerException(streamer.name, streamer.url, streamer.platform, Clock.System.now(), e))
        when (e) {
          is IllegalArgumentException, is UnsupportedOperationException -> {
            streamer.isLive = false
            updateLiveStatusCallback(streamer.id, false)
            logger.error("${streamer.name} invalid url or invalid engine : ${e.message}")
            throw e
          }

          is CancellationException -> {
            isCancelled.value = true
            throw e
          }

          else -> {
            logger.error("${streamer.name} Error while getting stream data : ${e.message}")
            null
          }
        }
      } finally {
        isDownloading = false
      }
    }
  }


  private suspend fun updateLastLiveTime() {
    val now = Clock.System.now()
    if (checkShouldUpdateStreamerLastLiveTime(streamer.id, streamer.lastLiveTime ?: 0, now.epochSeconds)) {
      updateStreamerLastLiveTime(streamer.id, now.epochSeconds)
      streamer.lastLiveTime = now.epochSeconds
    }
  }

  private suspend fun saveStreamData(stream: StreamData) = withIOContext {
    try {
      onSavedToDb(stream)
      logger.debug("saved to db : {}", stream)
    } catch (e: Exception) {
      logger.error("${streamer.name} error while saving $stream : ${e.message}")
    }
    dataList.add(stream)
    logger.info("${streamer.name} downloaded : $stream}")
    // execute post parted download actions
    launch {
      try {
        executePostPartedDownloadActions(streamer, stream)
      } catch (e: Exception) {
        logger.error("${streamer.name} error while executing post parted download actions : ${e.message}")
      }
    }
  }


  private fun handleOfflineStreamer() {
    if (dataList.isNotEmpty()) {
      logger.error("${streamer.name} unable to get stream data (${retryCount + 1}/$maxRetry)")
    } else {
      logger.info("${streamer.name} is not live")
    }
  }


  suspend fun start(): Unit = supervisorScope {
    // download the stream
    while (!isCancelled.value) {
      launch {
        isCancelled.collect {
          if (it) {
            // await for the download to finish
            stop()
            if (!isDownloading) {
              // break the loop if download is not in progress
              logger.info("Download not in progress, cancelling download for ${streamer.name}")
              this@supervisorScope.cancel("Download cancelled")
            }
          }
        }
      }

      if (retryCount >= maxRetry) {
        handleMaxRetry()
        continue
      }
      val isLive = checkStreamerLiveStatus()

      if (isLive) {
        handleLiveStreamer()
      } else {
        handleOfflineStreamer()
      }
      if (isCancelled.value) break
      retryCount++
      delay(getDelay())
    }
    throw CancellationException("Download cancelled")
  }

  /**
   * Returns the delay to wait before checking the stream again
   * if a data list is not empty, then it means the stream has ended
   * wait [retryDelay] seconds before checking again
   * otherwise wait [downloadInterval] seconds
   *
   * @return [Duration] delay
   */
  private fun getDelay(): Duration {
    return if (dataList.isNotEmpty()) {
      retryDelay
    } else {
      downloadInterval
    }
  }

  private suspend fun stop() {
    isCancelled.value = true
    plugin.stopDownload()
  }

  suspend fun cancel() {
    logger.info("Cancelling download for ${streamer.name}, isDownloading : $isDownloading")
    isCancelled.value = true
  }


  private suspend fun bindOnStreamingEndActions(streamer: Streamer, streamDataList: List<StreamData>) {
    val actions =
      streamer.templateStreamer?.downloadConfig?.onStreamingFinished ?: streamer.downloadConfig?.onStreamingFinished
    actions?.let {
      onRunningActions(streamDataList, it)
    } ?: run {
      // check if on parted download is also empty
      val partedActions =
        streamer.templateStreamer?.downloadConfig?.onPartedDownload ?: streamer.downloadConfig?.onPartedDownload
      if (partedActions.isNullOrEmpty()) {
        // delete files if both onStreamFinished and onPartedDownload are empty
        if (app.config.deleteFilesAfterUpload) {
          streamDataList.forEach { Path(it.outputFilePath).deleteFile() }
        }
      }
    }
  }

  private suspend fun executePostPartedDownloadActions(streamer: Streamer, streamData: StreamData) {
    val actions =
      streamer.templateStreamer?.downloadConfig?.onPartedDownload ?: streamer.downloadConfig?.onPartedDownload
    actions?.let {
      onRunningActions(listOf(streamData), it)
    }
  }

  fun onRunningActions(onRunningActions: suspend (data: List<StreamData>, actions: List<Action>) -> Unit) {
    this.onRunningActions = onRunningActions
  }

  fun onAvatarUpdate(avatarUpdateCallback: (id: Long, avatarUrl: String) -> Unit) {
    this.avatarUpdateCallback = avatarUpdateCallback
  }

  fun onDescriptionUpdate(onDescriptionUpdateCallback: (id: Long, description: String) -> Unit) {
    this.onDescriptionUpdateCallback = onDescriptionUpdateCallback
  }

  fun onLiveStatusUpdate(updateStreamerLiveStatus: suspend (id: Long, isLive: Boolean) -> Unit) {
    this.updateLiveStatusCallback = updateStreamerLiveStatus
  }

  fun onLastLiveTimeUpdate(updateStreamerLastLiveTime: suspend (id: Long, lastLiveTime: Long) -> Unit) {
    this.updateStreamerLastLiveTime = updateStreamerLastLiveTime
  }

  fun onCheckLastLiveTime(checkShouldUpdateStreamerLastLiveTime: suspend (id: Long, lastLiveTime: Long, now: Long) -> Boolean) {
    this.checkShouldUpdateStreamerLastLiveTime = checkShouldUpdateStreamerLastLiveTime
  }

  fun onSavedToDb(onSavedToDb: suspend (stream: StreamData) -> Unit) {
    this.onSavedToDb = onSavedToDb
  }

}