package com.baidu.hugegraph.client;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;

import com.baidu.hugegraph.driver.GraphManager;
import com.baidu.hugegraph.driver.GremlinManager;
import com.baidu.hugegraph.driver.HugeClient;
import com.baidu.hugegraph.driver.SchemaManager;

public class BaseClientTest {

    protected static String BASE_URL = "http://127.0.0.1:8080";
    protected static String GRAPH = "hugegraph";

    private static HugeClient client;

    protected static HugeClient open() {
        return new HugeClient(BASE_URL, GRAPH);
    }

    @BeforeClass
    public static void init() {
        client = open();
    }

    @AfterClass
    public static void clear() throws Exception {
        // client.close();
    }

    public static SchemaManager schema() {
        return client.schema();
    }

    public static GraphManager graph() {
        return client.graph();
    }

    public static GremlinManager gremlin() {
        return client.gremlin();
    }

    @Before
    public void setup() {
        // this.clearData();
    }

    @After
    public void teardown() throws Exception {
        // pass
    }
}
