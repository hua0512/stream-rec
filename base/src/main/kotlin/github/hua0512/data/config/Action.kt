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

import github.hua0512.data.dto.RcloneConfigDTO
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Sealed class for actions that can be performed after a stream is downloaded
 * @author hua0512
 * @date : 2024/2/13 13:26
 */
@Serializable
sealed class Action {

  abstract val enabled: Boolean

  @Serializable
  @SerialName("command")
  data class CommandAction(
    var program: String = "",
    var args: List<String> = emptyList(),
    override val enabled: Boolean = true,
  ) : Action()


  /**
   * Rclone action
   * @author hua0512
   * @date : 2024/2/13 13:26
   */
  @Serializable
  @SerialName("rclone")
  data class RcloneAction(
    override var rcloneOperation: String = "",
    override var remotePath: String = "",
    override var args: List<String> = emptyList(),
    override val enabled: Boolean = true,
  ) : Action(), RcloneConfigDTO

  @Serializable
  @SerialName("remove")
  data class RemoveAction(override val enabled: Boolean = true) : Action()

  @Serializable
  @SerialName("move")
  data class MoveAction(val destination: String, override val enabled: Boolean = true) : Action()

  /**
   * Action to copy the file to a destination
   */
  @Serializable
  @SerialName("copy")
  data class CopyAction(val destination: String, override val enabled: Boolean = true) : Action()
}