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
    "testing"
)

func TestPropertyKeyUpdateUserdataRequest_Do(t *testing.T) {
    client, err := hugegraph.NewDefaultCommonClient()
    if err != nil {
        log.Println(err)
    }
    resp, err := client.PropertyKey.PropertyKeyUpdateUserdata(
        client.PropertyKey.PropertyKeyUpdateUserdata.WithReqData(
            v1.PropertyKeyUpdateUserdataRequestData{
                Action: model.ActionAppend,
                Name:   "age",
                UserData: struct {
                    Min int `json:"min"`
                    Max int `json:"max"`
                }(struct {
                    Min int
                    Max int
                }{Min: 0, Max: 101}),
            },
        ),
    )
    if err != nil {
        log.Println(err)
    }

    fmt.Println(resp)
}
