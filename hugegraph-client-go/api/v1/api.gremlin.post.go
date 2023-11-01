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
    "github.com/apache/incubator-hugegraph-toolchain/hugegraph-client-go/api"
    "io"
    "io/ioutil"
    "net/http"
    "strings"
)

// ----- API Definition -------------------------------------------------------
// View Create a vertex
//
// See full documentation at https://hugegraph.apache.org/docs/clients/restful-api/vertex/#211-create-a-vertex
func newGremlinPostFunc(t api.Transport) GremlinPost {
    return func(o ...func(*GremlinPostRequest)) (*GremlinPostResponse, error) {
        var r = GremlinPostRequest{}
        for _, f := range o {
            f(&r)
        }
        return r.Do(r.ctx, t)
    }
}

type GremlinPost func(o ...func(*GremlinPostRequest)) (*GremlinPostResponse, error)

type GremlinPostRequest struct {
    ctx      context.Context
    body     io.ReadCloser
    gremlin  string
    bindings map[string]string
    language string
    aliases  struct {
        //Graph string `json:"graph"`
        //G     string `json:"g"`
    }
}

type GremlinPostRequestData struct {
    Gremlin  string            `json:"gremlin"`
    Bindings map[string]string `json:"bindings,omitempty"`
    Language string            `json:"language,omitempty"`
    Aliases  struct {
        //Graph string `json:"graph"`
        //G     string `json:"g"`
    } `json:"aliases,omitempty"`
}

type GremlinPostResponse struct {
    StatusCode int                      `json:"-"`
    Header     http.Header              `json:"-"`
    Body       io.ReadCloser            `json:"-"`
    Data       *GremlinPostResponseData `json:"data"`
}

type GremlinPostResponseData struct {
    RequestID string `json:"requestId,omitempty"`
    Status    struct {
        Message    string `json:"message"`
        Code       int    `json:"code"`
        Attributes struct {
        } `json:"attributes"`
    } `json:"status"`
    Result struct {
        Data interface{} `json:"data"`
        Meta interface{} `json:"meta"`
    } `json:"result,omitempty"`
    Exception string   `json:"exception,omitempty"`
    Message   string   `json:"message,omitempty"`
    Cause     string   `json:"cause,omitempty"`
    Trace     []string `json:"trace,omitempty"`
}

func (g GremlinPostRequest) Do(ctx context.Context, transport api.Transport) (*GremlinPostResponse, error) {

    if len(g.gremlin) < 1 {
        return nil, errors.New("GremlinPostRequest param error , gremlin is empty")
    }

    if len(g.language) < 1 {
        g.language = "gremlin-groovy"
    }

    gd := &GremlinPostRequestData{
        Gremlin:  g.gremlin,
        Bindings: g.bindings,
        Language: g.language,
        Aliases:  g.aliases,
    }

    byteBody, err := json.Marshal(&gd) // 序列化

    if err != nil {
        return nil, err
    }

    reader := strings.NewReader(string(byteBody))
    req, _ := api.NewRequest("POST", "/gremlin", nil, reader)

    if ctx != nil {
        req = req.WithContext(ctx)
    }

    res, err := transport.Perform(req)
    if err != nil {
        return nil, err
    }

    gremlinPostResp := &GremlinPostResponse{}
    bytes, err := ioutil.ReadAll(res.Body)
    if err != nil {
        return nil, err
    }

    respData := &GremlinPostResponseData{}
    err = json.Unmarshal(bytes, respData)
    if err != nil {
        return nil, err
    }
    gremlinPostResp.StatusCode = res.StatusCode
    gremlinPostResp.Header = res.Header
    gremlinPostResp.Body = res.Body
    gremlinPostResp.Data = respData
    return gremlinPostResp, nil
}

func (g GremlinPost) WithGremlin(gremlin string) func(request *GremlinPostRequest) {
    return func(r *GremlinPostRequest) {
        r.gremlin = gremlin
    }
}
