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

package github.hua0512.data.upload.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

/**
 * Upload result entity
 * @author hua0512
 * @date : 2024/5/18 14:12
 */
@Entity(
  tableName = "UploadResult",
  foreignKeys = [
    ForeignKey(
      entity = UploadDataEntity::class,
      parentColumns = ["id"],
      childColumns = ["uploadDataId"],
      onDelete = ForeignKey.CASCADE
    )
  ]
)
data class UploadResultEntity(
  @PrimaryKey(autoGenerate = true)
  val id: Long,
  @ColumnInfo(name = "startTime")
  val startTime: Long = 0,
  @ColumnInfo(name = "endTime")
  val endTime: Long = 0,
  @ColumnInfo(name = "isSuccess")
  val isSuccess: Boolean,
  @ColumnInfo(name = "message")
  val message: String?,
  @ColumnInfo(name = "uploadDataId", index = true)
  val uploadDataId: Long,
)
