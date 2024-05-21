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

package github.hua0512.services

import github.hua0512.app.App
import github.hua0512.data.event.StreamerEvent.StreamerException
import github.hua0512.data.event.StreamerEvent.StreamerRecordStop
import github.hua0512.data.stream.Streamer
import github.hua0512.plugins.download.StreamerDownloadManager
import github.hua0512.plugins.download.platformConfig
import github.hua0512.plugins.event.EventCenter
import github.hua0512.repo.stream.StreamDataRepo
import github.hua0512.repo.stream.StreamerRepo
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.sync.Semaphore
import kotlinx.datetime.Clock
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import kotlin.time.DurationUnit
import kotlin.time.toDuration

class DownloadService(
  private val app: App,
  private val actionService: ActionService,
  private val repo: StreamerRepo,
  private val streamDataRepository: StreamDataRepo,
) {

  private var isInitialized = false

  companion object {
    @JvmStatic
    private val logger: Logger = LoggerFactory.getLogger(DownloadService::class.java)
  }

  // semaphore to limit the number of concurrent downloads
  private lateinit var downloadSemaphore: Semaphore

  // map of streamer to job
  private val taskJobs = mutableMapOf<Streamer, Job?>()

  // map of streamer to download manager
  private val managers = mutableMapOf<Streamer, StreamerDownloadManager>()

  /**
   * Starts the download service.
   */
  suspend fun run() = coroutineScope {
    downloadSemaphore = Semaphore(app.config.maxConcurrentDownloads)
    // listen to streamer changes
    listenToStreamerChanges()
    // fetch all active non-template streamers, group by platform
    // start a download job for each platform with a delay
    repo.getStreamersActive().groupBy {
      it.platform
    }.map { entry ->
      val fetchDelay = (entry.key.platformConfig(app.config).fetchDelay ?: 0).toDuration(DurationUnit.SECONDS)
      launch {
        val streamers = entry.value
        streamers.forEach {
          startDownloadJob(it)
          delay(fetchDelay)
        }
      }
    }.joinAll()
    isInitialized = true
  }

  private fun CoroutineScope.listenToStreamerChanges() {
    launch {
      repo.stream().distinctUntilChanged().buffer().collect { streamerList ->
        if (!isInitialized) {
          logger.info("Service not initialized yet, ignoring streamers change event")
          return@collect
        }
        logger.info("Streamers changed, reloading...")

        // compare the new streamers with the old ones, first by url, then by entity equals
        // if a stream is not in the new list, cancel the job
        // if a stream is in the new list but not in the old one, start a new job
        // if a stream is in both lists, do nothing

        if (streamerList.isEmpty()) {
          logger.info("No streamers to download")
          // the new list is empty, cancel all jobs
          taskJobs.values.forEach { it?.cancel() }
          return@collect
        }

        val oldStreamers = taskJobs.keys.map { it }

        val newStreamers = streamerList.filterNot { it.isTemplate }
        // cancel the jobs of the streamers that are not in the new list
        oldStreamers.filter { old ->
          newStreamers.none { new -> new.url == old.url }
        }.forEach { streamer ->
          cancelJob(streamer, "delete")
        }

        // diff the new streamers with the old ones
        // if a stream has the same url but different entity, cancel the old job and start a new one
        // if a stream is not in the old list, start a new job
        // if a stream is in both lists, do nothing
        newStreamers.forEach { new ->
          val old = oldStreamers.find { it.url == new.url } ?: run {
            if (validateActivation(new)) return@forEach
            startDownloadJob(new)
            return@forEach
          }
          // if the entity is different, cancel the old job and start a new one
          // find the change reason
          if (old != new) {
            val reason = when {
              old.isActivated != new.isActivated -> "activation"
              old.url != new.url -> "url"
              old.downloadConfig != new.downloadConfig -> "download config"
              old.platform != new.platform -> "platform"
              old.name != new.name -> "name"
              old.isTemplate != new.isTemplate -> "as template"
              old.templateId != new.templateId -> "template id"
              old.templateStreamer?.downloadConfig != new.templateStreamer?.downloadConfig -> "template stream download config"
              // other changes are ignored
              else -> return@forEach
            }
            logger.debug("Detected entity change for {}, {}", new, old)
            cancelJob(old, "entity changed : $reason")
            if (validateActivation(new)) return@forEach
            startDownloadJob(new)
          }
        }
      }
    }
  }


  private suspend fun downloadStreamer(streamer: Streamer) = supervisorScope {
    val plugin = try {
      PlatformDownloaderFactory.createDownloader(app, streamer.platform, streamer.url)
    } catch (e: Exception) {
      logger.error("${streamer.name} platform not supported by the downloader : ${app.config.engine}")
      EventCenter.sendEvent(StreamerException(streamer.name, streamer.url, streamer.platform, Clock.System.now(), e))
      return@supervisorScope
    }

    val streamerDownload = StreamerDownloadManager(
      app,
      streamer,
      plugin,
      downloadSemaphore
    ).apply {
      onLiveStatusUpdate { id, isLive ->
        repo.update(streamer.copy(isLive = isLive))
      }

      onLastLiveTimeUpdate { id, lastLiveTime ->
        repo.update(streamer.copy(lastLiveTime = lastLiveTime))
      }

      onSavedToDb {
        streamDataRepository.save(it)
      }

      onDescriptionUpdate { id, description ->
        launch {
          repo.update(streamer.copy(streamTitle = description))
        }
      }

      onAvatarUpdate { id, avatarUrl ->
        launch {
          repo.update(streamer.copy(avatar = avatarUrl))
        }
      }

      onRunningActions { data, actions ->
        actionService.runActions(data, actions)
      }

      init()
    }
    managers[streamer] = streamerDownload
    try {
      streamerDownload.start()
      // wait for cancellation
      awaitCancellation()
    } finally {
      logger.info("${streamer.name}, ${streamer.url} job finished")
    }
  }


  /**
   * Starts a new download job for a given [Streamer].
   *
   * @param streamer The [Streamer] object for which to start the download job.
   */
  private fun CoroutineScope.startDownloadJob(streamer: Streamer) {
    val newJob = async { downloadStreamer(streamer) }
    taskJobs[streamer] = newJob
    logger.info("${streamer.name}, ${streamer.url} job started")
  }

  /**
   * Validates the activation status of a given [Streamer].
   *
   * @param new The [Streamer] object to validate.
   * @return true if the [Streamer] is not activated, false otherwise.
   */
  private fun validateActivation(new: Streamer): Boolean {
    if (!new.isActivated) {
      logger.info("${new.name}, ${new.url} is not activated")
      return true
    }
    return false
  }

  /**
   * Cancels the job of a given [Streamer].
   *
   * @param streamer The [Streamer] object for which to cancel the job.
   * @param reason The reason for cancelling the job.
   * @return The [Streamer] object that was cancelled.
   */
  private suspend fun cancelJob(streamer: Streamer, reason: String = ""): Streamer {
    // stop the download
    managers[streamer]?.cancel()
//    // await the job to finish
    taskJobs[streamer]?.join()
    // cancel the job
    taskJobs[streamer]?.cancel(reason)?.also {
      logger.info("${streamer.name}, ${streamer.url} job cancelled : $reason")
      EventCenter.sendEvent(
        StreamerRecordStop(
          streamer.name,
          streamer.url,
          streamer.platform,
          Clock.System.now(),
          reason = CancellationException(reason)
        )
      )
    }
    taskJobs.remove(streamer)
    managers.remove(streamer)
    return streamer
  }
}