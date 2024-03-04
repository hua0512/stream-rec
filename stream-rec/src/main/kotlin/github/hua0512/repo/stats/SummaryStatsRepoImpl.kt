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

package github.hua0512.repo.stats

import github.hua0512.dao.stats.StatsDao
import github.hua0512.data.stats.Stats
import github.hua0512.data.stats.SummaryStats
import kotlinx.datetime.DateTimePeriod
import kotlinx.datetime.Instant

/**
 * @author hua0512
 * @date : 2024/3/4 10:44
 */
class SummaryStatsRepoImpl(val statsDao: StatsDao) : SummaryStatsRepo {
  override fun getSummaryStats(): SummaryStats {
    TODO("Not yet implemented")
  }

  override fun getSummaryStatsFromTo(from: Long, to: Long): SummaryStats {
    val stats = statsDao.getStatsFromTo(from, to)
    val fromDate = Instant.fromEpochMilliseconds(from)
    val toDate = Instant.fromEpochMilliseconds(to)
    return SummaryStats(0, 0, 0, 0)
  }

  override fun getStatsFromTo(from: Long, to: Long): List<Stats> {
    TODO("Not yet implemented")
  }

  override fun getStatsFrom(from: Long): List<Stats> {
    TODO("Not yet implemented")
  }

}