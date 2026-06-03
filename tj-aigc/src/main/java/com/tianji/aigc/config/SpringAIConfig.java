package com.tianji.aigc.config;

import com.tianji.aigc.advisor.RecordOptimizationAdvisor;
import com.tianji.aigc.memory.MyChatMemoryRepository;
import com.tianji.aigc.memory.MysqlChatMemoryRepository;
import com.tianji.aigc.memory.RedisChatMemoryRepository;
import com.tianji.aigc.tools.CourseTools;
import com.tianji.aigc.tools.OrderTools;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Slf4j
@Configuration
public class SpringAIConfig {

    @Value("${tj.ai.memory.max:100}")
    private Integer maxMessages;

    /**
     * 配置 ChatClient
     */
    @Bean
    @Primary
    public ChatClient chatClient(ChatClient.Builder chatClientBuilder,
                                 Advisor loggerAdvisor,
                                 Advisor messageChatMemoryAdvisor,
                                 Advisor recordOptimizationAdvisor,
                                 CourseTools courseTools,
                                 OrderTools orderTools
    ) {  // 日志记录器
        return chatClientBuilder
                .defaultAdvisors(loggerAdvisor,messageChatMemoryAdvisor,recordOptimizationAdvisor) //添加 Advisor 功能增强
//                .defaultTools(courseTools,orderTools) //添加默认工具
                .build();
    }

    @Bean("openAiChatClient")
    @ConditionalOnProperty(prefix = "spring.ai.openai", name = "api-key")
    public ChatClient openAiChatClient(
            @Value("${spring.ai.openai.api-key}") String apiKey,
            @Value("${spring.ai.openai.base-url}") String baseUrl,
            @Value("${spring.ai.openai.chat.options.model}") String model,
            Advisor loggerAdvisor
    ) {
        OpenAiApi openAiApi = OpenAiApi.builder()
                .apiKey(apiKey)
                .baseUrl(baseUrl)
                .build();

        OpenAiChatModel openAiChatModel = OpenAiChatModel.builder()
                .openAiApi(openAiApi)
                .defaultOptions(OpenAiChatOptions.builder().model(model).build())
                .build();

        log.info("初始化 openAiChatClient，实际ChatModel={}，实际model={}，baseUrl={}",
                openAiChatModel.getClass().getName(),
                openAiChatModel.getDefaultOptions().getModel(),
                baseUrl);

        return ChatClient.builder(openAiChatModel)
                .defaultAdvisors(loggerAdvisor)
                .build();
    }



    /**
     * 日志记录器,记录 ChatClient 调用大模型时的日志。
     */
    @Bean
    public Advisor loggerAdvisor() {
        return new SimpleLoggerAdvisor();
    }

    @Bean
    @ConditionalOnProperty(value = "type" ,prefix ="tj.ai.memory" ,havingValue = "Redis")
    public RedisChatMemoryRepository redisChatMemoryRepository() {
        return new RedisChatMemoryRepository(); //重构的ChatMemoryRepository
    }

    @Bean
    @ConditionalOnProperty(value = "type" ,prefix ="tj.ai.memory" ,havingValue = "Mysql")
    public MysqlChatMemoryRepository mysqlChatMemoryRepository() {
        return new MysqlChatMemoryRepository(); //重构的ChatMemoryRepository
    }

    @Bean
    public ChatMemory chatMemory(ChatMemoryRepository chatMemoryRepository) {
        // 基于 chatMemoryRepository 对象构建 chatMemory 对象
        return MessageWindowChatMemory.builder()
                .chatMemoryRepository(chatMemoryRepository)
                .maxMessages(this.maxMessages) // 最多保存 100 条对话, 如果超出的话，会自动删除最旧的对话
                .build();
    }

    /**
     * 基于Redis的会话记忆，聊天记忆整合到message列表中实现多轮对话
     */
    @Bean
    public Advisor messageChatMemoryAdvisor(ChatMemory chatMemory) {
        // 创建基于 chatMemory 的 Advisor 对象
        return MessageChatMemoryAdvisor.builder(chatMemory).build();
    }

    @Bean
    public Advisor recordOptimizationAdvisor(MyChatMemoryRepository myChatMemoryRepository){
        return new RecordOptimizationAdvisor(myChatMemoryRepository);
    }





}
