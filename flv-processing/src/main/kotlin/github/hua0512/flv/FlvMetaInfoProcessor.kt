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

import github.hua0512.flv.data.FlvTag
import github.hua0512.flv.data.amf.Amf0Keyframes
import github.hua0512.flv.data.amf.Amf0Value
import github.hua0512.flv.data.amf.Amf0Value.*
import github.hua0512.flv.data.amf.AmfValue
import github.hua0512.flv.data.other.FlvKeyframe
import github.hua0512.flv.data.other.FlvMetadataInfo
import github.hua0512.flv.data.tag.FlvTagHeader
import github.hua0512.flv.operators.sortKeys
import github.hua0512.flv.operators.toAmfMap
import github.hua0512.flv.utils.ScriptData
import github.hua0512.utils.logger
import io.exoquery.pprint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.DataOutputStream
import java.io.File
import java.io.InputStream
import java.io.RandomAccessFile
import kotlin.time.measureTime

private const val TAG = "FlvMetaInfoProcessor"
private val logger = logger(TAG)

internal val naturalMetadataKeyOrder by lazy {
  listOf(
    "hasAudio",
    "hasVideo",
    "hasMetadata",
    "hasKeyframes",
    "canSeekToEnd",
    "duration",
    "datasize",
    "filesize",
    "audiosize",
    "audiocodecid",
    "audiodatarate",
    "audiosamplerate",
    "audiosamplesize",
    "stereo",
    "videosize",
    "frameRate",
    "videocodecid",
    "videocodecid",
    "videodatarate",
    "width",
    "height",
    "lasttimestamp",
    "lastkeyframelocation",
    "lastkeyframetimestamp",
    "metadatacreator",
    "metadatadate",
    "keyframes",
  )
}

private const val HEADER_BYTES = 13L


/**
 * FLV metadata info processor
 * @author hua0512
 * @date : 2024/9/9 12:37
 */
object FlvMetaInfoProcessor {

  private fun InputStream.parseScriptTag(): FlvTag {
    val header: FlvTagHeader = parseTagHeader()
    var scriptTagData: Pair<ScriptData, Long> = parseScriptTagData(header.dataSize.toInt())
    return FlvTag(1, header, scriptTagData.first, scriptTagData.second)
  }

  /**
   * Inject metadata into FLV file
   * @param path FLV file path
   * @param metaInfo metadata info
   * @param deleteOriginal delete original file after processing
   * @return true if success, false otherwise
   */
  suspend fun process(path: String, metaInfo: FlvMetadataInfo, deleteOriginal: Boolean = false): Boolean = withContext(Dispatchers.IO) {
    val file = File(path)
    if (!file.exists() || !file.canRead()) return@withContext false

    val time = measureTime {
      file.inputStream().buffered().use { ins ->
        var needRewrite = false
        var headerBytes: ByteArray? = null

        var injected: FlvTag? = null

        RandomAccessFile(file, "rw").use { raf ->
          // skip header and previous tag size
          headerBytes = ins.readNBytes(HEADER_BYTES.toInt())
          raf.seek(HEADER_BYTES)

          val tag = ins.parseScriptTag()
          injected = tag.inject(metaInfo)
          if (tag.header.dataSize == injected.header.dataSize) {
            // skip tag header
            raf.skipBytes(FlvParser.TAG_HEADER_SIZE)
            val bytes = (injected.data as ScriptData).toByteArray()
            assert(bytes.size == injected.header.dataSize)
            // write script tag data
            raf.write(bytes)
            return@use
          } else {
            // size changed, need to rewrite the whole flv file
            needRewrite = true
          }
        }

        // cases when:
        // 1. injectMetadata operator is not applied
        // 2. processing a file that is generated by other tools
        if (needRewrite) {
          val tempFile = File(file.parent, "${file.nameWithoutExtension}_inject.${file.extension}")
          DataOutputStream(tempFile.outputStream().buffered()).use { dos ->
            // write the header
            headerBytes!!.let { dos.write(it) }
            // write the injected tag
            injected!!.header.write(dos)
            (injected.data as ScriptData).write(dos)
            // write injected tag size
            dos.writeInt(injected.header.dataSize)
            // skip old tag size pointer
            ins.skipNBytes(4)
            // write the rest of the file
            ins.copyTo(dos)
          }

          if (deleteOriginal) {
            file.delete()
          }
        }

        logger.info("Corrected metadata : $path")
      }
    }

    logger.debug("Processed file: $path, time spent: $time")
    true
  }


  /**
   * Inject metadata into script tag
   * @param metaInfo metadata info
   * @return injected tag
   */
  private suspend fun FlvTag.inject(metaInfo: FlvMetadataInfo): FlvTag = withContext(Dispatchers.Default) {
    val data = this@inject.data as ScriptData
    val oldSize = data.bodySize

    if (data.valuesCount < 2) {
      logger.warn("Missing onMetaData, skip injection...")
      return@withContext this@inject
    }

    val properties = data[1].getProperties()

    val mutableMap = properties.toMutableMap()
//    logger.info("current metadata: {}", pprint(data, defaultHeight = 100))

    var isGeneratedByOurself = false
    var amf0Keyframes: Amf0Keyframes? = null

    with(mutableMap) {
      metaInfo.toAmfMap().forEach { (k, v) ->
        this[k] = v
      }
      if (metaInfo.hasKeyframes) {
        // Update keyframes to the metadata object
        amf0Keyframes = this["keyframes"] as Amf0Keyframes
        // check if keyframes contains keyframes
        // this means that the keyframes and spacers are already injected, by ourselves
        if (amf0Keyframes.properties.containsKey(Amf0Keyframes.KEY_SPACER)) {
          isGeneratedByOurself = true
        }
      }
    }

    // order the metadata properties by natural order, place unknown properties at the end
    val orderedMap = if (!isGeneratedByOurself) {
      mutableMap.sortKeys()
    } else {
      mutableMap
    }

    val secondData = if (data[1] is EcmaArray) {
      (data[1] as EcmaArray).copy(properties = orderedMap)
    } else {
      Object(orderedMap)
    }

    // new data
    var newData = data.copy(values = listOf(data[0], secondData))
    // new body size
    val newDataSize = newData.bodySize

    if (newDataSize == oldSize) {
      logger.debug("metadata size not changed")
    } else {
      // the diff of metadata size
      val delta = newDataSize - oldSize
      logger.info("metadata size changed from $oldSize to $newDataSize")
      // recalculate keyframes
      val properties = newData[1].getProperties().toMutableMap()
      properties["filesize"] = Number((properties["filesize"] as Amf0Value.Number).value + delta)
      // update lastkeyframelocation
      if (properties.containsKey("lastkeyframelocation")) {
        val lastKeyframeLocation = (properties["lastkeyframelocation"] as Amf0Value.Number).value + delta
        properties["lastkeyframelocation"] = Number(lastKeyframeLocation)
      }
      // update keyframes filepositions with delta
      val keyframes = (properties["keyframes"] as Amf0Keyframes).run {
        val oldKeyframes = this.getKeyframes()
        val newKeyframes = oldKeyframes.map { FlvKeyframe(it.timestamp, it.filePosition + delta) }
        copy(keyframes = ArrayList(newKeyframes))
      }
      properties["keyframes"] = keyframes

      val secondData = if (data[1] is EcmaArray) {
        (data[1] as EcmaArray).copy(properties = properties)
      } else {
        Object(properties)
      }

      newData = newData.copy(
        values = listOf(newData[0], secondData)
      )
    }
    logger.info("Injected metadata: {}", pprint(newData, defaultHeight = 50))
    return@withContext this@inject.copy(data = newData, header = header.copy(dataSize = newDataSize))
  }


  private fun AmfValue.getProperties(): Map<String, Amf0Value> {
    return when (this) {
      is EcmaArray -> this.properties
      is Object -> this.properties
      else -> {
        logger.warn("Unexpected AMF value type in metadata : ${this::class.qualifiedName}")
        throw IllegalArgumentException("Unexpected AMF value type in metadata : ${this::class.qualifiedName}")
      }
    }
  }

}