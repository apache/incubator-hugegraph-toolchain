package org.apache.hugegraph.service.auth;

import lombok.extern.log4j.Log4j2;
import org.apache.hugegraph.driver.HugeClient;
import org.apache.hugegraph.driver.WhiteIpListManager;
import org.apache.hugegraph.entity.auth.WhiteIpListEntity;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Log4j2
@Service
public class WhiteIpListService extends AuthService{
    public Map<String, Object> get(HugeClient client) {
        WhiteIpListManager whiteIpListManager = client.whiteIpListManager();
        Map<String, Object> whiteIpList = whiteIpListManager.list();
        return whiteIpList;
    }

    public Map<String, Object> batch(HugeClient client, WhiteIpListEntity whiteIpListEntity) {
        WhiteIpListManager whiteIpListManager = client.whiteIpListManager();
        Map<String, Object> actionMap = new HashMap<>();
        actionMap.put("action", whiteIpListEntity.getAction());
        actionMap.put("ips", whiteIpListEntity.getIps());
        Map<String, Object> whiteIpList = whiteIpListManager.batch(actionMap);
        return whiteIpList;
    }

    public Map<String, Object> updatestatus(HugeClient client, boolean status) {
        WhiteIpListManager whiteIpListManager = client.whiteIpListManager();
        Map<String, Object> whiteIpList = whiteIpListManager.update(status);
        return whiteIpList;
    }
}
