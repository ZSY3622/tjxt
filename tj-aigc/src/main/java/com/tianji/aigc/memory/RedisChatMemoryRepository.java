package com.tianji.aigc.memory;

import cn.hutool.core.lang.Assert;
import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import jakarta.annotation.Resource;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.messages.Message;
import org.springframework.data.redis.core.BoundListOperations;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.List;
import java.util.Set;

/**
 * 重构报存上下文信息，原本实现为内存报存，服务重启后就消失
 */
public class RedisChatMemoryRepository implements ChatMemoryRepository {


    // 默认redis中key的前缀
    public static final String DEFAULT_PREFIX = "CHAT:";

    private final String prefix;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    public RedisChatMemoryRepository() {
        this.prefix = DEFAULT_PREFIX;
    }

    public RedisChatMemoryRepository(String prefix) {
        this.prefix = prefix;
    }

    /**
     * 获取所有对话id
     * @return
     */
    @Override
    public List<String> findConversationIds() {
        Set<String> keys = stringRedisTemplate.keys(this.prefix + "*");
        if (null == keys){
            return List.of();
        }
        return keys.stream().map(key-> StrUtil.replace(key,this.prefix,"")).toList();
    }

    /**
     * 根据对话id查询message列表
     * @param conversationId
     * @return
     */

    @Override
    public List<Message> findByConversationId(String conversationId) {
        String key = getKey(conversationId);
        //获取string类型的messages
        List<String> strMessages = stringRedisTemplate.opsForList().range(key, 0, -1);
        if (ArrayUtil.isEmpty(strMessages)){
            return List.of();
        }
        return strMessages.stream().map(message -> MessageUtil.toMessage(message)).toList();
    }

    /**
     * 报存所有message列表数据
     * @param conversationId
     * @param messages
     */
    @Override
    public void saveAll(String conversationId, List<Message> messages) {
        Assert.notEmpty(messages, "消息列表不能为空");
        // 删除旧的message
        String key = this.getKey(conversationId);
        //绑定 不用在指定key
        BoundListOperations<String, String> listOps = stringRedisTemplate.boundListOps(key);
        this.deleteByConversationId(conversationId);
        // 添加总messages
        messages.forEach(message -> listOps.rightPush(MessageUtil.toJson(message)));

    }

    /**
     * 根据对话id删除数据
     * @param conversationId
     */
    @Override
    public void deleteByConversationId(String conversationId) {
        String key = this.getKey(conversationId);
        stringRedisTemplate.delete(key);
    }

    private String getKey(String conversationId) {
        return prefix + conversationId;
    }
}
