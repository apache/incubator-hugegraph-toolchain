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

package v1_test

import (
    "context"

    "io"
    "reflect"
    "testing"

    hugegraph "hugegraph.apache.org/client-go"
    "hugegraph.apache.org/client-go/api"
    v1 "hugegraph.apache.org/client-go/api/v1"
)

func TestVersionRequest_Do(t *testing.T) {

    client, err := hugegraph.NewDefaultCommonClient()
    if err != nil {
        t.Errorf("NewDefaultCommonClient() error = %v", err)
    }
    ctx := context.Background()

    type fields struct {
        Body io.Reader
        ctx  context.Context
    }
    type args struct {
        ctx       context.Context
        transport api.Transport
    }
    tests := []struct {
        name    string
        fields  fields
        args    args
        want    v1.VersionResponseData
        wantErr bool
    }{

        {
            name: "test-version",
            fields: fields{
                Body: nil,
                ctx:  ctx,
            },
            args: args{
                ctx:       ctx,
                transport: client.Transport,
            },
            want: v1.VersionResponseData{
                Version: "v1",
                Core:    "1.0.0",
                Gremlin: "3.4.3",
                API:     "0.69.0.0",
            },
        },
    }
    for _, tt := range tests {
        t.Run(tt.name, func(t *testing.T) {
            got, err := client.Version()
            if (err != nil) != tt.wantErr {
                t.Errorf("Do() error = %v, wantErr %v", err, tt.wantErr)
                return
            }
            if !reflect.DeepEqual(got.Versions, tt.want) {
                t.Errorf("Do() got = %v, want %v", got.Versions, tt.want)
            }
        })
    }
}
