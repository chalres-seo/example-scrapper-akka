package com.example.akka.actor

import java.util.UUID

import akka.actor.{Actor, ActorLogging, Props}
import com.example.service.selenium.SeleniumClient
import com.typesafe.scalalogging.LazyLogging
import org.jsoup.nodes.Element

object SeleniumClientActor extends LazyLogging {
  def props(): Props = Props(new SeleniumClientActor)

  final case class RequestBodyElement(requestId: UUID, url: String)
  final case class RequestBodyElementWithRetry(requestId: UUID, url: String, checkElementEmpty: Element => Boolean)
  final case class ResponseBodyElement(requestId: UUID, url: String, bodyElement: Option[Element])

  final case class GetBodyElement(url: String)
  final case class ReturnBodyElement(url: String, bodyElement: Option[Element])
}

class SeleniumClientActor extends Actor with ActorLogging {
  import SeleniumClientActor._

  private var seleniumClient: SeleniumClient = _

  override def preStart(): Unit = {
    log.info("selenium actor started")
    seleniumClient = SeleniumClient.createSeleniumClient
  }
  override def postStop(): Unit = {
    seleniumClient.close()
    log.info("selenium actor stopped")
  }

  override def receive: Receive = {
    case RequestBodyElement(requestId, url) =>
      sender() ! ResponseBodyElement(requestId, url, seleniumClient.getBodyElement(url))
    case RequestBodyElementWithRetry(requestId, url, checkElementEmpty) =>
      sender() ! ResponseBodyElement(requestId, url, seleniumClient.getBodyElementWithRetry(url)(checkElementEmpty))
    case GetBodyElement(url) =>
      sender() ! ReturnBodyElement(url, seleniumClient.getBodyElement(url))
  }
}
