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

package org.apache.hugegraph.loader.test.unit;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import org.apache.commons.io.FileUtils;
import org.apache.hugegraph.loader.MappingConverter;
import org.junit.Assert;
import org.junit.Test;

public class MappingConverterTest {

    @Test
    public void testConvertV1ToV2() throws IOException {
        String v1Json = "{" +
                        "  \"vertices\": [" +
                        "    {" +
                        "      \"label\": \"user\"," +
                        "      \"input\": {" +
                        "        \"type\": \"file\"," +
                        "        \"path\": \"users.dat\"," +
                        "        \"format\": \"TEXT\"," +
                        "        \"delimiter\": \"::\"," +
                        "        \"header\": [\"UserID\", \"Gender\", \"Age\", " +
                        "\"Occupation\", \"Zip-code\"]" +
                        "      }," +
                        "      \"ignored\": [\"Gender\", \"Age\", \"Occupation\", " +
                        "\"Zip-code\"]," +
                        "      \"field_mapping\": {" +
                        "        \"UserID\": \"id\"" +
                        "      }" +
                        "    }" +
                        "  ]," +
                        "  \"edges\": [" +
                        "    {" +
                        "      \"label\": \"rating\"," +
                        "      \"source\": [\"UserID\"]," +
                        "      \"target\": [\"MovieID\"]," +
                        "      \"input\": {" +
                        "        \"type\": \"file\"," +
                        "        \"path\": \"ratings.dat\"," +
                        "        \"format\": \"TEXT\"," +
                        "        \"delimiter\": \"::\"," +
                        "        \"header\": [\"UserID\", \"MovieID\", \"Rating\", " +
                        "\"Timestamp\"]" +
                        "      }," +
                        "      \"ignored\": [\"Timestamp\"]," +
                        "      \"field_mapping\": {" +
                        "        \"UserID\": \"id\"," +
                        "        \"MovieID\": \"id\"," +
                        "        \"Rating\": \"rate\"" +
                        "      }" +
                        "    }" +
                        "  ]," +
                        "  \"backendStoreInfo\":" +
                        "  {" +
                        "    \"edge_tablename\": \"hugegraph:g_oe\"," +
                        "    \"vertex_tablename\": \"hugegraph:g_v\"," +
                        "    \"hbase_zookeeper_quorum\": \"127.0.0.1\"," +
                        "    \"hbase_zookeeper_property_clientPort\": \"2181\"," +
                        "    \"zookeeper_znode_parent\": \"/hbase\"" +
                        "  }" +
                        "}";
        String input = "struct.json";
        File inputFile = new File(input);
        Charset charset = StandardCharsets.UTF_8;
        FileUtils.writeStringToFile(inputFile, v1Json, charset);
        MappingConverter.main(new String[]{input});

        File outputFile = FileUtils.getFile("struct-v2.json");
        String actualV2Json = FileUtils.readFileToString(outputFile, charset);
        String expectV2Json = "{\"version\":\"2.0\"," +
                              "\"structs\":[{\"id\":\"1\",\"skip\":false," +
                              "\"input\":{\"type\":\"FILE\",\"path\":\"users.dat\"," +
                              "\"file_filter\":{\"extensions\":[\"*\"]}," +
                              "\"format\":\"TEXT\",\"delimiter\":\"::\"," +
                              "\"date_format\":\"yyyy-MM-dd HH:mm:ss\"," +
                              "\"time_zone\":\"GMT+8\",\"skipped_line\":{\"regex\":\"" +
                              "(^#|^//).*|\"},\"compression\":\"NONE\"," +
                              "\"batch_size\":500,\"header\":[\"UserID\",\"Gender\"," +
                              "\"Age\",\"Occupation\",\"Zip-code\"]," +
                              "\"charset\":\"UTF-8\",\"list_format\":null}," +
                              "\"vertices\":[{\"label\":\"user\",\"skip\":false," +
                              "\"id\":null,\"unfold\":false," +
                              "\"field_mapping\":{\"UserID\":\"id\"}," +
                              "\"value_mapping\":{},\"selected\":[]," +
                              "\"ignored\":[\"Occupation\",\"Zip-code\",\"Gender\"," +
                              "\"Age\"],\"null_values\":[\"\"]," +
                              "\"update_strategies\":{},\"batch_size\":500}],\"edges\":[]}," +
                              "{\"id\":\"2\"," +
                              "\"skip\":false,\"input\":{\"type\":\"FILE\"," +
                              "\"path\":\"ratings.dat\"," +
                              "\"file_filter\":{\"extensions\":[\"*\"]}," +
                              "\"format\":\"TEXT\",\"delimiter\":\"::\"," +
                              "\"date_format\":\"yyyy-MM-dd HH:mm:ss\"," +
                              "\"time_zone\":\"GMT+8\",\"skipped_line\":{\"regex\":\"" +
                              "(^#|^//).*|\"},\"compression\":\"NONE\"," +
                              "\"batch_size\":500,\"header\":[\"UserID\",\"MovieID\"," +
                              "\"Rating\",\"Timestamp\"],\"charset\":\"UTF-8\"," +
                              "\"list_format\":null},\"vertices\":[]," +
                              "\"edges\":[{\"label\":\"rating\",\"skip\":false," +
                              "\"source\":[\"UserID\"],\"unfold_source\":false," +
                              "\"target\":[\"MovieID\"],\"unfold_target\":false," +
                              "\"field_mapping\":{\"UserID\":\"id\",\"MovieID\":\"id\"," +
                              "\"Rating\":\"rate\"},\"value_mapping\":{},\"selected\":[]," +
                              "\"ignored\":[\"Timestamp\"],\"null_values\":[\"\"]," +
                              "\"update_strategies\":{},\"batch_size\":500}]}]," +
                              "\"backendStoreInfo\":{" +
                              "\"edge_tablename\":\"hugegraph:g_oe\"," +
                              "\"vertex_tablename\":\"hugegraph:g_v\"," +
                              "\"hbase_zookeeper_quorum\":\"127.0.0.1\"," +
                              "\"hbase_zookeeper_property_clientPort\":\"2181\"," +
                              "\"zookeeper_znode_parent\":\"/hbase\"}}";
        Assert.assertEquals(expectV2Json, actualV2Json);

        FileUtils.forceDelete(inputFile);
        FileUtils.forceDelete(outputFile);
    }
}
