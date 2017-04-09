package org.bigbluebutton.endpoint.redis

import akka.actor.Props
//import redis.RedisClient
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global
import akka.actor.ActorSystem
import scala.concurrent.Await
import akka.actor.Actor
import org.bigbluebutton.SystemConfiguration

import org.apache.activemq.ActiveMQConnectionFactory
import org.apache.activemq.command.ActiveMQTopic

import javax.jms.Session

class RedisPublisher(val system: ActorSystem) extends SystemConfiguration {

  //val redis = RedisClient(redisHost, redisPort)(system)
  val connFactory = new ActiveMQConnectionFactory("tcp://" + ActiveMQHost + ":" + ActiveMQPort)

  // Set the name of this client to be able to distinguish when doing
  // CLIENT LIST on redis-cli
  //redis.clientSetname("BbbAppsAkkaPub")

  def publish(channel: String, data: String) {
    //println("PUBLISH TO [" + channel + "]: \n [" + data + "]")
    //redis.publish(channel, data)
    val conn = connFactory.createConnection()
    conn.start()

    val sess = conn.createSession(false, Session.AUTO_ACKNOWLEDGE)

    val t = new ActiveMQTopic(channel)

    val prod = sess.createProducer(t)

    val msg = sess.createTextMessage(data)

    prod.send(msg)

    conn.close()
  }

}
