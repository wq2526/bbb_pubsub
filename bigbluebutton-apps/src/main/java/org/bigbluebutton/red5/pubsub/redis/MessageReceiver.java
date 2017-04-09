package org.bigbluebutton.red5.pubsub.redis;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import org.bigbluebutton.common.messages.MessagingConstants;
import org.red5.logging.Red5LoggerFactory;
import org.slf4j.Logger;

/*import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPubSub;
import redis.clients.jedis.exceptions.JedisConnectionException;*/

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageListener;
import javax.jms.Session;
import javax.jms.TextMessage;
import javax.jms.Topic;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.command.ActiveMQTopic;

public class MessageReceiver {
	private static Logger log = Red5LoggerFactory.getLogger(MessageReceiver.class, "bigbluebutton");
	
	private ReceivedMessageHandler handler;
	
	//private Jedis jedis;
	private ConnectionFactory connectionFactory;
	private volatile boolean receiveMessage = false;
	
	private final Executor msgReceiverExec = Executors.newSingleThreadExecutor();

	private String host;
	private int port;
	
	public void stop() {
		receiveMessage = false;
	}
	
	public void start() {
		log.info("Ready to receive messages from ActiveMQ pubsub.");
		try {
			receiveMessage = true;
			//jedis = new Jedis(host, port);
			// Set the name of this client to be able to distinguish when doing
			// CLIENT LIST on redis-cli
			//jedis.clientSetname("BbbRed5AppsSub");
			connectionFactory = new ActiveMQConnectionFactory("tcp://" + host + ":" + port);
			
			Runnable messageReceiver = new Runnable() {
			    public void run() {
			    	if (receiveMessage) {
			    		try {
			    			//jedis.psubscribe(new PubSubListener(),MessagingConstants.FROM_BBB_APPS_PATTERN);
							Connection connection = connectionFactory.createConnection();
			                connection.start();

			                Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
			                Topic topic = new ActiveMQTopic(MessagingConstants.FROM_BBB_APPS_TOPICS);

			                MessageConsumer consumer = session.createConsumer(topic);
			                consumer.setMessageListener(new PubSubListener());
			    		} catch(JMSException ex) {
			    			log.warn("Exception on ActiveMQ connection. Resubscribing to pubsub.");
			    			start();
			    		}
			    		 
			    	}
			    }
			};
			msgReceiverExec.execute(messageReceiver);
		} catch (Exception e) {
			log.error("Error subscribing to channels: " + e.getMessage());
		}			
	}
	
	public void setHost(String host){
		this.host = host;
	}
	
	public void setPort(int port) {
		this.port = port;
	}
	
	public void setMessageHandler(ReceivedMessageHandler handler) {
		this.handler = handler;
	}

	private class PubSubListener implements MessageListener {

		@Override
		public void onMessage(Message message) {
			// TODO Auto-generated method stub
			
			
					String msg = "";
					String topic = "";
					try {
			            if (message instanceof TextMessage) {
			            	msg = ((TextMessage) message).getText();
			                Topic t = (Topic) message.getJMSDestination();
			                topic = t.getTopicName();
			                handler.handleMessage(MessagingConstants.FROM_BBB_APPS_PATTERN, topic, msg);
			            }
			        } catch (JMSException e) {
			            log.warn("Got a JMS Exception!");
			        }
				
		}

	}
	
	/*private class PubSubListener extends JedisPubSub {
		
		public PubSubListener() {
			super();			
		}

		@Override
		public void onMessage(String channel, String message) {
			// Not used.
		}

		@Override
		public void onPMessage(String pattern, String channel, String message) {
			handler.handleMessage(pattern, channel, message);			
		}

		@Override
		public void onPSubscribe(String pattern, int subscribedChannels) {
			log.debug("Subscribed to the pattern: " + pattern);
		}

		@Override
		public void onPUnsubscribe(String pattern, int subscribedChannels) {
			// Not used.
		}

		@Override
		public void onSubscribe(String channel, int subscribedChannels) {
			// Not used.
		}

		@Override
		public void onUnsubscribe(String channel, int subscribedChannels) {
			// Not used.
		}		
	}*/
}
