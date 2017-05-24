package com.baidu.hugegraph.driver;

import org.junit.After;
import org.junit.Before;

/**
 * Created by liningrui on 2017/5/24.
 */
public class BaseTest {

    public static String BASE_URL = "http://127.0.0.1:8080";
    public static String GRAPH = "hugegraph";

    private HugeClient client;

    @Before
    public void init() {
        this.client = HugeClient.open(BASE_URL, GRAPH);
    }

    @After
    public void clear() {
        //        this.client.close();
    }

    public HugeClient client() {
        return this.client;
    }

    public static HugeClient newClient() {
        return new HugeClient(BASE_URL, GRAPH);
    }

}
