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

export const keywords =
  'g|V|E|has|open|close|inV|inE|out|outV|outE|label|store|next|addVertex|clazz|' +
  'limit|traversal|withBulk|values|schema|except|ifNotExist|addEdge|addVertex|property|io|' +
  'filter|loops|readGraph|tree|properties|graph|value|bothE|addV|where|hidden|bothV|without' +
  'both|is|path|it|get|from|to|select|otherV|within|inside|outside|withSack';

export const buildinFunctions =
  'targetLabel|sourceLabel|indexLabel|indexLabels|edgeLabel|vertexLabel|propertyKey|getPropertyKey|' +
  'getVertexLabel|getEdgeLabel|getIndexLabel|getPropertyKeys|getVertexLabels|getEdgeLabels|getIndexLabels|' +
  'coin|count|coalesce|createIndex|hasLabel|getLabelId|create|build|append|eliminate|remove|rebuildIndex|' +
  'constant|isDirected|desc|inject|profile|simplePath|eq|neq|gt|gte|lt|lte|queryType|indexFields|frequency|' +
  'links|type|in|on|by|checkDataType|checkValue|validValue|secondary|drop|search|makeEdgeLabel|cyclicPath|' +
  'hasKey|match|sack|aggregate|between|baseType|baseValue|indexType|rebuild|choose|aggregate|iterate|lte|dedup|' +
  'identity|groupCount|until|barrier|fold|unfold|schemaId|checkName|makeIndexLabel|makeVertexLabel|makePropertyKey|' +
  'sideEffect|hasNext|toList|toSet|cap|option|branch|choose|repeat|emit|order|mean|withComputer|subgraph|' +
  'getObjectsAtDepth|hasValue|hasNot|hasId|nullableKey|nullableKeys|sortKeys|link|singleTime|multiTimes|' +
  'enableLabelIndex|userdata|checkExist|linkWithLabel|directed|idStrategy|primaryKeys|primaryKey';

export const dataTypes =
  'int|numeric|decimal|date|varchar|char|bigint|float|double|bit|binary|text|set|timestamp|toString|primitive|' +
  'money|real|number|integer|asInt|asText|dataType|cardinality|asText|asInt|asTimestamp|flatMap|valueMap|' +
  'asByte|asBlob|asDouble|asDate|asFloat|asLong|valueSingle|asBoolean|valueList|valueSet|asUuid|null|Infinity|NaN|undefined';

export const baseData = [
  ...keywords.split('|'),
  ...buildinFunctions.split('|'),
  ...dataTypes.split('|')
];
