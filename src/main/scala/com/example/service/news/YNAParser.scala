package com.example.service.news

import com.example.model.persistent.table.YNANews.YNANewsRecord
import com.example.service.news.YNAParser.YNASection.YNASection
import com.typesafe.scalalogging.LazyLogging
import org.joda.time.DateTime
import org.jsoup.nodes.Element

import scala.collection.JavaConversions._

object YNAParser extends LazyLogging {
  final object YNASection extends Enumeration {
    type YNASection = Value
    val culture: YNAParser.YNASection.Value = Value("culture")
    val economy: YNAParser.YNASection.Value = Value("economy")
    val entertainment: YNAParser.YNASection.Value = Value("entertainment")
    val festival: YNAParser.YNASection.Value = Value("festival")
    val international: YNAParser.YNASection.Value = Value("international")
    val it: YNAParser.YNASection.Value = Value("it")
    val politics: YNAParser.YNASection.Value = Value("politics")
    val society: YNAParser.YNASection.Value = Value("society")
    val sports: YNAParser.YNASection.Value = Value("sports")
    val stock: YNAParser.YNASection.Value = Value("stock")
    val travel: YNAParser.YNASection.Value = Value("travel")
  }

  def parseSectionNewsList(ynaSection: YNASection, bodyElement: Element): Vector[YNANewsPreViewRecord] = {
    val parser = selectSectionNewsListParser(ynaSection)
    parser(bodyElement)
  }

  def parseSectionNews(ynaNewsPreViewRecord: YNANewsPreViewRecord, bodyElement: Element): YNANewsRecord = {
    val parser = defaultSectionNewsParser
    parser(ynaNewsPreViewRecord, bodyElement)
  }

  private def selectSectionNewsListParser: YNASection => Element => Vector[YNANewsPreViewRecord] = {
    case ynaSection @ YNASection.travel => travelAndFestivalSectionNewsListParser.curried(ynaSection)
    case ynaSection @ YNASection.festival => travelAndFestivalSectionNewsListParser.curried(ynaSection)
    case ynaSection => sectionNewsListParser.curried(ynaSection)
  }

  private def sectionNewsListParser: (YNASection, Element) => Vector[YNANewsPreViewRecord]
  = (ynaSection: YNASection, bodyElement: Element) => {
    bodyElement
      .select("div.con")
      .withFilter(_.hasText)
      .map { content =>
        val lead = content.getElementsByClass("lead")
        val url = "https:" + lead.select("a").attr("href")
        val title = content.getElementsByClass("news-tl").text()
        val preView = lead.select("a").text()
        //val createDateTime = DateTime.parse(this.dateTimeConvert(lead.select("span").text()))

        YNANewsPreViewRecord(url, ynaSection, title, preView)
      }.toVector
  }

  private def travelAndFestivalSectionNewsListParser: (YNASection, Element) => Vector[YNANewsPreViewRecord]
  = (ynaSection: YNASection, bodyElement: Element) => {
    bodyElement
      .select("div.con")
      .withFilter(_.hasText)
      .map { content =>
        val lead = content.getElementsByClass("lead")
        val url = "https:" + lead.select("a").attr("href")
        val title = content.getElementsByClass("news-tl").text()
        val preView = lead.select("a").text()
        //val createDateTime = DateTime.parse(this.dateTimeConvert(lead.select("span").text()))

        YNANewsPreViewRecord(url, ynaSection, title, preView)
      }.toVector
  }

  private def defaultSectionNewsParser: (YNANewsPreViewRecord, Element) => YNANewsRecord
  = (ynaNewsPreViewRecord: YNANewsPreViewRecord, bodyElement: Element) => {
    new YNANewsRecord(
      ynaNewsPreViewRecord.newsUrl,
      ynaNewsPreViewRecord.ynaSection,
      ynaNewsPreViewRecord.title,
      this.getSectionNewsContents(bodyElement),
      this.getSectionNewsPublishDateTime(bodyElement),
      DateTime.now
    )
  }

  private def getSectionNewsPublishDateTime: Element => DateTime
  = (bodyElement: Element) => {
    DateTime.now
  }

  private def getSectionNewsContents: Element => String
  = (bodyElement: Element) => {
    bodyElement
      .select("div.article")
      .select("p")
      .not("p.adrs")
      .withFilter(_.hasText)
      .map(_.text())
      .dropRight(1)
      .mkString(" ")
  }

  case class YNANewsPreViewRecord(newsUrl: String, ynaSection: YNASection, title: String, preView: String)
}
