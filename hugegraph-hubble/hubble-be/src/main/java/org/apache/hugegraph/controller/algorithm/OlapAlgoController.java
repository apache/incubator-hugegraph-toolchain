package org.apache.hugegraph.controller.algorithm;

import lombok.extern.log4j.Log4j2;
import org.apache.hugegraph.common.Constant;
import org.apache.hugegraph.controller.BaseController;
import org.apache.hugegraph.driver.HugeClient;
import org.apache.hugegraph.entity.algorithm.OlapEntity;
import org.apache.hugegraph.entity.query.OlapView;
import org.apache.hugegraph.service.algorithm.OlapAlgoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@Log4j2
@RestController
@RequestMapping(Constant.API_VERSION + "graphspaces/{graphspace}/graphs" +
        "/{graph}/algorithms/olap")
public class OlapAlgoController extends BaseController {
    @Autowired
    private OlapAlgoService service;

    @PostMapping
    public OlapView olapView(@PathVariable("graphspace") String graphspace,
                             @PathVariable("graph") String graph,
                             @RequestBody OlapEntity body) {
        HugeClient client = this.authClient(graphspace, graph);
        return this.service.olapView(client, graphspace, body);
    }
}
