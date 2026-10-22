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

const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const {loadPlaywright} = require('./graph_acceptance_helpers');

const origin = process.env.HUBBLE_BROWSER_URL || 'http://127.0.0.1:3000';
const space = process.env.VERMEER_GRAPHSPACE || 'DEFAULT';
const graph = process.env.VERMEER_GRAPH || 'vermeer_demo';
const output = process.env.VERMEER_BROWSER_ARTIFACTS || '/tmp/vermeer-browser-evidence';
const api = `${origin}/api/v1.3/graphspaces/${space}/graphs/${graph}`;
const credentials = process.env.VERMEER_TEST_CREDENTIALS
    ? JSON.parse(fs.readFileSync(process.env.VERMEER_TEST_CREDENTIALS, 'utf8')) : undefined;

async function check(page, role) {
    const errors = [];
    page.on('pageerror', error => errors.push(error.message));
    if (credentials) {
        await page.goto(`${origin}/login`);
        await page.locator('#normal_login_user_name').fill(
            role === 'admin' ? credentials.username : credentials.observer
        );
        await page.locator('#normal_login_user_password').fill(
            role === 'admin' ? credentials.password : credentials.observer_password
        );
        await page.getByRole('button', {name: 'Sign In', exact: true}).click();
        await page.waitForURL(url => url.pathname !== '/login');
    }
    const capability = await (await page.request.get(`${origin}/api/v1.3/vermeer`)).json();
    assert.equal(capability.status, 200);
    assert.equal(capability.data.enable, role !== 'observer');
    await page.goto(`${origin}/algorithms/${space}/${graph}`);
    if (role !== 'observer') {
        if (process.env.VERMEER_WORKER_PID_FILE) {
            const pid = Number(fs.readFileSync(process.env.VERMEER_WORKER_PID_FILE, 'utf8'));
            // Opt-in fault injection only for the dedicated local development Worker.
            assert(fs.readFileSync(`/proc/${pid}/cmdline`, 'utf8').includes('vermeer-bin'));
            const reload = page.getByRole('button', {name: 'Reload to Vermeer', exact: true});
            await reload.waitFor();
            process.kill(pid, 'SIGSTOP');
            try {
                await reload.click();
                await page.getByText('Loading', {exact: true}).waitFor();
                await page.waitForTimeout(4500);
                assert(await page.getByText('Loading', {exact: true}).isVisible());
                await page.screenshot({path: path.join(output, `${role}-slow-load.png`), fullPage: true});
            }
            finally {
                process.kill(pid, 'SIGCONT');
            }
            await page.getByText('Loaded', {exact: true}).waitFor({timeout: 45000});
            assert(await reload.isEnabled());
        }
        await page.getByText('PageRank', {exact: true}).click();
        const panel = page.locator('.ant-collapse-item').filter({
            has: page.getByText('PageRank', {exact: true}),
        });
        const response = page.waitForResponse(result =>
            result.url().endsWith('/algorithms/vermeer') && result.request().method() === 'POST'
        );
        await panel.locator('button.ant-btn-primary').click();
        const submitted = await (await response).json();
        assert.equal(submitted.status, 200);
        assert(submitted.data.task_id < 0);
        let completed = false;
        for (let attempt = 0; attempt < 60; attempt++) {
            const task = await (await page.request.get(`${api}/async-tasks/${submitted.data.task_id}`)).json();
            assert.equal(task.status, 200);
            if (task.data.task_status === 'success') {
                assert(JSON.parse(task.data.task_result).vertices.length > 0);
                completed = true;
                break;
            }
            assert(!['failed', 'cancelled'].includes(task.data.task_status));
            await page.waitForTimeout(500);
        }
        assert(completed, 'PageRank must complete within 30 seconds');
        await page.goto(`${origin}/asyncTasks/${space}/${graph}`);
        await page.getByRole('button', {name: 'View Result', exact: true}).first().click();
        await page.getByText('demo1', {exact: false}).first().waitFor();
    }
    else {
        await page.goto(`${origin}/asyncTasks/${space}/${graph}`);
        const listing = await (await page.request.get(`${api}/async-tasks`)).json();
        assert.equal(listing.status, 200);
        assert(listing.data.records.every(task => task.id >= 0));
        assert.equal(await page.getByRole('button', {name: /Load to Vermeer|Reload to Vermeer/}).count(), 0);
        const denied = await page.request.post(`${api}/algorithms/vermeer`, {
            data: {params: {'compute.algorithm': 'pagerank'}},
        });
        assert.equal(denied.status(), 403);
    }
    await page.screenshot({path: path.join(output, `${role}-tasks.png`), fullPage: true});
    if (role !== 'admin') {
        assert.equal(await page.getByRole('link', {name: 'Users', exact: true}).count(), 0);
    }
    await page.goto(`${origin}/asyncTasks/${space}/hugegraph`);
    await page.getByText('hugegraph', {exact: true}).first().waitFor();
    assert.equal(await page.getByText('demo1', {exact: false}).count(), 0);
    assert.deepEqual(errors, []);
    return {role, capability: capability.data.enable, page_errors: errors};
}

(async () => {
    fs.mkdirSync(output, {recursive: true});
    const browser = await loadPlaywright().chromium.launch({headless: true,
        ...(process.env.BROWSER_EXECUTABLE_PATH ? {executablePath: process.env.BROWSER_EXECUTABLE_PATH} : {}),
        args: ['--no-sandbox'],
    });
    try {
        const report = [];
        for (const role of credentials ? ['admin', 'observer'] : ['anonymous']) {
            const context = await browser.newContext({viewport: {width: 1440, height: 1100}, locale: 'en-US'});
            try {
                report.push(await check(await context.newPage(), role));
            }
            finally {
                await context.close();
            }
        }
        fs.writeFileSync(path.join(output, 'report.json'), JSON.stringify(report, null, 2));
        console.log(JSON.stringify(report, null, 2));
    }
    finally {
        await browser.close();
    }
})().catch(error => {
    console.error(error);
    process.exitCode = 1;
});
