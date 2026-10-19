/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
'use strict';

const test = require('node:test');
const assert = require('node:assert/strict');
const {NeedsInput, redact, publicHttpUrl, typedId, assertExportMatches, assertFixtureGraph,
  fixtureFromPrefix, pngDimensions, scopePath} = require('./graph_acceptance_helpers');

const raw = {
  vertices: [{id: 7, label: 'person', properties: {name: '甲', score: 0}, statistics: {degree: 2}},
    {id: '7', label: 'person', properties: {name: '乙'}}],
  edges: [{id: 'edge', source: 7, target: '7', label: 'knows', properties: {weight: .25}}],
};
test('raw export comparison accepts record order but preserves complete typed records', () => {
  assertExportMatches({...raw, vertices: [...raw.vertices].reverse()}, raw);
  const changedId = structuredClone(raw);
  changedId.vertices[0].id = '7';
  assert.throws(() => assertExportMatches(changedId, raw));
  const changedEndpoint = structuredClone(raw);
  changedEndpoint.edges[0].source = '7';
  assert.throws(() => assertExportMatches(changedEndpoint, raw));
  const omittedStatistic = structuredClone(raw);
  delete omittedStatistic.vertices[0].statistics;
  assert.throws(() => assertExportMatches(omittedStatistic, raw));
});
test('fixture gate rejects empty, unrelated, and imprecise numeric ID evidence', () => {
  assert.throws(() => assertFixtureGraph(raw, {}), NeedsInput);
  assertFixtureGraph(raw, {expected: {vertexIds: [7, '7'], edgeCount: 1}});
  assert.throws(() => assertFixtureGraph(raw, {expected: {vertexIds: ['wrong'], edgeCount: 1}}));
  assert.throws(() => typedId(Number.MAX_SAFE_INTEGER + 1), /cannot be verified exactly/);
});
test('existing public Loader fixture prefix creates a read-only query, never a new fixture', () => {
  const fixture = fixtureFromPrefix('existing_123', 'DEFAULT', 'g');
  assert.equal(fixture.query, "g.V().hasLabel('existing_123_person').bothE().dedup()");
  assert.equal(fixture.expected.edgeCount, 2);
  assert.deepEqual(fixture.expected.vertexIds, ['existing_123_alice', 'existing_123_bob', 'existing_123_carol']);
  assert.throws(() => fixtureFromPrefix("bad');drop()", 'DEFAULT', 'g'));
  assert.equal(scopePath({graphspace: 'A B', graph: 'g/x'}), '/api/v1.3/graphspaces/A%20B/graphs/g%2Fx');
});
test('redaction removes configured credentials and URL userinfo', () => {
  const output = redact('failed secret-one https://u:secret-two@example.invalid/api', ['secret-one']);
  assert.equal(output, 'failed [REDACTED] https://[REDACTED]@example.invalid/api');
  assert.ok(!output.includes('secret-one') && !output.includes('secret-two'));
});
test('PNG validation rejects arbitrary download bytes and invalid dimensions', () => {
  assert.throws(() => pngDimensions(Buffer.from('not an image')));
  const header = Buffer.alloc(24);
  Buffer.from([137, 80, 78, 71, 13, 10, 26, 10]).copy(header);
  header.write('IHDR', 12, 'ascii');
  header.writeUInt32BE(800, 16);
  header.writeUInt32BE(600, 20);
  assert.deepEqual(pngDimensions(header), {width: 800, height: 600});
  header.writeUInt32BE(0, 16);
  assert.throws(() => pngDimensions(header));
});

test('public acceptance URLs preserve HTTP(S) origins and deployment paths', () => {
  for (const value of ['http://127.0.0.1:8088', 'https://example.invalid/deployment/hubble/',
    'https://example.invalid/deployment/visualization.html', 'http://[::1]:8088/prefix%20name']) {
    assert.equal(publicHttpUrl(value, 'hubbleUrl'), value);
  }
});
test('URL validation rejects credential/query/fragment inputs without reflecting secrets', () => {
  const sentinel = 'URL_INPUT_SECRET_7aQ';
  const encoded = [...sentinel].map(value => `%${value.charCodeAt(0).toString(16)}`).join('');
  for (const value of [
    `https://user:${sentinel}@example.invalid/`, `https://${encoded}@example.invalid/`,
    `https://user:${encoded}@example.invalid/`, `https://example.invalid/?token=${encoded}`,
    `https://example.invalid/#${encoded}`, 'https://@example.invalid/',
    'https://example.invalid/?', 'https://example.invalid/#', 'file:///tmp/example',
    'https:example.invalid', `https://example.invalid/\n${sentinel}`, `not-a-url-${sentinel}`
  ]) {
    assert.throws(() => publicHttpUrl(value, 'exampleUrl'), error => {
      assert.equal(error.message, 'Invalid exampleUrl');
      assert.ok(!String(error).includes(sentinel) && !String(error).includes(encoded));
      return true;
    });
  }
});
