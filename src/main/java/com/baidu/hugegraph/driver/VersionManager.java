package com.baidu.hugegraph.driver;

import com.baidu.hugegraph.api.version.VersionApi;
import com.baidu.hugegraph.client.RestClient;
import com.baidu.hugegraph.structure.version.Versions;

public class VersionManager {

    private VersionApi versionApi;

    public VersionManager(String url) {
        RestClient client = new RestClient(url);
        this.versionApi = new VersionApi(client);
    }

    public String getCoreVersion() {
        Versions versions = this.versionApi.get();
        return versions.get("core");
    }

    public String getGremlinVersion() {
        Versions versions = this.versionApi.get();
        return versions.get("gremlin");
    }

    public String getApiVersion() {
        Versions versions = this.versionApi.get();
        return versions.get("api");
    }
}
