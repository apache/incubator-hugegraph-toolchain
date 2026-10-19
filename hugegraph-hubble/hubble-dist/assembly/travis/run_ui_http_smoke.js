#!/usr/bin/env node
/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

'use strict';

const fs = require('fs');
const path = require('path');
const {chromiumExecutablePath, loadPlaywright} = require('./run_ui_browser_smoke');

function argValue(name, fallback) {
  const index = process.argv.indexOf(name);
  return index >= 0 && index + 1 < process.argv.length
    ? process.argv[index + 1] : fallback;
}

function insecureHttpTarget(hubbleUrl) {
  const target = new URL(hubbleUrl);
  if (target.protocol === 'https:') {
    return {url: target.href, args: [], skipReason:
      'HTTPS cannot exercise insecure HTTP; use an HTTP Hubble URL for this gate.'};
  }
  if (target.protocol !== 'http:') {
    throw new Error('HTTP browser smoke requires an HTTP or HTTPS Hubble URL');
  }
  const hostname = target.hostname.toLowerCase();
  const loopback = hostname === 'localhost' || hostname.endsWith('.localhost') ||
    /^127(?:\.\d{1,3}){3}$/.test(hostname) || hostname === '[::1]';
  if (!loopback) {
    // Keep virtual hosting, proxies and remote target routing unchanged.
    return {url: target.href, args: []};
  }
  // Chromium trusts loopback origins even over HTTP. Resolve a non-localhost
  // name to the same service so the browser exercises a real insecure context.
  target.hostname = 'hubble-http.test';
  return {url: target.href, args: [
    `--host-resolver-rules=MAP hubble-http.test ${hostname}`,
    '--proxy-bypass-list=hubble-http.test'
  ]};
}

async function runHttpSmoke(chromium, hubbleUrl, outputDir, executablePath) {
  const target = insecureHttpTarget(hubbleUrl);
  const report = {
    hubbleUrl,
    url: target.url,
    status: 'failed',
    isSecureContext: null,
    rootMounted: false,
    pageErrors: [],
    error: null,
    screenshot: path.join(outputDir, 'insecure-http.png')
  };
  if (target.skipReason) {
    return {...report, status: 'skipped', reason: target.skipReason, screenshot: null};
  }
  fs.mkdirSync(outputDir, {recursive: true});
  const browser = await chromium.launch({headless: true, executablePath, args: target.args});
  try {
    const page = await browser.newPage({viewport: {width: 1440, height: 900}});
    page.on('pageerror', error => report.pageErrors.push(error.message));
    try {
      const response = await page.goto(target.url, {
        waitUntil: 'load', timeout: 30000
      });
      report.url = page.url();
      report.isSecureContext = await page.evaluate(() => window.isSecureContext);
      if (!response || !response.ok()) {
        throw new Error(`Hubble page returned HTTP ${response ? response.status() : 'no response'}`);
      }
      await page.waitForFunction(() => {
        const root = document.getElementById('root');
        return root && root.childElementCount > 0 && root.innerText.trim().length > 0;
      }, null, {timeout: 10000});
      report.rootMounted = await page.locator('#root').isVisible();
      // Catch errors from startup tasks immediately following the first render.
      await page.waitForTimeout(500);
    } catch (error) {
      report.error = error.message;
    }
    await page.screenshot({path: report.screenshot, fullPage: true});
    report.status = report.isSecureContext === false && report.rootMounted &&
      report.pageErrors.length === 0 && !report.error ? 'passed' : 'failed';
  } finally {
    await browser.close();
  }
  return report;
}

async function main() {
  const hubbleUrl = argValue('--hubble-url', process.env.HUBBLE_URL || 'http://127.0.0.1:8088');
  const outputDir = path.resolve(argValue('--output-dir',
    '.workflow/hubble-v2-issue-694/evidence/ui'));
  const jsonOutput = path.resolve(argValue('--json-output', path.join(outputDir, 'ui-http-smoke.json')));
  const target = insecureHttpTarget(hubbleUrl);
  const {chromium} = target.skipReason ? {} : await loadPlaywright();
  const report = await runHttpSmoke(chromium, hubbleUrl, outputDir, chromiumExecutablePath());
  fs.mkdirSync(path.dirname(jsonOutput), {recursive: true});
  fs.writeFileSync(jsonOutput, JSON.stringify(report, null, 2) + '\n');
  console.log(JSON.stringify(report, null, 2));
  if (report.status === 'failed') {
    process.exitCode = 1;
  }
}

if (require.main === module) {
  main().catch(error => {
    console.error(error.message);
    process.exitCode = 1;
  });
}

module.exports = {insecureHttpTarget, runHttpSmoke};
