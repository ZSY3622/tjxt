package com.tianji.aigc.config;

import com.alibaba.cloud.nacos.NacosConfigManager;
import com.alibaba.nacos.api.config.listener.Listener;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicReference;

@Slf4j
@Getter
@Configuration
@RequiredArgsConstructor
public class SystemPromptConfig {

    /**
     * Nacos 配置管理器，用它可以拿到 ConfigService，
     * 从而读取配置内容并注册配置变更监听器。
     */
    private final NacosConfigManager nacosConfigManager;

    /**
     * 读取 application.yml 中 tj.ai.prompt 前缀下的配置。
     * 这里主要用到 dataId、group、timeoutMs，用来定位 Nacos 中的系统提示词配置。
     */
    private final AIProperties aiProperties;

    /**
     * 当前聊天场景使用的系统提示词内容。
     *
     * 使用 AtomicReference 是为了保证多线程下读写安全：
     * - 业务线程可能随时读取系统提示词；
     * - Nacos 监听器回调线程可能在配置变更时更新系统提示词。
     */
    private final AtomicReference<String> chatSystemMessage = new AtomicReference<>();

    private final AtomicReference<String> routeAgentSystemMessage = new AtomicReference<>();

    private final AtomicReference<String> recommendAgentSystemMessage = new AtomicReference<>();

    private final AtomicReference<String> buyAgentSystemMessage = new AtomicReference<>();

    private final AtomicReference<String> consultAgentSystemMessage = new AtomicReference<>();

    private final AtomicReference<String> knowledgeAgentSystemMessage = new AtomicReference<>();

    private final AtomicReference<String> textSystemMessage = new AtomicReference<>();
    /**
     * Spring 创建并注入完当前 Bean 后会执行这个方法。
     * 服务启动时先主动从 Nacos 拉取一次配置，避免等到配置变更后才有系统提示词。
     */
    @PostConstruct
    public void init() {
        // 加载聊天场景的系统提示词，并把内容保存到 chatSystemMessage 中。
        loadConfig(aiProperties.getSystem().getChat(), chatSystemMessage);
        // 读取配置
        loadConfig(aiProperties.getSystem().getRouteAgent(), routeAgentSystemMessage);
        loadConfig(aiProperties.getSystem().getRecommendAgent(),recommendAgentSystemMessage);
        loadConfig(aiProperties.getSystem().getBuyAgent(),buyAgentSystemMessage);
        loadConfig(aiProperties.getSystem().getConsultAgent(),consultAgentSystemMessage);
        loadConfig(aiProperties.getSystem().getKnowledgeAgent(),knowledgeAgentSystemMessage);
        loadConfig(aiProperties.getSystem().getText(),textSystemMessage);

    }

    /**
     * 从 Nacos 读取指定配置，并把读取到的内容写入 target。
     * 同时注册监听器，后续 Nacos 中该配置发生变化时，内存中的 target 也会同步刷新。
     *
     * @param chatConfig 系统提示词在 Nacos 中的位置配置，包括 dataId、group 和读取超时时间
     * @param target     保存系统提示词内容的内存引用
     */
    private void loadConfig(AIProperties.System.Chat chatConfig, AtomicReference<String> target) {
        try {
            // dataId 对应 Nacos 配置文件名，例如 system-chat-message.txt。
            var dataId = chatConfig.getDataId();
            // group 对应 Nacos 配置分组，默认通常是 DEFAULT_GROUP。
            var group = chatConfig.getGroup();
            // timeoutMs 表示本次读取 Nacos 配置最多等待多久，单位是毫秒。
            var timeoutMs = chatConfig.getTimeoutMs();

            // 服务启动时主动读取一次配置，并缓存到内存中。
            var config = nacosConfigManager.getConfigService().getConfig(dataId, group, timeoutMs);
            target.set(config);
            log.info("读取{}成功，内容为：{}", target, config);
            // 注册监听器：Nacos 中该 dataId/group 的配置发生变化时，会回调 receiveConfigInfo。
            nacosConfigManager.getConfigService().addListener(dataId, group, new Listener() {
                @Override
                public Executor getExecutor() {
                    // 返回 null 表示使用 Nacos 默认线程池执行监听回调。
                    return null;
                }

                @Override
                public void receiveConfigInfo(String info) {
                    // Nacos 推送新配置后，更新内存中的系统提示词，业务侧后续读取到的就是最新内容。
                    target.set(info);
                    log.info("更新{}成功，内容为：{}", target, info);
                }
            });
        } catch (Exception e) {
            // 这里捕获异常，避免配置中心短暂不可用时直接导致应用启动失败。
            log.error("加载配置失败", e);
        }
    }

}
