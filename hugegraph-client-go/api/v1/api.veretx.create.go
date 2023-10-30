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
    "log"
    "net/http"

    "github.com/apache/incubator-hugegraph-toolchain/hugegraph-client-go/api"
    "github.com/apache/incubator-hugegraph-toolchain/hugegraph-client-go/hgtransport"
    "github.com/apache/incubator-hugegraph-toolchain/hugegraph-client-go/internal/model"
)

// ----- API Definition -------------------------------------------------------
// View Create a vertex
//
// See full documentation at https://hugegraph.apache.org/docs/clients/restful-api/vertex/#211-create-a-vertex
func newCreateFunc(t api.Transport) Create {
    return func(o ...func(*CreateRequest)) (*CreateResponse, error) {
        var r = CreateRequest{}
        for _, f := range o {
            f(&r)
        }
        return r.Do(r.ctx, t)
    }
}

type Create func(o ...func(*CreateRequest)) (*CreateResponse, error)

type CreateRequest struct {
    Body       io.Reader
    ctx        context.Context
    config     hgtransport.Config
    Label      string
    Properties []byte
}

type CreateResponse struct {
    StatusCode int                 `json:"-"`
    Header     http.Header         `json:"-"`
    Body       io.ReadCloser       `json:"-"`
    CreateData *CreateResponseData `json:"Create"`
}

type CreateResponseData struct {
    ID         string `json:"id"`
    Label      string `json:"label"`
    Type       string `json:"type"`
    Properties any    `json:"properties"`
}

func (r CreateRequest) Do(ctx context.Context, transport api.Transport) (*CreateResponse, error) {

    config := transport.GetConfig()
    url := ""
    if len(config.GraphSpace) > 0 {
        url = fmt.Sprintf("/graphspaces/%s/graphs/%s/graph/vertices", config.GraphSpace, config.Graph)
    } else {
        url = fmt.Sprintf("/graphs/%s/graph/vertices", config.Graph)
    }

    req, err := api.NewRequest("POST", url, r.Body)
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

    CreateRespData := &CreateResponseData{}
    bytes, err := ioutil.ReadAll(res.Body)
    if err != nil {
        return nil, err
    }
    err = json.Unmarshal(bytes, CreateRespData)
    if err != nil {
        return nil, err
    }
    CreateResp := &CreateResponse{}
    CreateResp.StatusCode = res.StatusCode
    CreateResp.Header = res.Header
    CreateResp.Body = res.Body
    CreateResp.CreateData = CreateRespData
    return CreateResp, nil
}

func (c Create) WithVertex(vertex *model.Vertex[any]) func(*CreateRequest) {
    return func(r *CreateRequest) {
        jsonStr, err := json.Marshal(vertex.Properties)
        if err != nil {
            log.Println(err)
        }
        r.Properties = jsonStr
    }
}
