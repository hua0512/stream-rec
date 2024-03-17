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

package github.hua0512.data.event

import github.hua0512.data.stream.StreamData
import github.hua0512.data.stream.StreamingPlatform
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable


/**
 * StreamerEvent is a sealed class that represents events related to streamers.
 */
@Serializable
sealed class StreamerEvent : Event {

  /**
   * The name of the streamer.
   */
  abstract val streamer: String

  /**
   * The URL of the streamer.
   */
  abstract val streamerUrl: String

  /**
   * The platform of the streamer.
   */
  abstract val streamerPlatform: StreamingPlatform

  @Serializable
  data class StreamerOnline(
    override val streamer: String,
    override val streamerUrl: String,
    override val streamerPlatform: StreamingPlatform,
    val streamTitle: String,
    val time: Instant,
  ) : StreamerEvent()

  /**
   * StreamerRecordStop represents a streamer record stop event.
   * @property streamer The name of the streamer.
   * @property streamerUrl The URL of the streamer.
   * @property streamerPlatform The platform of the streamer.
   * @property time The time when the record stopped.
   */
  data class StreamerRecordStop(
    override val streamer: String,
    override val streamerUrl: String,
    override val streamerPlatform: StreamingPlatform,
    val time: Instant,
    val reason: Exception?,
  ) : StreamerEvent()

  @Serializable
  data class StreamerOffline(
    override val streamer: String,
    override val streamerUrl: String,
    override val streamerPlatform: StreamingPlatform,
    val time: Instant,
    val data: List<StreamData>? = emptyList(),
  ) : StreamerEvent()

  /**
   * StreamerException represents a streamer exception event.
   * @property streamer The name of the streamer.
   * @property streamerUrl The URL of the streamer.
   * @property streamerPlatform The platform of the streamer.
   * @property time The time when the exception occurred.
   * @property exception The exception that occurred.
   */
  data class StreamerException(
    override val streamer: String,
    override val streamerUrl: String,
    override val streamerPlatform: StreamingPlatform,
    val time: Instant,
    val exception: Exception,
  ) : StreamerEvent()

}
