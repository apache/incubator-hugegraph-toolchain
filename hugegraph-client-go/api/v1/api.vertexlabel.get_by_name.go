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
    "io"
    "io/ioutil"
    "net/http"

    "github.com/apache/incubator-hugegraph-toolchain/hugegraph-client-go/api"
)

// ----- API Definition -------------------------------------------------------
// Get VertexLabel by name
//
// See full documentation at https://hugegraph.apache.org/docs/clients/restful-api/vertexlabel/#134-get-vertexlabel-by-name
func newVertexlabelGetFunc(t api.Transport) VertexlabelGet {
    return func(o ...func(*VertexlabelGetRequest)) (*VertexlabelGetResponse, error) {
        var r = VertexlabelGetRequest{}
        for _, f := range o {
            f(&r)
        }
        return r.Do(r.ctx, t)
    }
}

type VertexlabelGet func(o ...func(*VertexlabelGetRequest)) (*VertexlabelGetResponse, error)

type VertexlabelGetRequest struct {
    Body io.Reader
    ctx  context.Context
    name string
}

type VertexlabelGetResponse struct {
    StatusCode     int                        `json:"-"`
    Header         http.Header                `json:"-"`
    Body           io.ReadCloser              `json:"-"`
    VertexlabelGet VertexlabelGetResponseData `json:"-"`
}

type VertexlabelGetResponseData struct {
    ID               int           `json:"id"`
    Name             string        `json:"name"`
    IDStrategy       string        `json:"id_strategy"`
    PrimaryKeys      []string      `json:"primary_keys"`
    NullableKeys     []interface{} `json:"nullable_keys"`
    IndexLabels      []string      `json:"index_labels"`
    Properties       []string      `json:"properties"`
    Status           string        `json:"status"`
    TTL              int           `json:"ttl"`
    EnableLabelIndex bool          `json:"enable_label_index"`
    UserData         struct {
        CreateTime string `json:"~create_time"`
    } `json:"user_data"`
}

func (r VertexlabelGetRequest) Do(ctx context.Context, transport api.Transport) (*VertexlabelGetResponse, error) {

    if len(r.name) <= 0 {
        return nil, errors.New("VertexlabelGetRequest must set name")
    }

    req, err := api.NewRequest("GET", fmt.Sprintf("/graphs/%s/schema/vertexlabels/%s", transport.GetConfig().Graph, r.name), nil, r.Body)
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

    resp := &VertexlabelGetResponse{}
    bytes, err := ioutil.ReadAll(res.Body)
    if err != nil {
        return nil, err
    }
    respData := VertexlabelGetResponseData{}
    err = json.Unmarshal(bytes, &respData)
    if err != nil {
        return nil, err
    }
    resp.StatusCode = res.StatusCode
    resp.Header = res.Header
    resp.Body = res.Body
    resp.VertexlabelGet = respData
    return resp, nil
}

func (v VertexlabelGet) WithName(name string) func(r *VertexlabelGetRequest) {
    return func(r *VertexlabelGetRequest) {
        r.name = name
    }
}
