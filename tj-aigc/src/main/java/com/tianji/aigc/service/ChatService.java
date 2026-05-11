package com.tianji.aigc.service;

import com.tianji.aigc.vo.ChatEventVO;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;
import reactor.core.publisher.Flux;

public interface ChatService {


    Flux<ChatEventVO> chat(String sessionId, String question);
}
