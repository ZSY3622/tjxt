package com.tianji.aigc.memory;

import cn.hutool.core.convert.Convert;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.json.JSONUtil;
import com.alibaba.fastjson.JSON;
import com.tianji.aigc.config.ToolResultHolder;
import com.tianji.aigc.constants.Constant;
import com.tianji.common.utils.BeanUtils;
import org.springframework.ai.chat.messages.*;

import java.util.Map;

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
            //大模型发过来的消息
            myMessage.setToolCalls(assistantMessage.getToolCalls()); //设置调用工具字段
            //获取工具调用结果，放到params
            String messageId = Convert.toStr(assistantMessage.getMetadata().get(Constant.ID));
            String requestId = Convert.toStr(ToolResultHolder.get(messageId,Constant.REQUEST_ID));
            Map<String, Object> paramsMap = ToolResultHolder.get(requestId);
            if (ObjectUtil.isNotEmpty(paramsMap)) {
                myMessage.setParams(paramsMap); //序列化加入参数
            }
            ToolResultHolder.remove(messageId);
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
                //需要将params也序列化
//                return new AssistantMessage(myMessage.getTextContent(), myMessage.getMetadata(), myMessage.getToolCalls());
                return new MyAssistantMessage(myMessage.getTextContent(), myMessage.getMetadata(), myMessage.getToolCalls(),myMessage.getMedia(),myMessage.getParams());
            }
            case TOOL -> {
                return new ToolResponseMessage(myMessage.getToolResponses(), myMessage.getMetadata());
            }
        }
        throw new RuntimeException("Message data conversion failed.");//没有对应类型
    }
}
