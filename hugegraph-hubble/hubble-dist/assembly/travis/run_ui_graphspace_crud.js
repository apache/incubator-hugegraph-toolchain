#!/usr/bin/env node
/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
'use strict';

// Real Chromium and frontend; all API data and failure injection are local fixtures.
const fs = require('node:fs');
const path = require('node:path');
const os = require('node:os');
const assert = require('node:assert/strict');
const hubbleRoot = path.resolve(__dirname, '../../..');
const {chromium} = require(path.join(hubbleRoot, 'hubble-fe/node_modules/playwright'));
const base = process.env.HUBBLE_URL || 'http://127.0.0.1:3000';
const output = process.env.HUBBLE_UI_EVIDENCE_DIR
    || path.join(os.tmpdir(), 'hubble-graphspace-crud');
fs.mkdirSync(output, {recursive: true});
const checks = [];
const errors = [];
const unexpected = new Set();
const state = {spaces: [space('DEFAULT', '默认图空间'), space('soc_graph', '记忆库')],
    fail: null, writes: [], requests: [], delayedSave: null};
function space(name, nickname) {
    return {name, nickname, create_time: '2026-10-23', auth: true,
        max_graph_number: 100, max_role_number: 100, cpu_limit: 64, memory_limit: 128,
        compute_cpu_limit: 0, compute_memory_limit: 0, storage_limit: 100,
        storage_used_kb: 1024, description: '', graphspace_admin: []};
}
function paged(records, url) {
    const current = Number(url.searchParams.get('page_no') || 1);
    const size = Number(url.searchParams.get('page_size') || 100);
    return {records: records.slice((current - 1) * size, current * size), total: records.length,
        current, size, pages: Math.ceil(records.length / size), selected: null};
}
async function fixture(route) {
    const request = route.request();
    const url = new URL(request.url());
    const name = url.pathname.replace(/^\/api\/v[\d.]+/, '');
    const method = request.method();
    state.requests.push({method, name, params: Object.fromEntries(url.searchParams)});
    let data;
    const failure = state.fail === `${method} ${name}`;
    if (failure) {
        state.fail = null;
        return route.fulfill({status: 503, contentType: 'application/json',
            body: JSON.stringify({status: 503, message: 'Fixture: temporary service failure'})});
    }
    if (name === '/config') {
        data = {pd_enabled: true, auth_enabled: true, server_capabilities_verified: true};
    } else if (name === '/auth/login') {
        data = {id: 'crud-admin', user_name: 'crud_admin', is_superadmin: true};
    } else if (name === '/auth/context') {
        data = {mode: 'AUTH', role: 'SUPERADMIN', capabilities: ['graphspaces_read', 'accounts_manage'],
            actions: {graphspaces: ['create', 'read', 'update', 'delete']}, scopes: {all_graphspaces: true}};
    } else if (name === '/auth/status') {
        data = {};
    } else if (name === '/auth/users/list') {
        data = {users: []};
    } else if (name === '/graphspaces/list') {
        data = {graphspaces: state.spaces.map(s => s.name)};
    } else if (name === '/graphspaces' && method === 'GET') {
        const query = url.searchParams.get('query') || '';
        data = paged(state.spaces.filter(s => s.name.includes(query) || s.nickname.includes(query)), url);
    } else if (name === '/graphspaces' && method === 'POST') {
        const values = request.postDataJSON();
        assert(!state.spaces.some(s => s.name === values.name), 'Fixture must not create duplicate names');
        data = {...space(values.name, values.nickname), ...values};
        state.spaces.push(data);
        state.writes.push({method, name, values});
    } else if (/^\/graphspaces\/[^/]+$/.test(name)) {
        const id = decodeURIComponent(name.split('/')[2]);
        data = state.spaces.find(s => s.name === id);
        if (method === 'PUT') {
            const values = request.postDataJSON();
            if (state.delayedSave) await state.delayedSave;
            Object.assign(data, values);
            state.writes.push({method, name, values});
        } else if (method === 'DELETE') {
            state.spaces = state.spaces.filter(s => s.name !== id);
            state.writes.push({method, name});
            data = {};
        }
    } else if (name.endsWith('/graphs')) {
        data = paged([], url);
    } else {
        unexpected.add(`${method} ${name}`);
        data = {};
    }
    return route.fulfill({status: 200, contentType: 'application/json',
        body: JSON.stringify({status: 200, data})});
}
let page;
async function step(id, description, work) {
    const start = Date.now();
    await work();
    const interactionMs = Date.now() - start;
    await page.mouse.move(20, 20);
    await page.waitForTimeout(200);
    const screenshot = `${id}.png`;
    await page.screenshot({path: path.join(output, screenshot), fullPage: true});
    checks.push({id, description, status: 'passed', screenshot, interaction_ms: interactionMs,
        focused_control: await page.evaluate(() => document.activeElement?.outerHTML.slice(0, 350))});
    console.log(`CAPTURE ${id}: ${description} (${interactionMs} ms)`);
    if (process.env.HUBBLE_AUDIT_INTERACTIVE === '1'
        && Number(id.slice(0, 2)) >= Number(process.env.HUBBLE_AUDIT_PAUSE_FROM || 0)) {
        await new Promise(resolve => process.stdin.once('data', resolve));
    }
}
const dialog = () => page.getByRole('dialog');
const card = nickname => page.locator('.ant-card').filter({has: page.getByText(nickname, {exact: true})}).first();
const nameField = () => page.locator('#control-hooks_name');
const aliasField = () => page.locator('#control-hooks_nickname');
const quotaField = () => page.locator('#control-hooks_storage_limit');
async function edit(nickname) {
    await card(nickname).getByRole('button', {name: /更多/}).click();
    await page.getByRole('menuitem', {name: '编辑', exact: true}).click();
    await aliasField().waitFor();
    await page.waitForFunction(() => !document.querySelector('#control-hooks_nickname')?.disabled);
}
async function deleteSpace(nickname) {
    await card(nickname).getByRole('button', {name: /更多/}).click();
    await page.getByRole('menuitem', {name: '删除', exact: true}).click();
    await dialog().waitFor();
}
async function messageGone() {
    await page.locator('.ant-message-notice').waitFor({state: 'hidden'}).catch(() => {});
}
let browser;
(async () => {
    try {
        browser = await chromium.launch({headless: true, executablePath: '/usr/bin/chromium', args: ['--no-sandbox']});
        const context = await browser.newContext({viewport: {width: 1440, height: 1000}, timezoneId: 'Asia/Taipei'});
        await context.addInitScript(() => {
            localStorage.setItem('languageType', 'zh-CN');
            window.auditLongTasks = [];
            new PerformanceObserver(list => window.auditLongTasks.push(...list.getEntries()
                .map(entry => ({start: entry.startTime, duration: entry.duration}))))
                .observe({type: 'longtask', buffered: true});
        });
        await context.route('**/api/v*/**', fixture);
        page = await context.newPage();
        page.setDefaultTimeout(10000);
        page.on('pageerror', error => errors.push(error.message));
        await page.goto(`${base}/graphspace`);
        await page.getByRole('textbox', {name: '用户名', exact: true}).fill('crud_admin');
        await page.locator('input[type=password]').fill('fixture-password');
        await page.getByRole('button', {name: /登\s*录/}).click();
        await step('01-list', 'Authenticated graphspace card list and keyboard create entry', async () => {
            await card('记忆库').waitFor();
            await page.getByRole('button', {name: '创建图空间', exact: true}).focus();
            assert(await page.getByRole('button', {name: '创建图空间', exact: true}).evaluate(el => el === document.activeElement));
        });
        await step('02-invalid-create', 'Keyboard create opens modal; invalid name is rejected inline', async () => {
            await page.keyboard.press('Enter');
            await nameField().fill('Invalid ID');
            await dialog().getByRole('button', {name: /创\s*建/, exact: true}).click();
            await dialog().getByText('以字母开头,只能包含小写字母、数字、_, 最长48位', {exact: true}).waitFor();
            assert.equal(state.writes.length, 0);
            assert.equal(await nameField().getAttribute('aria-invalid'), 'true');
        });
        await step('03-create-failure', 'Create API failure preserves editable values', async () => {
            await nameField().fill('crud_demo');
            await aliasField().fill('审计测试空间');
            state.fail = 'POST /graphspaces';
            await dialog().getByRole('button', {name: /创\s*建/, exact: true}).click();
            await page.locator('.ant-message-error').waitFor();
            assert.equal(await aliasField().inputValue(), '审计测试空间');
            assert.equal(state.writes.length, 0);
        });
        await messageGone();
        await step('04-created', 'Retry creates once and immediately returns to the card list', async () => {
            await dialog().getByRole('button', {name: /创\s*建/, exact: true}).click();
            await card('审计测试空间').waitFor();
            assert.equal(state.writes.length, 1);
        });
        await messageGone();
        await step('05-edit-resources', 'Edit alias and quota with current metadata loaded', async () => {
            await edit('审计测试空间');
            await aliasField().fill('已更新空间');
            await dialog().getByText('高级部署与资源限制', {exact: true}).click();
            await quotaField().fill('25');
            await quotaField().press('Tab');
            assert.equal(await quotaField().inputValue(), '25');
        });
        await step('06-save-failure', 'Save API failure preserves changes for retry', async () => {
            state.fail = 'PUT /graphspaces/crud_demo';
            await dialog().getByRole('button', {name: /保\s*存/, exact: true}).click();
            await page.locator('.ant-message-error').waitFor();
            assert.equal(await quotaField().inputValue(), '25');
            assert.equal(state.spaces.find(s => s.name === 'crud_demo').storage_limit, 10240);
        });
        await messageGone();
        let releaseSave;
        await step('07a-saving', 'Pending save keeps the form visible and prevents duplicate submissions', async () => {
            state.delayedSave = new Promise(resolve => {releaseSave = resolve;});
            const save = dialog().getByRole('button', {name: /保\s*存/, exact: true});
            await save.click();
            await page.waitForFunction(() => document.querySelector('#control-hooks_storage_limit')?.disabled);
            assert(await quotaField().isVisible());
            assert(await dialog().getByRole('button', {name: /取\s*消/, exact: true}).isDisabled());
            const box = await save.boundingBox();
            await page.mouse.click(box.x + box.width / 2, box.y + box.height / 2);
            assert.equal(state.requests.filter(r => r.method === 'PUT').length, 2);
        });
        state.delayedSave = null;
        releaseSave();
        await step('07-saved', 'Save retry updates alias and quota in the list', async () => {
            await card('已更新空间').waitFor();
            assert.equal(state.spaces.find(s => s.name === 'crud_demo').storage_limit, 25);
            assert(await card('已更新空间').getByText('1 MB / 25 GB').isVisible());
        });
        await messageGone();
        await step('08-cancel-edit', 'Cancel discards alias and quota edits', async () => {
            await edit('已更新空间');
            await aliasField().fill('不应保存');
            await dialog().getByText('高级部署与资源限制', {exact: true}).click();
            await quotaField().fill('40');
            await dialog().getByRole('button', {name: /取\s*消/, exact: true}).click();
            await card('已更新空间').waitFor();
            assert.equal(state.spaces.find(s => s.name === 'crud_demo').storage_limit, 25);
            assert.equal(state.writes.filter(r => r.method === 'PUT').length, 1);
        });
        await step('09-table-mode', 'Table mode retains updated values and actions', async () => {
            await page.getByText('列表模式', {exact: true}).click();
            await page.getByRole('row').filter({hasText: '已更新空间'}).waitFor();
        });
        await step('10-filter-empty', 'Search shows an explicit filtered empty state', async () => {
            await page.getByPlaceholder('请输入图空间名称').fill('不存在');
            await Promise.all([
                page.waitForResponse(r => r.url().includes('/graphspaces?') && r.url().includes('query=')),
                page.getByPlaceholder('请输入图空间名称').press('Enter'),
            ]);
            await page.getByText('没有符合当前筛选条件的图空间', {exact: true}).waitFor();
            assert.equal(await page.getByRole('row').filter({hasText: '已更新空间'}).count(), 0);
        });
        await step('11-filter-recovery', 'Clear search recovers records and return to card mode', async () => {
            await page.getByPlaceholder('请输入图空间名称').fill('');
            await page.getByPlaceholder('请输入图空间名称').press('Enter');
            await page.getByRole('row').filter({hasText: '已更新空间'}).waitFor();
            await page.getByText('图模式', {exact: true}).click();
            await card('已更新空间').waitFor();
        });
        await step('12-delete-cancel', 'Deletion has a confirmation and cancel preserves the space', async () => {
            await deleteSpace('已更新空间');
            assert(await dialog().getByText('crud_demo', {exact: true}).isVisible());
        });
        await dialog().getByRole('button', {name: /取\s*消/, exact: true}).click();
        assert(state.spaces.some(s => s.name === 'crud_demo'));
        await step('13-delete-failure', 'Delete API failure preserves the item and allows retry', async () => {
            await deleteSpace('已更新空间');
            state.fail = 'DELETE /graphspaces/crud_demo';
            await dialog().getByRole('button', {name: /确\s*定/, exact: true}).click();
            await page.locator('.ant-message-error').waitFor();
            assert(state.spaces.some(s => s.name === 'crud_demo'));
            assert(await dialog().isVisible());
        });
        await messageGone();
        await step('14-deleted', 'Confirmed retry removes the selected space', async () => {
            await dialog().getByRole('button', {name: /确\s*定/, exact: true}).click();
            await card('已更新空间').waitFor({state: 'hidden'});
            assert(!state.spaces.some(s => s.name === 'crud_demo'));
        });
        await messageGone();
        await step('15-page-two', 'Second page with one record is reachable', async () => {
            while (state.spaces.length < 12) state.spaces.push(space(`filler_${state.spaces.length}`, `示例空间${state.spaces.length}`));
            await page.reload();
            await card('默认图空间').waitFor();
            await page.locator('.ant-pagination-item-2').click();
            await card('示例空间11').waitFor();
        });
        await step('16-search-resets-page', 'Search from page two returns the matching first-page space', async () => {
            await page.getByPlaceholder('请输入图空间名称').fill('记忆库');
            await page.getByPlaceholder('请输入图空间名称').press('Enter');
            await card('记忆库').waitFor();
        });
        await step('17-list-failure', 'List API failure is distinct from an empty list and offers retry', async () => {
            state.fail = 'GET /graphspaces';
            await page.getByPlaceholder('请输入图空间名称').fill('');
            await page.getByPlaceholder('请输入图空间名称').press('Enter');
            await page.getByText('无法加载图空间列表。', {exact: true}).waitFor();
        });
        await step('18-list-retry', 'List retry restores cards', async () => {
            await page.getByRole('button', {name: '重试图空间列表'}).click();
            await card('记忆库').waitFor();
        });
        await step('19-delete-last-page', 'Deleting the only item on page two returns to the previous page', async () => {
            await page.locator('.ant-pagination-item-2').click();
            await card('示例空间11').waitFor();
            await deleteSpace('示例空间11');
            await dialog().getByRole('button', {name: /确\s*定/, exact: true}).click();
            await card('记忆库').waitFor();
            assert(!state.spaces.some(s => s.name === 'filler_11'));
            assert(await page.locator('.ant-pagination-item-1.ant-pagination-item-active').count());
        });
        assert.deepEqual(errors, []);
        assert.deepEqual([...unexpected], []);
    } catch (error) {
        checks.push({status: 'failed', error: error.stack});
        console.error(error);
        if (page) await page.screenshot({path: path.join(output, 'failure.png'), fullPage: true});
        process.exitCode = 1;
    } finally {
        const longTasks = page ? await page.evaluate(() => window.auditLongTasks || []).catch(() => []) : [];
        fs.writeFileSync(path.join(output, 'report.json'), JSON.stringify({
            scope: 'Chromium with stateful mocked APIs; not live-cluster or production performance validation',
            viewport: '1440x1000', checks, errors, unexpected: [...unexpected], writes: state.writes,
            request_counts: state.requests.reduce((counts, {method, name, params}) => {
                const query = new URLSearchParams(params).toString();
                const key = `${method} ${name}${query ? `?${query}` : ''}`;
                counts[key] = (counts[key] || 0) + 1;
                return counts;
            }, {}),
            final_document_long_tasks: longTasks,
        }, null, 2));
        if (browser) await browser.close();
        process.stdin.pause();
    }
})();
