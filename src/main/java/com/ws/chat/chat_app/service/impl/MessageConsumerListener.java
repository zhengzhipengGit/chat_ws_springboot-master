package com.ws.chat.chat_app.service.impl;

import com.google.gson.Gson;
import com.ws.chat.chat_app.constants.Constants;
import com.ws.chat.chat_app.mongo.pojo.MessageLog;
import com.ws.chat.chat_app.websocket.MyWebSocketHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import javax.annotation.Resource;
import java.io.IOException;

/**
 * Created by fangjicai on 2020/4/3.
 */

@Component
@Slf4j
public class MessageConsumerListener {
    @Resource
    private KafkaTemplate<String,String> kafkaTemplate;

    @Resource
    private MyWebSocketHandler webSocketHandler;

    @KafkaListener(topics = {Constants.CHAT_TOPIC})
    public void receive(@Payload String message, @Header(KafkaHeaders.RECEIVED_TOPIC) String topic) {
        MessageLog msg = new Gson().fromJson(message, MessageLog.class);
        //获取fromId 如果是-1则广播
        Long from = msg.getFrom();
        if (from == -1) {
            try {
                webSocketHandler.broadcast(new TextMessage(message));
            } catch (IOException e) {
                log.error(e.getMessage());
            }
        } else {
            //获取目标用户ID
            Long toId = msg.getTo();

            WebSocketSession session = MyWebSocketHandler.userSocketSessionMap.get(toId);
            if (session != null) {
                try {
                    session.sendMessage(new TextMessage(message));
                } catch (IOException e) {
                    log.error(e.getMessage());
                }
            }
        }
    }
}
