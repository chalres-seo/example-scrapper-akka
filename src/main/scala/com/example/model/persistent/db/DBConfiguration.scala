package com.example.model.persistent.db

import java.sql.Timestamp
import java.util.concurrent.Executors

import com.example.service.news.YNAParser.YNASection
import com.example.service.news.YNAParser.YNASection.YNASection
import com.example.utils.AppConfig
import com.example.utils.AppConfig.{conf, logger}
import com.typesafe.scalalogging.LazyLogging
import org.joda.time.DateTime
import slick.basic.DatabaseConfig
import slick.jdbc.{JdbcBackend, JdbcProfile}

import scala.concurrent.{ExecutionContext, ExecutionContextExecutorService}

trait DBConfiguration extends LazyLogging {
  implicit val dbActionThreadPool: ExecutionContextExecutorService = DBConfiguration.dbActionThreadPool

  // abstract member for get database config on runtime
  val dbConfig: DatabaseConfig[JdbcProfile]
  val profile: JdbcProfile = dbConfig.profile

  import profile.api._

  // implicit datetime <> timestamp type convert
  implicit val yodaDateTimeRepType: profile.BaseColumnType[DateTime] = profile.MappedColumnType.base[ DateTime, Timestamp ] (
    dt => new Timestamp( dt.getMillis ),
    ts => new DateTime( ts.getTime )
  )
  // implicit (enum)ynaSection <> string type convert
  implicit val ynaSectionType: profile.BaseColumnType[YNASection] = profile.MappedColumnType.base[ YNASection, String ] (
    ynaSection => ynaSection.toString,
    str => YNASection.withName(str)
  )

  def getDBConnection: JdbcBackend#DatabaseDef = {
    logger.info("get database connection.")
    dbConfig.db
  }

  def closeDBConnection(): Unit = {
    logger.info("close database connection.")
    dbConfig.db.close()
  }
//  def dbClose(): Unit = dbConfig.db.close()
//  val getResultToResultSet = GetResult(_.rs)
//  def exec[T](action: DBIO[T]): T = Await.result(dbConfig.db.run(action), AppConfig.defaultFutureTimeout)
}

object DBConfiguration extends LazyLogging {
  private val dbActionThreadPool: ExecutionContextExecutorService =
    ExecutionContext.fromExecutorService(Executors.newWorkStealingPool(AppConfig.dbIOThreadPoolSize))
  sys.addShutdownHook(dbActionThreadPool.shutdown())


}