package com.baidu.hugegraph.api.version;

import com.baidu.hugegraph.api.API;
import com.baidu.hugegraph.client.RestClient;
import com.baidu.hugegraph.client.RestResult;
import com.baidu.hugegraph.structure.version.Versions;

public class VersionApi extends API {

    public VersionApi(RestClient client) {
        super(client);
        this.path("versions");
    }

    public Versions get() {
        RestResult result = this.client.get(path());
        return result.readObject(Versions.class);
    }
}
