package com.example.model.persistent.table

import com.example.model.persistent.db.DBConfiguration
import com.example.model.persistent.table.YNANews.YNANewsRecord
import com.example.service.news.YNAParser.YNASection.YNASection
import org.joda.time.DateTime
import slick.lifted.ProvenShape

trait YNANews extends DBConfiguration {
  import profile.api._

  val tableName: String = "TB_YNA_NEWS"
  val tableQuery: TableQuery[YNANews] = TableQuery[YNANews]

  final protected class YNANews(tag: Tag) extends Table[YNANewsRecord](tag, tableName) {
    def newsUrl: Rep[String] = column[String]("news_url", O.Length(64))
    def ynaSection: Rep[YNASection] = column[YNASection]("section", O.Length(16))
    def title: Rep[String] = column[String]("title", O.Length(64))
    def contents: Rep[String] = column[String]("contents")
    def publishDateTime: Rep[DateTime] = column[DateTime]("publish_datetime")
    def updateTime: Rep[DateTime] = column[DateTime]("update_datetime")
    def pk = primaryKey(tableName + "_PK", newsUrl)
    def sectionIdx = index(tableName + "_SECTION_IDX", ynaSection)
    override def * : ProvenShape[YNANewsRecord] = (newsUrl, ynaSection, title, contents, publishDateTime, updateTime).mapTo[YNANewsRecord]
  }
}

object YNANews {
  final case class YNANewsRecord(newsUrl: String,
                                 ynaSection: YNASection,
                                 title: String,
                                 contents: String,
                                 publishDateTime: DateTime,
                                 updateTime: DateTime) extends TableRecords
}