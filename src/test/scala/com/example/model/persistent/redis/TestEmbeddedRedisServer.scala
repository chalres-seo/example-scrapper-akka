package com.example.model.persistent.redis

import com.typesafe.scalalogging.LazyLogging
import org.junit.{Assert, Test}
import org.hamcrest.CoreMatchers._

class TestEmbeddedRedisServer extends LazyLogging {
  @Test
  def testEmbeddedRedisServer = {
    val server = EmbeddedRedisServer.createInstance()

    Assert.assertThat(server.isReady, is(false))
    Assert.assertThat(server.isActive, is(false))
    Assert.assertThat(server.getPid.isDefined, is(false))

    Assert.assertThat(server.start(), is(true))
    Assert.assertThat(server.isReady, is(true))
    Assert.assertThat(server.isActive, is(true))
    Assert.assertThat(server.getPid.isDefined, is(true))

    server.stop()
    Assert.assertThat(server.isReady, is(false))
    Assert.assertThat(server.isActive, is(false))
    Assert.assertThat(server.getPid.isDefined, is(false))

    Assert.assertThat(server.start(), is(true))
    Assert.assertThat(server.isReady, is(true))
    Assert.assertThat(server.isActive, is(true))
    Assert.assertThat(server.getPid.isDefined, is(true))

    server.stop()
    Assert.assertThat(server.isReady, is(false))
    Assert.assertThat(server.isActive, is(false))
    Assert.assertThat(server.getPid.isDefined, is(false))
  }
}