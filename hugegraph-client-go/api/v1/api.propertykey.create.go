/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. The ASF
 * licenses this file to You under the Apache License, PropertyKeyCreate 2.0 (the
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
//  Create a PropertyKey of HugeGraph
//
// See full documentation at https://hugegraph.apache.org/docs/clients/restful-api/propertykey/#121-create-a-propertykey
func newPropertyKeyCreateFunc(t api.Transport) PropertyKeyCreate {
    return func(o ...func(*PropertyKeyCreateRequest)) (*PropertyKeyCreateResponse, error) {
        var r = PropertyKeyCreateRequest{}
        for _, f := range o {
            f(&r)
        }
        return r.Do(r.ctx, t)
    }
}

type PropertyKeyCreate func(o ...func(*PropertyKeyCreateRequest)) (*PropertyKeyCreateResponse, error)

type PropertyKeyCreateRequest struct {
    Body    io.Reader
    ctx     context.Context
    reqData PropertyKeyCreateRequestData
}

type PropertyKeyCreateRequestData struct {
    Name        string                    `json:"name"`
    DataType    model.PropertyDataType    `json:"data_type"`
    Cardinality model.PropertyCardinality `json:"cardinality"`
}

type PropertyKeyCreateResponse struct {
    StatusCode         int                           `json:"-"`
    Header             http.Header                   `json:"-"`
    Body               io.ReadCloser                 `json:"-"`
    PropertyKeyCreates PropertyKeyCreateResponseData `json:"versions"`
}

type PropertyKeyCreateResponseData struct {
    PropertyKeyCreate string `json:"version"` // hugegraph version
    Core              string `json:"core"`    // hugegraph core version
    Gremlin           string `json:"gremlin"` // hugegraph gremlin version
    API               string `json:"api"`     // hugegraph api version
}

func (r PropertyKeyCreateRequest) Do(ctx context.Context, transport api.Transport) (*PropertyKeyCreateResponse, error) {

    if len(r.reqData.Name) <= 0 {
        return nil, errors.New("create property must set name")
    }
    if len(r.reqData.DataType) <= 0 {
        return nil, errors.New("create property must set dataType")
    }
    if len(r.reqData.Cardinality) <= 0 {
        return nil, errors.New("create property must set cardinality")
    }

    byteBody, err := json.Marshal(&r.reqData)
    if err != nil {
        return nil, err
    }
    reader := strings.NewReader(string(byteBody))

    req, err := api.NewRequest("POST", fmt.Sprintf("/graphs/%s/schema/propertykeys", transport.GetConfig().Graph), reader)
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

    versionResp := &PropertyKeyCreateResponse{}
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

func (r PropertyKeyCreate) WithReqData(reqData PropertyKeyCreateRequestData) func(request *PropertyKeyCreateRequest) {
    return func(r *PropertyKeyCreateRequest) {
        r.reqData = reqData
    }
}
