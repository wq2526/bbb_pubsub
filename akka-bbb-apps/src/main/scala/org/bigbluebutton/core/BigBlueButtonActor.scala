package org.bigbluebutton.core

import akka.actor._
import akka.actor.ActorLogging
import akka.pattern.{ ask, pipe }
import akka.util.Timeout
import scala.concurrent.duration._
import scala.collection.mutable.HashMap
import org.bigbluebutton.core.api._
import org.bigbluebutton.core.util._
import org.bigbluebutton.core.api.ValidateAuthTokenTimedOut
import scala.util.Success
import scala.util.Failure
import org.bigbluebutton.SystemConfiguration
import org.bigbluebutton.core.recorders.events.VoiceUserJoinedRecordEvent
import org.bigbluebutton.core.recorders.events.VoiceUserLeftRecordEvent
import org.bigbluebutton.core.recorders.events.VoiceUserLockedRecordEvent
import org.bigbluebutton.core.recorders.events.VoiceUserMutedRecordEvent
import org.bigbluebutton.core.recorders.events.VoiceStartRecordingRecordEvent
import org.bigbluebutton.core.recorders.events.VoiceUserTalkingRecordEvent
import org.bigbluebutton.core.service.recorder.RecorderApplication
import scala.collection._
import com.google.gson.Gson

object BigBlueButtonActor extends SystemConfiguration {
  def props(system: ActorSystem, recorderApp: RecorderApplication, messageSender: MessageSender): Props =
    Props(classOf[BigBlueButtonActor], system, recorderApp, messageSender)
}

class BigBlueButtonActor(val system: ActorSystem, recorderApp: RecorderApplication, messageSender: MessageSender) extends Actor with ActorLogging {
  implicit def executionContext = system.dispatcher
  implicit val timeout = Timeout(5 seconds)

  private var meetings = new collection.immutable.HashMap[String, RunningMeeting]
  private val outGW = new OutMessageGateway("bbbActorOutGW", recorderApp, messageSender)

  def receive = {
    case msg: CreateMeeting => handleCreateMeeting(msg)
    case msg: DestroyMeeting => handleDestroyMeeting(msg)
    case msg: KeepAliveMessage => handleKeepAliveMessage(msg)
    case msg: PubSubPing => handlePubSubPingMessage(msg)
    case msg: ValidateAuthToken => handleValidateAuthToken(msg)
    case msg: GetAllMeetingsRequest => handleGetAllMeetingsRequest(msg)
    case msg: UserJoinedVoiceConfMessage => handleUserJoinedVoiceConfMessage(msg)
    case msg: UserLeftVoiceConfMessage => handleUserLeftVoiceConfMessage(msg)
    case msg: UserLockedInVoiceConfMessage => handleUserLockedInVoiceConfMessage(msg)
    case msg: UserMutedInVoiceConfMessage => handleUserMutedInVoiceConfMessage(msg)
    case msg: UserTalkingInVoiceConfMessage => handleUserTalkingInVoiceConfMessage(msg)
    case msg: VoiceConfRecordingStartedMessage => handleVoiceConfRecordingStartedMessage(msg)
    case msg: InMessage => handleMeetingMessage(msg)
    case _ => // do nothing
  }

  private def findMeetingWithVoiceConfId(voiceConfId: String): Option[RunningMeeting] = {
    meetings.values.find(m => m.mProps.voiceBridge == voiceConfId)
  }

  private def handleUserJoinedVoiceConfMessage(msg: UserJoinedVoiceConfMessage) {
    findMeetingWithVoiceConfId(msg.voiceConfId) foreach { m =>
      m.actorRef ! msg
    }
  }

  private def handleUserLeftVoiceConfMessage(msg: UserLeftVoiceConfMessage) {
    findMeetingWithVoiceConfId(msg.voiceConfId) foreach { m =>
      m.actorRef ! msg
    }
  }

  private def handleUserLockedInVoiceConfMessage(msg: UserLockedInVoiceConfMessage) {
    findMeetingWithVoiceConfId(msg.voiceConfId) foreach { m =>
      m.actorRef ! msg
    }
  }

  private def handleUserMutedInVoiceConfMessage(msg: UserMutedInVoiceConfMessage) {
    findMeetingWithVoiceConfId(msg.voiceConfId) foreach { m =>
      m.actorRef ! msg
    }
  }

  private def handleVoiceConfRecordingStartedMessage(msg: VoiceConfRecordingStartedMessage) {
    findMeetingWithVoiceConfId(msg.voiceConfId) foreach { m =>
      m.actorRef ! msg
    }

  }

  private def handleUserTalkingInVoiceConfMessage(msg: UserTalkingInVoiceConfMessage) {
    findMeetingWithVoiceConfId(msg.voiceConfId) foreach { m =>
      m.actorRef ! msg
    }

  }

  private def handleValidateAuthToken(msg: ValidateAuthToken) {
    meetings.get(msg.meetingID) foreach { m =>
      val future = m.actorRef.ask(msg)(5 seconds)

      future onComplete {
        case Success(result) => {
          log.info("Validate auth token response. meetingId=" + msg.meetingID + " userId=" + msg.userId + " token=" + msg.token)
          /**
           * Received a reply from MeetingActor which means hasn't hung!
           * Sometimes, the actor seems to hang and doesn't anymore accept messages. This is a simple
           * audit to check whether the actor is still alive. (ralam feb 25, 2015)
           */
        }
        case Failure(failure) => {
          log.warning("Validate auth token timeout. meetingId=" + msg.meetingID + " userId=" + msg.userId + " token=" + msg.token)
          outGW.send(new ValidateAuthTokenTimedOut(msg.meetingID, msg.userId, msg.token, false, msg.correlationId))
        }
      }
    }
  }

  private def handleMeetingMessage(msg: InMessage): Unit = {
    msg match {
      case ucm: UserConnectedToGlobalAudio => {
        val m = meetings.values.find(m => m.mProps.voiceBridge == ucm.voiceConf)
        m foreach { mActor => mActor.actorRef ! ucm }
      }
      case udm: UserDisconnectedFromGlobalAudio => {
        val m = meetings.values.find(m => m.mProps.voiceBridge == udm.voiceConf)
        m foreach { mActor => mActor.actorRef ! udm }
      }
      case allOthers => {
        meetings.get(allOthers.meetingID) match {
          case None => handleMeetingNotFound(allOthers)
          case Some(m) => {
            // log.debug("Forwarding message [{}] to meeting [{}]", msg.meetingID)
            m.actorRef ! allOthers
          }
        }
      }
    }
  }

  private def handleMeetingNotFound(msg: InMessage) {
    msg match {
      case vat: ValidateAuthToken => {
        log.info("No meeting [" + vat.meetingID + "] for auth token [" + vat.token + "]")
        outGW.send(new ValidateAuthTokenReply(vat.meetingID, vat.userId, vat.token, false, vat.correlationId))
      }
      case _ => {
        log.info("No meeting [" + msg.meetingID + "] for message type [" + msg.getClass() + "]")
        // do nothing
      }
    }
  }

  private def handleKeepAliveMessage(msg: KeepAliveMessage): Unit = {
    outGW.send(new KeepAliveMessageReply(msg.aliveID))
  }

  private def handlePubSubPingMessage(msg: PubSubPing): Unit = {
    //log.info("PubSubPing from [" + msg.system + "]")
    outGW.send(new PubSubPong(msg.system, msg.timestamp))
  }

  private def handleDestroyMeeting(msg: DestroyMeeting) {
    log.info("Received DestroyMeeting message for meetingId={}", msg.meetingID)
    meetings.get(msg.meetingID) match {
      case None => log.info("Could not find meetingId={}", msg.meetingID)
      case Some(m) => {
        meetings -= msg.meetingID
        log.info("Kick everyone out on meetingId={}", msg.meetingID)
        outGW.send(new EndAndKickAll(msg.meetingID, m.mProps.recorded))
        outGW.send(new DisconnectAllUsers(msg.meetingID))
        log.info("Destroyed meetingId={}", msg.meetingID)
        outGW.send(new MeetingDestroyed(msg.meetingID))

        // Stop the meeting actor.
        context.stop(m.actorRef)
      }
    }
  }

  private def handleCreateMeeting(msg: CreateMeeting): Unit = {
    meetings.get(msg.meetingID) match {
      case None => {
        log.info("Create meeting request. meetingId={}", msg.mProps.meetingID)
        val moutGW = new OutMessageGateway("meetingOutGW-" + msg.meetingID, recorderApp, messageSender)
        var m = RunningMeeting(msg.mProps, moutGW)

        meetings += m.mProps.meetingID -> m
        outGW.send(new MeetingCreated(m.mProps.meetingID, m.mProps.externalMeetingID, m.mProps.recorded, m.mProps.meetingName,
          m.mProps.voiceBridge, msg.mProps.duration, msg.mProps.moderatorPass,
          msg.mProps.viewerPass, msg.mProps.createTime, msg.mProps.createDate))

        m.actorRef ! new InitializeMeeting(m.mProps.meetingID, m.mProps.recorded)
        m.actorRef ! "StartTimer"
      }
      case Some(m) => {
        log.info("Meeting already created. meetingID={}", msg.mProps.meetingID)
        // do nothing
      }
    }
  }

  private def handleGetAllMeetingsRequest(msg: GetAllMeetingsRequest) {

    var len = meetings.keys.size
    println("meetings.size=" + meetings.size)
    println("len_=" + len)

    val set = meetings.keySet
    val arr: Array[String] = new Array[String](len)
    set.copyToArray(arr)
    val resultArray: Array[MeetingInfo] = new Array[MeetingInfo](len)

    for (i <- 0 until arr.length) {
      val id = arr(i)
      val duration = meetings.get(arr(i)).head.mProps.duration
      val name = meetings.get(arr(i)).head.mProps.meetingName
      val recorded = meetings.get(arr(i)).head.mProps.recorded
      val voiceBridge = meetings.get(arr(i)).head.mProps.voiceBridge

      var info = new MeetingInfo(id, name, recorded, voiceBridge, duration)
      resultArray(i) = info

      //send the users
      self ! (new GetUsers(id, "nodeJSapp"))

      //send the presentation
      self ! (new GetPresentationInfo(id, "nodeJSapp", "nodeJSapp"))

      //send chat history
      self ! (new GetChatHistoryRequest(id, "nodeJSapp", "nodeJSapp"))

      //send lock settings
      self ! (new GetLockSettings(id, "nodeJSapp"))
    }

    outGW.send(new GetAllMeetingsReply(resultArray))
  }

}
