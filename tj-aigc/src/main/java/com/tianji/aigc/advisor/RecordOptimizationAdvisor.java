package com.tianji.aigc.advisor;

import cn.hutool.core.map.MapUtil;
import com.tianji.aigc.enums.AgentTypeEnum;
import com.tianji.aigc.memory.MyChatMemoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.chat.client.advisor.api.AdvisorChain;
import org.springframework.ai.chat.client.advisor.api.BaseAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.model.ChatResponse;

@RequiredArgsConstructor
public class RecordOptimizationAdvisor implements BaseAdvisor {
    private final MyChatMemoryRepository myChatMemoryRepository;

    @Override
    public ChatClientRequest before(ChatClientRequest chatClientRequest, AdvisorChain advisorChain) {
        return chatClientRequest;
    }

    @Override
    public ChatClientResponse after(ChatClientResponse chatClientResponse, AdvisorChain advisorChain) {
        // 获取大模型的相应文本
        ChatResponse chatResponse = chatClientResponse.chatResponse();
        String text = chatResponse.getResult().getOutput().getText();
        AgentTypeEnum agentTypeEnum = AgentTypeEnum.agentNameOf(text);
        // 返回属于 Agent名称
        if (null != agentTypeEnum){
            //需要优化
            String conversationId = MapUtil.getStr(chatClientResponse.context(), ChatMemory.CONVERSATION_ID);
            myChatMemoryRepository.optimization(conversationId);
        }
        return chatClientResponse;
    }

    @Override
    public int getOrder() {
        return Advisor.DEFAULT_CHAT_MEMORY_PRECEDENCE_ORDER-100;
    }
}
