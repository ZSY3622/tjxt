package com.tianji.aigc.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tianji.aigc.entity.ChatRecord;
import com.tianji.aigc.mapper.ChatRecordMapper;
import com.tianji.aigc.service.ChatRecordService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ChatRecordServiceImpl extends ServiceImpl<ChatRecordMapper, ChatRecord> implements ChatRecordService {

    @Override
    public List<String> findConversationIds() {
        return lambdaQuery()
                .select(ChatRecord::getConversationId)
                .isNotNull(ChatRecord::getConversationId)
                .list()
                .stream()
                .map(ChatRecord::getConversationId)
                .distinct()
                .toList();
    }

    @Override
    public void removeByConversationId(String conversationId) {
        lambdaUpdate()
                .eq(ChatRecord::getConversationId, conversationId)
                .remove();
    }


}
