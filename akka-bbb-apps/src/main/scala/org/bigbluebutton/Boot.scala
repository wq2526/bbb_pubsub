package org.bigbluebutton

import akka.actor.{ ActorSystem, Props }
import scala.concurrent.duration._
import redis.RedisClient
import scala.concurrent.{ Future, Await }
import scala.concurrent.ExecutionContext.Implicits.global
import org.bigbluebutton.endpoint.redis.RedisPublisher
import org.bigbluebutton.endpoint.redis.KeepAliveRedisPublisher
import org.bigbluebutton.endpoint.redis.AppsRedisSubscriberActor
import org.bigbluebutton.core.api.MessageOutGateway
import org.bigbluebutton.core.api.IBigBlueButtonInGW
import org.bigbluebutton.core.BigBlueButtonInGW
import org.bigbluebutton.core.MessageSender
import org.bigbluebutton.core.pubsub.receivers.RedisMessageReceiver
import org.bigbluebutton.core.api.OutMessageListener2
import org.bigbluebutton.core.pubsub.senders._
import org.bigbluebutton.core.service.recorder.RedisDispatcher
import org.bigbluebutton.core.service.recorder.RecorderApplication

object Boot extends App with SystemConfiguration {

  implicit val system = ActorSystem("bigbluebutton-apps-system")

  val redisPublisher = new RedisPublisher(system)
  val msgSender = new MessageSender(redisPublisher)

  val redisDispatcher = new RedisDispatcher(redisHost, redisPort, redisPassword, keysExpiresInSec)
  val recorderApp = new RecorderApplication(redisDispatcher)
  recorderApp.start()

  val bbbInGW = new BigBlueButtonInGW(system, recorderApp, msgSender)
  val redisMsgReceiver = new RedisMessageReceiver(bbbInGW)

  //val redisSubscriberActor = system.actorOf(AppsRedisSubscriberActor.props(redisMsgReceiver), "redis-subscriber")
  val ActiveMQSubscriberActor = system.actorOf(AppsRedisSubscriberActor.props(redisMsgReceiver), "redis-subscriber")
  ActiveMQSubscriberActor ! "startSubscribe"

  val keepAliveRedisPublisher = new KeepAliveRedisPublisher(system, redisPublisher)

}