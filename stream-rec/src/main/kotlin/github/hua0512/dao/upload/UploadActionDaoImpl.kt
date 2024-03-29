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

import github.hua0512.StreamRecDatabase
import github.hua0512.dao.BaseDaoImpl
import github.hua0512.data.UploadActionId
import github.hua0512.utils.UploadActionEntity

/**
 * @author hua0512
 * @date : 2024/2/19 10:58
 */
class UploadActionDaoImpl(override val database: StreamRecDatabase) : BaseDaoImpl, UploadActionDao {
  override fun getUploadActionById(uploadId: UploadActionId): UploadActionEntity? {
    return queries.getUploadActionById(uploadId.value).executeAsOneOrNull()
  }

  override fun getUploadActionByUploadId(uploadId: UploadActionId): List<UploadActionEntity> {
    return queries.getUploadActionById(uploadId.value).executeAsList()
  }

  override fun saveUploadAction(time: Long, configString: String): UploadActionId {
    queries.insertUploadAction(time, configString)
    // TODO: Queries are not returning the id of the inserted row
    // In multi-threaded environment, select last_insert_rowid() is not safe
    // so we need to use another way to get the id of the inserted row
    // time should be unique, but we check it with configString to be sure.
    // Meanwhile, we should use a random number to fasten the query instead of performing a select with the configString which slows down the query.
    // But, we are not developing a high performance application, so we can use this way.
    // SUBJECT TO CHANGE IN THE FUTURE
    return queries.getUploadActionIdByTimeAndConfig(time, configString).executeAsOne().run {
      UploadActionId(this)
    }
  }


  override fun deleteUploadAction(uploadActionId: UploadActionId) {
    return queries.deleteUploadAction(uploadActionId.value)
  }


}