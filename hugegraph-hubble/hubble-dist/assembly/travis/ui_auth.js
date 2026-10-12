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

async function authenticateUi(context, page, hubbleUrl, username, password) {
  const baseUrl = hubbleUrl.replace(/\/$/, '');
  const user = await payload(await context.request.post(
    `${baseUrl}/api/v1.3/auth/login`,
    {data: {user_name: username, user_password: password}}
  ), 'login');
  const status = await payload(await context.request.get(
    `${baseUrl}/api/v1.3/auth/status`
  ), 'auth status');
  if (!status || !status.level) {
    throw new Error('auth status response did not include level');
  }
  if (!user || !user.user_name) {
    throw new Error('login response did not include user_name');
  }
  const config = await payload(await context.request.get(
    `${baseUrl}/api/v1.3/config`
  ), 'config');
  if (!config || typeof config.pd_enabled !== 'boolean') {
    throw new Error('config response did not include pd_enabled');
  }
  await page.addInitScript(session => {
    window.sessionStorage.setItem('user_', JSON.stringify(session.user));
    window.sessionStorage.setItem('hubble_config_', JSON.stringify(session.config));
  }, {user, config});
  return {user, level: status.level, pdEnabled: config.pd_enabled};
}

module.exports = {authenticateUi};
