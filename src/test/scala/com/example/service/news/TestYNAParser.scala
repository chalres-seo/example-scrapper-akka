package com.example.service.news

import java.time.DateTimeException
import java.util.concurrent.TimeUnit

import com.example.service.news.YNAParser.{YNANewsPreViewRecord, YNASection}
import com.example.service.selenium.SeleniumClient.webDriverOptions
import com.example.utils.{AppConfig, AppUtils}
import org.jsoup.Jsoup
import org.jsoup.nodes.{Document, Element}
import org.junit.{Assert, Test}
import org.hamcrest.CoreMatchers._
import org.joda.time.DateTime
import org.openqa.selenium.chrome.ChromeDriver

import scala.io.Source

class TestYNAParser {

  @Test
  def testYNAPoliticsParse(): Unit = {
    //https://www.yna.co.kr/politics/all
    val sampleFile = "sample_source/yna/sample_politics.html"
    val bodyElement: Element = Jsoup.parse(Source.fromFile(sampleFile).mkString).body()
    val list = YNAParser.parseSectionNewsList(YNASection.politics, bodyElement)
    Assert.assertThat(list.nonEmpty, is(true))
    list.foreach(println)
  }

  @Test
  def testYNAEconomyParse(): Unit = {
    //https://www.yna.co.kr/economy/all
    val sampleFile = "sample_source/yna/sample_economy.html"
    val bodyElement: Element = Jsoup.parse(Source.fromFile(sampleFile).mkString).body()
    val list = YNAParser.parseSectionNewsList(YNASection.economy, bodyElement)
    Assert.assertThat(list.nonEmpty, is(true))
    list.foreach(println)
  }

  @Test
  def testYNAStockParse(): Unit = {
    //https://www.yna.co.kr/stock/all
    val sampleFile = "sample_source/yna/sample_stock.html"
    val bodyElement: Element = Jsoup.parse(Source.fromFile(sampleFile).mkString).body()
    val list = YNAParser.parseSectionNewsList(YNASection.stock, bodyElement)
    Assert.assertThat(list.nonEmpty, is(true))
    list.foreach(println)
  }

  @Test
  def testYNAInformationTechParse(): Unit = {
    //https://www.yna.co.kr/it/all
    val sampleFile = "sample_source/yna/sample_it.html"
    val bodyElement: Element = Jsoup.parse(Source.fromFile(sampleFile).mkString).body()
    val list = YNAParser.parseSectionNewsList(YNASection.it, bodyElement)
    Assert.assertThat(list.nonEmpty, is(true))
    list.foreach(println)
  }

  @Test
  def testYNASocietyParse(): Unit = {
    //https://www.yna.co.kr/society/all
    val sampleFile = "sample_source/yna/sample_society.html"
    val bodyElement: Element = Jsoup.parse(Source.fromFile(sampleFile).mkString).body()
    val list = YNAParser.parseSectionNewsList(YNASection.society, bodyElement)
    Assert.assertThat(list.nonEmpty, is(true))
    list.foreach(println)
  }

  @Test
  def testYNAEntertainmentParse(): Unit = {
    //https://www.yna.co.kr/entertainment/all
    val sampleFile = "sample_source/yna/sample_entertainment.html"
    val bodyElement: Element = Jsoup.parse(Source.fromFile(sampleFile).mkString).body()
    val list = YNAParser.parseSectionNewsList(YNASection.entertainment, bodyElement)
    Assert.assertThat(list.nonEmpty, is(true))
    list.foreach(println)
  }

  @Test
  def testYNACultureParse(): Unit = {
    //https://www.yna.co.kr/culture/all
    val sampleFile = "sample_source/yna/sample_culture.html"
    val bodyElement: Element = Jsoup.parse(Source.fromFile(sampleFile).mkString).body()
    val list = YNAParser.parseSectionNewsList(YNASection.culture, bodyElement)
    Assert.assertThat(list.nonEmpty, is(true))
    list.foreach(println)
  }

  @Test
  def testYNASportsParse(): Unit = {
    //https://www.yna.co.kr/sports/all
    val sampleFile = "sample_source/yna/sample_sports.html"
    val bodyElement: Element = Jsoup.parse(Source.fromFile(sampleFile).mkString).body()
    val list = YNAParser.parseSectionNewsList(YNASection.sports, bodyElement)
    Assert.assertThat(list.nonEmpty, is(true))
    list.foreach(println)
  }

  @Test
  def testYNAInternationalParse(): Unit = {
    //https://www.yna.co.kr/international/all
    val sampleFile = "sample_source/yna/sample_international.html"
    val bodyElement: Element = Jsoup.parse(Source.fromFile(sampleFile).mkString).body()
    val list = YNAParser.parseSectionNewsList(YNASection.international, bodyElement)
    Assert.assertThat(list.nonEmpty, is(true))
    list.foreach(println)
  }

  @Test
  def testYNATravelParse(): Unit = {
    //https://www.yna.co.kr/travel-festival/travel
    val sampleFile = "sample_source/yna/sample_travel.html"
    val bodyElement: Element = Jsoup.parse(Source.fromFile(sampleFile).mkString).body()
    val list = YNAParser.parseSectionNewsList(YNASection.travel, bodyElement)
    Assert.assertThat(list.nonEmpty, is(true))
    list.foreach(println)
  }

  @Test
  def testYNAFestivalParse(): Unit = {
    //https://www.yna.co.kr/travel-festival/festival
    val sampleFile = "sample_source/yna/sample_festival.html"
    val bodyElement: Element = Jsoup.parse(Source.fromFile(sampleFile).mkString).body()
    val list = YNAParser.parseSectionNewsList(YNASection.festival, bodyElement)
    Assert.assertThat(list.nonEmpty, is(true))
    list.foreach(println)
  }

  @Test
  def testYNADefaultNewsParse(): Unit = {
    val sampleFile = "sample_source/yna/sample_default_news.html"
    val bodyElement: Element = Jsoup.parse(Source.fromFile(sampleFile).mkString).body()
    val preViewRecord = YNANewsPreViewRecord("newsUrl", YNASection.politics,"title","priview")
    val str = YNAParser.parseSectionNews(preViewRecord, bodyElement)

    Assert.assertThat(str.newsUrl, is(preViewRecord.newsUrl))
    Assert.assertThat(str.ynaSection, is(preViewRecord.ynaSection))
    Assert.assertThat(str.title, is(preViewRecord.title))
  }

  def createSamples = {
    System.setProperty(AppConfig.seleniumWebDriverName, AppConfig.seleniumWebDriverPath)

    val sampleUrlAndSavePaths = Vector(
      "https://www.yna.co.kr/politics/all" -> "sample_source/yna/sample_politics.html",
      "https://www.yna.co.kr/nk/news/all" -> "sample_source/yna/sample_nk.html",
      "https://www.yna.co.kr/economy/all" -> "sample_source/yna/sample_economy.html",
      "https://www.yna.co.kr/stock/all" -> "sample_source/yna/sample_stock.html",
      "https://www.yna.co.kr/it/all" -> "sample_source/yna/sample_it.html",
      "https://www.yna.co.kr/society/all" -> "sample_source/yna/sample_society.html",
      "https://www.yna.co.kr/entertainment/all" -> "sample_source/yna/sample_entertainment.html",
      "https://www.yna.co.kr/culture/all" -> "sample_source/yna/sample_culture.html",
      "https://www.yna.co.kr/sports/all" -> "sample_source/yna/sample_sports.html",
      "https://www.yna.co.kr/international/all" -> "sample_source/yna/sample_international.html",
      "https://www.yna.co.kr/travel-festival/travel" -> "sample_source/yna/sample_travel.html",
      "https://www.yna.co.kr/travel-festival/festival" -> "sample_source/yna/sample_festival.html"
    )

    val webDriver = new ChromeDriver()
    webDriver.manage().timeouts().implicitlyWait(AppConfig.seleniumWebDriverMaxWaitTimeSec, TimeUnit.SECONDS)

    sampleUrlAndSavePaths.foreach {
      case (url, savePath) =>
        webDriver.get(url)
        val pageSource = webDriver.getPageSource
        println(pageSource)
        AppUtils.writeFile(savePath, pageSource, false)
    }
  }
}
