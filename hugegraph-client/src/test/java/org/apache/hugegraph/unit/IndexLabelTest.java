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

import org.apache.hugegraph.exception.NotSupportException;
import org.apache.hugegraph.structure.constant.HugeType;
import org.apache.hugegraph.structure.schema.IndexLabel;
import org.apache.hugegraph.testutil.Assert;
import org.apache.hugegraph.util.JsonUtil;
import org.junit.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class IndexLabelTest {

    @Test
    public void testIndexLabel() throws JsonProcessingException {
        IndexLabel.Builder builder = new IndexLabel.BuilderImpl("personByAge",
                                                                null);
        IndexLabel indexLabel = builder.onV("person")
                                       .secondary()
                                       .by("age")
                                       .build();

        String json = "{\"name\":\"personByAge\",\"id\":0," +
                      "\"check_exist\":true,\"user_data\":{}," +
                      "\"base_type\":\"VERTEX_LABEL\"," +
                      "\"base_value\":\"person\"," +
                      "\"index_type\":\"SECONDARY\",\"fields\":[\"age\"]," +
                      "\"rebuild\":true}";
        ObjectMapper mapper = new ObjectMapper();
        Assert.assertEquals(mapper.readTree(json), mapper.readTree(JsonUtil.toJson(indexLabel)));
        Assert.assertEquals(HugeType.INDEX_LABEL.string(), indexLabel.type());
    }

    @Test
    public void testIndexLabelV49() throws JsonProcessingException {
        IndexLabel.Builder builder = new IndexLabel.BuilderImpl("personByAge",
                                                                null);
        IndexLabel indexLabel = builder.onV("person")
                                       .secondary()
                                       .by("age")
                                       .build();

        IndexLabel.IndexLabelV49 indexLabelV49 = indexLabel.switchV49();
        // Without userdata
        String json = "{\"id\":0,\"name\":\"personByAge\"," +
                      "\"check_exist\":true,\"base_type\":\"VERTEX_LABEL\"," +
                      "\"base_value\":\"person\"," +
                      "\"index_type\":\"SECONDARY\",\"fields\":[\"age\"]}";
        ObjectMapper mapper = new ObjectMapper();
        Assert.assertEquals(mapper.readTree(json), mapper.readTree(JsonUtil.toJson(indexLabelV49)));
        Assert.assertEquals(HugeType.INDEX_LABEL.string(),
                            indexLabelV49.type());

        Assert.assertThrows(NotSupportException.class, indexLabelV49::userdata);
    }

    @Test
    public void testIndexLabelV56() throws JsonProcessingException {
        IndexLabel.Builder builder = new IndexLabel.BuilderImpl("personByAge",
                                                                null);
        IndexLabel indexLabel = builder.onV("person")
                                       .secondary()
                                       .by("age")
                                       .build();

        IndexLabel.IndexLabelV56 indexLabelV56 = indexLabel.switchV56();

        String json = "{\"id\":0,\"name\":\"personByAge\"," +
                      "\"check_exist\":true,\"user_data\":{}," +
                      "\"base_type\":\"VERTEX_LABEL\",\"base_value\":\"person\"," +
                      "\"index_type\":\"SECONDARY\",\"fields\":[\"age\"]}";
        ObjectMapper mapper = new ObjectMapper();
        Assert.assertEquals(mapper.readTree(json), mapper.readTree(JsonUtil.toJson(indexLabelV56)));
        Assert.assertEquals(HugeType.INDEX_LABEL.string(),
                            indexLabelV56.type());

        Assert.assertThrows(NotSupportException.class, indexLabelV56::rebuild);
    }
}
