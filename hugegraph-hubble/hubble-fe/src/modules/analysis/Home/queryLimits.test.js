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
    ['g.V().limit(10000L)', true],
    ['g.V().limit(10000l)', true],
    ['g.V().limit(100_000)', true],
    ['g.V().limit(100__000)', true],
    ['g.V().limit(10_000I)', true],
    ['g.V().limit(10_000g)', true],
    ['g.V().limit(0x2710L)', true],
    ['g.V().limit(0b10_0111_0001_0000)', true],
    ['g.V().limit(023420)', true],
    ['g.V().limit(+ 10000)', true],
    ['g.V().limit(Scope.global, 10000L)', true],
    ['g.V().limit(global, 100_000)', true],
    ['g.V().limit(Scope.local, 10000L)', false],
    ['g.V().limit(0_10)', false],
    ['g.V().limit(08)', false],
    ['g.V().limit(1_L)', false],
    ['g.V().limit(2147483648I)', false],
    ['g.V().limit(9223372036854775808L)', false],
    ['g.V().limit(1e4)', false],
    ['g.V().limit(1.0G)', false],
    ['g.V().limit(10000D)', false],
    ['g.V().limit(-1)', false],
    ['g.V().outE().inV().path().limit(12000)', true],
    ['g.E().limit(1001).toList()', true],
    ['g.V().limit(1000)', false],
    ['g.V().limit(10000).limit(10)', false],
    ['g.V().limit(10000).range(0, 10)', false],
    ['g.V().limit(10000).range(0, 10).toList()', false],
    ['g.V().limit(10000).range(Scope.global, 10, 20)', false],
    ['g.V().limit(10000).range(0, 10).out()', true],
    ['g.V().limit(10000).range(Scope.local, 0, 10)', true],
    ['g.V().limit(1).out().limit(10000)', true],
    ['g.V().local(__.out().limit(10000))', false],
    ['g.V().has("name", ".limit(10000)")', false],
    ['g.V() // .limit(10000)', false],
    ['g.V().limit(10) /* .limit(10000) */', false],
    ['g.V().limit(10000).count()', false],
    ['g.V().limit(10000).count(Scope.global)', false],
    ['g.V().limit(10000).hasNext()', false],
    ['g.V().limit(10000).next()', false],
    ['g.V().limit(10000).toList()', true],
    ['g.V().limit(10000).valueMap().count(local)', true],
    ["g.V().aggregate('seen').limit(10000)", true],
    ['g.V().sideEffect(__.out()).limit(10000)', true],
    ['g.V().limit(10000).property("x", 1)', false],
    ['g.V().limit(10000); g.addV()', false],
    ['g.V().has("unterminated .limit(10000)', false],
])('preflights common reads without interpreting strings, nested limits or mutations: %s', (code, expected) => {
    expect(hasLargeExplicitReadLimit(code)).toBe(expected);
});

test.each([
    ['MATCH (n) RETURN n LIMIT 10000', true],
    ['UNWIND range(1, 1000000) AS n RETURN n LIMIT 1000000', true],
    ['WITH range(1, 1000000) AS ns UNWIND ns AS n RETURN n LIMIT 1000000', true],
    ['MATCH (n) RETURN n LIMIT 300', false],
    ['UNWIND range(1, 1000000) AS n RETURN n LIMIT 500000 + 500000', true],
    ['MATCH (n) RETURN n LIMIT $rows', true],
    ['MATCH (n) WITH n LIMIT 10000 RETURN n LIMIT 10', false],
    ['MATCH (n) RETURN count(n) LIMIT 10000', true],
    ['MATCH (n) RETURN n.city, count(*) LIMIT 10000', true],
    ['MATCH (n) RETURN n.city, sum(n.amount) LIMIT 10000', true],
    ['MATCH (n) RETURN count(n)', false],
    ['MATCH (n) RETURN n.city, count(*) LIMIT 1000', false],
    ['MATCH (n) SET n.x = 1 RETURN n LIMIT 10000', false],
    ['MATCH (n) RETURN "LIMIT 10000"', false],
])('preflights simple Cypher reads: %s', (code, expected) => {
    expect(hasLargeExplicitReadLimit(code, 'cypher')).toBe(expected);
});
