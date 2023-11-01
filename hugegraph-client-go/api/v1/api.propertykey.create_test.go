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

func TestPropertyKeyCreateRequest_Do(t *testing.T) {
    client, err := hugegraph.NewDefaultCommonClient()
    if err != nil {
        log.Println(err)
    }
    rand.Seed(time.Now().UnixNano())
    name := fmt.Sprintf("testProperty%d", rand.Intn(99999))
    resp, err := client.PropertyKey.PropertyKeyCreate(
        client.PropertyKey.PropertyKeyCreate.WithReqData(
            v1.PropertyKeyCreateRequestData{
                Name:        name,
                DataType:    model.PropertyDataTypeInt,
                Cardinality: model.PropertyCardinalitySingle,
            },
        ),
    )
    if err != nil {
        log.Println(err)
    }

    fmt.Println(resp)

    // propertyKey delete after create
    respDelete, err := client.PropertyKey.PropertyKeyDeleteByName(
        client.PropertyKey.PropertyKeyDeleteByName.WithName(name),
    )
    if err != nil {
        log.Println(err)
    }
    fmt.Println(respDelete)
}
