package com.example.model.repository

import com.example.model.persistent.redis.Redis
import com.example.service.news.YNAParser.YNASection.YNASection
import com.typesafe.scalalogging.LazyLogging

class YNALastNewsDao(private val redisClient: Redis) {
  def getAllLastSectionNews: Map[YNASection, String] = ???

  def getLastSectionNews(ynaSection: YNASection): String = ???

  def setAllLastSectionNews(lastSectionNews: Map[YNASection, String]) = ???

  def setLastSectionNews(ynaSection: YNASection, url: String) = ???
}

object YNALastNewsDao extends LazyLogging {
  private lazy val instance = this.createInstance

  def getInstance: YNALastNewsDao = this.instance

  private def createInstance: YNALastNewsDao = {
    val instance = new YNALastNewsDao(Redis.createDefaultInstance())
    logger.info(s"create ${instance.getClass.getSimpleName} object.")
    instance
  }
}