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

package github.hua0512.dao.upload

import github.hua0512.data.StreamDataId
import github.hua0512.data.StreamerId
import github.hua0512.data.UploadDataId
import github.hua0512.utils.UploadDataEntity

/**
 * @author hua0512
 * @date : 2024/2/18 16:06
 */
interface UploadDataDao {

  fun getAllUploadData(): List<UploadDataEntity>
  fun getAllUploadDataPaginated(
    page: Int,
    pageSize: Int,
    filter: String,
    status: Collection<Long>?,
    streamerIds: Collection<StreamerId>?,
    allStreamers: Boolean?,
    sortColumn: String,
    sortOrder: String,
  ): List<UploadDataEntity>

  fun countAllUploadData(status: Collection<Long>?, filter: String, streamerIds: Collection<StreamerId>?): Long

  fun getUploadDatasByStatus(status: Long): List<UploadDataEntity>
  fun getUploadDataById(id: UploadDataId): UploadDataEntity?
  fun insertUploadData(filePath: String, streamDataId: StreamDataId, status: Long): Long
  fun updateUploadDataStatus(id: UploadDataId, status: Long)
  fun deleteUploadData(id: UploadDataId)
}