package org.bigbluebutton.air.deskshare.views {
	
	import flash.net.NetConnection;
	
	import spark.components.Group;
	import spark.components.Label;
	
	public interface IDeskshareView {
		function get deskshareGroup():Group;
		function stopStream():void;
		function startStream(connection:NetConnection, name:String, streamName:String, userID:String, width:Number, height:Number):void;
		function get noDeskshareMessage():Label;
		function changeMouseLocation(x:Number, y:Number):void;
	}
}
