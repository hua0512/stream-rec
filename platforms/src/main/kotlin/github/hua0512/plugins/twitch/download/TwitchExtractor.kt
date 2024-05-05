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

package github.hua0512.plugins.twitch.download

import github.hua0512.data.media.MediaInfo
import github.hua0512.data.media.VideoFormat
import github.hua0512.data.stream.StreamInfo
import github.hua0512.plugins.base.Extractor
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import kotlinx.serialization.json.*

/**
 * Extractor for Twitch. Used to extract media information from Twitch.
 * @param http HttpClient
 * @param json Json
 * @author hua0512
 * @date : 2024/4/27 14:29
 */
class TwitchExtractor(http: HttpClient, json: Json, override val url: String) : Extractor(http, json) {

  companion object {
    internal const val BASE_URL = "https://www.twitch.tv"
    internal const val URL_REGEX = """https?://(?:www\.)?twitch\.tv/([^/]+)"""
    internal val bandwidthPattern = Regex("BANDWIDTH=(\\d+)")
    internal val resolutionPattern = Regex("RESOLUTION=(\\d+x\\d+)")
    internal val videoPattern = Regex("VIDEO=\"([^\"]+)\"")
    internal val frameRatePattern = Regex("FRAME-RATE=(\\d+)")
  }

  override val regexPattern: Regex = URL_REGEX.toRegex()

  internal lateinit var id: String

  private lateinit var bodyJson: JsonObject

  internal lateinit var authToken: String

  override suspend fun isLive(): Boolean {
    id = regexPattern.find(url)?.groupValues?.get(1) ?: throw IllegalArgumentException("Failed to extract id from Twitch url, url: $url")
    // queries
    val queries = arrayOf(
      buildPersistedQueryRequest(
        "ChannelShell",
        "c3ea5a669ec074a58df5c11ce3c27093fa38534c94286dc14b68a25d5adcbf55",
        buildJsonObject {
          put("login", id)
          put("lcpVideosEnabled", false)
        }
      ),
      buildPersistedQueryRequest(
        "StreamMetadata",
        "059c4653b788f5bdb2f5a2d2a24b0ddc3831a15079001a3d927556a96fb0517f",
        buildJsonObject {
          put("channelLogin", id)
          put("previewImageURL", "")
        }
      ),
    )
    val response = twitchPostQPL(http, json, queries.contentToString(), authToken)
    val data =
      response.jsonArray[1].jsonObject["data"]?.jsonObject ?: throw IllegalArgumentException("($id) failed to get stream data, response: $response")
    val user = data["user"] as? JsonObject ?: throw IllegalArgumentException("($id) failed to get stream user metadata, response: $response")

    val stream = user["stream"] ?: return false
    // if stream is not live, stream is a JsonNull
    if (stream !is JsonObject) return false
    // get stream type
    val type = stream["type"] ?: return false
    if (type.jsonPrimitive.content != "live") {
      return false
    }
    bodyJson = user
    return true
  }


  override suspend fun extract(): MediaInfo {
    val isLive = isLive()
    var mediaInfo = MediaInfo(url, "", id, "", "")

    if (!isLive) {
      return mediaInfo
    }

    val lastBroadcast = bodyJson["lastBroadcast"]?.jsonObject ?: throw IllegalArgumentException("Invalid response, response: $bodyJson")
    val title = lastBroadcast["title"]?.jsonPrimitive?.content ?: ""
    val artistProfileUrl = bodyJson["profileImageURL"]?.jsonPrimitive?.content ?: ""

    val accessTokenResponse = twitchPostQPL(
      http,
      json,
      buildPersistedQueryRequest("PlaybackAccessToken", "0828119ded1c13477966434e15800ff57ddacf13ba1911c129dc2200705b0712", buildJsonObject {
        put("isLive", true)
        put("login", id)
        put("isVod", false)
        put("vodID", "")
        put("playerType", "site")
        put("isClip", false)
        put("clipID", "")
      }),
      authToken
    )

    val accessToken = accessTokenResponse.jsonObject["data"]?.jsonObject?.get("streamPlaybackAccessToken")?.jsonObject
      ?: throw IllegalStateException("($id) failed to get stream playback access token, response: $accessTokenResponse")

    val valueToken = accessToken["value"]?.jsonPrimitive?.content ?: ""
    val signature = accessToken["signature"]?.jsonPrimitive?.content ?: ""

    val resp = getResponse("https://usher.ttvnw.net/api/channel/hls/$id.m3u8") {
      parameter("player", "twitchweb")
      parameter("p", (System.currentTimeMillis() / 1000).toString())
      parameter("allow_source", "true")
      parameter("allow_audio_only", "true")
      parameter("allow_spectre", "true")
      parameter("fast_bread", "true")
      parameter("sig", signature)
      parameter("token", valueToken)
    }

    val body = resp.bodyAsText()
    val streams = mutableListOf<StreamInfo>()
    val lines = body.lines()
    lines.indices.filter { lines[it].startsWith("#EXT-X-STREAM-INF") }.forEach { index ->
      val line = lines[index]
      val bandwidth = bandwidthPattern.find(line)?.groupValues?.get(1)?.toLong() ?: 0
      val resolution = resolutionPattern.find(line)?.groupValues?.get(1)?.let { mapOf("resolution" to it) } ?: emptyMap()
      val video = videoPattern.find(line)?.groupValues?.get(1)!!
      val frameRate = frameRatePattern.find(line)?.groupValues?.get(1)?.toDouble() ?: 0.0
      val url = lines[index + 1]
      streams.add(StreamInfo(url, VideoFormat.hls, video, bandwidth, frameRate = frameRate, extras = resolution))
    }
    mediaInfo = mediaInfo.copy(artistImageUrl = artistProfileUrl, title = title, live = true, streams = streams)
    return mediaInfo
  }
}