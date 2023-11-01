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

package api

import (
    "bytes"
    "context"
    "io"
    "io/ioutil"
    "net/http"
    "net/url"
    "strings"
)

const (
    headerContentType = "Content-Type"
)

var (
    headerContentTypeJSON = []string{"application/json"}
)

// Request defines the API request.
type Request interface {
    Do(ctx context.Context, transport Transport) (*Response, error)
}

// newRequest creates an HTTP request.
func NewRequest(method, path string, params *url.Values, body io.Reader) (*http.Request, error) {

    u := &url.URL{
        Path: path,
    }
    if params != nil {
        u.RawQuery = params.Encode()
    }
    r := http.Request{
        Method:     method,
        URL:        u,
        Proto:      "HTTP/1.1",
        ProtoMajor: 1,
        ProtoMinor: 1,
        Header:     make(http.Header),
    }

    if body != nil {
        switch b := body.(type) {
        case *bytes.Buffer:
            r.Body = ioutil.NopCloser(body)
            r.ContentLength = int64(b.Len())
        case *bytes.Reader:
            r.Body = ioutil.NopCloser(body)
            r.ContentLength = int64(b.Len())
        case *strings.Reader:
            r.Body = ioutil.NopCloser(body)
            r.ContentLength = int64(b.Len())
        default:
            r.Body = ioutil.NopCloser(body)
        }
    }

    return &r, nil
}
