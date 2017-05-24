package com.baidu.hugegraph.api.schema;

import java.util.List;

import com.baidu.hugegraph.client.RestClient;
import com.baidu.hugegraph.client.RestResult;
import com.baidu.hugegraph.structure.constant.HugeType;
import com.baidu.hugegraph.structure.schema.IndexLabel;

/**
 * Created by liningrui on 2017/5/23.
 */
public class IndexLabelAPI extends SchemaAPI {

    public IndexLabelAPI(RestClient client, String graph) {
        super(client, graph);
    }

    @Override
    public String type() {
        return HugeType.INDEX_LABEL.string();
    }

    public void create(IndexLabel indexLabel) {
        this.client.post(path(), indexLabel);
    }

    public IndexLabel get(String name) {
        RestResult result = this.client.get(path(), name);
        return result.readObject(IndexLabel.class);
    }

    public List<IndexLabel> list() {
        RestResult result = this.client.get(path());
        return result.readList(type(), IndexLabel.class);
    }

    public void delete(String name) {
        this.client.delete(path(), name);
    }
}
