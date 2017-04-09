package org.bigbluebutton.freeswitch.voice;

public interface IVoiceConferenceService {
	void voiceConfRecordingStarted(String voiceConfId, String recordStream, Boolean recording, String timestamp);	
	void userJoinedVoiceConf(String voiceConfId, String voiceUserId, String userId, String callerIdName, 
			String callerIdNum, Boolean muted, Boolean speaking);
	void userLeftVoiceConf(String voiceConfId, String voiceUserId);
	void userLockedInVoiceConf(String voiceConfId, String voiceUserId, Boolean locked);
	void userMutedInVoiceConf(String voiceConfId, String voiceUserId, Boolean muted);
	void userTalkingInVoiceConf(String voiceConfId, String voiceUserId, Boolean talking);

}
