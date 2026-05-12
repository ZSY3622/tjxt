package com.tianji.aigc.memory;

import cn.hutool.core.convert.Convert;
import cn.hutool.core.util.StrUtil;
import com.tianji.aigc.entity.ChatRecord;
import com.tianji.aigc.service.ChatRecordService;
import com.tianji.common.utils.UserContext;
import jakarta.annotation.Resource;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.messages.Message;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Map;

public class MysqlChatMemoryRepository implements ChatMemoryRepository {
    @Resource
    private ChatRecordService chatRecordService;

    @Override
    public List<String> findConversationIds() {
        return chatRecordService.findConversationIds();
    }

    @Override
    public List<Message> findByConversationId(String conversationId) {
        List<ChatRecord> chatRecordList = chatRecordService.lambdaQuery()
                .eq(ChatRecord::getConversationId, conversationId)
                .orderByAsc(ChatRecord::getCreateTime)
                .list();
        return chatRecordList.stream().map(chatRecord -> MessageUtil.toMessage(chatRecord.getData())).toList();
    }

    @Override
    public void saveAll(String conversationId, List<Message> messages) {
        //先删除
        deleteByConversationId(conversationId);
        //通过对话id获取用户id
        Long userId = Convert.toLong(StrUtil.subBefore(conversationId, "_", false));


        //保存
        List<ChatRecord> list = messages.stream().map(message -> ChatRecord.builder()
                        .data(MessageUtil.toJson(message))
                        .conversationId(conversationId)
                        .creater(userId)
                        .updater(userId)
                        .build())
                .toList();
        chatRecordService.saveBatch(list);
    }

    @Override
    public void deleteByConversationId(String conversationId) {
        chatRecordService.removeByConversationId(conversationId);
    }
}
