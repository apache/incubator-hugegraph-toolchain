/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. The ASF
 * licenses this file to You under the Apache License, VertexlabelCreate 2.0 (the
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
    "github.com/apache/incubator-hugegraph-toolchain/hugegraph-client-go/internal/model"
    "io"
    "io/ioutil"
    "net/http"
    "strings"

    "github.com/apache/incubator-hugegraph-toolchain/hugegraph-client-go/api"
)

// ----- API Definition -------------------------------------------------------
// Create a VertexLabel
//
// See full documentation at https://hugegraph.apache.org/docs/clients/restful-api/vertexlabel/#131-create-a-vertexlabel
func newVertexlabelCreateFunc(t api.Transport) VertexlabelCreate {
    return func(o ...func(*VertexlabelCreateRequest)) (*VertexlabelCreateResponse, error) {
        var r = VertexlabelCreateRequest{}
        for _, f := range o {
            f(&r)
        }
        return r.Do(r.ctx, t)
    }
}

type VertexlabelCreate func(o ...func(*VertexlabelCreateRequest)) (*VertexlabelCreateResponse, error)

type VertexlabelCreateRequest struct {
    Body    io.Reader
    ctx     context.Context
    reqData VertexlabelCreateRequestData
}

type VertexlabelCreateRequestData struct {
    Name             string           `json:"name"`
    IDStrategy       model.IDStrategy `json:"id_strategy"`
    Properties       []string         `json:"properties"`
    PrimaryKeys      []string         `json:"primary_keys"`
    NullableKeys     []string         `json:"nullable_keys"`
    EnableLabelIndex bool             `json:"enable_label_index"`
}

type VertexlabelCreateResponse struct {
    StatusCode int                           `json:"-"`
    Header     http.Header                   `json:"-"`
    Body       io.ReadCloser                 `json:"-"`
    RespData   VertexlabelCreateResponseData `json:"-"`
}

type VertexlabelCreateResponseData struct {
    ID               int              `json:"id"`
    PrimaryKeys      []string         `json:"primary_keys"`
    IDStrategy       model.IDStrategy `json:"id_strategy"`
    Name             string           `json:"name"`
    IndexNames       []string         `json:"index_names"`
    Properties       []string         `json:"properties"`
    NullableKeys     []string         `json:"nullable_keys"`
    EnableLabelIndex bool             `json:"enable_label_index"`
    UserData         struct {
    } `json:"user_data"`
}

func (r VertexlabelCreateRequest) Do(ctx context.Context, transport api.Transport) (*VertexlabelCreateResponse, error) {

    if len(r.reqData.Name) <= 0 {
        return nil, errors.New("create property must set name")
    }
    if len(r.reqData.IDStrategy) <= 0 {
        return nil, errors.New("create property must set id_strategy")
    }
    if len(r.reqData.Properties) <= 0 {
        return nil, errors.New("create property must set properties")
    }

    byteBody, err := json.Marshal(&r.reqData)
    if err != nil {
        return nil, err
    }
    reader := strings.NewReader(string(byteBody))

    req, err := api.NewRequest("POST", fmt.Sprintf("/graphs/%s/schema/vertexlabels", transport.GetConfig().Graph), nil, reader)
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

    resp := &VertexlabelCreateResponse{}
    respData := VertexlabelCreateResponseData{}
    bytes, err := ioutil.ReadAll(res.Body)
    if err != nil {
        return nil, err
    }
    err = json.Unmarshal(bytes, &respData)
    if err != nil {
        return nil, err
    }
    resp.StatusCode = res.StatusCode
    resp.Header = res.Header
    resp.Body = res.Body
    resp.RespData = respData
    return resp, nil
}

func (r VertexlabelCreate) WithReqData(reqData VertexlabelCreateRequestData) func(request *VertexlabelCreateRequest) {
    return func(r *VertexlabelCreateRequest) {
        r.reqData = reqData
    }
}
