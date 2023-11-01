/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. The ASF
 * licenses this file to You under the Apache License, VertexlabelUpdateUserdata 2.0 (the
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
    "net/url"
    "strings"

    "github.com/apache/incubator-hugegraph-toolchain/hugegraph-client-go/api"
)

// ----- API Definition -------------------------------------------------------
//  Add or Remove userdata for an existing PropertyKey
//
// See full documentation at https://hugegraph.apache.org/docs/clients/restful-api/propertykey/#122-add-or-remove-userdata-for-an-existing-propertykey
func newVertexlabelUpdateUserdataFunc(t api.Transport) VertexlabelUpdateUserdata {
    return func(o ...func(*VertexlabelUpdateUserdataRequest)) (*VertexlabelUpdateUserdataResponse, error) {
        var r = VertexlabelUpdateUserdataRequest{}
        for _, f := range o {
            f(&r)
        }
        return r.Do(r.ctx, t)
    }
}

type VertexlabelUpdateUserdata func(o ...func(*VertexlabelUpdateUserdataRequest)) (*VertexlabelUpdateUserdataResponse, error)

type VertexlabelUpdateUserdataRequest struct {
    Body    io.Reader
    ctx     context.Context
    reqData VertexlabelUpdateUserdataRequestData
}

type VertexlabelUpdateUserdataRequestData struct {
    Action       model.Action `json:"-"`
    Name         string       `json:"name"`
    Properties   []string     `json:"properties"`
    NullableKeys []string     `json:"nullable_keys"`
    UserData     struct {
        Super string `json:"super"`
    } `json:"user_data"`
}

type VertexlabelUpdateUserdataResponse struct {
    StatusCode int                                   `json:"-"`
    Header     http.Header                           `json:"-"`
    Body       io.ReadCloser                         `json:"-"`
    Data       VertexlabelUpdateUserdataResponseData `json:"-"`
}

type VertexlabelUpdateUserdataResponseData struct {
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
    } `json:"user_data"`
}

func (r VertexlabelUpdateUserdataRequest) Do(ctx context.Context, transport api.Transport) (*VertexlabelUpdateUserdataResponse, error) {

    params := &url.Values{}
    if len(r.reqData.Action) <= 0 {
        return nil, errors.New("property update userdata must set action")
    } else {
        params.Add("action", string(r.reqData.Action))
    }
    if len(r.reqData.Name) <= 0 {
        return nil, errors.New("property update userdata must set name")
    }

    byteBody, err := json.Marshal(&r.reqData)
    if err != nil {
        return nil, err
    }
    reader := strings.NewReader(string(byteBody))

    req, err := api.NewRequest("PUT", fmt.Sprintf("/graphs/%s/schema/vertexlabels/%s", transport.GetConfig().Graph, r.reqData.Name), params, reader)
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

    resp := &VertexlabelUpdateUserdataResponse{}
    bytes, err := ioutil.ReadAll(res.Body)
    if err != nil {
        return nil, err
    }
    respData := VertexlabelUpdateUserdataResponseData{}
    err = json.Unmarshal(bytes, &respData)
    if err != nil {
        return nil, err
    }
    resp.StatusCode = res.StatusCode
    resp.Header = res.Header
    resp.Body = res.Body
    resp.Data = respData
    return resp, nil
}

func (r VertexlabelUpdateUserdata) WithReqData(reqData VertexlabelUpdateUserdataRequestData) func(request *VertexlabelUpdateUserdataRequest) {
    return func(r *VertexlabelUpdateUserdataRequest) {
        r.reqData = reqData
    }
}
