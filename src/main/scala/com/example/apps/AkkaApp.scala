package com.example.apps

import java.util.concurrent.{ExecutorService, Executors, TimeUnit}

import akka.actor.{ActorRef, ActorSystem, Cancellable}
import com.example.akka.actor.YNAActor.UpdateYNANewsList
import com.example.akka.actor.{ScrapperActor, SeleniumClientActor, YNAActor}
import com.example.service.news.YNAParser
import com.example.service.news.YNAParser.YNASection
import com.example.utils.{AppConfig, AppUtils}
import com.typesafe.scalalogging.LazyLogging

import scala.concurrent.duration.Duration

object AkkaApp extends LazyLogging {
  val system = ActorSystem(AppConfig.applicationName)
  import system.dispatcher

  val scrapperActor: ActorRef = system.actorOf(ScrapperActor.props(), "scrapper-manager-actor")
  val seleniumActor: ActorRef = system.actorOf(SeleniumClientActor.props(), "selenium-client-actor")
  val ynaActor: ActorRef = system.actorOf(YNAActor.props(seleniumActor), "yna-scrapper-actor")

  def main(args: Array[String]): Unit = {
    val schedules: Vector[(YNAParser.YNASection.Value, Cancellable)] = Vector(
      YNASection.politics -> this.runUpdatePolitics
    )

    try {
      while (true) {
        schedules.foreach { case (section, scheduleJob) =>
          logger.info(s"$section scheduler stats. isCancelled: ${scheduleJob.isCancelled}")
        }
        Thread.sleep(Long.MaxValue)
      }
    } catch {
      case e: Exception =>
        logger.info(e.getMessage)
        logger.info("stop watch scheduler stats. start shutdown parse.")
        this.shutDown()
    }
  }

  def runUpdatePolitics: Cancellable = {
    val updatePoliticsMsg = UpdateYNANewsList(YNASection.politics)
    val updatePoliticsInterval = Duration(5L, TimeUnit.MINUTES)

    system.scheduler.schedule(Duration.Zero,
      updatePoliticsInterval,
      ynaActor,
      updatePoliticsMsg)
  }

  def shutDown(): Unit = {
    logger.info("start shutdown parse.")
    AppUtils.awaitResultFuture(system.terminate())
    logger.info("end shutdown parse.")
  }
}
