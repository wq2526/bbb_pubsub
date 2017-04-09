package org.bigbluebutton.lib.main.commands {
	
	import org.bigbluebutton.lib.main.models.IUserSession;
	
	import robotlegs.bender.bundles.mvcs.Command;
	
	public class DisconnectUserCommand extends Command {
		
		[Inject]
		public var disconnectionStatusCode:int;
		
		[Inject]
		public var userSession:IUserSession;
		
		public function DisconnectUserCommand() {
			super();
		}
		
		override public function execute():void {
			//userUISession.pushPage(PagesENUM.DISCONNECT, disconnectionStatusCode);
			userSession.mainConnection.connection.close();
			userSession.videoConnection.connection.close();
			userSession.voiceConnection.connection.close();
		}
	}
}
