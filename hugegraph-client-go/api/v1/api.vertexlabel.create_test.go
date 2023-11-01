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

package v1_test

import (
    "fmt"
    "github.com/apache/incubator-hugegraph-toolchain/hugegraph-client-go"
    v1 "github.com/apache/incubator-hugegraph-toolchain/hugegraph-client-go/api/v1"
    "github.com/apache/incubator-hugegraph-toolchain/hugegraph-client-go/internal/model"
    "log"
    "math/rand"
    "testing"
    "time"
)

func TestVertexlabelCreateRequest_Do(t *testing.T) {
    client, err := hugegraph.NewDefaultCommonClient()
    if err != nil {
        log.Println(err)
    }

    rand.Seed(time.Now().UnixNano())
    randNum := rand.Intn(99999)

    vertexLabelName := fmt.Sprintf("testVertexLabel%d", randNum)
    pk1 := v1.PropertyKeyCreateRequestData{
        Name:        fmt.Sprintf("testPropertyKey%d", randNum),
        DataType:    model.PropertyDataTypeInt,
        Cardinality: model.PropertyCardinalitySingle,
    }

    pk1Resp, err := client.PropertyKey.PropertyKeyCreate(
        client.PropertyKey.PropertyKeyCreate.WithReqData(
            pk1,
        ),
    )
    fmt.Println("create testPropertyKey :" + pk1.Name)
    fmt.Println(pk1Resp)

    createResp, err := client.Vertexlabel.VertexlabelCreate(
        client.Vertexlabel.VertexlabelCreate.WithReqData(
            v1.VertexlabelCreateRequestData{
                Name:       vertexLabelName,
                IDStrategy: model.IDStrategyDefault,
                Properties: []string{
                    pk1.Name,
                },
                PrimaryKeys: []string{
                    pk1.Name,
                },
                NullableKeys:     []string{},
                EnableLabelIndex: true,
            },
        ),
    )
    if err != nil {
        log.Println(err)
    }
    fmt.Println("create testVertexLabel :" + vertexLabelName)
    fmt.Println(createResp)

    deleteVertexLabelResp, err := client.Vertexlabel.VertexlabelDeleteByName(
        client.Vertexlabel.VertexlabelDeleteByName.WithName(vertexLabelName),
    )
    if err != nil {
        log.Println(err)
    }
    fmt.Println("delete testVertexLabel :" + vertexLabelName)
    fmt.Println(deleteVertexLabelResp)

    deletePk1, err := client.PropertyKey.PropertyKeyDeleteByName(
        client.PropertyKey.PropertyKeyDeleteByName.WithName(pk1.Name),
    )
    if err != nil {
        log.Println(err)
    }
    fmt.Println("delete testPropertyKey :" + pk1.Name)
    fmt.Println(deletePk1)
}
