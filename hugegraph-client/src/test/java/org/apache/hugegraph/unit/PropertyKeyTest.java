/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. The ASF
 * licenses this file to You under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

package org.apache.hugegraph.unit;

import org.apache.hugegraph.structure.constant.Cardinality;
import org.apache.hugegraph.structure.constant.DataType;
import org.apache.hugegraph.structure.constant.HugeType;
import org.apache.hugegraph.structure.schema.PropertyKey;
import org.apache.hugegraph.testutil.Assert;
import org.junit.Test;

public class PropertyKeyTest {

    @Test
    public void testPropertyKey() {
        PropertyKey.Builder builder = new PropertyKey.BuilderImpl("name",
                                                                  null);
        PropertyKey propertyKey = builder.dataType(DataType.INT)
                                         .cardinality(Cardinality.SINGLE)
                                         .userdata("min", 1)
                                         .userdata("max", 100)
                                         .build();

        String pkString = "{name=name, cardinality=SINGLE, dataType=INT, " +
                          "aggregateType=NONE, properties=[], " +
                          "writeType=OLTP}";
        Assert.assertEquals(pkString, propertyKey.toString());
        Assert.assertEquals(HugeType.PROPERTY_KEY.string(), propertyKey.type());
        Assert.assertEquals(0, propertyKey.aggregateType().code());
        Assert.assertEquals("none", propertyKey.aggregateType().string());
    }

    @Test
    public void testPropertyKeyV46() {
        PropertyKey.Builder builder = new PropertyKey.BuilderImpl("name",
                                                                  null);
        PropertyKey propertyKey = builder.dataType(DataType.INT)
                                         .cardinality(Cardinality.SINGLE)
                                         .userdata("min", 1)
                                         .userdata("max", 100)
                                         .build();

        PropertyKey.PropertyKeyV46 propertyKeyV46 = propertyKey.switchV46();
        String pkV46String = "{name=name, cardinality=SINGLE, " +
                             "dataType=INT, properties=[]}";
        Assert.assertEquals(pkV46String, propertyKeyV46.toString());
        Assert.assertEquals(HugeType.PROPERTY_KEY.string(),
                            propertyKeyV46.type());
    }

    @Test
    public void testPropertyKeyV58() {
        PropertyKey.Builder builder = new PropertyKey.BuilderImpl("name",
                                                                  null);
        PropertyKey propertyKey = builder.dataType(DataType.INT)
                                         .cardinality(Cardinality.SINGLE)
                                         .userdata("min", 1)
                                         .userdata("max", 100)
                                         .build();

        PropertyKey.PropertyKeyV58 propertyKeyV58 = propertyKey.switchV58();
        String pkV58String = "{name=name, cardinality=SINGLE, " +
                             "dataType=INT, aggregateType=NONE, properties=[]}";
        Assert.assertEquals(pkV58String, propertyKeyV58.toString());
        Assert.assertEquals(HugeType.PROPERTY_KEY.string(),
                            propertyKeyV58.type());
    }
}
