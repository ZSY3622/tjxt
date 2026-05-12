package com.tianji.aigc.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.RandomUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tianji.aigc.entity.ChatSession;
import com.tianji.aigc.enums.MessageTypeEnum;
import com.tianji.aigc.mapper.ChatSessionMapper;
import com.tianji.aigc.service.ChatService;
import com.tianji.aigc.service.ChatSessionService;
import com.tianji.aigc.vo.MessageVO;
import com.tianji.aigc.vo.SessionVO;
import com.tianji.common.utils.UserContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.stereotype.Service;
import com.tianji.aigc.config.SessionProperties;

import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChatSessionServiceImpl extends ServiceImpl<ChatSessionMapper, ChatSession> implements ChatSessionService {

    private final SessionProperties sessionProperties;

    private final ChatMemory chatMemory;

    @Override
    public List<MessageVO> queryBySessionId(String sessionId) {
        String conversationId = ChatService.getConversationId(sessionId);
        List<Message> messages = chatMemory.get(conversationId);

        return messages.stream()
                .filter(message -> message.getMessageType() == MessageType.ASSISTANT || message.getMessageType() == MessageType.USER)//过滤掉非ai和用户消息
                .map(message -> MessageVO.builder()
                        .type(MessageTypeEnum.valueOf(message.getMessageType().name()))
                        .content(message.getText()).build())
                .toList();
    }

    @Override
    public SessionVO createSession(Integer n) {
        SessionVO sessionVO = BeanUtil.toBean(sessionProperties, SessionVO.class);
        // 随机获取examples
        sessionVO.setExamples(RandomUtil.randomEleList(sessionProperties.getExamples(),n));
        // 随机生成uuuid
        sessionVO.setSessionId(IdUtil.fastSimpleUUID());
        //将本次会话保存到数据库
        ChatSession build = ChatSession.builder().sessionId(sessionVO.getSessionId())
                .userId(UserContext.getUser())
                .build();
        this.save(build);
        return sessionVO;
    }

    @Override
    public List<SessionVO.Example> hotExamples(Integer num) {
        List<SessionVO.Example> examples = sessionProperties.getExamples();
        if (examples == null || examples.isEmpty()) {
            return Collections.emptyList();
        }
        int count = num == null ? 3 : num;
        if (count <= 0) {
            return Collections.emptyList();
        }
        return RandomUtil.randomEleList(examples, Math.min(count, examples.size()));
    }


}
