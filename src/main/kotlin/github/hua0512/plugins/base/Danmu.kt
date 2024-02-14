package github.hua0512.plugins.base

import github.hua0512.app.App
import github.hua0512.data.DanmuData
import github.hua0512.data.Streamer
import io.ktor.client.plugins.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import org.redundent.kotlin.xml.xml
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Danmu (Bullet screen comments) downloader base class
 *
 * Uses websocket to connect to danmu server and fetch danmu.
 * @param app app config
 * @author hua0512
 * @date : 2024/2/9 13:31
 */
abstract class Danmu(val app: App) {

  companion object {
    @JvmStatic
    protected val logger: Logger = LoggerFactory.getLogger(Danmu::class.java)
  }

  var enableWrite: Boolean = false

  /**
   * Whether the danmu is initialized
   */
  protected val isInitialized = AtomicBoolean(false)

  /**
   * Danmu websocket url
   */
  abstract val websocketUrl: String

  /**
   * Heart beat delay
   */
  abstract val heartBeatDelay: Long

  /**
   * Heart beat pack
   */
  abstract val heartBeatPack: ByteArray

  /**
   * Danmu file name
   */
  var filePath: String = "${System.currentTimeMillis()}.xml"
    set(value) {
      field = value
      danmuFile = Path.of(filePath).toFile()
    }


  /**
   * Danmu file
   */
  lateinit var danmuFile: File

  /**
   * Represents the start time of the danmu download.
   */
  var startTime: Long = System.currentTimeMillis()

  /**
   * A shared flow to write danmu data to file
   */
  private val writeToFileFlow = MutableSharedFlow<DanmuData?>(replay = 1)

  /**
   * Initialize danmu
   *
   * @param streamer streamer
   * @param startTime start time
   * @return true if initialized successfully
   */
  abstract suspend fun init(streamer: Streamer, startTime: Long): Boolean

  /**
   * Send one hello
   *
   * @return bytearray hello pack
   */
  abstract fun oneHello(): ByteArray

  /**
   * Fetch danmu from server using websocket
   *
   */
  suspend fun fetchDanmu() {
    if (!isInitialized.get()) {
      logger.error("Danmu is not initialized")
      return
    }
    if (websocketUrl.isEmpty()) return

    // fetch danmu
    withContext(Dispatchers.IO) {
      app.client.webSocket(websocketUrl) {
        // launch a coroutine to write danmu to file
        launchIOTask()
        // make an initial hello
        send(oneHello())
        // launch a coroutine to send heart beat
        launchHeartBeatJob()
        while (true) {
          // received socket frame
          when (val frame = incoming.receive()) {
            is Frame.Binary -> {
              val data = frame.readBytes()
              // decode danmu
              try {
                decodeDanmu(data)?.also {
                  // emit danmu to write to file
                  val danmuTime = (System.currentTimeMillis() - startTime).run {
                    val time = if (this < 0) 0L else this
                    // format to 3 decimal places
                    String.format("%.3f", time / 1000.0).toDouble()
                  }

                  writeToFileFlow.tryEmit(it.copy(time = danmuTime))
                }
              } catch (e: Exception) {
                logger.error("Error decoding danmu: $e")
              } ?: continue
            }

            is Frame.Close -> {
              logger.info("Danmu connection closed")
              break
            }
            // ignore other frames
            else -> {}
          }
        }
      }
    }
  }

  /**
   * Launches a coroutine to perform an IO task to write danmu to file.
   *
   * @receiver The [CoroutineScope] on which the coroutine will be launched.
   */
  private fun CoroutineScope.launchIOTask() {
    launch(Dispatchers.IO) {
      // buffer 5 danmus
      writeToFileFlow
        .onStart {
          // check if danmuFile is initialized
          logger.info("Start writing danmu to : ${danmuFile.absolutePath}")
          // create file if not exists
          if (!Files.exists(danmuFile.toPath())) {
            danmuFile.createNewFile()
            danmuFile.writeText("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<root>\n")
          } else {
            logger.info("${danmuFile.absolutePath} file exists, appending to it.")
          }
        }
        .buffer(5)
        .onEach {
          // if it is null, finish writing to file
          if (it == null) {
            logger.info("Finish writing danmu to : ${danmuFile.absolutePath}")
            danmuFile.appendText("</root>")
            return@onEach
          }
          // write danmu to file
          writeToDanmu(it)
        }
        .flowOn(Dispatchers.IO)
        .catch {
          logger.error("Error writing to file: $it")
        }
        .collect()
    }
  }

  /**
   * Writes the given [DanmuData] object to the danmu file.
   *
   * @param data The [DanmuData] object to be written.
   */
  private fun writeToDanmu(data: DanmuData) {
    if (!enableWrite) return
    val xml = xml("d") {
      val time = data.time
      val color = if (data.color == -1) "16777215" else data.color
      attribute("p", "$time,1,25,$color,0,0,0,0")
      text(data.content)
    }
//    logger.debug("Writing danmu to file: ${xml.toString(prettyFormat = false)}")
    danmuFile.appendText("  ${xml.toString(false)}\n")
  }

  /**
   * Launches a coroutine to send heartbeats on the given WebSocket session.
   */
  private fun DefaultClientWebSocketSession.launchHeartBeatJob() {
    launch {
      while (true) {
        // send heart beat with delay
        send(heartBeatPack)
        delay(heartBeatDelay)
      }
    }
  }


  /**
   * Decodes the given byte array into a DanmuData object.
   *
   * @param data The byte array to be decoded.
   * @return The decoded DanmuData object, or null if decoding fails.
   */
  abstract fun decodeDanmu(data: ByteArray): DanmuData?

  /**
   * Finish writting danmu to file
   */
  suspend fun finish() {
    logger.info("$filePath danmu file finished")
    isInitialized.set(false)
    // send null to write channel to finish writing
    withContext(Dispatchers.IO) {
      writeToFileFlow.emit(null)
      writeToFileFlow.resetReplayCache()
    }
  }

}