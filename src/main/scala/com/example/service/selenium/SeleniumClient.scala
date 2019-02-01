package com.example.service.selenium

import java.util.concurrent.TimeUnit

import com.example.utils.AppConfig
import com.typesafe.scalalogging.LazyLogging
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import org.jsoup.select.Elements
import org.openqa.selenium.chrome.{ChromeDriver, ChromeOptions}
import org.openqa.selenium.remote.RemoteWebDriver

import scala.annotation.tailrec

class SeleniumClient(private val webDriver: RemoteWebDriver) extends LazyLogging {
  private val maxRetryCount = AppConfig.defaultMaxRetryCount
  private val beforeScrappingTimeWaitMillis = AppConfig.defaultTimeWaitMillis

  def getPageSource(url: String): Option[String] = {
    logger.info(s"get url page source. url: $url")
    this.timeWaitBeforeScrapping()

    this.synchronized {
      try {
        webDriver.get(url)
        val html = webDriver.getPageSource
        logger.info(s"page source result size: ${html.length}")

        Option(html)
      } catch {
        case e: Exception =>
          logger.error(s"failed get url page source. url: $url, msg: ${e.getMessage}", e)
          None
      }
    }
  }

  def getBodyElement(url: String): Option[Element] = {
    logger.info(s"get url page source body elements. url: $url")
    this.getPageSource(url).map(Jsoup.parse(_).body)
  }

  def getBodyElementWithRetry(url: String)(checkElementEmpty: Element => Boolean): Option[Element] = {
    logger.info(s"get url page source body element with retry. url: $url")
    @tailrec
    def loop(maxRetryCount: Int): Option[Element] = {
      this.getBodyElement(url) match {
        case Some(bodyElement) =>
          if (maxRetryCount > 0 && checkElementEmpty(bodyElement)) {
            logger.error(s"failed get body elements validation check. remain retry count $maxRetryCount")
            loop(maxRetryCount - 1)
          } else None
        case None => None
      }
    }

    loop(maxRetryCount)
  }

  private def timeWaitBeforeScrapping(): Unit = {
    logger.info(s"wait $beforeScrappingTimeWaitMillis millis before get url page source..")
    try {
      Thread.sleep(beforeScrappingTimeWaitMillis)
    } catch {
      case e: Exception => logger.error(s"interrupted time wait before scrapping. msg: ${e.getMessage}")
    }
  }

  def close(): Unit = {
    logger.info("close selenium driver.")
    try {
      webDriver.close()
      webDriver.quit()
    } catch {
      case e: org.openqa.selenium.WebDriverException =>
        logger.warn(s"already close selenium driver. exception msg: ${e.getMessage}")
      case e: Exception =>
        logger.error(s"failed close selenium driver. exception msg: ${e.getMessage}", e)
        logger.error(e.getClass.toString)
      case t: Throwable =>
        logger.error(s"failed close selenium driver. thrown msg: ${t.getMessage}", t)
    }
  }
}

object SeleniumClient extends LazyLogging {
  System.setProperty(AppConfig.seleniumWebDriverName, AppConfig.seleniumWebDriverPath)

  private val webDriverOptions = new ChromeOptions()
  webDriverOptions.setHeadless(AppConfig.seleniumWebDriverHeadLess)

  def createSeleniumClient: SeleniumClient = new SeleniumClient(this.createWebDriver)

  private def createWebDriver: RemoteWebDriver = {
    logger.info("create webDriver")
    val webDriver = new ChromeDriver(webDriverOptions)
    webDriver.manage().timeouts().implicitlyWait(AppConfig.seleniumWebDriverMaxWaitTimeSec, TimeUnit.SECONDS)

    webDriver
  }
}