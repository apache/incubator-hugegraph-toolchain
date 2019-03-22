package com.baidu.hugegraph.unit;

import com.baidu.hugegraph.util.JsonUtil;

public class BaseUnitTest {

    public static <T> String serialize(T data) {
        return JsonUtil.toJson(data);
    }

    public static <T> T deserialize(String json, Class<T> clazz) {
        return JsonUtil.fromJson(json, clazz);
    }
}
