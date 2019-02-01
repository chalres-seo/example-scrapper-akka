package com.example.akka.actor

import java.util.UUID

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import com.example.service.news.YNAParser
import com.example.service.news.YNAParser.{YNANewsPreViewRecord, YNASection}
import com.example.service.news.YNAParser.YNASection.YNASection
import org.jsoup.nodes.Element

object YNAActor {
  private val newsListUrlToSection: Map[String, YNASection] = Map(
    "https://www.yna.co.kr/politics/all" -> YNASection.politics,
    "https://www.yna.co.kr/sports/all" -> YNASection.sports
  )

  private val sectionToNewsListUrl: Map[YNASection, String] = Map(
    YNASection.politics -> "https://www.yna.co.kr/politics/all",
    YNASection.sports -> "https://www.yna.co.kr/sports/all"
  )

  final case class UpdateYNANewsList(ynaSection: YNASection)

  sealed private trait InternalActorMsg { val requestId: UUID }
  final private case class RequestYNASectionNewsList(requestId: UUID, ynaSection: YNASection) extends InternalActorMsg
  final private case class RequestYNASectionNews(requestId: UUID, ynaNewsPreViewRecord: YNANewsPreViewRecord) extends InternalActorMsg

  def props(seleniumClientActor: ActorRef) = Props(new YNAActor(seleniumClientActor))
}

class YNAActor(private val seleniumClientActor: ActorRef) extends Actor with ActorLogging {
  import YNAActor._
  import SeleniumClientActor._

  private var requestIdToActorMsg: Map[UUID, InternalActorMsg] = Map.empty
  private var lastSectionNews: Map[YNASection, String] = Map.empty

  override def preStart(): Unit = {
    log.info("YNA actor started")
    log.info(s"news list url to section:\n\t${newsListUrlToSection.mkString("\n\t")}")
    log.info(s"section to news list url:\n\t${sectionToNewsListUrl.mkString("\n\t")}")
    this.logCurrentRequestIdToActorMsg()
  }
  override def postStop(): Unit = {
    log.info("YNA actor stopped")
  }

  override def receive: Receive = {
    case UpdateYNANewsList(ynaSection) =>
      log.info(s"update yna news list. section: $ynaSection")
      self ! RequestYNASectionNewsList(this.createRequestId, ynaSection)

    case internalActorMsg @ RequestYNASectionNewsList(requestId: UUID, ynaSection: YNASection) =>
      log.info(s"request YNA section news list. request id: $requestId, section: $ynaSection")
      this.addRequest(internalActorMsg)
      seleniumClientActor.tell(RequestBodyElement(requestId, sectionToNewsListUrl(ynaSection)), self)

    case internalActorMsg @ RequestYNASectionNews(requestId: UUID, ynaNewsPreViewRecord: YNANewsPreViewRecord) =>
      log.info(s"request YNA section news. request id: $requestId, preview: $ynaNewsPreViewRecord")
      this.addRequest(internalActorMsg)
      seleniumClientActor.tell(RequestBodyElement(requestId, ynaNewsPreViewRecord.newsUrl), self)

    case ResponseBodyElement(requestId: UUID, _: String, bodyElement: Option[Element]) =>
      (requestIdToActorMsg.get(requestId), bodyElement) match {
        case (Some(internalActorMsg), Some(element)) =>
          log.info(s"response body element. requestId: $requestId, internalActorMsg: $internalActorMsg")
          this.responseBodyElement(internalActorMsg, element)
          this.removeRequest(internalActorMsg)
        case (Some(internalActorMsg), None) =>
          log.error(s"empty body element. requestId: $requestId, internalActorMsg: $internalActorMsg, bodyElement: $bodyElement")
          this.removeRequest(internalActorMsg)
        case (None, Some(element)) =>
          log.error(s"unknown requestId. requestId: $requestId, internalActorMsg: None, bodyElement: $bodyElement")
        case (None, None) =>
          log.error(s"unknown requestId. requestId: $requestId, internalActorMsg: None, bodyelement: $bodyElement")
      }

    case unknown => log.error(s"unknown actor msg: $unknown")
  }

  private def responseBodyElement(internalActorMsg: YNAActor.InternalActorMsg, element: Element): Unit = {
    internalActorMsg match {
      case RequestYNASectionNewsList(_: UUID, ynaSection: YNASection) =>
        responseYNASectionNewsList(ynaSection, element)
      case RequestYNASectionNews(_: UUID, ynaNewsPreViewRecord: YNANewsPreViewRecord) =>
        responseYNASectionNews(ynaNewsPreViewRecord, element)
    }
  }

  private def responseYNASectionNewsList(ynaSection: YNASection, bodyElement: Element): Unit = {
    val previewList: Vector[YNAParser.YNANewsPreViewRecord] = YNAParser.parseSectionNewsList(ynaSection, bodyElement)
    log.info(s"yna section news List:\n\t${previewList.mkString("\n\t")}")
    previewList.foreach(self ! RequestYNASectionNews(this.createRequestId, _))
  }

  private def responseYNASectionNews(ynaNewsPreViewRecord: YNANewsPreViewRecord, bodyElement: Element): Unit = {
    val ynaNewsRecord = YNAParser.parseSectionNews(ynaNewsPreViewRecord, bodyElement)
    println(ynaNewsRecord)
  }

  private def createRequestId: UUID = {
    val requestId = UUID.randomUUID()
    log.info(s"create request id: $requestId")
    requestId
  }

  private def addRequest(internalActorMsg: InternalActorMsg): Unit = {
    requestIdToActorMsg += internalActorMsg.requestId -> internalActorMsg
    log.info(s"add requestId to actor msg. requestId: ${internalActorMsg.requestId}, actor msg: $internalActorMsg")
    this.logCurrentRequestIdToActorMsg()
  }

  private def removeRequest(internalActorMsg: InternalActorMsg): Unit = {
    requestIdToActorMsg -= internalActorMsg.requestId
    log.info(s"remove requestId. requestId: ${internalActorMsg.requestId}, actor msg: $internalActorMsg")
    this.logCurrentRequestIdToActorMsg()
  }

  private def logCurrentRequestIdToActorMsg(): Unit = {
    if (requestIdToActorMsg.isEmpty) {
      log.info(s"requestId to actorMsg: Empty")
    } else {
      log.info(s"requestId to actorMsg count : ${requestIdToActorMsg.size}")
      log.debug(s"requestId to actorMsg:\n\t${requestIdToActorMsg.mkString("\n\t")}")
    }
  }
}


//  private def getYNANewsList(ynaSection: YNASection, bodyElement: Element): Unit = {
//    val previewList: Vector[YNAParser.YNANewsPreViewRecord] = YNAParser.parse(ynaSection, bodyElement)
//    previewList.foreach(println)
//  }
//
//  private def getYNASectionNews(ynaSection: YNASection, bodyElement: Element): Unit = {
//    val news = YNAParser.parseSectionNews(bodyElement)
//    println(news)
//  }


//case UpdateYNANewsList(ynaSection) =>
//log.info(s"update yna news list. section: $ynaSection")
//sectionToUrl.get(ynaSection) match {
//  case Some(url) => seleniumClientActor.tell(GetBodyElement(url), self)
//  case None => log.error(s"unknown YNA section. section: $ynaSection")
//}

//case GetYNASectionNews(ynaSection, url) =>
//log.info(s"get yna news. url: $url")
//urlToSection += url -> ynaSection
//seleniumClientActor.tell(GetBodyElement(url), self)


//case ReturnBodyElement(url, bodyElement) =>
//log.info(s"return body element. url: $url")
//(urlToSection.get(url), bodyElement) match {
//  case (Some(ynaSection), Some(element)) =>
//  log.info(s"succeed YNA news list update. url: $url, ynaSection: $ynaSection")
//  this.updateYNANewsList(ynaSection, element)
//
//  case (None, Some(element)) =>
//  log.info(s"succeed get YNA news body element. url: $url")
//
//  this.updateYNASectionNews(urlToSection(url), element)
//  urlToSection -= url
//
//  case (Some(ynaSection), None) =>
//  log.error(s"failed YNA news list update. empty body element. ynaSection: $ynaSection, url: $url")
//
//  case (None, None) =>
//  log.error(s"failed YNA new update. empty body element. url: $url")
//}

  //    case ResponseBodyElement(requestId, url, bodyElement) =>
//      requestIdToActorMsg.get(requestId) match {
//        case Some(_) =>  {
//          case UpdateYNANewsList => this.updateYNANewsList(url, bodyElement)
//        }
//        case None =>
//          log.error(s"unknown requestId. requestId: $requestId")
//      }

//    case ReturnBodyElement(url, bodyElement) =>
//      log.info(s"return body element. url: $url")
//      (urlToSection.get(url), bodyElement) match {
//        case (Some(ynaSection), Some(element)) =>
//          log.info(s"succeed YNA news list update. url: $url, ynaSection: $ynaSection")
//          this.updateYNANewsList(ynaSection, element)
//
//        case (None, Some(element)) =>
//          log.info(s"succeed get YNA news body element. url: $url")
//          this.updateYNANews(element)
//
//        case (Some(ynaSection), None) =>
//          log.error(s"failed YNA news list update. empty body element. ynaSection: $ynaSection, url: $url")
//
//        case (None, None) =>
//          log.error(s"failed YNA new update. empty body element. url: $url")
//      }
//  }

//  private def updateYNANewsList(url: String, bodyElement: Option[Element]) = {
//    (urlToSection.get(url), bodyElement) match {
//      case (Some(ynaSection), Some(element)) =>
//        log.info(s"succeed YNA news list update. url: $url, ynaSection: $ynaSection")
//        val list = YNAParser.parse(ynaSection, element)
//        list.foreach(println)
//
//      case (None, Some(element)) =>
//        log.info(s"succeed get YNA news body element. url: $url")
//        val list = YNAParser.parse(element)
//        list.foreach(println)
//
//      case (Some(ynaSection), None) =>
//        log.error(s"failed YNA news list update. empty body element. ynaSection: $ynaSection, url: $url")
//
//      case (None, None) =>
//        log.error(s"failed YNA new update. empty body element. url: $url")
//
//    }
//  }

//  private def updateYNANewsList(ynaSection: YNASection, bodyElement: Element): Unit = {
//    val list = YNAParser.parse(ynaSection, bodyElement)
//    list.foreach(println)
//  }
//
//  private def updateYNANews(bodyElement: Element): Unit = {
//    val news = YNAParser.parse(bodyElement)
//    println(news)
//  }
//
//  private def createRequestId: UUID = {
//    UUID.randomUUID()
//  }
//
//  private def requestBodyElement(msg: RequestBodyElement) = {
//    requestIdToActorMsg += msg.requestId -> msg
//    seleniumClientActor.tell(msg, self)
//  }
//
//  private def getBodyElement(url: String): Unit = {
//    seleniumClientActor.tell(GetBodyElement(url), self)
//  }
//}