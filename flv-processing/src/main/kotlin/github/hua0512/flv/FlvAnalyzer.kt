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

package github.hua0512.flv

import github.hua0512.flv.data.FlvHeader
import github.hua0512.flv.data.FlvTag
import github.hua0512.flv.data.other.FlvKeyframe
import github.hua0512.flv.data.other.FlvMetadataInfo
import github.hua0512.flv.data.video.VideoResolution
import github.hua0512.flv.utils.AudioData
import github.hua0512.flv.utils.ScriptData
import github.hua0512.flv.utils.VideoData
import github.hua0512.flv.utils.isAudioTag
import github.hua0512.flv.utils.isKeyFrame
import github.hua0512.flv.utils.isScriptTag
import github.hua0512.flv.utils.isVideoSequenceHeader
import github.hua0512.flv.utils.isVideoTag
import github.hua0512.flv.utils.logger
import io.exoquery.pprint
import kotlinx.coroutines.flow.MutableStateFlow


typealias FlvAnalyzerSizedUpdater = (Long, Float, Float) -> Unit

/**
 * FLV analyzer
 * @author hua0512
 * @date : 2024/9/8 21:18
 */
class FlvAnalyzer(private val sizedUpdater: FlvAnalyzerSizedUpdater = { _, _, _ -> }) {


  companion object {

    private const val TAG = "FlvAnalyzer"
    private val logger = logger(TAG)

  }

  private var numTags = 0
  private var numAudioTags = 0
  private var numVideoTags = 0


  private var tagsSize: Long = 0
  private var dataSize: Long = 0
  private var audioTagsSize: Long = 0
  private var audioDataSize: Long = 0
  private var videoTagsSize: Long = 0
  private var videoDataSize: Long = 0
  private var lastTimestamp: Long = 0
  private var lastAudioTimestamp: Long = 0
  private var lastVideoTimestamp: Long = 0

  private var resolution: VideoResolution? = null
  private var keyframesMap = mutableMapOf<Long, Long>()
  private var lastKeyframeTimestamp: Long = 0
  private var hasAudio = false
  private var audioInfo: AudioData? = null


  private var hasVideo = false
  private var videoInfo: VideoData? = null

  private var headerSize = 0

  private var startTimestamp: Long = 0


  val durationFlow = MutableStateFlow<Float>(0f)


  val fileSize: Long
    get() {
      // header + tags + numTags * pointer
      return headerSize + FlvParser.POINTER_SIZE + tagsSize + numTags * FlvParser.POINTER_SIZE
    }

  val frameRate: Float
    get() {
      return try {
        numVideoTags / lastVideoTimestamp.toFloat() * 1000
      } catch (e: Exception) {
        0.0f
      }
    }

  val audioDataRate: Float
    get() {
      return try {
        audioDataSize * 8f / lastAudioTimestamp
      } catch (e: Exception) {
        0.0f
      }
    }

  val videoDataRate: Float
    get() {
      return try {
        videoDataSize * 8f / lastVideoTimestamp
      } catch (e: Exception) {
        0.0f
      }
    }

  val downloadBitrate: Float
    get() {
      val currentTime = System.currentTimeMillis()
      val elapsedTime = currentTime - startTimestamp
      return if (elapsedTime > 0) {
        (fileSize * 8) / (elapsedTime / 1000) / 1024f // kbps
      } else {
        0.0f
      }
    }

  internal fun makeMetaInfo(): FlvMetadataInfo {
    assert(resolution != null)
    val keyframes = keyframesMap.map { (timestamp, position) -> FlvKeyframe(timestamp, position) }.sortedBy { it.timestamp }
    return FlvMetadataInfo(
      hasAudio = hasAudio,
      hasVideo = hasVideo,
      hasScript = true,
      hasKeyframes = keyframes.isNotEmpty(),
      canSeekToEnd = lastVideoTimestamp == lastKeyframeTimestamp,
      duration = lastTimestamp / 1000,
      fileSize = fileSize,
      audioSize = audioTagsSize,
      audioDataSize = audioDataSize,
      audioCodecId = audioInfo?.format,
      audioSampleRate = audioInfo?.rate,
      audioSampleSize = audioInfo?.size,
      audioSoundType = audioInfo?.type,
      videoSize = videoTagsSize,
      videoDataSize = videoDataSize,
      frameRate = frameRate,
      videoCodecId = videoInfo?.codecId,
      videoDataRate = videoDataRate,
      width = resolution!!.width,
      height = resolution!!.height,
      lastTimestamp = lastTimestamp,
      lastKeyframeTimestamp = lastKeyframeTimestamp,
      lastKeyframeFilePosition = keyframesMap[lastKeyframeTimestamp] ?: 0,
      keyframes = keyframes
    )

  }


  fun reset() {
    numTags = 0
    numAudioTags = 0
    numVideoTags = 0
    tagsSize = 0
    dataSize = 0
    audioTagsSize = 0
    audioDataSize = 0
    videoTagsSize = 0
    videoDataSize = 0
    lastTimestamp = 0
    lastAudioTimestamp = 0
    lastVideoTimestamp = 0
    resolution = null
    keyframesMap.clear()
    lastKeyframeTimestamp = 0
    hasAudio = false
    audioInfo = null
    hasVideo = false
    videoInfo = null
    headerSize = 0
    durationFlow.value = 0f
    startTimestamp = 0
    sizedUpdater(0, 0f, 0f)
  }


  fun analyzeHeader(header: FlvHeader) {
    this.headerSize = header.headerSize.toInt()
    this.startTimestamp = System.currentTimeMillis()
    sizedUpdater(fileSize, durationFlow.value, 0f)
  }

  fun analyzeTag(tag: FlvTag) {
    when {
      tag.isAudioTag() -> analyzeAudioTag(tag)
      tag.isVideoTag() -> analyzeVideoTag(tag)
      tag.isScriptTag() -> analyzeScriptTag(tag)
      else -> throw IllegalArgumentException("Unknown tag type: ${tag.header.tagType}")
    }

    numTags++
    tagsSize += tag.size
    dataSize += tag.header.dataSize.toLong()
    lastTimestamp = tag.header.timestamp
    durationFlow.value = lastTimestamp / 1000f
    sizedUpdater(fileSize, durationFlow.value, downloadBitrate)
  }

  private fun analyzeScriptTag(tag: FlvTag) {
    // do nothing
    tag.data as ScriptData
    return
  }


  private fun analyzeAudioTag(tag: FlvTag) {
    tag.data as AudioData
    if (!hasAudio) {
      hasAudio = true
      audioInfo = tag.data.copy(binaryData = byteArrayOf())
      logger.debug("Audio info: {}", pprint(audioInfo))
    }

    numAudioTags++
    audioTagsSize += tag.size
    audioDataSize += tag.header.dataSize.toLong()
    lastAudioTimestamp = tag.header.timestamp
  }

  private fun analyzeVideoTag(tag: FlvTag) {

    tag.data as VideoData

    if (tag.isKeyFrame()) {
      keyframesMap[tag.header.timestamp] = fileSize
      if (tag.isVideoSequenceHeader() && resolution == null) {
        resolution = tag.data.resolution
        logger.debug("Video resolution: {}", pprint(resolution))
      }
      lastKeyframeTimestamp = tag.header.timestamp
    }

    if (videoInfo == null) {
      hasVideo = true
      videoInfo = tag.data.copy(binaryData = byteArrayOf())
      logger.debug("Video info: {}", pprint(videoInfo))
    }

    numVideoTags++
    videoTagsSize += tag.size
    videoDataSize += tag.header.dataSize.toLong()
    lastVideoTimestamp = tag.header.timestamp
  }


}