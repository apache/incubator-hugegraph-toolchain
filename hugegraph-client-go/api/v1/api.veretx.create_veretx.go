/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. The ASF
 * licenses this file to You under the Apache License, CreateVertex 2.0 (the
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
	"hugegraph.apache.org/client-go/hgtransport"
	"hugegraph.apache.org/client-go/internal/model"
	"io"
	"io/ioutil"
	"log"
	"net/http"

	"hugegraph.apache.org/client-go/api"
)

// ----- API Definition -------------------------------------------------------
// View Create a vertex
//
// See full documentation at https://hugegraph.apache.org/docs/clients/restful-api/vertex/#211-create-a-vertex
func newCreateVertexFunc(t api.Transport) CreateVertex {
	return func(o ...func(*CreateVertexRequest)) (*CreateVertexResponse, error) {
		var r = CreateVertexRequest{}
		for _, f := range o {
			f(&r)
		}
		return r.Do(r.ctx, t)
	}
}

type CreateVertex func(o ...func(*CreateVertexRequest)) (*CreateVertexResponse, error)

type CreateVertexRequest struct {
	Body       io.Reader
	ctx        context.Context
	config     hgtransport.Config
	Label      string
	Properties []byte
}

type CreateVertexResponse struct {
	StatusCode   int                       `json:"-"`
	Header       http.Header               `json:"-"`
	Body         io.ReadCloser             `json:"-"`
	CreateVertex *CreateVertexResponseData `json:"CreateVertex"`
}

type CreateVertexResponseData struct {
	ID         string `json:"id"`
	Label      string `json:"label"`
	Type       string `json:"type"`
	Properties any    `json:"properties"`
}

func (r CreateVertexRequest) Do(ctx context.Context, transport api.Transport) (*CreateVertexResponse, error) {

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

	createVertexRespData := &CreateVertexResponseData{}
	bytes, err := ioutil.ReadAll(res.Body)
	if err != nil {
		return nil, err
	}
	err = json.Unmarshal(bytes, createVertexRespData)
	if err != nil {
		return nil, err
	}
	createVertexResp := &CreateVertexResponse{}
	createVertexResp.StatusCode = res.StatusCode
	createVertexResp.Header = res.Header
	createVertexResp.Body = res.Body
	createVertexResp.CreateVertex = createVertexRespData
	return createVertexResp, nil
}

func (c CreateVertex) WithVertex(vertex *model.Vertex[any]) func(*CreateVertexRequest) {
	return func(r *CreateVertexRequest) {
		jsonStr, err := json.Marshal(vertex.Properties)
		if err != nil {
			log.Println(err)
		}
		r.Properties = jsonStr
	}
}
