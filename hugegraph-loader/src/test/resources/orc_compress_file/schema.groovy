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
// Define schema
schema.propertyKey("name").asText().ifNotExist().create();
schema.propertyKey("p_boolean").asBoolean().ifNotExist().create();
schema.propertyKey("p_byte").asByte().ifNotExist().create();
schema.propertyKey("p_int").asInt().ifNotExist().create();
schema.propertyKey("p_long").asLong().ifNotExist().create();
schema.propertyKey("p_float").asFloat().ifNotExist().create();
schema.propertyKey("p_double").asDouble().ifNotExist().create();
schema.propertyKey("p_string").asText().ifNotExist().create();
schema.propertyKey("p_date").asDate().ifNotExist().create();

schema.vertexLabel("person")
      .properties("name", "p_boolean", "p_byte", "p_int", "p_long", "p_float", "p_double", "p_string", "p_date")
      .primaryKeys("name")
      .ifNotExist().create();
