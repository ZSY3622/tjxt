package com.tianji.aigc.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.tianji.aigc.entity.ChatSession;
import com.tianji.aigc.vo.ChatSessionVO;
import com.tianji.aigc.vo.MessageVO;
import com.tianji.aigc.vo.SessionVO;

import java.util.List;
import java.util.Map;

public interface ChatSessionService extends IService<ChatSession> {

    SessionVO createSession(Integer n);

    List<SessionVO.Example> hotExamples(Integer num);

    List<MessageVO> queryBySessionId(String sessionId);

    /**
     * 更新会话更新时间
     *
     * @param sessionId 会话ID，用于标识特定的聊天会话
     * @param title     新的会话标题，如果为空则不进行更新
     * @param userId    用户ID
     */
    void update(String sessionId, String title, Long userId);

    Map<String, List<ChatSessionVO>> queryHistorySession();

    /**
     * 删除指定历史会话，并清理该会话的聊天上下文。
     *
     * @param sessionId 会话id
     */
    void deleteHistorySession(String sessionId);

    /**
     * 更新历史会话标题。
     *
     * @param sessionId 会话id
     * @param title     会话标题，最大长度100
     */
    void updateHistorySessionTitle(String sessionId, String title);
}
