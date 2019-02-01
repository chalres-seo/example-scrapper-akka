package com.example.service.news

import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import org.junit.{Assert, Test}

import scala.io.Source
import org.hamcrest.CoreMatchers._

class TestSitePolicies {
  @Test
  def testYNAPolicies(): Unit = {
    val sampleFile = "sample_source/yna/sample_robots.html"
    val bodyElement: Element = Jsoup.parse(Source.fromFile(sampleFile).mkString).body()
    SitePolicies.updateYNAPolicy(bodyElement)

    Assert.assertThat(SitePolicies.checkYNAPolicy(SitePolicies.ynaRootURL), is(true))
    Assert.assertThat(SitePolicies.checkYNAPolicy(SitePolicies.ynaRootURL + "/web/"), is(false))
  }
}
