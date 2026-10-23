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

import {hasLargeExplicitReadLimit} from './queryLimits';
import {GRAPH_NODE_LIMIT} from '../QueryResult/Home/utils';

test.each([
    ['g.V().limit(10000)', true],
    ['g.E().limit(1001);', true],
    ['g.V().outE().inV().path().limit(12000)', true],
    ['g.V().limit(1000)', false],
    ['g.V().limit(10000).limit(10)', false],
    ['g.V().local(__.out().limit(10000))', false],
    ['g.V().has("name", ".limit(10000)")', false],
    ['g.V().property("limit", 10000)', false],
    ['g.V() // .limit(10000)', false],
    ['g.V().limit(10) /* .limit(10000) */', false],
    ['g.V().limit(10000); g.V()', false],
    ['g.V().has("unterminated .limit(10000)', false],
    ['g.V().limit(rows)', false],
    ['g.V().limit(5000 + 5000)', false],
    ['g.V().limit(10000L)', false],
    ['g.V().limit(100_000)', false],
    ['g.V().limit(0x2710)', false],
    ['g.V().limit(01001)', false],
    ['g.V().limit(10000).count()', false],
    ['g.V().limit(10000).toList()', false],
])('checks only the common terminal decimal Gremlin spelling: %s', (code, expected) => {
    expect(hasLargeExplicitReadLimit(code)).toBe(expected);
});

test.each([
    ['MATCH (n) RETURN n LIMIT 10000', true],
    ['WITH 1 AS n RETURN n LIMIT 1001;', true],
    ['MATCH (n) RETURN n LIMIT 1000', false],
    ['MATCH (n) RETURN n.limit + 1', false],
    ['MATCH (n) RETURN n . LIMIT + 1', false],
    ['MATCH (n) RETURN n./* property */limit + 1', false],
    ['MATCH (n) RETURN n.limit LIMIT 10000', true],
    ['MATCH (n) WITH n LIMIT 10000 RETURN n LIMIT 10', false],
    ['MATCH (n) WITH n LIMIT 10000 RETURN n', false],
    ['MATCH (n) WITH n ORDER BY n.age LIMIT 5 RETURN n.name', false],
    ['MATCH (n) WITH n LIMIT 10 RETURN n LIMIT 10000', true],
    ['MATCH (n) WITH n.return LIMIT 10000 RETURN n', false],
    ['MATCH (n) RETURN n LIMIT $rows', false],
    ['MATCH (n) RETURN n LIMIT 5000 + 5000', false],
    ['MATCH (n) RETURN "LIMIT 10000"', false],
    ['MATCH (n) RETURN exists { MATCH (m) RETURN m LIMIT 10000 }', false],
])('checks a final decimal Cypher limit without interpreting WITH inputs or properties: %s', (code, expected) => {
    expect(hasLargeExplicitReadLimit(code, 'cypher')).toBe(expected);
});

test('shares the existing canvas threshold', () => {
    const query = `g.V().limit(${GRAPH_NODE_LIMIT + 1})`;
    expect(hasLargeExplicitReadLimit(query)).toBe(true);
    expect(hasLargeExplicitReadLimit(query, 'gremlin', GRAPH_NODE_LIMIT + 1)).toBe(false);
});
