package org.apache.hugegraph.api.space;

import org.apache.hugegraph.api.API;
import org.apache.hugegraph.client.RestClient;
import org.apache.hugegraph.rest.RestResult;
import org.apache.hugegraph.structure.constant.HugeType;
import org.apache.hugegraph.structure.space.SchemaTemplate;

import java.util.List;
import java.util.Map;

public class SchemaTemplateAPI extends API {
    private static final String PATH = "graphspaces/%s/schematemplates";

    public SchemaTemplateAPI(RestClient client, String graphSpace) {
        super(client);
        this.path(String.format(PATH, graphSpace));
    }

    @Override
    protected String type() {
        return HugeType.SCHEMATEMPLATES.string();
    }

    public Map create(SchemaTemplate template) {
        RestResult result = this.client.post(this.path(), template);
        return result.readObject(Map.class);
    }

    public List<String> list() {
        RestResult result = this.client.get(this.path());
        return result.readList(this.type(), String.class);
    }

    public Map get(String name) {
        RestResult result = this.client.get(this.path(), name);
        return result.readObject(Map.class);
    }

    public void delete(String name) {
        this.client.delete(this.path(), name);
    }

    public Map update(SchemaTemplate template) {
        RestResult result = this.client.put(this.path(), template.name(),
                template);

        return result.readObject(Map.class);
    }
}
