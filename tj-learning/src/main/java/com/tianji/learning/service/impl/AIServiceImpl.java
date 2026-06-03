package com.tianji.learning.service.impl;

import cn.hutool.core.util.StrUtil;
import com.tianji.api.client.aigc.AigcClient;
import com.tianji.common.utils.UserContext;
import com.tianji.learning.domain.dto.ReplyDTO;
import com.tianji.learning.domain.po.InteractionQuestion;
import com.tianji.learning.service.AIService;
import com.tianji.learning.service.IInteractionReplyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class AIServiceImpl implements AIService {
    private final AigcClient aigcClient;
    private final IInteractionReplyService iInteractionReplyService;


    @Value("${tj.ai.user-id:9999}")
    private Long aiUserId;

    /**
     * 异步自动回复学生提出的互动问题
     *
     * @param interactionQuestion 互动问题对象，包含问题的标题、描述和唯一标识等信息
     * <p>
     * 实现说明：
     * 1. 使用格式化字符串构建包含问题标题和描述的查询内容
     * 2. 调用AI文本生成接口获取专业回复
     * 3. 构造系统回复DTO对象，设置系统用户ID（9999）和问题关联信息
     * 4. 通过服务层持久化回复数据
     */
    @Override
    @Async
    public void autoReply(InteractionQuestion interactionQuestion) {
        // 构建包含完整问题信息的查询模板（标题+描述）
        var question = StrUtil.format("""
                这是一个学生提出的问题，请以专业的角度进行回答，不要随意编造。
                标题：{} 。
                描述：{} 。""", interactionQuestion.getTitle(), interactionQuestion.getDescription());

        // 设置当前用户id，否在会出现401错误
        UserContext.setUser(interactionQuestion.getUserId());

        String reply = aigcClient.chatText(question);

        ReplyDTO replyDTO = ReplyDTO.builder()
                .userId(aiUserId)
                .content(reply)
                .anonymity(false)
                .questionId(interactionQuestion.getId())
                .isStudent(false) //非学生回复
                .build();
        this.iInteractionReplyService.saveReply(replyDTO);

    }
}
