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

package model

const (
	PropertyDataTypeInt PropertyDataType = "INT" // data_type int

	PropertyCardinalitySingle PropertyCardinality = "SINGLE" // cardinality single

	ActionAppend    Action = "append"    // append action
	ActionEliminate Action = "eliminate" // eliminate(remove) action

	IDStrategyDefault IDStrategy = "DEFAULT" // default id_strategy,The default strategy is primary key ID.

	FrequencySingle Frequency = "SINGLE" // single frequency
)

type PropertyDataType string
type PropertyCardinality string
type Action string
type IDStrategy string
type Frequency string

// Vertex models that support generic types
type Vertex[T any] struct {
	ID         string `json:"id,omitempty"`
	Label      string `json:"label"`
	Typ        string `json:"type,omitempty"`
	Properties T      `json:"properties"`
}

// Edge models that support generic types
type Edge[T any] struct {
	ID         string `json:"id,omitempty"`
	Label      string `json:"label"`
	Typ        string `json:"type,omitempty"`
	OutV       string `json:"outV"`
	OutVLabel  string `json:"outVLabel"`
	InV        string `json:"inV"`
	InVLabel   string `json:"inVLabel"`
	Properties T      `json:"properties"`
}
