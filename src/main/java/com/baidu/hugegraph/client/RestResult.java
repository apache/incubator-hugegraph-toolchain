package com.baidu.hugegraph.client;

import java.io.IOException;
import java.util.List;

import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

import com.baidu.hugegraph.exception.SerializeException;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Created by liningrui on 2017/5/23.
 */
public class RestResult {

    private int status;
    private MultivaluedMap<String, Object> headers;
    private String content;

    public RestResult(Response response) {
        this.status = response.getStatus();
        this.headers = response.getHeaders();
        this.content = response.readEntity(String.class);
    }

    public int status() {
        return status;
    }

    public MultivaluedMap<String, Object> headers() {
        return headers;
    }

    public String content() {
        return content;
    }

    public <T> T readObject(Class<T> clazz) {
        T obj;
        ObjectMapper mapper = new ObjectMapper();
        try {
            obj = mapper.readValue(this.content, clazz);
        } catch (Exception e) {
            throw new SerializeException(String.format(
                    "Failed to deserialize %s", this.content), e);
        }
        return obj;
    }

    public <T> List<T> readList(String key, Class<T> clazz) {
        List<T> objList;
        ObjectMapper mapper = new ObjectMapper();
        try {
            JsonNode root = mapper.readTree(this.content);
            JsonNode element = root.get(key);
            if (element == null) {
                throw new SerializeException(String.format(
                        "Can not find value of the key: %s in json.", key));
            }
            JavaType type = mapper.getTypeFactory()
                    .constructParametricType(List.class, clazz);
            objList = mapper.readValue(element.toString(), type);

        } catch (IOException e) {
            throw new SerializeException(String.format(
                    "Failed to deserialize %s", this.content), e);
        }
        return objList;
    }

    @Override
    public String toString() {
        return String.format("{status=%s, headers=%s, content=%s",
                this.status,
                this.headers,
                this.content);
    }

}
