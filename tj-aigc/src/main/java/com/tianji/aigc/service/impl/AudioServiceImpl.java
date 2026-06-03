package com.tianji.aigc.service.impl;

import com.tianji.aigc.agent.AudioAgent;
import com.tianji.aigc.service.AudioService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyEmitter;

@Slf4j
@Service
@RequiredArgsConstructor
public class AudioServiceImpl implements AudioService {
    private final AudioAgent audioAgent;

    @Override
    public ResponseBodyEmitter ttsStream(String text) {
        log.info(text);




        return audioAgent.ttsStream(text);

    }
}
