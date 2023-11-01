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

package v1

import (
    "context"
    "encoding/json"
    "errors"
    "fmt"
    "github.com/apache/incubator-hugegraph-toolchain/hugegraph-client-go/api"
    "io"
    "io/ioutil"
    "net/http"
    url2 "net/url"
)

// ----- API Definition -------------------------------------------------------
// View Create a vertex
//
// See full documentation at https://hugegraph.apache.org/docs/clients/restful-api/vertex/#211-create-a-vertex
func newGremlinGetFunc(t api.Transport) GremlinGet {
    return func(o ...func(*GremlinGetRequest)) (*GremlinGetResponse, error) {
        var r = GremlinGetRequest{}
        for _, f := range o {
            f(&r)
        }
        return r.Do(r.ctx, t)
    }
}

type GremlinGet func(o ...func(*GremlinGetRequest)) (*GremlinGetResponse, error)

type GremlinGetRequest struct {
    ctx      context.Context
    gremlin  string
    bindings map[string]string
    language string
    aliases  map[string]string
}

type GremlinGetResponse struct {
    StatusCode int           `json:"-"`
    Header     http.Header   `json:"-"`
    Body       io.ReadCloser `json:"-"`
}

func (g GremlinGetRequest) Do(ctx context.Context, transport api.Transport) (*GremlinGetResponse, error) {

    url := "/gremlin"
    params := &url2.Values{}
    if len(g.gremlin) <= 0 {
        return nil, errors.New("please set gremlin")
    } else {
        params.Add("gremlin", g.gremlin)
    }
    if len(g.language) > 0 {
        params.Add("language", g.language)
    }

    if g.aliases != nil && len(g.aliases) >= 0 {
        aliasesJsonStr, err := json.Marshal(g.aliases)
        if err != nil {
            return nil, err
        }
        params.Add("aliases", string(aliasesJsonStr))
    }

    if g.bindings != nil && len(g.bindings) >= 0 {
        bindingsJsonStr, err := json.Marshal(g.bindings)
        if err != nil {
            return nil, err
        }
        params.Add("bindings", string(bindingsJsonStr))
    }

    req, err := api.NewRequest("GET", url, params, nil)
    if err != nil {
        return nil, err
    }
    if ctx != nil {
        req = req.WithContext(ctx)
    }

    res, err := transport.Perform(req)
    if err != nil {
        return nil, err
    }

    bytes, err := ioutil.ReadAll(res.Body)
    if err != nil {
        return nil, err
    }

    fmt.Println(string(bytes))

    gremlinGetResponse := &GremlinGetResponse{}
    gremlinGetResponse.StatusCode = res.StatusCode
    return gremlinGetResponse, nil
}

func (g GremlinGet) WithGremlin(gremlin string) func(request *GremlinGetRequest) {
    return func(r *GremlinGetRequest) {
        r.gremlin = gremlin
    }
}
