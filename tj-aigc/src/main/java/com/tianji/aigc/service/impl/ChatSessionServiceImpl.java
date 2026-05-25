package com.tianji.aigc.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tianji.aigc.entity.ChatSession;
import com.tianji.aigc.enums.MessageTypeEnum;
import com.tianji.aigc.mapper.ChatSessionMapper;
import com.tianji.aigc.memory.MyAssistantMessage;
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
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import com.tianji.aigc.config.SessionProperties;

import java.time.LocalDateTime;
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
                .map(message ->{
                    if (message instanceof MyAssistantMessage){
                       return MessageVO.builder()
                                .content(message.getText())
                                .type(MessageTypeEnum.valueOf(message.getMessageType().name()))
                                .params(((MyAssistantMessage) message).getParams())
                                .build();
                    }
                    return MessageVO.builder()
                            .content(message.getText())
                            .type(MessageTypeEnum.valueOf(message.getMessageType().name()))
                            .build();
                }).toList();
    }
    /**
     * 异步更新聊天会话的标题
     *
     * @param sessionId 会话ID，用于标识特定的聊天会话
     * @param title     新的会话标题，如果为空则不进行更新
     * @param userId    用户ID
     */
    @Override
    @Async //异步执行
    public void update(String sessionId, String title, Long userId) {
        //查找对应的聊天会话列表
        List<ChatSession> list = this.lambdaQuery()
                .eq(ChatSession::getSessionId, sessionId)
                .eq(ChatSession::getUserId, userId)
                .list();
        //没对应列表
        if (CollUtil.isEmpty(list)){
            return;
        }
        // 获取列表中的第一个聊天会话实例
        ChatSession chatSession = list.get(0);
        if (StrUtil.isEmpty(chatSession.getTitle()) && StrUtil.isNotEmpty(title)){
            chatSession.setTitle(StrUtil.sub(title, 0, 100));
            chatSession.setUpdateTime(LocalDateTime.now());
            super.updateById(chatSession);
        }
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
