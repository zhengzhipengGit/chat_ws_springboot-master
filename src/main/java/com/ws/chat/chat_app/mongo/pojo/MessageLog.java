package com.ws.chat.chat_app.mongo.pojo;

import com.ws.chat.chat_app.po.Message;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * Created by fangjicai on 2020/4/3.
 */
@Document(collection = "message")
@Data
public class MessageLog extends Message {
    @Id
    private String id;
    /**
     * 0:未发送 1.已发送
     */
    private Integer status;
    /**
     * 消息类型 0：单发 1.群发
     */
    private Integer type;
}
