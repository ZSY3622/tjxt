package com.tianji.aigc.service.impl;

import cn.hutool.extra.spring.SpringUtil;
import com.tianji.aigc.agent.AbstractAgent;
import com.tianji.aigc.agent.Agent;
import com.tianji.aigc.config.SystemPromptConfig;
import com.tianji.aigc.enums.AgentTypeEnum;
import com.tianji.aigc.enums.ChatEventTypeEnum;
import com.tianji.aigc.service.ChatService;
import com.tianji.aigc.vo.ChatEventVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 基于路由工作流
 */
@Slf4j
@Service
@ConditionalOnProperty(prefix = "tj.ai", name = "chat-type", havingValue = "ROUTE") // 条件注册
public class AgentServiceImpl implements ChatService {

    private final ChatClient openAiChatClient;

    private final SystemPromptConfig systemPromptConfig;

    //支持多线程安全访问
    private static final Map<String, Boolean> GENERATE_STATUS = new ConcurrentHashMap<>();

    public AgentServiceImpl(@Qualifier("openAiChatClient") ChatClient openAiChatClient,
                            SystemPromptConfig systemPromptConfig) {
        this.openAiChatClient = openAiChatClient;
        this.systemPromptConfig = systemPromptConfig;
    }

    @Override
    public Flux<ChatEventVO> chat(String sessionId, String question) {
        //先将问题发送给 router ，根据路由结果调用对应的智能体
        String routerResult = findAgentByType(AgentTypeEnum.ROUTE).process(question, sessionId);
        AgentTypeEnum agentTypeEnum = AgentTypeEnum.agentNameOf(routerResult);

        Agent agent = findAgentByType(agentTypeEnum);
        if (agent == null) {
            ChatEventVO chatEventVO = ChatEventVO.builder()
                    .eventType(ChatEventTypeEnum.DATA.getValue())
                    .eventData(routerResult)
                    .build();
            return Flux.just(chatEventVO, AbstractAgent.STOP_EVENT);
        }
        return agent.processStream(question,sessionId);


    }

    /**
     * 根据代理类型查找对应的Agent实例
     *
     * @param agentTypeEnum 要查找的代理类型
     * @return 与给定类型匹配的Agent实例，如果未找到或类型为null则返回null
     */
    private Agent findAgentByType(AgentTypeEnum agentTypeEnum){
        if (agentTypeEnum == null)
            return null;
        //返回所有类型为agent的bean
        Map<String, Agent> beansOfType = SpringUtil.getBeansOfType(Agent.class);
        for (Agent agent:beansOfType.values()){
            if (agentTypeEnum == agent.getAgentType()){
                return agent;
            }
        }
        return null;
    }




    /**
     * 停止生成
     *
     * @param sessionId 会话ID
     */
    @Override
    public void stop(String sessionId) {
        this.findAgentByType(AgentTypeEnum.ROUTE).stop(sessionId);
    }

    @Override
    public String chatText(String question) {
        log.info("chatText 使用 openAiChatClient 处理文本请求");
        return this.openAiChatClient.prompt()
                .system(promptSystemSpec -> promptSystemSpec.text(this.systemPromptConfig.getTextSystemMessage().get()))
                .user(question)
                .call()
                .content();
    }
}
