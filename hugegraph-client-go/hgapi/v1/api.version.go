// Licensed to the Apache Software Foundation (ASF) under one or more
// contributor license agreements. See the NOTICE file distributed with this
// work for additional information regarding copyright ownership. The ASF
// licenses this file to You under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
// WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
// License for the specific language governing permissions and limitations
// under the License.

package v1

import (
	"context"
	"encoding/json"
	"io"
	"io/ioutil"
	"net/http"

	"hugegraph.apache.org/client-go/hgapi"
)

// ----- API Definition -------------------------------------------------------

// 查看HugeGraph的版本信息
//
// See full documentation at https://hugegraph.apache.org/cn/docs/clients/restful-api/other/#1011-%E6%9F%A5%E7%9C%8Bhugegraph%E7%9A%84%E7%89%88%E6%9C%AC%E4%BF%A1%E6%81%AF
func newVersionFunc(t hgapi.Transport) Version {
	return func(o ...func(*VersionRequest)) (*VersionResponse, error) {
		var r = VersionRequest{}
		for _, f := range o {
			f(&r)
		}
		return r.Do(r.ctx, t)
	}
}

type Version func(o ...func(*VersionRequest)) (*VersionResponse, error)

type VersionRequest struct {
	Body io.Reader
	ctx  context.Context
}

type VersionResponse struct {
	StatusCode int                 `json:"-"`
	Header     http.Header         `json:"-"`
	Body       io.ReadCloser       `json:"-"`
	Versions   VersionResponseData `json:"versions"`
}

type VersionResponseData struct {
	Version string `json:"version"`
	Core    string `json:"core"`
	Gremlin string `json:"gremlin"`
	API     string `json:"api"`
}

func (r VersionRequest) Do(ctx context.Context, transport hgapi.Transport) (*VersionResponse, error) {

	req, _ := hgapi.NewRequest("GET", "/versions", r.Body)

	if ctx != nil {
		req = req.WithContext(ctx)
	}

	res, err := transport.Perform(req)
	if err != nil {
		return nil, err
	}

	versionResp := &VersionResponse{}
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
