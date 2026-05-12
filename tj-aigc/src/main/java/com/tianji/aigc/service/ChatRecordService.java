package com.tianji.aigc.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.tianji.aigc.entity.ChatRecord;

import java.util.List;
import java.util.Map;

public interface ChatRecordService extends IService<ChatRecord> {
    /**
     * 返回所有对话ids
     * @return
     */
    List<String> findConversationIds();

    /**
     * 根据对话id删除记录
     * @param conversationId
     */
    void removeByConversationId(String conversationId);
}
