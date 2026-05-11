package com.tianji.aigc.controller;

import com.tianji.aigc.dto.ChatDTO;
import com.tianji.aigc.service.ChatService;
import com.tianji.aigc.vo.ChatEventVO;
import com.tianji.common.annotations.NoWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;
import reactor.core.publisher.Flux;

import java.util.Map;

@RestController
@RequestMapping("/chat")
@Slf4j
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    @PostMapping("")
    @NoWrapper
    public Flux<ChatEventVO> chat(@RequestBody ChatDTO chatDTO) {
        String sessionId = chatDTO == null ? null : chatDTO.getSessionId();
        String question = chatDTO == null ? null : chatDTO.getQuestion();
        return chatService.chat(sessionId, question);
    }

    @PostMapping("/stop")
    public void stop(@RequestParam String sessionId){
        chatService.stop(sessionId);
    }
}
