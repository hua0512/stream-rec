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

package github.hua0512.flv.data

/**
 * Flv header flag wrapper
 * @author hua0512
 * @date : 2024/6/8 13:12
 */
@JvmInline
value class FlvHeaderFlags(val value: Int) {

  init {
    require(value in 0..0x07) { "Invalid flags value: $value" }

    // assert that 5 bits are 0
    require((value and 0xF8) == 0) { "Invalid flags value: $value" }

    // assert that 7 bit is 0 (reserved)
    require((value and 0x02) == 0) { "Flv header reserved flag must be zero" }
  }

  /**
   * Whether the flv has audio
   * @return Boolean true if the flv has audio
   */
  val hasAudio: Boolean
    get() = (value and 0x04) != 0

  /**
   * Whether the flv has video
   * @return Boolean true if the flv has video
   */
  val hasVideo: Boolean
    get() = (value and 0x01) != 0

  /**
   * Whether the flv has audio and video
   * @return Boolean true if the flv has audio and video
   */
  val hasAudioAndVideo: Boolean
    get() = (value and 0x05) == 0x05
}