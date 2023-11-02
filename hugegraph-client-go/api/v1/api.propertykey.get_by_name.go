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
//  Get PropertyKey according to name of HugeGraph
//
// See full documentation at https://hugegraph.apache.org/docs/clients/restful-api/propertykey/#124-get-propertykey-according-to-name
func newPropertyKeyGetByNameFunc(t api.Transport) PropertyKeyGetByName {
    return func(o ...func(*PropertyKeyGetByNameRequest)) (*PropertyKeyGetByNameResponse, error) {
        var r = PropertyKeyGetByNameRequest{}
        for _, f := range o {
            f(&r)
        }
        return r.Do(r.ctx, t)
    }
}

type PropertyKeyGetByName func(o ...func(*PropertyKeyGetByNameRequest)) (*PropertyKeyGetByNameResponse, error)

type PropertyKeyGetByNameRequest struct {
    Body io.Reader
    ctx  context.Context
    name string
}

type PropertyKeyGetByNameResponse struct {
    StatusCode           int                              `json:"-"`
    Header               http.Header                      `json:"-"`
    Body                 io.ReadCloser                    `json:"-"`
    PropertyKeyGetByName PropertyKeyGetByNameResponseData `json:"-"`
}

type PropertyKeyGetByNameResponseData struct {
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
}

func (r PropertyKeyGetByNameRequest) Do(ctx context.Context, transport api.Transport) (*PropertyKeyGetByNameResponse, error) {

    if len(r.name) <= 0 {
        return nil, errors.New("get_by_name must set name")
    }

    req, err := api.NewRequest("GET", fmt.Sprintf("/graphs/%s/schema/propertykeys/%s", transport.GetConfig().Graph, r.name), nil, r.Body)
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

    resp := &PropertyKeyGetByNameResponse{}
    bytes, err := ioutil.ReadAll(res.Body)
    if err != nil {
        return nil, err
    }
    respData := PropertyKeyGetByNameResponseData{}
    err = json.Unmarshal(bytes, &respData)
    if err != nil {
        return nil, err
    }
    resp.StatusCode = res.StatusCode
    resp.Header = res.Header
    resp.Body = res.Body
    resp.PropertyKeyGetByName = respData
    return resp, nil
}

func (r PropertyKeyGetByName) WithName(name string) func(request *PropertyKeyGetByNameRequest) {
    return func(r *PropertyKeyGetByNameRequest) {
        r.name = name
    }
}
