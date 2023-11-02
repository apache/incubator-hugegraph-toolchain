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
    "fmt"
    "io"
    "io/ioutil"
    "net/http"

    "github.com/apache/incubator-hugegraph-toolchain/hugegraph-client-go/api"
)

// ----- API Definition -------------------------------------------------------
// Get all VertexLabels
//
// See full documentation at https://hugegraph.apache.org/docs/clients/restful-api/vertexlabel/#133-get-all-vertexlabels
func newVertexlabelGetAllFunc(t api.Transport) VertexlabelGetAll {
    return func(o ...func(*VertexlabelGetAllRequest)) (*VertexlabelGetAllResponse, error) {
        var r = VertexlabelGetAllRequest{}
        for _, f := range o {
            f(&r)
        }
        return r.Do(r.ctx, t)
    }
}

type VertexlabelGetAll func(o ...func(*VertexlabelGetAllRequest)) (*VertexlabelGetAllResponse, error)

type VertexlabelGetAllRequest struct {
    Body io.Reader
    ctx  context.Context
}

type VertexlabelGetAllResponse struct {
    StatusCode        int                           `json:"-"`
    Header            http.Header                   `json:"-"`
    Body              io.ReadCloser                 `json:"-"`
    VertexlabelGetAll VertexlabelGetAllResponseData `json:"-"`
}

type VertexlabelGetAllResponseData struct {
    Vertexlabels []struct {
        ID               int           `json:"id"`
        PrimaryKeys      []string      `json:"primary_keys"`
        IDStrategy       string        `json:"id_strategy"`
        Name             string        `json:"name"`
        IndexNames       []interface{} `json:"index_names"`
        Properties       []string      `json:"properties"`
        NullableKeys     []string      `json:"nullable_keys"`
        EnableLabelIndex bool          `json:"enable_label_index"`
        UserData         struct {
            Super string `json:"super"`
        } `json:"user_data,omitempty"`
    } `json:"vertexlabels"`
}

func (r VertexlabelGetAllRequest) Do(ctx context.Context, transport api.Transport) (*VertexlabelGetAllResponse, error) {

    req, err := api.NewRequest("GET", fmt.Sprintf("/graphs/%s/schema/vertexlabels", transport.GetConfig().Graph), nil, r.Body)
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

    resp := &VertexlabelGetAllResponse{}
    bytes, err := ioutil.ReadAll(res.Body)
    if err != nil {
        return nil, err
    }
    respData := VertexlabelGetAllResponseData{}
    err = json.Unmarshal(bytes, &respData)
    if err != nil {
        return nil, err
    }
    resp.StatusCode = res.StatusCode
    resp.Header = res.Header
    resp.Body = res.Body
    resp.VertexlabelGetAll = respData
    return resp, nil
}
