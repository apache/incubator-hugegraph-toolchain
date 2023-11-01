/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. The ASF
 * licenses this file to You under the Apache License, PropertyKeyGetAll 2.0 (the
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
//  Get all PropertyKeys of HugeGraph
//
// See full documentation at https://hugegraph.apache.org/docs/clients/restful-api/propertykey/#123-get-all-propertykeys
func newPropertyKeyGetAllFunc(t api.Transport) PropertyKeyGetAll {
    return func(o ...func(*PropertyKeyGetAllRequest)) (*PropertyKeyGetAllResponse, error) {
        var r = PropertyKeyGetAllRequest{}
        for _, f := range o {
            f(&r)
        }
        return r.Do(r.ctx, t)
    }
}

type PropertyKeyGetAll func(o ...func(*PropertyKeyGetAllRequest)) (*PropertyKeyGetAllResponse, error)

type PropertyKeyGetAllRequest struct {
    Body io.Reader
    ctx  context.Context
}

type PropertyKeyGetAllResponse struct {
    StatusCode        int                           `json:"-"`
    Header            http.Header                   `json:"-"`
    Body              io.ReadCloser                 `json:"-"`
    PropertyKeyGetAll PropertyKeyGetAllResponseData `json:"-"`
}

type PropertyKeyGetAllResponseData struct {
    Propertykeys []struct {
        ID          int           `json:"id"`
        Name        string        `json:"name"`
        DataType    string        `json:"data_type"`
        Cardinality string        `json:"cardinality"`
        Properties  []interface{} `json:"properties"`
        UserData    struct {
        } `json:"user_data"`
    } `json:"propertykeys"`
}

func (r PropertyKeyGetAllRequest) Do(ctx context.Context, transport api.Transport) (*PropertyKeyGetAllResponse, error) {

    req, err := api.NewRequest("GET", fmt.Sprintf("/graphs/%s/schema/propertykeys", transport.GetConfig().Graph), r.Body)
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

    resp := &PropertyKeyGetAllResponse{}
    bytes, err := ioutil.ReadAll(res.Body)
    if err != nil {
        return nil, err
    }
    respData := PropertyKeyGetAllResponseData{}
    err = json.Unmarshal(bytes, &respData)
    if err != nil {
        return nil, err
    }
    resp.StatusCode = res.StatusCode
    resp.Header = res.Header
    resp.Body = res.Body
    resp.PropertyKeyGetAll = respData
    return resp, nil
}
