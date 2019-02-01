package com.example.model.repository

import com.example.model.persistent.table.YNANews
import com.example.model.persistent.table.YNANews.YNANewsRecord
import com.example.utils.AppConfig
import com.typesafe.scalalogging.LazyLogging
import slick.basic.{DatabaseConfig, DatabasePublisher}
import slick.jdbc.{JdbcProfile, ResultSetConcurrency, ResultSetType}

import scala.concurrent.Future
import scala.util.Try

class YNANewsDao(val dbConfig: DatabaseConfig[JdbcProfile]) extends LazyLogging with YNANews with CommonRDB[YNANewsRecord] {
  import dbConfig.profile.api._

  type TableRecord = YNANewsRecord
  private val dbConn = this.getDBConnection

  // common DDL action
  override def getCreateDDL: Iterator[String] = tableQuery.schema.createStatements
  override def getDropDDL: Iterator[String] = tableQuery.schema.dropStatements
  override def truncateDDL: Iterator[String] = tableQuery.schema.truncateStatements
  override def create(): Future[Try[Unit]] = {
    logger.info(s"create table. table name: $tableName")
    dbConn.run(tableQuery.schema.create.asTry)
  }
  override def drop(): Future[Try[Unit]] = {
    logger.info(s"drop table. table name: $tableName")
    dbConn.run(tableQuery.schema.drop.asTry)
  }
  override def truncate(): Future[Try[Unit]] = {
    logger.info(s"truncate table. table name: $tableName")
    dbConn.run(tableQuery.schema.truncate.asTry)
  }

  // common IO action
  override def insert(record: TableRecord): Future[Try[Int]] = {
    logger.info(s"insert or update record. table name: $tableName")
    logger.debug(s"record info: $record")
    dbConn.run(tableQuery.forceInsert(record).asTry)
  }
  override def insertOrUpdateRecord(record: TableRecord): Future[Try[Int]] = {
    logger.info(s"insert or update record. table name: $tableName")
    logger.debug(s"record info: $record")
    dbConn.run(tableQuery.insertOrUpdate(record).asTry)
  }
  override def insertAll(records: Vector[TableRecord]): Future[Try[Option[Int]]] = {
    logger.info(s"insert multiple records. record count: ${records.length}, table name: $tableName")
    logger.debug(s"records info:\n\t|${records.mkString("\n\t|")}")
    dbConn.run(tableQuery.forceInsertAll(records).transactionally.asTry)
  }
  override def selectAllRecord: Future[Try[Seq[TableRecord]]] = {
    logger.info(s"select all record. table name: $tableName")
    dbConn.run(tableQuery.result.asTry)
  }
  override def streamRecords(fetchRowSize: Int = AppConfig.defaultFetchSize): DatabasePublisher[TableRecord] = {
    logger.info(s"stream records. table name: $tableName")
    dbConn.stream(tableQuery
      .result
      .withStatementParameters(
        rsType = ResultSetType.ForwardOnly,
        rsConcurrency = ResultSetConcurrency.ReadOnly,
        fetchSize = fetchRowSize)
      .transactionally)
  }
}

object YNANewsDao extends LazyLogging {
  private lazy val instance = this.createInstance

  def getInstance: YNANewsDao = this.instance

  private def createInstance: YNANewsDao = {
    val instance = new YNANewsDao(AppConfig.mainDbConfig)
    logger.info(s"create ${instance.getClass.getSimpleName} object.")
    instance
  }
}