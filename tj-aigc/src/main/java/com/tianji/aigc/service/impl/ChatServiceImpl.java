package com.tianji.aigc.service.impl;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.tianji.aigc.config.SystemPromptConfig;
import com.tianji.aigc.enums.ChatEventTypeEnum;
import com.tianji.aigc.service.ChatService;
import com.tianji.aigc.vo.ChatEventVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
@RequiredArgsConstructor
public class ChatServiceImpl implements ChatService {
//    @Qualifier("chatClient1") 用于指定bean
    private final ChatMemory chatMemory;

    private final ChatClient chatClient;
    private final SystemPromptConfig systemPromptConfig;
    //支持多线程安全访问
    private static final Map<String, Boolean> GENERATE_STATUS = new ConcurrentHashMap<>();


    @Override
    public Flux<ChatEventVO> chat(String sessionId, String question) {
        //获取对话id
        String conversationId = ChatService.getConversationId(sessionId);
        //输出缓存
        StringBuilder outputBuilder = new StringBuilder();


        return this.chatClient.prompt()
                .system(promptSystem -> promptSystem.text(systemPromptConfig.getChatSystemMessage().get()) //设置提示词
                        .param("now", DateUtil.now()) //设置提示词中的时间参数
                )
                .advisors(advisor -> advisor.param(ChatMemory.CONVERSATION_ID,conversationId))// 生成对话id的逻辑
                .user(question)
                .stream()
                .chatResponse()
                .doFirst(()->GENERATE_STATUS.put(sessionId,true))//第一次生成时候执行
                .doOnError(throwable -> GENERATE_STATUS.remove(sessionId)) // 出现异常时，删除标识
                .doOnComplete(() -> GENERATE_STATUS.remove(sessionId)) // 完成时执行，删除标识
                .doOnCancel(()->{
                    saveStopHistoryRecord(conversationId,outputBuilder.toString());
                })//中断输出,执行
                .takeWhile(chatResponse -> GENERATE_STATUS.getOrDefault(sessionId,false)) //根据sessionIdd 状态来判断是否停止生成
                .map(chatResponse -> {
                    // 获取大模型的输出的文字内容
                    String text = chatResponse.getResult().getOutput().getText();
                    outputBuilder.append(text); //加入缓存
                    // 封装响应对象
                    return ChatEventVO.builder()
                            .eventData(text)
                            .eventType(ChatEventTypeEnum.DATA.getValue())
                            .build();
                }).concatWith(Flux.just(ChatEventVO.builder()  // 最后加入标记输出结束
                        .eventType(ChatEventTypeEnum.STOP.getValue())
                        .build()));
    }

    @Override
    public void stop(String sessionId) {
//        GENERATE_STATUS.put(sessionId,false);
        GENERATE_STATUS.remove(sessionId);//删除

    }

    private void saveStopHistoryRecord(String conversationId, String content) {
        chatMemory.add(conversationId, new AssistantMessage(content));
    }


}
