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
    "hugegraph.apache.org/client-go/api"
)

// API contains the Hugegraph APIs
type APIV1 struct {
    Version Version
    Vertex  struct {
        Create
    }
}

// New creates new API
func New(t api.Transport) *APIV1 {
    return &APIV1{
        Version: newVersionFunc(t),
        Vertex: struct {
            Create
        }{
            Create: newCreateFunc(t),
        },
    }
}
