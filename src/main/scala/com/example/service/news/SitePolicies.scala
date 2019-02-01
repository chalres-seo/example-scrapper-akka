package com.example.service.news

import com.typesafe.scalalogging.LazyLogging
import org.jsoup.nodes.Element

import scala.util.Try

object SitePolicies extends LazyLogging {
  val ynaRootURL = "https://www.yna.co.kr"

  @volatile var ynaPolicy = Map.empty[String, Boolean]

  def updateYNAPolicy(bodyDocument: Element): Unit = {
    logger.info(s"update yna policy.")
    val robotsTxt = bodyDocument.text()

    if (robotsTxt != "") {
      val robotsTxtLines = robotsTxt.split("\n").toVector
      val policyGroupStartLineNumbers = robotsTxtLines.zipWithIndex.filter(_._1.startsWith("User-agent:"))

      val policyStartLineNumber = Try(policyGroupStartLineNumbers.filter(_._1.equals("User-agent: *")).map(_._2).head).toOption
      val policyEndLineNumber = policyStartLineNumber.map { startLineNumber =>
        if (policyGroupStartLineNumbers.length > 1) {
          policyGroupStartLineNumbers.filter(_._2 > startLineNumber).map(_._2).head - 1
        } else robotsTxtLines.length - 1
      }

      (policyStartLineNumber, policyEndLineNumber) match {
        case (Some(s), Some(e)) =>
          val updatedYNAPolicy = for {
            line <- robotsTxtLines.slice(s, e)
            if line != ""
            splitLine = line.replace(" ", "").split(":")
            if splitLine(0) == "Allow" || splitLine(0) == "Disallow"
          } yield {
            splitLine(0) match {
              case "Allow" =>
                ynaRootURL + splitLine(1) -> true
              case "Disallow" =>
                ynaRootURL + splitLine(1) -> false
            }
          }
          ynaPolicy = updatedYNAPolicy.toMap
          logger.info(s"yna policy updated:\n${ynaPolicy.mkString("\t\n")}")
        case _ => Unit
      }
    }
  }
  def checkYNAPolicy(ynaSiteUrl: String): Boolean = {
    logger.info(s"check yna policy. url: $ynaSiteUrl")
    this.ynaPolicy.getOrElse(ynaSiteUrl, true)
  }

}
