package com.example.akka.actor

import java.util.UUID
import java.util.concurrent.TimeUnit

import akka.actor.ActorSystem
import akka.testkit.{TestKit, TestProbe}
import com.example.akka.actor.SeleniumClientActor.{RequestBodyElement, ResponseBodyElement}
import com.example.utils.{AppConfig, AppUtils}
import com.typesafe.scalalogging.LazyLogging
import org.junit.{AfterClass, BeforeClass, Test}

import scala.concurrent.duration.{Duration, FiniteDuration}

class TestSeleniumClientActor extends LazyLogging {
  import TestSeleniumClientActor._

  val seleniumClientActor = testSystem.actorOf(SeleniumClientActor.props(), "selenium-client-actor")

  @Test
  def testRequest: Unit = {
    val uuid = UUID.randomUUID()
    val test1RequestBodyElements = RequestBodyElement(uuid, "https://www.naver.com")
    val test2RequestBodyElements = RequestBodyElement(uuid, "https://www.daum.net")

    seleniumClientActor.tell(test1RequestBodyElements, testProbe.ref)
    val returnMsg1: ResponseBodyElement = testProbe.expectMsgType[ResponseBodyElement](waitTestProbeMsg)

    println(returnMsg1)

    seleniumClientActor.tell(test2RequestBodyElements, testProbe.ref)
    val returnMsg2: ResponseBodyElement = testProbe.expectMsgType[ResponseBodyElement](waitTestProbeMsg)
    println(returnMsg2)
  }
}

object TestSeleniumClientActor extends LazyLogging {
  val testSystem: ActorSystem = ActorSystem(AppConfig.applicationName)
  val testProbe = TestProbe()(testSystem)
  val waitTestProbeMsg = FiniteDuration(1, TimeUnit.MINUTES)

  @BeforeClass
  def setup: Unit = {
    logger.info("setup before test class.")
    //nothing to do before unit test
  }

  @AfterClass
  def cleanup: Unit = {
    logger.info("cleanup after test class")
    AppUtils.awaitResultFuture(testSystem.terminate())
  }
}