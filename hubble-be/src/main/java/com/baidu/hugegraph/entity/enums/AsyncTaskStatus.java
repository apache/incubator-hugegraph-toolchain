package com.baidu.hugegraph.entity.enums;

import com.baomidou.mybatisplus.core.enums.IEnum;

public enum AsyncTaskStatus implements IEnum<Byte> {

    UNKNOWN(0),

    NEW(1),
    SCHEDULING(2),
    SCHEDULED(3),
    QUEUED(4),
    RESTORING(5),
    RUNNING(6),
    SUCCESS(7),
    CANCELLING(8),
    CANCELLED(9),
    FAILED(10);

    private byte code;

    AsyncTaskStatus(int code) {
        assert code < 256;
        this.code = (byte) code;
    }

    @Override
    public Byte getValue() {
        return this.code;
    }
}
