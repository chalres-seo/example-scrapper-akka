package com.example.model.persistent.redis

import java.nio.charset.StandardCharsets

import com.example.utils.AppConfig
import com.redis.serialization.Parse
import com.redis.{RedisClient, RedisClientPool}
import com.typesafe.scalalogging.LazyLogging

import scala.annotation.tailrec

class Redis(redisClientPool: RedisClientPool) {
  import Parse.Implicits._

  def getHashMap[K, MapK, MapV](key: String, valueMapKeys: MapK*)(implicit parseV: Parse[MapV]): Option[Map[MapK, MapV]] = {
    this.run(_.hmget[MapK, MapV](key, valueMapKeys:_*))
  }

  def setHashMap[K, MapK, MapV](key: String, valueMap: Map[MapK, MapV]): Boolean = {
    this.run(_.hmset(key, valueMap))
  }

  def set[K, V](key: K, value: V): Boolean = {
    this.run(_.set(key, value))
  }

  def set[K, V](keyValue: (K, V)): Boolean = {
    this.set(keyValue._1, keyValue._2)
  }

  def setAll[K, V](keyValueList: Iterable[(K, V)]): Iterable[(K, V)] = {
    @tailrec
    def loop(keyValueList: Iterable[(K, V)]): Iterable[(K, V)] = {
      if (keyValueList.nonEmpty) {
        //val resultList: Vector[(K, V)] = getResultList(keyValueList)
        val resultList: Iterable[(K, V)] = for {
          result <- keyValueList.zip(keyValueList.map(this.set(_)))
          if !result._2
        } yield result._1

        if (resultList.nonEmpty) {
          loop(resultList)
        } else {
          Vector.empty[(K, V)]
        }
      } else Vector.empty[(K, V)]
    }

    loop(keyValueList)
  }

  def getString[K](key: K): Option[String] = {
    this.run(_.get[String](key))
  }

  def getInt[K](key: K): Option[Int] = {
    this.run(_.get[Int](key))
  }

  def getLong[K](key: K): Option[Long] = {
    this.run(_.get[Long](key))
  }

  def getDouble[K](key: K): Option[Double] = {
    this.run(_.get[Double](key))
  }

  def getByteArray[K](key: K): Option[Array[Byte]] = {
    this.run(_.get[Array[Byte]](key))
  }

  def del[K](key: K): Option[Long] = {
    this.run(_.del(key))
  }

  def delAll[K](keyList: Seq[K]): Option[Long] = {
    this.run(_.del(keyList.head, keyList:_*))
  }

  def close(): Unit = {
    redisClientPool.close
  }

  private def run[K, T](redisCommand: RedisClient => T): T = {
    redisClientPool.withClient(redisCommand)
  }
}

object Redis extends LazyLogging {
  def createDefaultInstance() = new Redis(this.createRedisPool)
  def createInstance(host: String, port: Int): Redis = new Redis(this.createRedisPool(host, port))

  private def createRedisPool(host: String, port: Int): RedisClientPool = {
    logger.info(s"create redis pool. servers: ${AppConfig.redisServers}, port: ${AppConfig.redisPort}")
    new RedisClientPool(host = host,
      port = port,
      maxIdle = AppConfig.redisConnections,
      maxConnections = AppConfig.redisMaxConnections)
  }
  private def createRedisPool: RedisClientPool = {
    logger.info(s"create redis pool. servers: ${AppConfig.redisServers}, port: ${AppConfig.redisPort}")
    new RedisClientPool(host = AppConfig.redisServers,
      port = AppConfig.redisPort,
      maxIdle = AppConfig.redisConnections,
      maxConnections = AppConfig.redisMaxConnections)
  }
}
