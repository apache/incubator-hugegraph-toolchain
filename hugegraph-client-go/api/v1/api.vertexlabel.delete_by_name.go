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
// Delete VertexLabel by name
//
// See full documentation at https://hugegraph.apache.org/docs/clients/restful-api/vertexlabel/#135-delete-vertexlabel-by-name
func newVertexlabelDeleteByNameFunc(t api.Transport) VertexlabelDeleteByName {
    return func(o ...func(*VertexlabelDeleteByNameRequest)) (*VertexlabelDeleteByNameResponse, error) {
        var r = VertexlabelDeleteByNameRequest{}
        for _, f := range o {
            f(&r)
        }
        return r.Do(r.ctx, t)
    }
}

type VertexlabelDeleteByName func(o ...func(*VertexlabelDeleteByNameRequest)) (*VertexlabelDeleteByNameResponse, error)

type VertexlabelDeleteByNameRequest struct {
    Body io.Reader
    ctx  context.Context
    name string
}

type VertexlabelDeleteByNameResponse struct {
    StatusCode               int                                 `json:"-"`
    Header                   http.Header                         `json:"-"`
    Body                     io.ReadCloser                       `json:"-"`
    VertexlabelDeleteByNames VertexlabelDeleteByNameResponseData `json:"versions"`
}

type VertexlabelDeleteByNameResponseData struct {
    TaskID int `json:"task_id"`
}

func (r VertexlabelDeleteByNameRequest) Do(ctx context.Context, transport api.Transport) (*VertexlabelDeleteByNameResponse, error) {

    if len(r.name) <= 0 {
        return nil, errors.New("delete by name ,please set name")
    }
    req, err := api.NewRequest("DELETE", fmt.Sprintf("/graphs/%s/schema/vertexlabels/%s", transport.GetConfig().Graph, r.name), nil, r.Body)
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

    versionResp := &VertexlabelDeleteByNameResponse{}
    bytes, err := ioutil.ReadAll(res.Body)
    if err != nil {
        return nil, err
    }
    err = json.Unmarshal(bytes, versionResp)
    if err != nil {
        return nil, err
    }
    versionResp.StatusCode = res.StatusCode
    versionResp.Header = res.Header
    versionResp.Body = res.Body
    return versionResp, nil
}

func (p VertexlabelDeleteByName) WithName(name string) func(request *VertexlabelDeleteByNameRequest) {
    return func(r *VertexlabelDeleteByNameRequest) {
        r.name = name
    }
}
