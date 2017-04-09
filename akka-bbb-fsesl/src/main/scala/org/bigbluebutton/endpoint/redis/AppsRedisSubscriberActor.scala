package org.bigbluebutton.endpoint.redis

import akka.actor.Props
import java.net.InetSocketAddress
//import redis.actors.RedisSubscriberActor
//import redis.api.pubsub.{ PMessage, Message }
import scala.concurrent.duration._
import akka.actor.ActorRef
import akka.actor.actorRef2Scala
import org.bigbluebutton.SystemConfiguration
import org.bigbluebutton.freeswitch.pubsub.receivers.RedisMessageReceiver
//import redis.api.servers.ClientSetname
import org.bigbluebutton.common.converters.FromJsonDecoder
import org.bigbluebutton.common.messages.PubSubPongMessage
import akka.actor.ActorSystem
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global

import akka.actor.Actor
import org.bigbluebutton.common.messages.MessagingConstants

import javax.jms.MessageListener;
import javax.jms.Session;
import javax.jms.Message;
import javax.jms.TextMessage;
import javax.jms.Topic;
import javax.jms.JMSException;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.command.ActiveMQTopic;

object AppsRedisSubscriberActor extends SystemConfiguration {

  val channels = MessagingConstants.FROM_BBB_APPS_TOPICS + "," + "bigbluebutton:to-voice-conf:system"
  val patterns = "bigbluebutton:to-voice-conf:*" + "bigbluebutton:from-bbb-apps:*"

  def props(system: ActorSystem, msgReceiver: RedisMessageReceiver): Props =
    Props(classOf[AppsRedisSubscriberActor], system, msgReceiver,
      ActiveMQHost, ActiveMQPort,
      channels, patterns).withDispatcher("akka.rediscala-subscriber-worker-dispatcher")
}

class AppsRedisSubscriberActor(val system: ActorSystem, msgReceiver: RedisMessageReceiver, ActiveMQHost: String,
  ActiveMQPort: Int, channels: String, patterns: String)
    extends Actor {

  val decoder = new FromJsonDecoder()

  var lastPongReceivedOn = 0L
  system.scheduler.schedule(10 seconds, 10 seconds)(checkPongMessage())

  def receive = {

    case "startSubscribe" => subscribe()

  }

  val connFactory = new ActiveMQConnectionFactory("tcp://" + ActiveMQHost + ":" + ActiveMQPort)

  def subscribe(): Unit = {

    val conn = connFactory.createConnection()

    conn.start();

    val session = conn.createSession(false, Session.AUTO_ACKNOWLEDGE)
    val topic = new ActiveMQTopic(channels)

    val consumer = session.createConsumer(topic)
    consumer.setMessageListener(new PubSubListener())

  }

  // Set the name of this client to be able to distinguish when doing
  // CLIENT LIST on redis-cli
  //write(ClientSetname("BbbFsEslAkkaSub").encodedRequest)

  def checkPongMessage() {
    val now = System.currentTimeMillis()

    if (lastPongReceivedOn != 0 && (now - lastPongReceivedOn > 30000)) {
      //log.error("FSESL pubsub error!");
    }
  }

  /* def onMessage(message: Message) {
    log.debug(s"message received: $message")
  }

  def onPMessage(pmessage: PMessage) {
    log.debug(s"pattern message received: $pmessage")

    val msg = decoder.decodeMessage(pmessage.data)

    if (msg != null) {
      msg match {
        case m: PubSubPongMessage => {
          if (m.payload.system == "BbbFsESL") {
            lastPongReceivedOn = System.currentTimeMillis()
          }
        }
        case _ => // do nothing
      }
    } else {
      msgReceiver.handleMessage(pmessage.patternMatched, pmessage.channel, pmessage.data)
    }

  }*/

  private class PubSubListener extends MessageListener {

    def onMessage(message: Message): Unit = {

      var msg = ""
      var topic = ""
      if (message.isInstanceOf[TextMessage]) {
        msg = message.asInstanceOf[TextMessage].getText()
        val t = message.getJMSDestination().asInstanceOf[Topic]
        topic = t.getTopicName()
        //log.debug("ActiveMQ message received: $msg")

        val dmsg = decoder.decodeMessage(msg)

        if (dmsg != null) {
          dmsg match {
            case m: PubSubPongMessage => {
              if (m.payload.system == "BbbFsESL") {
                lastPongReceivedOn = System.currentTimeMillis()
              }
            }
            case _ => // do nothing
          }
        } else {
          msgReceiver.handleMessage(patterns, topic, msg)
        }

      }

    }
  }

  def handleMessage(msg: String) {
    //log.warning("**** TODO: Handle pubsub messages. ****")
  }
}