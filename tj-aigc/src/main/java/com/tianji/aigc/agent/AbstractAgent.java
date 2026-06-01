package com.tianji.aigc.agent;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.tianji.aigc.config.ToolResultHolder;
import com.tianji.aigc.constants.Constant;
import com.tianji.aigc.enums.ChatEventTypeEnum;
import com.tianji.aigc.service.ChatService;
import com.tianji.aigc.service.ChatSessionService;
import com.tianji.aigc.vo.ChatEventVO;
import com.tianji.common.utils.UserContext;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.beans.factory.annotation.Autowired;
import reactor.core.publisher.Flux;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 *  agent的父类
 */
@Slf4j

public abstract class AbstractAgent implements Agent {
//    @Autowired ide检测当前类必须是bean
    @Resource
    private ChatSessionService chatSessionService;
    @Resource
    private ChatClient chatClient;
    @Resource
    private ChatMemory chatMemory;
    // 输出结束的标记
    public static final ChatEventVO STOP_EVENT = ChatEventVO.builder().eventType(ChatEventTypeEnum.STOP.getValue()).build();
    //支持多线程安全访问
    private static final Map<String, Boolean> GENERATE_STATUS = new ConcurrentHashMap<>();

    @Override
    public void stop(String sessionId) {
        GENERATE_STATUS.remove(sessionId);//删除
    }

    /**
     *
     * @param question  用户输入的问题
     * @param sessionId 会话唯一标识
     * @return
     */
    @Override
    public String process(String question, String sessionId) {
        Long userId = UserContext.getUser();
        String requestId = this.generateRequestId();
        chatSessionService.update(sessionId,question,userId);

        return this.getChatClientRequest(sessionId, requestId, question)
                .call()
                .content();

    }

    @Override
    public Flux<ChatEventVO> processStream(String question, String sessionId) {
        Long userId = UserContext.getUser();
        String requestId = this.generateRequestId();
        //输出缓存
        StringBuilder outputBuilder = new StringBuilder();
        //对话id
        String conversationId = ChatService.getConversationId(sessionId);

        chatSessionService.update(sessionId, question, userId);

        return this.getChatClientRequest(sessionId,requestId,question)
                .stream()
                .chatResponse()
                .doFirst(() ->GENERATE_STATUS.put(sessionId,true))
                .doOnError(throwable -> GENERATE_STATUS.remove(sessionId))
                .doOnComplete(()->GENERATE_STATUS.remove(sessionId))
                .doOnCancel(()->{
                        //保存历史上下文
                        this.saveStopHistoryRecord(conversationId, outputBuilder.toString());
                })
                .takeWhile(chatResponse -> {
                    // 通过返回值来控制Flux流是否继续，true：继续，false：终止
                    return GENERATE_STATUS.getOrDefault(sessionId,false);
                })
                .map(chatResponse -> {
                    String finishReason = chatResponse.getResult().getMetadata().getFinishReason();
                    if(StrUtil.equals(Constant.STOP,finishReason)){
                        // 对于响应结果进行处理，如果是最后一条数据，就把此次消息id放到内存中 ,让redis在根据requestid获取
                        String id = chatResponse.getMetadata().getId();
                        ToolResultHolder.put(id,Constant.REQUEST_ID,requestId);
                    }
                    //获取输出
                    String text = chatResponse.getResult().getOutput().getText();
                    outputBuilder.append(text);
                    return ChatEventVO.builder()
                            .eventData(text)
                            .eventType(ChatEventTypeEnum.DATA.getValue())
                            .build();
                })
                .concatWith(Flux.defer(()->{
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
    public Map<String, Object> advisorParams(String sessionId, String requestId) {
        //给advisor参数
        String conversationId = ChatService.getConversationId(sessionId); //用户——sessid存放对应参数
        return Map.of(ChatMemory.CONVERSATION_ID,conversationId);
    }

    private ChatClient.ChatClientRequestSpec getChatClientRequest(String sessionId, String requestId, String question){
        return this.chatClient.prompt()
                .system(promptSystem -> promptSystem.text(this.systemMessage()).params(this.systemMessageParams()))
                .advisors(advisorSpec -> advisorSpec.advisors(this.advisors()).params(this.advisorParams(sessionId,requestId))) //注入advisors，只需要重写对应方法即可
                .tools(this.tools()) //注入工具类
                .toolContext(this.toolContext(sessionId,requestId))
                .user(question);
    }

    private String generateRequestId(){
        return IdUtil.fastSimpleUUID();
    }

    private void saveStopHistoryRecord(String conversationId, String content) {
        chatMemory.add(conversationId, new AssistantMessage(content));
    }
}
