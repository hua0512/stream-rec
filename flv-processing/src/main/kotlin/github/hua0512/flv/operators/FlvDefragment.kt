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

package github.hua0512.flv.operators

import github.hua0512.flv.data.FlvData
import github.hua0512.flv.utils.isHeader
import github.hua0512.flv.utils.logger
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow


private const val TAG = "FlvDefragmentRule"
private val logger = logger(TAG)

private const val MIN_TAGS_NUM = 10

/**
 * @author hua0512
 * @date : 2024/9/8 14:24
 */
internal fun Flow<FlvData>.discardFragmented(): Flow<FlvData> = flow {


  var isGathering = false

  var tags: ArrayList<FlvData>? = null // do not preallocate anything

  fun reset() {
    isGathering = false
    tags?.clear()
    tags = null
  }


  suspend fun Flow<FlvData>.pushTags() {
    tags!!.forEach { emit(it) }
    reset()
  }


  collect { value ->

    if (value.isHeader()) {
      if (tags != null) {
        logger.warn("Discarded {} items, total size: {}", tags?.size, tags?.sumOf { it.size })
        reset()
      }
      isGathering = true
      logger.debug("Start gathering...")
    }

    if (isGathering) {
      tags = tags ?: ArrayList<FlvData>().also { tags = it }
      tags!!.add(value)
      if (tags!!.size >= MIN_TAGS_NUM) {
        logger.debug("Gathered {} items, total size: {}", tags?.size, tags?.sumOf { it.size })
        pushTags()
        reset()
        logger.debug("Not a fragmented sequence, stopped gathering...")
      }
      return@collect
    }
    emit(value)
  }

  reset()
}