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

@Service
@Slf4j
@RequiredArgsConstructor
public class ChatServiceImpl implements ChatService {
//    @Qualifier("chatClient1") 用于指定bean
    private final ChatClient chatClient;
    private final SystemPromptConfig systemPromptConfig;

    @Override
    public Flux<ChatEventVO> chat(String sessionId, String question) {
        System.out.println(systemPromptConfig.getChatSystemMessage().get());

        return this.chatClient.prompt()
                .system(promptSystem -> promptSystem.text(systemPromptConfig.getChatSystemMessage().get()) //设置提示词
                        .param("now", DateUtil.now()) //设置提示词中的时间参数
                )
                .user(question)
                .stream()
                .chatResponse()
                .map(chatResponse -> {
                    // 获取大模型的输出的文字内容
                    String text = chatResponse.getResult().getOutput().getText();
                    // 封装响应对象
                    return ChatEventVO.builder()
                            .eventData(text)
                            .eventType(ChatEventTypeEnum.DATA.getValue())
                            .build();
                }).concatWith(Flux.just(ChatEventVO.builder()  // 最后加入标记输出结束
                        .eventType(ChatEventTypeEnum.STOP.getValue())
                        .build()));
    }


}
