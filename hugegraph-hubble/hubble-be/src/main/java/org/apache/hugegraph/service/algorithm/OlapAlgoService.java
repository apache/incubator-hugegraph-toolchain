package org.apache.hugegraph.service.algorithm;

import lombok.extern.log4j.Log4j2;
import org.apache.hugegraph.config.HugeConfig;
import org.apache.hugegraph.driver.HugeClient;
import org.apache.hugegraph.entity.algorithm.OlapEntity;
import org.apache.hugegraph.entity.query.OlapView;
import org.apache.hugegraph.service.query.ExecuteHistoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Map;

@Log4j2
@Service
public class OlapAlgoService {
    @Autowired
    private HugeConfig config;
    @Autowired
    private ExecuteHistoryService historyService;

    public OlapView olapView(HugeClient client, String graphspace, OlapEntity body) {
        Map<String, Object> params = body.getParams();
        if (!"DEFAULT".equals(graphspace)) {
            params.put("k8s.master_cpu", "1");
            params.put("k8s.worker_cpu", "1");
            params.put("k8s.master_request_memory", "5Gi");
            params.put("k8s.worker_request_memory", "5Gi");
            params.put("k8s.master_memory", "5Gi");
            params.put("k8s.worker_memory", "5Gi");
            params.putAll(body.getParams());
        }
        long taskid = client.computer().create(body.getAlgorithm(), body.getWorker(), params);
//        String graphSpace = client.getGraphSpaceName();
//        String graph = client.getGraphName();
//        Date createTime = HubbleUtil.nowDate();
//        // Insert execute history
//        ExecuteStatus status = ExecuteStatus.SUCCESS;
//        ExecuteHistory history;
//        history = new ExecuteHistory(null, graphSpace, graph, 0L,
//                ExecuteType.ALGORITHM,
//                body.toString(), status,
//                AsyncTaskStatus.UNKNOWN, -1L, createTime);
//        this.historyService.save(history);
        return OlapView.builder().taskId(taskid).build();
    }
}
