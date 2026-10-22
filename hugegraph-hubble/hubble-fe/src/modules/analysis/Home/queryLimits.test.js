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

test.each([
    ['g.V().limit(10000)', true],
    ['g.V().outE().inV().path().limit(12000)', true],
    ['g.E().limit(1001).toList()', true],
    ['g.V().limit(1000)', false],
    ['g.V().limit(10000).limit(10)', false],
    ['g.V().limit(1).out().limit(10000)', true],
    ['g.V().local(__.out().limit(10000))', false],
    ['g.V().has("name", ".limit(10000)")', false],
    ['g.V() // .limit(10000)', false],
    ['g.V().limit(10) /* .limit(10000) */', false],
    ['g.V().limit(10000).count()', false],
    ['g.V().limit(10000).property("x", 1)', false],
    ['g.V().limit(10000); g.addV()', false],
    ['g.V().has("unterminated .limit(10000)', false],
])('preflights common reads without interpreting strings, nested limits or mutations: %s', (code, expected) => {
    expect(hasLargeExplicitReadLimit(code)).toBe(expected);
});

test.each([
    ['MATCH (n) RETURN n LIMIT 10000', true],
    ['MATCH (n) RETURN n LIMIT 300', false],
    ['MATCH (n) RETURN count(n) LIMIT 10000', false],
    ['MATCH (n) SET n.x = 1 RETURN n LIMIT 10000', false],
    ['MATCH (n) RETURN "LIMIT 10000"', false],
])('preflights simple Cypher reads: %s', (code, expected) => {
    expect(hasLargeExplicitReadLimit(code, 'cypher')).toBe(expected);
});
