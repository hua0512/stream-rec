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

package github.hua0512.data.config

import github.hua0512.data.StreamData
import github.hua0512.data.VideoFormat
import github.hua0512.data.dto.DouyinConfigDTO
import github.hua0512.data.platform.DouyinQuality
import kotlinx.serialization.Serializable

/**
 * Douyin config data class
 * @author hua0512
 * @date : 2024/2/11 13:29
 */
@Serializable
data class DouyinConfig(
  override val cookies: String? = null,
  override val quality: DouyinQuality = DouyinQuality.origin,
) : DouyinConfigDTO {
  override val danmu: Boolean
    get() = TODO("Not yet implemented")
  override val maxBitRate: Int?
    get() = TODO("Not yet implemented")
  override val outputFolder: String?
    get() = TODO("Not yet implemented")
  override val outputFileName: String?
    get() = TODO("Not yet implemented")
  override val outputFileExtension: VideoFormat?
    get() = TODO("Not yet implemented")
  override val onPartedDownload: List<Action>?
    get() = TODO("Not yet implemented")
  override val onStreamingFinished: List<Action>?
    get() = TODO("Not yet implemented")
}