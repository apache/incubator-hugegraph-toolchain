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
// Delete PropertyKey according to name
//
// See full documentation at https://hugegraph.apache.org/docs/clients/restful-api/propertykey/#125-delete-propertykey-according-to-name
func newPropertyKeyDeleteByNameFunc(t api.Transport) PropertyKeyDeleteByName {
    return func(o ...func(*PropertyKeyDeleteByNameRequest)) (*PropertyKeyDeleteByNameResponse, error) {
        var r = PropertyKeyDeleteByNameRequest{}
        for _, f := range o {
            f(&r)
        }
        return r.Do(r.ctx, t)
    }
}

type PropertyKeyDeleteByName func(o ...func(*PropertyKeyDeleteByNameRequest)) (*PropertyKeyDeleteByNameResponse, error)

type PropertyKeyDeleteByNameRequest struct {
    Body io.Reader
    ctx  context.Context
    name string
}

type PropertyKeyDeleteByNameResponse struct {
    StatusCode int                                 `json:"-"`
    Header     http.Header                         `json:"-"`
    Body       io.ReadCloser                       `json:"-"`
    Data       PropertyKeyDeleteByNameResponseData `json:"versions"`
}

type PropertyKeyDeleteByNameResponseData struct {
    TaskID int `json:"task_id"`
}

func (r PropertyKeyDeleteByNameRequest) Do(ctx context.Context, transport api.Transport) (*PropertyKeyDeleteByNameResponse, error) {

    if len(r.name) <= 0 {
        return nil, errors.New("delete by name ,please set name")
    }
    req, err := api.NewRequest("DELETE", fmt.Sprintf("/graphs/%s/schema/propertykeys/%s", transport.GetConfig().Graph, r.name), nil, r.Body)
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

    resp := &PropertyKeyDeleteByNameResponse{}
    respData := PropertyKeyDeleteByNameResponseData{}
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
    resp.Data = respData
    return resp, nil
}

func (p PropertyKeyDeleteByName) WithName(name string) func(request *PropertyKeyDeleteByNameRequest) {
    return func(r *PropertyKeyDeleteByNameRequest) {
        r.name = name
    }
}
