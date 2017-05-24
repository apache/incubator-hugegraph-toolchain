package com.baidu.hugegraph.driver;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import com.baidu.hugegraph.structure.constant.Cardinality;
import com.baidu.hugegraph.structure.constant.DataType;
import com.baidu.hugegraph.structure.schema.PropertyKey;

/**
 * Created by liningrui on 2017/5/24.
 */
public class PropertyKeyTest extends BaseTest {

    @BeforeClass
    public static void setup() {
        SchemaManager schema = newClient().schema();
        PropertyKey pk = schema.makePropertyKey("name").asText().create();
        Assert.assertEquals("name", pk.name());
        Assert.assertEquals(DataType.TEXT, pk.dataType());
        Assert.assertEquals(Cardinality.SINGLE, pk.cardinality());
    }

    @AfterClass
    public static void teardown() {

    }

    @Test
    public void testCreate() {

    }

    @Test
    public void testGet() {
    }

    @Test
    public void testGetNotFound() {

    }

    @Test
    public void testList() {

    }

    @Test
    public void testDelete() {

    }

}
