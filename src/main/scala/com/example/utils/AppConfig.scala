package com.example.utils

import java.util.concurrent.TimeUnit

import com.typesafe.config.{Config, ConfigFactory}
import com.typesafe.scalalogging.LazyLogging
import slick.basic.DatabaseConfig
import slick.jdbc.JdbcProfile

import scala.concurrent.duration.Duration

object AppConfig extends LazyLogging {
  // read application.conf
  private val conf: Config = ConfigFactory.load().resolve()

  // application config
  lazy val applicationName: String = conf.getString("application.name")
  lazy val defaultTimeOutSec: Duration = Duration.apply(conf.getLong("application.default.timeoutSec"), TimeUnit.SECONDS)
  lazy val defaultTimeWaitSec: Int = conf.getInt("application.default.timeWaitSec")
  lazy val defaultTimeWaitMillis: Long = defaultTimeWaitSec * 1000L
  lazy val defaultMaxRetryCount: Int = conf.getInt("application.default.maxTry")

  // future config
  lazy val defaultFutureTimeout: Duration = Duration.apply(conf.getLong("application.default.futureTimeWaitSec"), TimeUnit.SECONDS)
  lazy val defaultDBIOFutureTimeout: Duration = defaultFutureTimeout

  // database config, from resource application config
  lazy val mainDbConfig: DatabaseConfig[JdbcProfile] = DatabaseConfig.forConfig[JdbcProfile]("database.main")
  lazy val defaultFetchSize: Int = 10000
  lazy val dbIOThreadPoolSize: Int = conf.getInt("database.default.db.numThreads")

  // selenium config
  lazy val seleniumWebDriverName: String = "webdriver.chrome.driver"
  lazy val seleniumWebDriverPath: String = "webdriver/chromedriver"
  lazy val seleniumWebDriverMaxWaitTimeSec = 60
  lazy val seleniumWebDriverHeadLess: Boolean = false

  // redis config
  lazy val redisServers: String = conf.getString("redis.servers")
  lazy val redisPort: Int = conf.getInt("redis.port")
  lazy val redisConnections: Int = conf.getInt("redis.connections")
  lazy val redisMaxConnections: Int = conf.getInt("redis.maxConnections")

  // slack web hook url
  lazy val slackWebHookURL: String = conf.getString("slack.webHookURL")
}
