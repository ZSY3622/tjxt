package com.tianji.aigc.service;

import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyEmitter;

public interface AudioService {
    /**
     * 文中转语音
     * @param text
     * @return
     */
    ResponseBodyEmitter ttsStream(String text);

    /**
     * 语音转文本
     * @return
     */
    String stt(MultipartFile audioFile);
}
