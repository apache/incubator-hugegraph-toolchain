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
schema.propertyKey("age").asInt().ifNotExist().create();
schema.propertyKey("city").asText().ifNotExist().create();
schema.propertyKey("weight").asDouble().ifNotExist().create();
schema.propertyKey("lang").asText().ifNotExist().create();
schema.propertyKey("date").asText().ifNotExist().create();
schema.propertyKey("price").asDouble().ifNotExist().create();

schema.vertexLabel("person")
      .properties("name", "age", "city")
      .primaryKeys("name")
      .nullableKeys("age", "city")
      .ifNotExist()
      .create();
schema.vertexLabel("software")
      .useCustomizeNumberId()
      .properties("name", "lang", "price")
      .ifNotExist()
      .create();

schema.indexLabel("personByAge")
      .onV("person")
      .by("age")
      .range()
      .ifNotExist()
      .create();
schema.indexLabel("personByCity")
      .onV("person")
      .by("city")
      .secondary()
      .ifNotExist()
      .create();
schema.indexLabel("personByAgeAndCity")
      .onV("person")
      .by("age", "city")
      .secondary()
      .ifNotExist()
      .create();
schema.indexLabel("softwareByPrice")
      .onV("software")
      .by("price")
      .range()
      .ifNotExist()
      .create();

schema.edgeLabel("knows")
      .sourceLabel("person")
      .targetLabel("person")
      .properties("date", "weight")
      .ifNotExist()
      .create();
schema.edgeLabel("created")
      .sourceLabel("person")
      .targetLabel("software")
      .properties("date", "weight")
      .ifNotExist()
      .create();

schema.indexLabel("createdByDate")
      .onE("created")
      .by("date")
      .secondary()
      .ifNotExist()
      .create();
schema.indexLabel("createdByWeight")
      .onE("created")
      .by("weight")
      .range()
      .ifNotExist()
      .create();
schema.indexLabel("knowsByWeight")
      .onE("knows")
      .by("weight")
      .range()
      .ifNotExist()
      .create();
