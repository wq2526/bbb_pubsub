package org.bigbluebutton.api.messaging;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
/*import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.Protocol;*/

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.DeliveryMode;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.MessageProducer;
import javax.jms.Session;
import javax.jms.TextMessage;
import javax.jms.Topic;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.command.ActiveMQTopic;

public class MessageSender {
	private static Logger log = LoggerFactory.getLogger(MessageSender.class);
	
	//private JedisPool redisPool;
	private ConnectionFactory connectionFactory;
	private volatile boolean sendMessage = false;
	
	private final Executor msgSenderExec = Executors.newSingleThreadExecutor();
	private final Executor runExec = Executors.newSingleThreadExecutor();
	private BlockingQueue<MessageToSend> messages = new LinkedBlockingQueue<MessageToSend>();
	
	private String host;
	private int port;
	
	public void stop() {
		sendMessage = false;
		//redisPool.destroy();
	}
		
	public void start() {	
		GenericObjectPoolConfig config = new GenericObjectPoolConfig();
		config.setMaxTotal(32);
		config.setMaxIdle(8);
		config.setMinIdle(1);
		config.setTestOnBorrow(true);
		config.setTestOnReturn(true);
		config.setTestWhileIdle(true);
		config.setNumTestsPerEvictionRun(12);
		config.setMaxWaitMillis(5000);
		config.setTimeBetweenEvictionRunsMillis(60000);
		config.setBlockWhenExhausted(true);
		
		// Set the name of this client to be able to distinguish when doing
		// CLIENT LIST on redis-cli
		/*redisPool = new JedisPool(config, host, port, Protocol.DEFAULT_TIMEOUT, null,
		        Protocol.DEFAULT_DATABASE, "BbbWebPub");*/
		connectionFactory = new ActiveMQConnectionFactory("tcp://" + host + ":" + port);
		
		log.info("ActiveMQ message publisher starting!");
		try {
			sendMessage = true;
			
			Runnable messageSender = new Runnable() {
			  public void run() {
			   	while (sendMessage) {
				   	try {
				      MessageToSend msg = messages.take();
							publish(msg.getChannel(), msg.getMessage());
						} catch (InterruptedException e) {
							log.warn("Failed to get message from queue.");
						}    			    		
			    }
			  }
			};
			msgSenderExec.execute(messageSender);
		} catch (Exception e) {
			log.error("Error subscribing to channels: " + e.getMessage());
		}			
	}
	
	public void send(String channel, String message) {
		MessageToSend msg = new MessageToSend(channel, message);
		messages.add(msg);
	}
	
	private void publish(final String channel, final String message) {
		Runnable task = new Runnable() {
		  public void run() {
				//Jedis jedis = redisPool.getResource();
				Connection connection = null;
				try {
					//jedis.publish(channel, message);
					connection = connectionFactory.createConnection();
					connection.start();
					Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
					Topic t = new ActiveMQTopic(channel);
			
					MessageProducer producer = session.createProducer(t);
					producer.setDeliveryMode(DeliveryMode.PERSISTENT);
			
					TextMessage msg = session.createTextMessage(message);
			
					producer.send(msg);
				} catch(Exception e){
					log.warn("Cannot publish the message to pubsub", e);
				} finally {
					/*if (jedis != null) {
						jedis.close();
					}*/
					if (connection != null) {
						try {
	                		connection.close();
						} catch (JMSException e) {
							log.warn("Could not close an open connection...");
						}
					}
					
				}		  	
		  }
		};
		
		runExec.execute(task);
	}
	
	public void setHost(String host){
		this.host = host;
	}
	
	public void setPort(int port) {
		this.port = port;
	}
}
