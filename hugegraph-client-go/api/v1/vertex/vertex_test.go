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

package vertex_test

import (
    "testing"
)

type Person struct {
    Name string `json:"name,omitempty"`
    Age  uint64 `json:"age,omitempty"`
    City string `json:"city,omitempty"`
}

func TestVertex(t *testing.T) {

    // 1.create
    //client, err := hugegraph.NewDefaultCommonClient()
    //ctx := context.Background()
    //if err != nil {
    //    log.Println(err)
    //}
    //person := Person{
    //    Name: "tom",
    //    Age:  18,
    //    City: "beijing",
    //}
    //
    //vertex := model.Vertex[any]{
    //    Label:      "person",
    //    Properties: person,
    //}
    //
    //// create
    //respCreate, err := client.Vertex.Create(
    //    client.Vertex.Create.WithContext(ctx),
    //    client.Vertex.Create.WithVertex(vertex),
    //)
    //if err != nil {
    //    log.Println(err)
    //}
    //vertexID := respCreate.Data.ID
    //fmt.Println(vertexID)
    //
    //// batchCreate
    //vertices := []model.Vertex[any]{
    //    {
    //        Label: "person",
    //        Properties: Person{
    //            Name: "bob",
    //            Age:  22,
    //            City: "shanghai",
    //        },
    //    },
    //    {
    //        Label: "person",
    //        Properties: Person{
    //            Name: "angle",
    //            Age:  28,
    //            City: "guangzhou",
    //        },
    //    },
    //}
    //
    //respBatchResp, err := client.Vertex.BatchCreate(
    //    client.Vertex.BatchCreate.WithContext(ctx),
    //    client.Vertex.BatchCreate.WithVertices(vertices),
    //)
    //if err != nil {
    //    log.Println(err)
    //}
    //for i, datum := range respBatchResp.IDs {
    //    fmt.Printf("index:%d\tid:%s\n", i, datum)
    //}
    //
    //// update properties
    //updatePerson := model.Vertex[any]{
    //    Label: "person",
    //    Properties: Person{
    //        Age: 10,
    //    },
    //}
    //respUpdate, err := client.Vertex.UpdateProperties(
    //    client.Vertex.UpdateProperties.WithContext(ctx),
    //    client.Vertex.UpdateProperties.WithVertex(updatePerson),
    //    client.Vertex.UpdateProperties.WithID(vertexID),
    //    client.Vertex.UpdateProperties.WithAction(model.ActionAppend),
    //)
    //if respUpdate.Data.ID != vertexID {
    //    t.Errorf("error")
    //}
    //fmt.Println(respUpdate.Data.Properties)
}

func createVertexLabel() {

}
