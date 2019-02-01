package com.example.utils

import com.typesafe.scalalogging.LazyLogging
import org.apache.http.client.methods.{CloseableHttpResponse, HttpPost}
import org.apache.http.entity.StringEntity
import org.apache.http.impl.client.HttpClients

import scala.annotation.tailrec
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

object SendSlackMsg extends LazyLogging {
  private val MAX_RETRY_COUNT = AppConfig.defaultMaxRetryCount

  private val slackHttpClient = HttpClients.createDefault
  private val slackUrl = AppConfig.slackWebHookURL

  private val httpPost = new HttpPost(slackUrl)
  httpPost.setHeader("Accept", "application/json")
  httpPost.setHeader("Content-type", "application/json")

  def asyncSend(msg: String): Future[Int] = {
    logger.info(s"async send slack msg.")
    Future(this.send(msg))
  }

  def send(msg: String): Int = {
    logger.info(s"send slack msg. msg: $msg")

    val jsonString = s"""{"text":"$msg"}"""
    val httpStringEntity = new StringEntity(jsonString)

    httpPost.setEntity(httpStringEntity)

    @tailrec
    def loop(maxRetryCount: Int): Int = {
      val response: CloseableHttpResponse = slackHttpClient.execute(httpPost)
      val statusCode: Int = response.getStatusLine.getStatusCode
      response.close()

      if (maxRetryCount > 0 && statusCode != 200) {
        logger.error(s"failed slack http client execute. remain retry count: $maxRetryCount")
        loop(maxRetryCount - 1)
      } else {
        logger.info("succeed send slack msg.")
        statusCode
      }
    }

    loop(MAX_RETRY_COUNT)
  }
}
