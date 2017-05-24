package com.baidu.hugegraph.api.schema;

import java.util.List;

import com.baidu.hugegraph.client.RestClient;
import com.baidu.hugegraph.client.RestResult;
import com.baidu.hugegraph.structure.constant.HugeType;
import com.baidu.hugegraph.structure.schema.PropertyKey;

/**
 * Created by liningrui on 2017/5/23.
 */
public class PropertyKeyAPI extends SchemaAPI {

    public PropertyKeyAPI(RestClient client, String graph) {
        super(client, graph);
    }

    @Override
    public String type() {
        return HugeType.PROPERTY_KEY.string();
    }

    public void create(PropertyKey propertyKey) {
        this.client.post(path(), propertyKey);
    }

    public PropertyKey get(String name) {
        RestResult result = this.client.get(path(), name);
        return result.readObject(PropertyKey.class);
    }

    public List<PropertyKey> list() {
        RestResult result = this.client.get(path());
        return result.readList(type(), PropertyKey.class);
    }

    public void delete(String name) {
        this.client.delete(path(), name);
    }
}
