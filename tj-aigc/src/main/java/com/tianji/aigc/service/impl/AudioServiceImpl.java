package com.tianji.aigc.service.impl;

import com.alibaba.dashscope.exception.NoApiKeyException;
import com.alibaba.dashscope.exception.UploadFileException;
import com.tianji.aigc.agent.AudioAgent;
import com.tianji.aigc.service.AudioService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyEmitter;

import java.io.IOException;

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

    @Override
    public String stt(MultipartFile audioFile) {
        try {
            return audioAgent.stt(audioFile);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (NoApiKeyException e) {
            throw new RuntimeException(e);
        } catch (UploadFileException e) {
            throw new RuntimeException(e);
        }
    }
}
