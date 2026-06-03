package com.tianji.aigc.agent;

import com.alibaba.dashscope.aigc.multimodalconversation.AudioParameters;
import com.alibaba.dashscope.aigc.multimodalconversation.MultiModalConversation;
import com.alibaba.dashscope.aigc.multimodalconversation.MultiModalConversationParam;
import com.alibaba.dashscope.aigc.multimodalconversation.MultiModalConversationResult;
import com.alibaba.dashscope.exception.NoApiKeyException;
import com.alibaba.dashscope.exception.UploadFileException;
import io.reactivex.Flowable;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyEmitter;

import java.io.IOException;
import java.util.Base64;
import java.util.concurrent.CompletableFuture;
import java.util.Locale;

@Slf4j
@Component
@ConditionalOnProperty(prefix = "llm.qwen3-tts-flash", name = "api-key")
public class AudioAgent {
    @Value("${llm.qwen3-tts-flash.api-key}")
    private String apiKey;
    @Value("${llm.qwen3-tts-flash.base-url}")
    private String baseUrl;
    @Value("${llm.qwen3-tts-flash.model}")
    private String model;
    @Value("${llm.qwen3-tts-flash.voice:CHERRY}")
    private String voice;
    @Value("${llm.qwen3-tts-flash.language-type:Chinese}")
    private String languageType;

    private final MultiModalConversation conv = new MultiModalConversation();

    public ResponseBodyEmitter ttsStream(String text) {
        ResponseBodyEmitter emitter = new ResponseBodyEmitter();
        MultiModalConversationParam param = createParam(text);
        try {
            Flowable<MultiModalConversationResult> result = conv.streamCall(param);
            result.blockingForEach(r -> {
                String base64Data = r.getOutput().getAudio().getData();
                byte[] audioBytes = Base64.getDecoder().decode(base64Data);
                log.info(audioBytes.toString());
                emitter.send(audioBytes, MediaType.APPLICATION_OCTET_STREAM);
            });
            // 非常关键：流式响应结束后，一定要 complete
            log.info("TTS 流式合成完成");
            emitter.complete();

        }catch (Exception e){
            log.info(e.toString());
        }

        return emitter;
    }

    private MultiModalConversationParam createParam(String text) {
        return MultiModalConversationParam.builder()
                .apiKey(apiKey)
                .model(model)
                .text(text)
                .languageType(languageType)
                .voice(AudioParameters.Voice.CHERRY)
                .build();
    }


}
