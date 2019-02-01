package com.example.model.persistent.redis

import com.typesafe.scalalogging.LazyLogging
import org.junit.{After, Assert, Before, Test}
import org.hamcrest.CoreMatchers._

class TestRedisClient extends LazyLogging {
  var redisServer: EmbeddedRedisServer = EmbeddedRedisServer.createInstance()
  var redisClient: Redis = _

  @Before
  def setup(): Unit = {
    redisServer.start()
    redisClient = Redis.createInstance("localhost", redisServer.getPort)
  }

  @After
  def cleanup: Unit = {
    redisServer.stop()
    redisClient.close()
  }

  @Test
  def testSetAndGetAndDel(): Unit = {
    val key = "key-0"
    val value = "value-0"

    val sampleCount = 10
    val sampleList = (1 to sampleCount).map { k =>
      s"key-$k" -> s"value-$k"
    }

    Assert.assertThat(redisClient.set(key, value), is(true))
    Assert.assertThat(redisClient.getString(key), is(Option(value)))
    Assert.assertThat(redisClient.del(key), is(Option(1L)))
    Assert.assertThat(redisClient.getString(key), is(Option.empty[String]))
    Assert.assertThat(redisClient.setAll(sampleList).isEmpty, is(true))
    sampleList.foreach(r => Assert.assertThat(redisClient.getString(r._1), is(Option(r._2))))
    Assert.assertThat(redisClient.delAll(sampleList.map(_._1)), is(Option(sampleCount.toLong)))

    redisClient.close()
  }

  @Test
  def testHashMapSetAndHashMapGet(): Unit = {
    val key = "key-0"
    val valueMap = Map(
      "mapKey-1" -> "mapValue-1",
      "mapKey-2" -> "mapValue-2",
      "mapKey-3" -> "mapValue-3"
    )

    Assert.assertThat(redisClient.setHashMap(key, valueMap), is(true))
    Assert.assertThat(redisClient.getHashMap(key, valueMap.keys.toVector:_*).get, is(valueMap))
  }
}
