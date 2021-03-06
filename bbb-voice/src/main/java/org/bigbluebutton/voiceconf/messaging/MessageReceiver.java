package org.bigbluebutton.voiceconf.messaging;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import org.red5.logging.Red5LoggerFactory;
import org.slf4j.Logger;
/*import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPubSub;*/

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
	
	//private JedisPool redisPool;
	private ConnectionFactory connectionFactory;
	private String host;
	private int port;
	private volatile boolean receiveMessage = false;
	
	private final Executor msgReceiverExec = Executors.newSingleThreadExecutor();

	public void stop() {
		receiveMessage = false;
	}
	
	public void start() {
		connectionFactory = new ActiveMQConnectionFactory("tcp://" + host + ":" + port);
		log.info("Ready to receive messages from ActiveMQ pubsub.");
		try {
			receiveMessage = true;
			//final Jedis jedis = redisPool.getResource();
			
			Runnable messageReceiver = new Runnable() {
			    public void run() {
			    	if (receiveMessage) {
			    		//jedis.psubscribe(new PubSubListener(), MessagingConstants.TO_BBB_APPS_PATTERN); 
						try {
			    			Connection connection = connectionFactory.createConnection();
			                connection.start();

			                Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
			                Topic topic = new ActiveMQTopic(MessagingConstants.TO_BBB_APPS_TOPICS);

			                MessageConsumer consumer = session.createConsumer(topic);
			                consumer.setMessageListener(new PubSubListener());
						} catch (JMSException e) {
							// TODO Auto-generated catch block
							log.error("Cannot connect ActiveMQ");
						}
			    	}
			    }
			};
			msgReceiverExec.execute(messageReceiver);
		} catch (Exception e) {
			log.error("Error subscribing to channels: " + e.getMessage());
		}			
	}
	
	/*public void setRedisPool(JedisPool redisPool){
		this.redisPool = redisPool;
	}*/

	public void setHost(String host) {
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
			                handler.handleMessage(MessagingConstants.TO_BBB_APPS_PATTERN, topic, msg);
			            }
			        } catch (JMSException e) {
			            log.error("Got a JMS Exception!");
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
