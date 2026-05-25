package com.tianji.aigc.controller;

import com.tianji.aigc.service.ChatSessionService;
import com.tianji.aigc.vo.ChatSessionVO;
import com.tianji.aigc.vo.MessageVO;
import com.tianji.aigc.vo.SessionVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/session")
@RequiredArgsConstructor
public class SessionController {
    private final ChatSessionService chatSessionService;

    /**
     * 新建会话
     */
    @PostMapping
    public SessionVO createSession(@RequestParam(defaultValue = "3") Integer n) {
        return this.chatSessionService.createSession(n);
    }
    /**
     * 获取热门会话
     *
     */
    @GetMapping("/hot")
    public List<SessionVO.Example> hotExamples(@RequestParam(value = "n", defaultValue = "3") Integer num) {
        return this.chatSessionService.hotExamples(num);
    }

    /**
     *
     * @param sessionId
     * @return
     */
    @GetMapping("/{sessionId}")
    public List<MessageVO> queryBySessionId(@PathVariable("sessionId") String sessionId) {
        return this.chatSessionService.queryBySessionId(sessionId);
    }

    @GetMapping("/history")
    public Map<String,List<ChatSessionVO>> queryHistorySession(){
        return this.chatSessionService.queryHistorySession();
    }

    @DeleteMapping("/history")
    public void deleteHistorySession(@RequestParam("sessionId") String sessionId) {
        this.chatSessionService.deleteHistorySession(sessionId);
    }

    @PutMapping("/history")
    public void updateHistorySessionTitle(@RequestParam("sessionId") String sessionId,
                                          @RequestParam("title") String title) {
        this.chatSessionService.updateHistorySessionTitle(sessionId, title);
    }

}
