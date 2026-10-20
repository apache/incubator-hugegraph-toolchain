/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

'use strict';

async function payload(response, name) {
  let body;
  try {
    body = await response.json();
  } catch (error) {
    throw new Error(`${name} returned unreadable JSON: ${error.message}`);
  }
  if (!response.ok()) {
    throw new Error(`${name} failed: HTTP ${response.status()}`);
  }
  if (!body || body.status !== 200) {
    const detail = body && typeof body.message === 'string' && body.message.trim();
    throw new Error(`${name} failed: ${detail || `business status ${body?.status}`}`);
  }
  return body.data;
}

async function authenticateUi(context, page, hubbleUrl, username, password, assertRunning = () => {}) {
  const baseUrl = hubbleUrl.replace(/\/$/, '');
  assertRunning();
  const user = await payload(await context.request.post(
    `${baseUrl}/api/v1.3/auth/login`,
    {data: {user_name: username, user_password: password}, timeout: 5000}
  ), 'login');
  assertRunning();
  const status = await payload(await context.request.get(
    `${baseUrl}/api/v1.3/auth/status`, {timeout: 5000}
  ), 'auth status');
  assertRunning();
  if (!status || !status.level) {
    throw new Error('auth status response did not include level');
  }
  if (!user || user.user_name !== username) {
    throw new Error('login response did not match requested username');
  }
  const authContext = await payload(await context.request.get(
    `${baseUrl}/api/v1.3/auth/context`, {timeout: 5000}
  ), 'auth context');
  assertRunning();
  if (!authContext || authContext.username !== username) {
    throw new Error('auth context did not match requested username');
  }
  if (!['SUPERADMIN', 'SPACEADMIN', 'USER'].includes(authContext.role)) {
    throw new Error('auth context did not include a supported authenticated role');
  }
  const config = await payload(await context.request.get(
    `${baseUrl}/api/v1.3/config`, {timeout: 5000}
  ), 'config');
  assertRunning();
  if (!config || typeof config.pd_enabled !== 'boolean') {
    throw new Error('config response did not include pd_enabled');
  }
  await page.addInitScript(session => {
    window.sessionStorage.setItem('user_', JSON.stringify(session.user));
    window.sessionStorage.setItem('hubble_config_', JSON.stringify(session.config));
  }, {user, config});
  assertRunning();
  return {user, level: status.level, pdEnabled: config.pd_enabled,
    username: authContext.username, role: authContext.role};
}

// Keep the browser cookie jar intact: clearing local cookies first would hide a
// server session that logout failed to invalidate.
async function logoutUi(context, hubbleUrl) {
  const baseUrl = hubbleUrl.replace(/\/$/, '');
  await payload(await context.request.get(`${baseUrl}/api/v1.3/auth/logout`, {timeout: 5000}), 'logout');
  const response = await context.request.get(`${baseUrl}/api/v1.3/auth/status`, {timeout: 5000});
  if (response.status() === 401) return;
  if (response.status() === 200) {
    let body;
    try {body = await response.json();}
    catch {throw new Error('Logout verification returned unreadable JSON');}
    if (body?.status === 401) return;
  }
  throw new Error('Logout did not invalidate the actual browser session');
}

module.exports = {authenticateUi, logoutUi};
