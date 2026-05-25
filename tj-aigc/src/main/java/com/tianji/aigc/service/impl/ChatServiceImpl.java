package com.tianji.aigc.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.tianji.aigc.config.SystemPromptConfig;
import com.tianji.aigc.config.ToolResultHolder;
import com.tianji.aigc.constants.Constant;
import com.tianji.aigc.enums.ChatEventTypeEnum;
import com.tianji.aigc.service.ChatService;
import com.tianji.aigc.service.ChatSessionService;
import com.tianji.aigc.vo.ChatEventVO;
import com.tianji.common.utils.UserContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
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

    private final VectorStore vectorStore;

    private final ChatClient chatClient;
    private final SystemPromptConfig systemPromptConfig;
    //支持多线程安全访问
    private static final Map<String, Boolean> GENERATE_STATUS = new ConcurrentHashMap<>();

    private final ChatSessionService chatSessionService;

    // 输出结束的标记
    private static final ChatEventVO STOP_EVENT = ChatEventVO.builder().eventType(ChatEventTypeEnum.STOP.getValue()).build();


    @Override
    public Flux<ChatEventVO> chat(String sessionId, String question) {
        //获取对话id
        String conversationId = ChatService.getConversationId(sessionId);
        //输出缓存
        StringBuilder outputBuilder = new StringBuilder();
        // 生成请求id
        String requestId = IdUtil.fastSimpleUUID();

        Long userId = UserContext.getUser();


        chatSessionService.update(sessionId,question,userId);

        //创建RA增强
        QuestionAnswerAdvisor questionAnswerAdvisor = QuestionAnswerAdvisor.builder(vectorStore)
                .searchRequest(
                        SearchRequest.builder().similarityThreshold(0.6d) //相似度阈值
                                .topK(6) //最多返回6条文档
                                .build())
                .build();


        return this.chatClient.prompt()
                .system(promptSystem -> promptSystem.text(systemPromptConfig.getChatSystemMessage().get()) //设置提示词
                        .param("now", DateUtil.now()) //设置提示词中的时间参数
                )
                .advisors(advisor ->
                        advisor.advisors(questionAnswerAdvisor) //设置RAG增强
                                .param(ChatMemory.CONVERSATION_ID, conversationId))// 生成对话id的逻辑
                .toolContext(Map.of(Constant.REQUEST_ID, requestId, Constant.USER_ID, userId)) // 通过工具上下文传递工具
                .user(question)
                .stream()
                .chatResponse()
                .doFirst(() -> GENERATE_STATUS.put(sessionId, true))//第一次生成时候执行
                .doOnError(throwable -> GENERATE_STATUS.remove(sessionId)) // 出现异常时，删除标识
                .doOnComplete(() -> GENERATE_STATUS.remove(sessionId)) // 完成时执行，删除标识
                .doOnCancel(() -> {
                    saveStopHistoryRecord(conversationId, outputBuilder.toString());
                })//中断输出,执行
                .takeWhile(chatResponse -> GENERATE_STATUS.getOrDefault(sessionId, false)) //根据sessionIdd 状态来判断是否停止生成
                .map(chatResponse -> {
                    // 对于响应结果进行处理，如果是最后一条数据，就把此次消息id放到内存中 ,因为工具也是最后输出给前端的
                    String finishReason = chatResponse.getResult().getMetadata().getFinishReason();
                    if (StrUtil.equals(Constant.STOP, finishReason)) {
                        String messageId = chatResponse.getMetadata().getId();
                        // 将消息id与请求id相关联
                        ToolResultHolder.put(messageId, Constant.REQUEST_ID, requestId);
                    }
                    // 获取大模型的输出的文字内容
                    String text = chatResponse.getResult().getOutput().getText();
                    outputBuilder.append(text); //加入缓存
                    // 封装响应对象
                    return ChatEventVO.builder()
                            .eventData(text)
                            .eventType(ChatEventTypeEnum.DATA.getValue())
                            .build();
                })
                .concatWith(Flux.defer(() -> {
                    // 通过请求id获取到参数列表，如果不为空，就将其追加到返回结果中
                    Map<String, Object> map = ToolResultHolder.get(requestId);
                    if (CollUtil.isNotEmpty(map)) {
                        ToolResultHolder.remove(requestId); // 清除参数列表
                        // 将通过tools得到对应的数据返回给前端
                        ChatEventVO chatEventVO = ChatEventVO.builder().eventData(map).eventType(ChatEventTypeEnum.PARAM.getValue()).build();
                        return Flux.just(chatEventVO, STOP_EVENT);
                    }
                    return Flux.just(STOP_EVENT);
                }));
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
