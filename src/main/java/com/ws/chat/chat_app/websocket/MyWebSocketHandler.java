package com.ws.chat.chat_app.websocket;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.ws.chat.chat_app.constants.Constants;
import com.ws.chat.chat_app.mongo.pojo.MessageLog;
import com.ws.chat.chat_app.po.Message;
import com.ws.chat.chat_app.service.LoginService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;


import javax.annotation.Resource;
import java.io.IOException;
import java.util.Date;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Socket处理器
 */
@Component
public class MyWebSocketHandler implements WebSocketHandler {

	@Resource
	private MongoTemplate mongoTemplate;

	@Resource
	private KafkaTemplate<String,String> kafkaTemplate;


	//用于保存HttpSession与WebSocketSession的映射关系
	public static final Map<Long, WebSocketSession> userSocketSessionMap;

	@Autowired
	LoginService loginservice;
	
	static {
		//juc包下ConcurrentHashMap map结构，支持高并发，效率高，而且线程安全
		userSocketSessionMap = new ConcurrentHashMap<Long, WebSocketSession>();
	}
	
	/**
	 * 建立连接后,把登录用户的id写入WebSocketSession
	 */
	@Override
	public void afterConnectionEstablished(WebSocketSession session)
			throws Exception {
		Long uid = (Long) session.getAttributes().get("uid");
		String username=loginservice.getnamebyid(uid);
		if (userSocketSessionMap.get(uid) == null) {
			userSocketSessionMap.put(uid, session);
			Message msg = new Message();
			msg.setFrom(0L);//0表示上线消息
			msg.setText(username);
			this.broadcast(new TextMessage(new GsonBuilder().setDateFormat("yyyy-MM-dd HH:mm:ss").create().toJson(msg)));
			//同时通过kafka通知其他节点该用户上线 TODO
		}
	}

	/**
	 * 消息处理，在客户端通过Websocket API发送的消息会经过这里，然后进行相应的处理
	 */
	@Override
	public void handleMessage(WebSocketSession session, WebSocketMessage<?> message) throws Exception {
			if(message.getPayloadLength()==0)
			return;
			Message msg=new Gson().fromJson(message.getPayload().toString(),Message.class);
			msg.setDate(new Date());
			sendMessageToUser(msg.getTo(), new TextMessage(new GsonBuilder().setDateFormat("yyyy-MM-dd HH:mm:ss").create().toJson(msg)));
	}

	/**
	 * 消息传输错误处理
	 */
	@Override
	public void handleTransportError(WebSocketSession session,
			Throwable exception) throws Exception {
		if (session.isOpen()) {
			session.close();
		}
		Iterator<Entry<Long, WebSocketSession>> it = userSocketSessionMap.entrySet().iterator();
		// 移除当前抛出异常用户的Socket会话
		while (it.hasNext()) {
			Entry<Long, WebSocketSession> entry = it.next();
			if (entry.getValue().getId().equals(session.getId())) {
				userSocketSessionMap.remove(entry.getKey());
				System.out.println("Socket会话已经移除:用户ID" + entry.getKey());
				String username=loginservice.getnamebyid(entry.getKey());
				Message msg = new Message();
				msg.setFrom(-2L);
				msg.setText(username);
				this.broadcast(new TextMessage(new GsonBuilder().setDateFormat("yyyy-MM-dd HH:mm:ss").create().toJson(msg)));
				break;
			}
		}
	}

	/**
	 * 关闭连接后
	 */
	@Override
	public void afterConnectionClosed(WebSocketSession session,CloseStatus closeStatus) throws Exception {
		System.out.println("Websocket:" + session.getId() + "已经关闭");
		Iterator<Entry<Long, WebSocketSession>> it = userSocketSessionMap.entrySet().iterator();
		// 移除当前用户的Socket会话
		while (it.hasNext()) {
			Entry<Long, WebSocketSession> entry = it.next();
			if (entry.getValue().getId().equals(session.getId())) {
				userSocketSessionMap.remove(entry.getKey());
				System.out.println("Socket会话已经移除:用户ID" + entry.getKey());
				String username=loginservice.getnamebyid(entry.getKey());
				Message msg = new Message();
				msg.setFrom(-2L);//下线消息，用-2表示
				msg.setText(username);
				this.broadcast(new TextMessage(new GsonBuilder().setDateFormat("yyyy-MM-dd HH:mm:ss").create().toJson(msg)));
				break;
			}
		}
	}

	@Override
	public boolean supportsPartialMessages() {
		return false;
	}

	/**
	 * 给所有在线用户发送消息
	 * @param message
	 * @throws IOException
	 */
	public void broadcast(final TextMessage message) throws IOException {
		Iterator<Entry<Long, WebSocketSession>> it = userSocketSessionMap.entrySet().iterator();
		//消息持久化的mongo下，并保存消息状态为未读
		MessageLog msg=new Gson().fromJson(message.getPayload().toString(), MessageLog.class);
		msg.setStatus(0);
		msg.setType(1);
		//kafka群发消息
		this.kafkaTemplate.send(Constants.CHAT_TOPIC,new GsonBuilder().create().toJson(msg));
		//多线程群发
		while (it.hasNext()) {

			final Entry<Long, WebSocketSession> entry = it.next();
			//判断该WebSocketSession是否关闭
			if (entry.getValue().isOpen()) {
				// entry.getValue().sendMessage(message);
				//每个发送动作对应的一个线程，做到并发发送
				new Thread(new Runnable() {

					public void run() {
						try {
							if (entry.getValue().isOpen()) {
								//调用senmsg方法发送消息
								entry.getValue().sendMessage(message);
							}
						} catch (IOException e) {
							e.printStackTrace();
						}
					}

				}).start();
			}

		}
	}

	/**
	 * 给某个用户发送消息
	 * 
	 * @param uid
	 * @param message
	 * @throws IOException
	 */
	public void sendMessageToUser(Long uid, TextMessage message) throws IOException {
		//消息持久化的mongo下，并保存消息状态为未读
		MessageLog msg=new Gson().fromJson(message.getPayload().toString(), MessageLog.class);
		msg.setType(0);
		WebSocketSession session = userSocketSessionMap.get(uid);
		if (session != null && session.isOpen()) {
			session.sendMessage(message);
			//存入mongo下，并修改信息为已发送
			msg.setStatus(1);
			this.mongoTemplate.save(msg);
		}else{
			//当前jvm实例下没有接受用户关联的websocketsession时，进行广播通知 kafka广播 TODO
			msg.setStatus(0);
			this.mongoTemplate.save(msg);
			this.kafkaTemplate.send(Constants.CHAT_TOPIC,new GsonBuilder().create().toJson(msg));
		}
	}

}
