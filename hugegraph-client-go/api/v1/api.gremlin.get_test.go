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
    "io/ioutil"
    "log"
    "net/http"
    "testing"
)

func TestGremlinGetRequest_Do(t *testing.T) {

    client, err := hugegraph.NewDefaultCommonClient()
    if err != nil {
        log.Println(err)
    }
    resp, err := client.Gremlin.GremlinGet(
        client.Gremlin.GremlinGet.WithGremlin("hugegraph.traversal().V().limit(3)"),
    )
    if err != nil {
        log.Fatalln(err)
    }
    fmt.Println(resp.Body)

    t1()
}

func t1()  {
    url := "http://82.157.70.33:18080/gremlin?gremlin=hugegraph.traversal().V().limit(3)"

    req, _ := http.NewRequest("GET", url, nil)

    res, _ := http.DefaultClient.Do(req)

    defer res.Body.Close()
    body, _ := ioutil.ReadAll(res.Body)

    fmt.Println(res)
    fmt.Println(string(body))
}