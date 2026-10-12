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
	"io"
	"net"
	"net/http"
	"net/http/httptest"
	"strconv"
	"strings"
	"testing"

	hugegraph "github.com/apache/hugegraph-toolchain/hugegraph-client-go"
	"github.com/apache/hugegraph-toolchain/hugegraph-client-go/api/v1/edgelabel"
	"github.com/apache/hugegraph-toolchain/hugegraph-client-go/api/v1/propertykey"
	"github.com/apache/hugegraph-toolchain/hugegraph-client-go/api/v1/vertexlabel"
	"github.com/apache/hugegraph-toolchain/hugegraph-client-go/internal/model"
)

func TestSchemaGraphSpaceRoutes(t *testing.T) {
	operations := []struct {
		name, method, suffix, query string
		call                        func(*hugegraph.CommonClient, string) error
	}{
		{"schema", "GET", "", "", func(c *hugegraph.CommonClient, name string) error {
			response, err := c.Schema()
			if response != nil {
				defer response.Body.Close()
			}
			return err
		}},
		{"propertykey_create", "POST", "/propertykeys", "", func(c *hugegraph.CommonClient, name string) error {
			response, err := c.Propertykey.Create(c.Propertykey.Create.WithReqData(propertykey.CreateRequestData{Name: name, DataType: model.PropertyDataTypeInt, Cardinality: model.PropertyCardinalitySingle}))
			if response != nil {
				defer response.Body.Close()
			}
			return err
		}},
		{"propertykey_delete", "DELETE", "/propertykeys/{name}", "", func(c *hugegraph.CommonClient, name string) error {
			response, err := c.Propertykey.DeleteByName(c.Propertykey.DeleteByName.WithName(name))
			if response != nil {
				defer response.Body.Close()
			}
			return err
		}},
		{"propertykey_list", "GET", "/propertykeys", "", func(c *hugegraph.CommonClient, name string) error {
			response, err := c.Propertykey.GetAll()
			if response != nil {
				defer response.Body.Close()
			}
			return err
		}},
		{"propertykey_get", "GET", "/propertykeys/{name}", "", func(c *hugegraph.CommonClient, name string) error {
			response, err := c.Propertykey.GetByName(c.Propertykey.GetByName.WithName(name))
			if response != nil {
				defer response.Body.Close()
			}
			return err
		}},
		{"propertykey_update", "PUT", "/propertykeys/{name}", "action=append", func(c *hugegraph.CommonClient, name string) error {
			response, err := c.Propertykey.UpdateUserdata(c.Propertykey.UpdateUserdata.WithReqData(propertykey.UpdateUserdataRequestData{Name: name, Action: model.ActionAppend}))
			if response != nil {
				defer response.Body.Close()
			}
			return err
		}},
		{"vertexlabel_create", "POST", "/vertexlabels", "", func(c *hugegraph.CommonClient, name string) error {
			response, err := c.VertexLabel.Create(c.VertexLabel.Create.WithReqData(vertexlabel.CreateRequestData{Name: name, IDStrategy: model.IDStrategyDefault, Properties: []string{"age"}}))
			if response != nil {
				defer response.Body.Close()
			}
			return err
		}},
		{"vertexlabel_delete", "DELETE", "/vertexlabels/{name}", "", func(c *hugegraph.CommonClient, name string) error {
			response, err := c.VertexLabel.DeleteByName(c.VertexLabel.DeleteByName.WithName(name))
			if response != nil {
				defer response.Body.Close()
			}
			return err
		}},
		{"vertexlabel_list", "GET", "/vertexlabels", "", func(c *hugegraph.CommonClient, name string) error {
			response, err := c.VertexLabel.GetAll()
			if response != nil {
				defer response.Body.Close()
			}
			return err
		}},
		{"vertexlabel_get", "GET", "/vertexlabels/{name}", "", func(c *hugegraph.CommonClient, name string) error {
			response, err := c.VertexLabel.GetByName(c.VertexLabel.GetByName.WithName(name))
			if response != nil {
				defer response.Body.Close()
			}
			return err
		}},
		{"vertexlabel_update", "PUT", "/vertexlabels/{name}", "action=append", func(c *hugegraph.CommonClient, name string) error {
			response, err := c.VertexLabel.UpdateUserdata(c.VertexLabel.UpdateUserdata.WithReqData(vertexlabel.UpdateUserdataRequestData{Name: name, Action: model.ActionAppend}))
			if response != nil {
				defer response.Body.Close()
			}
			return err
		}},
		{"edgelabel_create", "POST", "/edgelabels", "", func(c *hugegraph.CommonClient, name string) error {
			response, err := c.EdgeLabel.Create(c.EdgeLabel.Create.WithReqData(edgelabel.CreateRequestData{Name: name, SourceLabel: "person", TargetLabel: "person", Properties: []string{"age"}}))
			if response != nil {
				defer response.Body.Close()
			}
			return err
		}},
		{"edgelabel_delete", "DELETE", "/edgelabels/{name}", "", func(c *hugegraph.CommonClient, name string) error {
			response, err := c.EdgeLabel.DeleteByName(c.EdgeLabel.DeleteByName.WithName(name))
			if response != nil {
				defer response.Body.Close()
			}
			return err
		}},
		{"edgelabel_list", "GET", "/edgelabels", "", func(c *hugegraph.CommonClient, name string) error {
			response, err := c.EdgeLabel.GetAll()
			if response != nil {
				defer response.Body.Close()
			}
			return err
		}},
	}
	configurations := []struct {
		name, space, graph, element, prefix, escapedElement string
	}{
		{"legacy", "", "hugegraph", "person", "/graphs/hugegraph/schema", "person"},
		{"default", "DEFAULT", "hugegraph", "person", "/graphspaces/DEFAULT/graphs/hugegraph/schema", "person"},
		{"custom", "analytics", "hugegraph", "person", "/graphspaces/analytics/graphs/hugegraph/schema", "person"},
		{"escaped", "analytics space", "graph name", "person name",
			"/graphspaces/analytics%20space/graphs/graph%20name/schema", "person%20name"},
	}
	for _, configuration := range configurations {
		t.Run(configuration.name, func(t *testing.T) {
			for _, operation := range operations {
				t.Run(operation.name, func(t *testing.T) {
					requests := make(chan struct{ method, uri, body string }, 1)
					server := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
						body, err := io.ReadAll(r.Body)
						if err != nil {
							t.Errorf("read request body: %v", err)
						}
						requests <- struct{ method, uri, body string }{r.Method, r.RequestURI, string(body)}
						w.Header().Set("Content-Type", "application/json")
						io.WriteString(w, "{}")
					}))
					defer server.Close()
					host, portString, err := net.SplitHostPort(server.Listener.Addr().String())
					if err != nil {
						t.Fatal(err)
					}
					port, err := strconv.Atoi(portString)
					if err != nil {
						t.Fatal(err)
					}
					client, err := hugegraph.NewCommonClient(hugegraph.Config{
						Host: host, Port: port, GraphSpace: configuration.space, Graph: configuration.graph,
					})
					if err != nil {
						t.Fatal(err)
					}
					if err := operation.call(client, configuration.element); err != nil {
						t.Fatalf("schema request: %v", err)
					}
					var request struct{ method, uri, body string }
					select {
					case request = <-requests:
					default:
						t.Fatal("schema operation did not send an HTTP request")
					}
					expectedURI := configuration.prefix + strings.ReplaceAll(operation.suffix, "{name}", configuration.escapedElement)
					if operation.query != "" {
						expectedURI += "?" + operation.query
					}
					if request.method != operation.method || request.uri != expectedURI {
						t.Fatalf("received %s %s; want %s %s", request.method, request.uri, operation.method, expectedURI)
					}
					if operation.method == "POST" || operation.method == "PUT" {
						if !strings.Contains(request.body, `"name":"`+configuration.element+`"`) {
							t.Fatalf("request body lost schema name: %s", request.body)
						}
					}
					t.Logf("HTTP %s %s", request.method, request.uri)
				})
			}
		})
	}
}
