package com.example.akka.actor

import akka.actor.{Actor, ActorLogging, Props}

object ScrapperActor {
  //val seleniumClientActor: ActorRef = context.system.actorOf(SeleniumClientActor.props(), "selenium-client-actor")

  def props(): Props = Props(new ScrapperActor)
}

class ScrapperActor extends Actor with ActorLogging {
  import ScrapperActor._

  override def preStart(): Unit = log.info("scrapper actor started")

  override def postStop(): Unit = log.info("scrapper actor stopped")

  // No need to handle any messages
  override def receive: Receive = {
    Actor.emptyBehavior
    //case msg:
  }
}