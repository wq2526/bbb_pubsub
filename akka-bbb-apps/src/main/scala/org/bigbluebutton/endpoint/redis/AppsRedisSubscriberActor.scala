package org.bigbluebutton.endpoint.redis

import akka.actor.Props
import java.net.InetSocketAddress
//import redis.actors.RedisSubscriberActor
//import redis.api.pubsub.{ PMessage, Message }
import scala.concurrent.duration._
import akka.actor.ActorRef
import akka.actor.actorRef2Scala
import org.bigbluebutton.SystemConfiguration
import org.bigbluebutton.core.pubsub.receivers.RedisMessageReceiver
//import redis.api.servers.ClientSetname

import org.bigbluebutton.common.messages.MessagingConstants
import akka.actor.Actor

import javax.jms.MessageListener
import javax.jms.Session
import javax.jms.Message
import javax.jms.TextMessage
import javax.jms.Topic
import javax.jms.JMSException

import org.apache.activemq.ActiveMQConnectionFactory
import org.apache.activemq.command.ActiveMQTopic

object AppsRedisSubscriberActor extends SystemConfiguration {

  val channels = MessagingConstants.TO_BBB_APPS_TOPICS + "," + "bigbluebutton:from-voice-conf:system"
  val patterns = "bigbluebutton:to-bbb-apps:*" + "bigbluebutton:from-voice-conf:*"

  def props(msgReceiver: RedisMessageReceiver): Props =
    Props(classOf[AppsRedisSubscriberActor], msgReceiver,
      ActiveMQHost, ActiveMQPort,
      channels, patterns).withDispatcher("akka.rediscala-subscriber-worker-dispatcher")
}

class AppsRedisSubscriberActor(msgReceiver: RedisMessageReceiver, ActiveMQHost: String,
  ActiveMQPort: Int,
  channels: String, patterns: String)
    extends Actor {

  def receive = {

    case "startSubscribe" => subscribe()

  }

  def subscribe(): Unit = {

    val connFactory = new ActiveMQConnectionFactory("tcp://" + ActiveMQHost + ":" + ActiveMQPort)

    val conn = connFactory.createConnection()
    conn.start()

    val session = conn.createSession(false, Session.AUTO_ACKNOWLEDGE)
    val topic = new ActiveMQTopic(channels)

    val consumer = session.createConsumer(topic)
    consumer.setMessageListener(new PubSubListener())

  }

  // Set the name of this client to be able to distinguish when doing
  // CLIENT LIST on redis-cli
  //write(ClientSetname("BbbAppsAkkaSub").encodedRequest)

  /*def onMessage(message: Message) {
    log.error(s"SHOULD NOT BE RECEIVING: $message")
  }

  def onPMessage(pmessage: PMessage) {
    //log.debug(s"RECEIVED:\n $pmessage \n")
    msgReceiver.handleMessage(pmessage.patternMatched, pmessage.channel, pmessage.data)
  }*/

  private class PubSubListener extends MessageListener {

    def onMessage(message: Message): Unit = {

      var msg = ""
      var topic = ""
      if (message.isInstanceOf[TextMessage]) {
        msg = message.asInstanceOf[TextMessage].getText()
        val t = message.getJMSDestination().asInstanceOf[Topic]
        topic = t.getTopicName()
        msgReceiver.handleMessage(patterns, topic, msg)
      }

    }

  }

}