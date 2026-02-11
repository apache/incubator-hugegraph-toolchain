/*
 *
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

const hint = [
    // constant/type/class/function
    // Clauses
    {label: 'CALL', type: 'method'},
    {label: 'CREATE', type: 'method'},
    {label: 'DELETE', type: 'method'},
    {label: 'DETACH', type: 'method'},
    {label: 'FOREACH', type: 'method'},
    {label: 'LOAD', type: 'method'},
    {label: 'MATCH', type: 'method'},
    {label: 'MERGE', type: 'method'},
    {label: 'OPTIONAL MATCH', type: 'method'},
    {label: 'REMOVE', type: 'method'},
    {label: 'RETURN', type: 'method'},
    {label: 'SET', type: 'method'},
    {label: 'START', type: 'method'},
    {label: 'UNION', type: 'method'},
    {label: 'UNWIND', type: 'method'},
    {label: 'WITH', type: 'method'},

    // Subclauses
    {label: 'LIMIT', type: 'constant'},
    {label: 'ORDER BY', type: 'constant'},
    {label: 'SKIP', type: 'constant'},
    {label: 'WHERE', type: 'constant'},
    {label: 'YIELD', type: 'constant'},

    // Modifiers
    {label: 'ASC', type: 'keyword'},
    {label: 'ASCENDING', type: 'keyword'},
    {label: 'ASSERT', type: 'keyword'},
    {label: 'BY', type: 'keyword'},
    {label: 'CSV', type: 'keyword'},
    {label: 'DESC', type: 'keyword'},
    {label: 'DESCENDING', type: 'keyword'},
    {label: 'ON', type: 'keyword'},

    // Expressions
    {label: 'ALL', type: 'keyword'},
    {label: 'CASE', type: 'keyword'},
    {label: 'COUNT', type: 'keyword'},
    {label: 'ELSE', type: 'keyword'},
    {label: 'END', type: 'keyword'},
    {label: 'EXISTS', type: 'keyword'},
    {label: 'THEN', type: 'keyword'},
    {label: 'WHEN', type: 'keyword'},

    // Operators
    {label: 'AND', type: 'keyword'},
    {label: 'AS', type: 'keyword'},
    {label: 'CONTAINS', type: 'keyword'},
    {label: 'DISTINCT', type: 'keyword'},
    {label: 'ENDS', type: 'keyword'},
    {label: 'IN', type: 'keyword'},
    {label: 'IS', type: 'keyword'},
    {label: 'NOT', type: 'keyword'},
    {label: 'OR', type: 'keyword'},
    {label: 'STARTS', type: 'keyword'},
    {label: 'XOR', type: 'keyword'},

    // Schema
    {label: 'CONSTRAINT', type: 'keyword'},
    {label: 'CREATE', type: 'keyword'},
    {label: 'DROP', type: 'keyword'},
    {label: 'EXISTS', type: 'keyword'},
    {label: 'INDEX', type: 'keyword'},
    {label: 'NODE', type: 'keyword'},
    {label: 'KEY', type: 'keyword'},
    {label: 'UNIQUE', type: 'keyword'},

    // Hints
    {label: 'INDEX', type: 'keyword'},
    {label: 'JOIN', type: 'keyword'},
    {label: 'SCAN', type: 'keyword'},
    {label: 'USING', type: 'keyword'},

    // Literals
    {label: 'false', type: 'keyword'},
    {label: 'null', type: 'keyword'},
    {label: 'true', type: 'keyword'},

    // Reserved for future use
    {label: 'ADD', type: 'keyword'},
    {label: 'DO', type: 'keyword'},
    {label: 'FOR', type: 'keyword'},
    {label: 'MANDATORY', type: 'keyword'},
    {label: 'OF', type: 'keyword'},
    {label: 'REQUIRE', type: 'keyword'},
    {label: 'SCALAR', type: 'keyword'},
];

const highlight = [
    {tag: 'CALL', color: '#fc6'},
    {tag: 'CREATE', color: '#fc6'},
    {tag: 'DELETE', color: '#fc6'},
    {tag: 'DETACH', color: '#fc6'},
    {tag: 'FOREACH', color: '#fc6'},
    {tag: 'LOAD', color: '#fc6'},
    {tag: 'MATCH', color: '#fc6'},
    {tag: 'MERGE', color: '#fc6'},
    {tag: 'OPTIONAL MATCH', color: '#fc6'},
    {tag: 'REMOVE', color: '#fc6'},
    {tag: 'RETURN', color: '#fc6'},
    {tag: 'SET', color: '#fc6'},
    {tag: 'START', color: '#fc6'},
    {tag: 'UNION', color: '#fc6'},
    {tag: 'UNWIND', color: '#fc6'},
    {tag: 'WITH', color: '#fc6'},
    // {tag: '', color: '#fc6'},
    // {tag: tags.comment, color: "#f5d", fontStyle: "italic"}
];

export {highlight, hint};
