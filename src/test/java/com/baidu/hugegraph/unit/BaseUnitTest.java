package com.baidu.hugegraph.unit;

import java.io.IOException;

import com.baidu.hugegraph.rest.SerializeException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class BaseUnitTest {

    private static final ObjectMapper mapper = new ObjectMapper();

    public static <T> String serialize(T data) {
        try {
            return mapper.writeValueAsString(data);
        } catch (IOException e) {
            throw new SerializeException("Failed to serialize '%s'", e, data);
        }
    }

    public static <T> T deserialize(String json, Class<T> clazz) {
        try {
            return mapper.readValue(json, clazz);
        } catch (IOException e) {
            throw new SerializeException(
                      "Failed to deserialize instance of '%s' from %s",
                      e, clazz, json);
        }
    }

}
