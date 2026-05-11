package com.tianji.aigc.memory;

import cn.hutool.json.JSONUtil;
import com.alibaba.fastjson.JSON;
import com.tianji.common.utils.BeanUtils;
import org.springframework.ai.chat.messages.*;

/**
 * 消息转换工具类，提供消息对象与JSON字符串之间的转换功能，主要用于Redis存储格式转换
 */
public class MessageUtil {
    /**
     * 序列化
     *
     * @param message
     * @return
     */
    public static String toJson(Message message) {
        MyMessage myMessage = BeanUtils.toBean(message, MyMessage.class);
        //设置消息类型
        myMessage.setTextContent(message.getText());
        if (message instanceof AssistantMessage assistantMessage) {
            myMessage.setToolCalls(assistantMessage.getToolCalls());
        }
        if (message instanceof ToolResponseMessage toolResponseMessage) {
            myMessage.setToolResponses(toolResponseMessage.getResponses());
        }
        return JSONUtil.toJsonStr(myMessage);
    }

    /**
     * 反序列化
     * @param json
     * @return
     */
    public static Message toMessage(String json) {
        MyMessage myMessage = JSONUtil.toBean(json, MyMessage.class);
        MessageType messageType = MessageType.valueOf(myMessage.getMessageType());
        switch (messageType) {
            case SYSTEM -> {
                return new SystemMessage(myMessage.getTextContent());
            }
            case USER -> {
                return UserMessage.builder()
                        .text(myMessage.getTextContent())
                        .metadata(myMessage.getMetadata())
                        .media(myMessage.getMedia())
                        .build();
            }
            case ASSISTANT -> {
                return new AssistantMessage(myMessage.getTextContent(), myMessage.getMetadata(), myMessage.getToolCalls());
            }
            case TOOL -> {
                return new ToolResponseMessage(myMessage.getToolResponses(), myMessage.getMetadata());
            }
        }
        throw new RuntimeException("Message data conversion failed.");//没有对应类型
    }
}
