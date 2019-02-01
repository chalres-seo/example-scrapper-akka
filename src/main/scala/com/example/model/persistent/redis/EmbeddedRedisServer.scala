package com.example.model.persistent.redis

import java.io._
import java.util.concurrent.{ExecutorService, Executors}
import java.util.stream

import com.example.model.persistent.redis.EmbeddedRedisServer.LogPatterns
import com.example.utils.AppConfig
import com.typesafe.scalalogging.LazyLogging

import scala.annotation.tailrec
import scala.concurrent.{Await, ExecutionContext, ExecutionContextExecutor, Future}
import scala.collection.JavaConversions._
import scala.collection.JavaConverters._
import scala.collection.immutable

class EmbeddedRedisServer(redisConf: Map[String, String]) extends LazyLogging {
  private implicit val redisLogPrinterExecutionContext: ExecutionContextExecutor = EmbeddedRedisServer.createredisLogPrinterExecutionContext

  @volatile private var active: Boolean = false
  @volatile private var ready: Boolean = false

  @volatile private var redisProcess: Option[Process] = None
  @volatile private var redisPid: Option[Long] = None

  private var redisLogReader: Option[BufferedReader] = None
  private var redisLogPrinter: Option[Future[Unit]] = None

  private val redisProcessBuilder = EmbeddedRedisServer.createRedisProcessBuilder(redisConf)
  private val redisPort = redisConf.getOrElse("port", "-1").toInt

  private val redisLogger = RedisLog

  def start(): Boolean = {
    (active, ready) match {
      case (true, true) =>
        redisLogger.info(s"already running server.")
        false
      case (true, false) =>
        redisLogger.info(s"already active server.")
        false
      case (false, true) =>
        redisLogger.info(s"active server but not running stats.")
        false
      case (false, false) =>
        startServer
    }
  }

  private def startServer: Boolean = {
    active = true
    redisProcess = Option(redisProcessBuilder.start())
    redisPid = redisProcess.map(_.pid())
    redisLogReader = redisProcess.map(process => new BufferedReader(new InputStreamReader(process.getInputStream)))

    redisLogger.info(s"server process start. active: $active, ready: $ready")

    ready = if (redisProcess.isDefined || redisPid.isDefined || redisLogReader.isDefined) {
      this.waitReady(redisLogReader.get)
    } else false

    if (ready) {
      redisPid.foreach(EmbeddedRedisServer.addServer(_, this.redisPort))
      redisLogPrinter = redisLogReader.map(this.startRedisLogPrinter)
      ready = true
      redisLogger.info(s"server is ready. active: $active, ready: $ready")
    } else {
      active = false
      redisProcess = None
      redisPid = None
      redisLogReader = None
      redisLogPrinter = None
    }
    ready
  }

  private def waitReady(redisLogReader: BufferedReader): Boolean = {
    redisLogger.info(s"wait for the server to be ready state., active: $active, ready: $ready")

    @tailrec
    def loop(logReader: BufferedReader): Boolean = {
      val line = logReader.readLine()
      if (line.contains('#') || line.contains('*')) {
        redisLogger.info(line)
        if (line.contains(EmbeddedRedisServer.LogPatterns.ready.toString)) {
          redisLogger.info(s"succeed start server and ready.")
          return true
        }
        if (line.contains(EmbeddedRedisServer.LogPatterns.conflictedPort.toString)) {
          redisLogger.info(s"failed start server, conflicted port.")
          false
        } else loop(logReader)
      } else {
        redisLogger.info(s"failed start server.")
        false
      }
    }

    try {
      loop(redisLogReader)
    } catch {
      case e: java.io.UncheckedIOException =>
        redisLogger.error(s"stream already closed. ${e.getMessage},")
        false
      case e: Exception =>
        redisLogger.error(s"${e.getMessage}", e)
        false
    }
  }

  def getPid: Option[Long] = this.redisPid
  def getPort: Int = this.redisPort

  def isActive: Boolean = {
    if (active) {
      redisLogger.info(s"server is active.")
    } else {
      redisLogger.info(s"server is not active.")
    }
    active
  }

  def isReady: Boolean = {
    if (active) {
      redisLogger.info(s"server is ready.")
    } else {
      redisLogger.info(s"server is not ready.")
    }
    active
  }

  private def startRedisLogPrinter(redisLogReader: BufferedReader): Future[Unit] = {
    Future {
      try {
        redisLogReader.lines().iterator().foreach { log =>
          redisLogger.info(log)
        }
      } catch {
        case e: java.io.UncheckedIOException =>
          redisLogger.error(s"stream already closed. ${e.getMessage}")
        case e: Exception =>
          redisLogger.error(s"${e.getMessage}", e)
      } finally {
        if (redisLogReader != null)
          redisLogReader.close()
      }
    }
  }

  def stop(): Unit = {
    for {
      pid <- redisPid
      process <- redisProcess
      logPrinter <- redisLogPrinter
      logReader <- redisLogReader
    } {
      try {
        redisLogger.info(s"destroy process.")
        process.destroy()
        process.waitFor()

        if (!logPrinter.isCompleted) {
          redisLogger.info(s"wait for stop redisLogger. redisLogPrinter: $logPrinter")
          Await.result(logPrinter, AppConfig.defaultFutureTimeout)
        }

        if (logReader != null) {
          logReader.close()
        }
        
        ready = false
        active = false
        redisPid = None
        redisProcess = None
        redisLogPrinter = None
        redisLogReader = None

        EmbeddedRedisServer.removeServer(pid, this.redisPort)
      } catch {
        case e: Exception => redisLogger.error(s"thrown exception when stopping phase. msg: ${e.getMessage}", e)
      }
    }
  }

  private object RedisLog {
    def info(msg: String): Unit = {
      logger.info(s"[pid:$redisPid/port:$redisPort] " + msg)
    }
    def error(msg: String): Unit = {
      logger.error(s"[pid:$redisPid/port:$redisPort] " + msg)
    }
    def error(msg: String, e: Exception): Unit = {
      logger.error(s"[pid:$redisPid/port:$redisPort] " + msg, e)
    }
  }
}

object EmbeddedRedisServer extends LazyLogging {
  private val defaultRedisBin = new File(s"redis_bin/redis-server")
  private val defaultRedisPort = 16379
  private val defaultRedisLogLevel = "debug"
  private val defaultRedisMaxMemoryMB = 32
  private val defaultRedisConf = this.createRedisConf

  private val defaultRedisLoggerPrinterPoolSize = 1
  private var serverPortList = Set.empty[(Long, Int)]

  final object LogPatterns extends Enumeration {
    type RedisServerLogPattern = Value
    val ready: LogPatterns.Value = Value("Ready to accept connections")
    val conflictedPort: LogPatterns.Value = Value("Could not create server TCP listening socket")
  }

  def createInstance(): EmbeddedRedisServer = {
    new EmbeddedRedisServer(defaultRedisConf)
  }

  def createInstance(port: Int): EmbeddedRedisServer = {
    new EmbeddedRedisServer(this.createRedisConf(port))
  }

  def getServerList: Vector[(Long, Int)] = serverPortList.toVector

  private def addServer(redisPid: Long, port: Int): Unit = {
    logger.info(s"server added. pid: $redisPid, port: $port")
    serverPortList += ((redisPid, port))
  }

  private def removeServer(redisPid: Long, port: Int): Unit = {
    logger.info(s"server removed. pid: $redisPid, port: $port")
    serverPortList -= ((redisPid, port))
  }

  private def createRedisConf(port: Int = defaultRedisPort,
                              logLevel: String = defaultRedisLogLevel,
                              maxMemoryMB: Int = defaultRedisMaxMemoryMB): Map[String, String] = {
    val redisConf = Map(
      "port" -> port.toString,
      "loglevel" -> logLevel,
      "maxmemory" -> s"${maxMemoryMB}mb",
      "save" -> "",
      "bind" -> "localhost"
    )
    redisConf
  }
  private def createRedisConf: Map[String, String] = {
    this.createRedisConf(defaultRedisPort, defaultRedisLogLevel, defaultRedisMaxMemoryMB)
  }

  def redisConfToCommand(redisConf: Map[String, String]): immutable.Iterable[String] = {
    redisConf.map(a => "--" + a._1 + " " + a._2)
  }

  private def createRedisProcessBuilder(redisConf: Map[String, String]): ProcessBuilder = {
    logger.info(s"create redis process builder:\n\t${redisConf.mkString("\n\t")}")
    new ProcessBuilder(Vector(defaultRedisBin.getAbsolutePath) ++ this.redisConfToCommand(redisConf))
      .redirectErrorStream(true)
      .directory(defaultRedisBin.getAbsoluteFile.getParentFile)
  }

  private def createredisLogPrinterExecutionContext(poolSize: Int): ExecutionContextExecutor = {
    val redisLoggerPrintExecutionContext = ExecutionContext.fromExecutor(Executors.newFixedThreadPool(poolSize))
    logger.info(s"create log printer execution context. name ${redisLoggerPrintExecutionContext.getClass.getSimpleName}, size: $poolSize")
    redisLoggerPrintExecutionContext
  }
  private def createredisLogPrinterExecutionContext: ExecutionContextExecutor =
    this.createredisLogPrinterExecutionContext(defaultRedisLoggerPrinterPoolSize)
}
