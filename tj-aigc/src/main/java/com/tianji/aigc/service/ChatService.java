package com.tianji.aigc.service;

import com.tianji.aigc.vo.ChatEventVO;
import com.tianji.common.utils.UserContext;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;
import reactor.core.publisher.Flux;

public interface ChatService {


    Flux<ChatEventVO> chat(String sessionId, String question);

    void stop(String sessionId);

    /**
     * 获取对话id，规则：用户id_会话id
     *
     * @param sessionId 会话id
     * @return 对话id
     */
    static String getConversationId(String sessionId) {
        return UserContext.getUser() + "_" + sessionId;
    }
}
