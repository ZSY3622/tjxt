package com.tianji.aigc.service;

import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyEmitter;

public interface AudioService {
    /**
     * 文中转语音
     * @param text
     * @return
     */
    ResponseBodyEmitter ttsStream(String text);
}
