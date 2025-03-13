package org.apache.hugegraph.api.auth;

import org.apache.commons.lang.StringUtils;
import org.apache.hugegraph.client.RestClient;
import org.apache.hugegraph.rest.RestResult;
import org.apache.hugegraph.structure.auth.AuthElement;
import org.apache.hugegraph.structure.auth.HugePermission;
import org.apache.hugegraph.structure.auth.UserManager;
import org.apache.hugegraph.structure.constant.HugeType;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ManagerAPI extends AuthAPI {

    public ManagerAPI(RestClient client, String graph) {
        super(client, graph);
    }

    public UserManager create(UserManager userManager) {
        RestResult result = this.client.post(this.path(), userManager);
        return result.readObject(UserManager.class);
    }

    public void delete(String user, HugePermission type, String graphSpace) {
        Map<String, Object> params = new HashMap<>();
        params.put("user", user);
        params.put("type", type);
        params.put("graphspace", graphSpace);
        this.client.delete(this.path(), params);
    }

    public List<String> list(HugePermission type, String graphSpace) {

        Map<String, Object> params = new HashMap<>();
        params.put("type", type);
        params.put("graphspace", graphSpace);

        RestResult result = this.client.get(this.path(), params);

        return result.readList("admins", String.class);
    }

    public boolean checkPermission(HugePermission type, String graphSpace) {

        Map<String, Object> params = new HashMap<>();
        params.put("type", type);
        params.put("graphspace", graphSpace);

        String path = this.path() + PATH_SPLITOR + "check";
        RestResult result = this.client.get(path, params);
        
        return (boolean) result.readObject(Map.class).getOrDefault("check", false);
    }

    public boolean checkDefaultRole(String graphSpace, String role,
                                    String graph) {
        String path = joinPath(this.path(), "default");
        Map<String, Object> params = new HashMap<>();
        params.put("graphspace", graphSpace);
        params.put("role", role);
        if (StringUtils.isNotEmpty(graph)) {
            params.put("graph", graph);
        }
        RestResult result = this.client.get(path, params);
        return (boolean) result.readObject(Map.class).getOrDefault("check",
                                                                   false);
    }

    @Override
    protected String type() {
        return HugeType.MANAGER.string();
    }

    @Override
    protected Object checkCreateOrUpdate(AuthElement authElement) {
        return null;
    }
}
