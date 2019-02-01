package com.example.model.repository

import com.example.model.persistent.db.DBConfiguration
import com.example.model.persistent.table.TableRecords
import com.example.utils.AppConfig
import com.typesafe.scalalogging.LazyLogging
import slick.basic.DatabasePublisher

import scala.concurrent.Future
import scala.util.Try

trait CommonRDB[R <: TableRecords] extends LazyLogging with DBConfiguration {

  /** common ddl action */
  def getCreateDDL: Iterator[String]
  def getDropDDL: Iterator[String]
  def truncateDDL: Iterator[String]
  def create(): Future[Try[Unit]]
  def drop(): Future[Try[Unit]]
  def truncate(): Future[Try[Unit]]

  /** common io action */
  def insert(record: R): Future[Try[Int]]
  def insertAll(records: Vector[R]): Future[Try[Option[Int]]]
  def insertOrUpdateRecord(record: R): Future[Try[Int]]
  def selectAllRecord: Future[Try[Seq[R]]]
  def streamRecords(fetchRowSize: Int = AppConfig.defaultFetchSize): DatabasePublisher[R]
}
