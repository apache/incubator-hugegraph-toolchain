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
func newPropertyKeyUpdateUserdataFunc(t api.Transport) PropertyKeyUpdateUserdata {
    return func(o ...func(*PropertyKeyUpdateUserdataRequest)) (*PropertyKeyUpdateUserdataResponse, error) {
        var r = PropertyKeyUpdateUserdataRequest{}
        for _, f := range o {
            f(&r)
        }
        return r.Do(r.ctx, t)
    }
}

type PropertyKeyUpdateUserdata func(o ...func(*PropertyKeyUpdateUserdataRequest)) (*PropertyKeyUpdateUserdataResponse, error)

type PropertyKeyUpdateUserdataRequest struct {
    Body    io.Reader
    ctx     context.Context
    reqData PropertyKeyUpdateUserdataRequestData
}

type PropertyKeyUpdateUserdataRequestData struct {
    Action   model.Action `json:"-"`
    Name     string       `json:"name"`
    UserData struct {
        Min int `json:"min"`
        Max int `json:"max"`
    } `json:"user_data"`
}

type PropertyKeyUpdateUserdataResponse struct {
    StatusCode                int                                   `json:"-"`
    Header                    http.Header                           `json:"-"`
    Body                      io.ReadCloser                         `json:"-"`
    PropertyKeyUpdateUserdata PropertyKeyUpdateUserdataResponseData `json:"-"`
}

type PropertyKeyUpdateUserdataResponseData struct {
    PropertyKey struct {
        ID            int           `json:"id"`
        Name          string        `json:"name"`
        DataType      string        `json:"data_type"`
        Cardinality   string        `json:"cardinality"`
        AggregateType string        `json:"aggregate_type"`
        WriteType     string        `json:"write_type"`
        Properties    []interface{} `json:"properties"`
        Status        string        `json:"status"`
        UserData      struct {
            Min        int    `json:"min"`
            Max        int    `json:"max"`
            CreateTime string `json:"~create_time"`
        } `json:"user_data"`
    } `json:"property_key"`
    TaskID int `json:"task_id"`
}

func (r PropertyKeyUpdateUserdataRequest) Do(ctx context.Context, transport api.Transport) (*PropertyKeyUpdateUserdataResponse, error) {

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

    req, err := api.NewRequest("PUT", fmt.Sprintf("/graphs/%s/schema/propertykeys/%s", transport.GetConfig().Graph, r.reqData.Name), params, reader)
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

    resp := &PropertyKeyUpdateUserdataResponse{}
    bytes, err := ioutil.ReadAll(res.Body)
    if err != nil {
        return nil, err
    }
    respData := PropertyKeyUpdateUserdataResponseData{}
    err = json.Unmarshal(bytes, &respData)
    if err != nil {
        return nil, err
    }
    resp.StatusCode = res.StatusCode
    resp.Header = res.Header
    resp.Body = res.Body
    resp.PropertyKeyUpdateUserdata = respData
    return resp, nil
}

func (r PropertyKeyUpdateUserdata) WithReqData(reqData PropertyKeyUpdateUserdataRequestData) func(request *PropertyKeyUpdateUserdataRequest) {
    return func(r *PropertyKeyUpdateUserdataRequest) {
        r.reqData = reqData
    }
}
