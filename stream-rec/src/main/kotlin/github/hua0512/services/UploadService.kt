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
import github.hua0512.data.UploadDataId
import github.hua0512.data.UploadResultId
import github.hua0512.data.upload.UploadAction
import github.hua0512.data.upload.UploadConfig
import github.hua0512.data.upload.UploadConfig.NoopConfig
import github.hua0512.data.upload.UploadConfig.RcloneConfig
import github.hua0512.data.upload.UploadData
import github.hua0512.data.upload.UploadResult
import github.hua0512.plugins.base.Upload
import github.hua0512.plugins.upload.NoopUploader
import github.hua0512.plugins.upload.RcloneUploader
import github.hua0512.plugins.upload.UploadInvalidArgumentsException
import github.hua0512.repo.uploads.UploadRepo
import github.hua0512.utils.withIOContext
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.datetime.Clock
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import kotlin.time.DurationUnit
import kotlin.time.toDuration


/**
 * Service class for managing upload actions.
 *
 * This class provides methods for running the upload service, uploading an upload action, and providing an uploader based on the upload configuration.
 * It also contains a flow of upload actions and a map to keep track of failed uploads and the number of times they failed (in-memory).
 *
 * @property app The application instance.
 * @property uploadRepo The repository for managing upload actions.
 *
 * @author hua0512
 * @date : 2024/2/19 15:30
 */
class UploadService(val app: App, private val uploadRepo: UploadRepo) {

  /**
   * A map to keep track of failed uploads and the number of times they failed.
   * The key is the ID of the upload data and the value is the number of times the upload has failed.
   */
  private val shouldNotRetryMap = mutableMapOf<UploadDataId, Int>()

  companion object {
    /**
     * Logger for this class.
     */
    @JvmStatic
    private val logger: Logger = LoggerFactory.getLogger(UploadService::class.java)
  }

  /**
   * A semaphore to limit the number of concurrent uploads.
   */
  private val uploadSemaphore: Semaphore by lazy { Semaphore(app.config.maxConcurrentUploads) }

  /**
   * Runs the upload service.
   * This function launches two coroutines. One for processing upload actions and another for handling failed uploads.
   */
  suspend fun run() = coroutineScope {
    // launch a coroutine scanning for failed uploads
    launch {
      uploadRepo.streamFailedUploadResults().onEach { failedResults ->
        // for each failed upload get its upload data
        failedResults.forEach { failedResult ->
          val uploadData = uploadRepo.getUploadData(UploadDataId(failedResult.uploadDataId))
          if (uploadData == null) {
            // if the upload data is null, delete the failedResults
            logger.info("Deleting failed upload failedResults: $failedResult")
            uploadRepo.deleteUploadResult(UploadResultId(failedResult.id))
          } else {
            // if the upload data is not null, re-upload the file
            // get upload action id
            val uploadDataId = UploadDataId(uploadData.id)
            val count = shouldNotRetryMap.getOrDefault(uploadDataId, 0)
            // ignore if the upload data has failed more than 3 times
            if (count >= 3) {
              logger.error("Failed to upload file: ${uploadData.filePath} more than 3 times, skipping")
              return@onEach
            }

            val uploadAction = uploadRepo.getUploadActionIdByUploadDataId(uploadDataId) ?: run {
              logger.error("Upload action not found for upload data: $uploadData")
              return@onEach  // skip this failed result
            }
            // calculate the next retry time, current retry count * 2 factor * 10m
            logger.info("Retrying failed upload: ${uploadData.filePath} in $count minutes")
            val delayInMinutes = (count + 1) * 2 * 10
            delay(duration = delayInMinutes.toDuration(DurationUnit.MINUTES))
            // emit the same upload action with the failed upload data
            upload(uploadAction.copy(files = setOf(uploadData)))
            // increment the count
            shouldNotRetryMap[uploadDataId] = count + 1
          }
        }
      }.catch {
        logger.error("Error in failed upload results flow", it)
      }.collect()
    }
  }

  /**
   * Uploads an upload action.
   * This function saves the upload action to the repository and emits it to the upload action flow.
   *
   * @param uploadAction The upload action to upload.
   */
  suspend fun upload(uploadAction: UploadAction) {
    val saved = withIOContext {
      uploadRepo.saveAction(uploadAction)
    }
    uploadAction.id = saved.value
    val uploader = provideUploader(uploadAction.uploadConfig)
    if (uploader is NoopUploader) {
      return
    }
    val deferredResults = CompletableDeferred<List<UploadResult>>()
    withIOContext {
      val results = parallelUpload(uploadAction.files, uploader)
      results.forEach {
        // save the result
        uploadRepo.saveResult(it)
        if (it.isSuccess) {
          // get the upload data and update the status
          val uploadDataId = it.uploadDataId
          uploadRepo.changeUploadDataStatus(uploadDataId, true)
        }
      }
      logger.debug("Upload results: {}", results)
      deferredResults.complete(results)
    }

    deferredResults.await()
  }


  /**
   * Uploads a collection of files in parallel.
   * This function uses the provided plugin to upload each file in the collection.
   * The number of concurrent uploads is determined by the application's configuration.
   *
   * @param files The collection of files to upload.
   * @param plugin The plugin to use for uploading the files.
   * @return A list of upload results.
   */
  @OptIn(ExperimentalCoroutinesApi::class)
  private suspend fun parallelUpload(files: Collection<UploadData>, plugin: Upload): List<UploadResult> = coroutineScope {
    files.asFlow()
      .flatMapMerge(concurrency = app.config.maxConcurrentUploads) { file ->
        uploadFileFlow(file, plugin)
      }
      .toList()
  }

  /**
   * Uploads a file and emits the result.
   * This function attempts to upload the file up to three times. If the upload fails three times, it emits a failed upload result.
   *
   * @param file The file to upload.
   * @param plugin The plugin to use for uploading the file.
   * @return A flow of upload results.
   */
  private fun uploadFileFlow(file: UploadData, plugin: Upload): Flow<UploadResult> = flow {
    var attempts = 0
    // Retry logic
    while (attempts < 3) {
      try {
        // upload the file
        uploadSemaphore.withPermit {
          val status = plugin.upload(file).apply {
            uploadData = file
          }
          file.status = status.isSuccess
          logger.info("Successfully uploaded file: ${file.filePath}")
          emit(status)
          attempts = 3
        }
      } catch (e: UploadInvalidArgumentsException) {
        logger.error("Invalid arguments for upload: ${file.filePath}")
        emit(
          UploadResult(
            time = Clock.System.now().epochSeconds, isSuccess = false, message = "Invalid arguments for upload: ${e.message}",
          ).also { it.uploadData = file }
        )
      } catch (e: Exception) {
        // other exceptions,
        // most likely network issues or an UploadFailedException
        attempts++
        logger.error("Failed to upload file: ${file.filePath}, attempt: $attempts", e)
        if (attempts >= 3) {
          emit(
            UploadResult(
              time = Clock.System.now().epochSeconds,
              isSuccess = false,
              message = "Failed to upload file : $e",
            ).also { it.uploadData = file })
        }
      }
    }
  }

  /**
   * Provides an uploader based on the upload configuration.
   * This function returns an uploader based on the type of the upload configuration.
   *
   * @param config The upload configuration.
   * @return An uploader.
   * @throws IllegalArgumentException If the upload configuration is invalid.
   */
  private fun provideUploader(config: UploadConfig) = when (config) {
    is RcloneConfig -> RcloneUploader(app, config)
    is NoopConfig -> NoopUploader(app)
    else -> throw IllegalArgumentException("Invalid config: $config")
  }
}