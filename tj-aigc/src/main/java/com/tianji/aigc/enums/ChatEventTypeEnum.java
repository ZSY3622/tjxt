package com.tianji.aigc.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ChatEventTypeEnum {
    DATA(1001, "数据事件"),
    STOP(1002, "停止事件"),
    PARAM(1003, "参数事件"),
    ;

    private final int value;
    private final String desc;


    @Override
    public String toString() {
        return this.name();
    }
}
