package com.tianji.aigc.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.tianji.aigc.entity.ChatSession;
import com.tianji.aigc.vo.MessageVO;
import com.tianji.aigc.vo.SessionVO;

import java.util.List;

public interface ChatSessionService extends IService<ChatSession> {

    SessionVO createSession(Integer n);

    List<SessionVO.Example> hotExamples(Integer num);

    List<MessageVO> queryBySessionId(String sessionId);
}
