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

import github.hua0512.utils.executeProcess
import github.hua0512.utils.process.InputSource
import github.hua0512.utils.process.Redirect
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import java.io.File
import kotlin.test.Test

class ProcessExecutorTest {


  @Test
  fun testInputString() = runTest {
    val files = arrayOf("inputString.mp4", "aaaaaaaa.xml")

    val inputStringJoined = files.joinToString("\n")
    val inputString = "inputString.mp4\naaaaaaaa.xml"

    assertEquals(inputString, inputStringJoined)
  }

//  @Test
//  fun `test execute`() = runTest {
//
//    println(System.getProperty("user.dir"))
//
//    val file = File(System.getProperty("user.dir")).parentFile
//    val files = arrayOf("inputString.mp4", "aaaaaaaa.xml")
//    val stringBuffer = StringBuffer()
//    files.forEach {
//      stringBuffer.append(it)
//      stringBuffer.append("\n")
//    }
//    val output = StringBuilder()
//    val exitCode = executeProcess(
//      "bash",
//      "ls",
//      stdin = InputSource.fromString(stringBuffer.toString()),
//      directory = file,
//      stdout = Redirect.CAPTURE,
//      consumer = output::append
//    )
//
//    // Assert
//    assertEquals(0, exitCode)
//  }
}