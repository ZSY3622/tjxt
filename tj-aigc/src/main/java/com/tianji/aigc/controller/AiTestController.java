package com.tianji.aigc.controller;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/test")
public class AiTestController {

    private final ObjectProvider<ChatModel> chatModelProvider;
    private final ObjectProvider<EmbeddingModel> embeddingModelProvider;

    public AiTestController(ObjectProvider<ChatModel> chatModelProvider,
                            ObjectProvider<EmbeddingModel> embeddingModelProvider) {
        this.chatModelProvider = chatModelProvider;
        this.embeddingModelProvider = embeddingModelProvider;
    }

    @GetMapping("/chat")
    public Map<String, Object> testChat(@RequestParam(defaultValue = "请用一句话介绍天机学堂") String prompt) {
        Map<String, Object> result = new LinkedHashMap<>();
        try {
            ChatModel chatModel = chatModelProvider.getIfAvailable();
            if (chatModel == null) {
                return failed("ChatModel bean not found");
            }
            String content = ChatClient.create(chatModel)
                    .prompt()
                    .user(prompt)
                    .call()
                    .content();
            result.put("success", true);
            result.put("prompt", prompt);
            result.put("content", content);
            return result;
        } catch (Exception e) {
            return failed(e);
        }
    }

    @GetMapping("/embedding")
    public Map<String, Object> testEmbedding(@RequestParam(defaultValue = "天机学堂") String text) {
        Map<String, Object> result = new LinkedHashMap<>();
        try {
            EmbeddingModel embeddingModel = embeddingModelProvider.getIfAvailable();
            if (embeddingModel == null) {
                return failed("EmbeddingModel bean not found");
            }
            float[] vector = embeddingModel.embed(text);
            result.put("success", true);
            result.put("text", text);
            result.put("dimensions", vector.length);
            result.put("sample", Arrays.copyOf(vector, Math.min(vector.length, 8)));
            return result;
        } catch (Exception e) {
            return failed(e);
        }
    }

    private Map<String, Object> failed(Exception e) {
        return failed(e.getClass().getSimpleName() + ": " + e.getMessage());
    }

    private Map<String, Object> failed(String message) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("success", false);
        result.put("error", message);
        return result;
    }
}
