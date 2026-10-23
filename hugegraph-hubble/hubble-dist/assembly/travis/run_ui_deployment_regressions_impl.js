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
// Browser integration fixture: every API response is mocked. This does not verify a live cluster.
const fs = require('fs');
const path = require('path');
const os = require('os');
const assert = require('node:assert/strict');
const hubbleRoot = path.resolve(__dirname, '../../..');
const { chromium } = require(path.join(hubbleRoot, 'hubble-fe/node_modules/playwright'));
const base = (process.env.HUBBLE_URL || 'http://127.0.0.1:3000').replace(/\/+$/, '');
const output = process.env.HUBBLE_UI_EVIDENCE_DIR || fs.mkdtempSync(path.join(os.tmpdir(), 'hubble-deployment-ui-'));
fs.mkdirSync(output, { recursive: true });
const checks = [];
const unexpected = new Set();
const errors = [];
let failure = null;
const spaces = [
    { name: 'DEFAULT', nickname: '默认图空间', create_time: '2026-10-13', auth: false, max_graph_number: 100, cpu_limit: 64, memory_limit: 128, compute_cpu_limit: 64, compute_memory_limit: 128, storage_limit: 100, storage_used_kb: 10752 },
    { name: 'soc_graph', nickname: '记忆库', create_time: '2026-10-15', auth: false, max_graph_number: 100, cpu_limit: 64, memory_limit: 128, compute_cpu_limit: 64, compute_memory_limit: 128, storage_limit: 10, storage_used_kb: 252590 },
];
const graph = (name, nickname) => ({ name, nickname, status: 'CREATED' });
// The destination graph must not exist in DEFAULT: stale selector data cannot pass.
const graphsBySpace = {
    DEFAULT: [graph('hugegraph', '记忆图'), graph('default_only', '默认空间专属图')],
    soc_graph: [graph('hugegraph', '记忆图'), graph('second', '测试图')],
};
const now = 1792757451000;
const node = (n) => ({ id: `pd-${n}`, name: `PD ${n}`, type: 'PD', role: n === 1 ? 'LEADER' : 'FOLLOWER', status: 'UP', observed_at: now, metrics: {}, metric_statuses: { system: { availability: 'UNAVAILABLE', reason: 'pd_metrics_unmapped', observed_at: now, fresh: false, stale: false } } });
const source = { status: 'UP', availability: 'PARTIAL', reason: 'pd_metrics_unmapped', observed_at: now, last_success_at: now, fresh: false, stale: false, diagnostics: { affected_node_count: 3, failed_metric_groups: ['system'], pd_metric_mapping: { configured_origin: 'http://pd-service:8620', status: 'no_match', matched_nodes: 0, discovered_nodes: 3, omitted_members: 0, members: [1, 2, 3].map(n => ({ node_id: 'pd-' + n, origin: 'http://pd-' + n + ':8620', endpoint_valid: true, origin_matches: false, base_path_matches: true })) } }, cluster_state: 'Cluster_Not_Ready', data_state: 'PState_Normal' };
const task = { id: 80001, task_name: 'g.V().limit(100002)', task_type: 'gremlin', task_status: 'failed', task_create: now - 49000, task_update: now - 45602 };
const taskResult = { ...task, task_result: 'org.apache.hugegraph.exception.LimitExceedException: Task result size 77849617 exceeded limit 16777216 bytes' };
function pageResult(records, selected) { return { records, total: records.length, current: 1, size: 100, pages: 1, selected: records.find(x => x.name === selected) ?? null }; }
async function setup(browser, auth) {
    const context = await browser.newContext({ viewport: { width: 1440, height: 1000 }, timezoneId: 'Asia/Taipei' });
    await context.addInitScript(() => localStorage.setItem('languageType', 'zh-CN'));
    const state = { auth, loggedIn: false, loginRequests: [], truncation: false, pendingDetail: null, taskLookups: 0 };
    const capabilities = ['graphspaces_read', 'operations_health_read', 'operations_topology_read', ...(auth ? ['accounts_manage', 'model_settings_manage'] : [])];
    await context.route('**/api/v*/**', async (route) => {
        const req = route.request();
        const url = new URL(req.url());
        const p = url.pathname.replace(/^\/api\/v[\d.]+/, '');
        const method = req.method();
        let data;
        if (p === '/config')
            data = { pd_enabled: true, auth_enabled: auth, server_capabilities_verified: true, graph_create_enabled: true, cypher_enabled: false };
        else if (p === '/auth/login') {
            const credentials = method === 'POST' ? req.postDataJSON() : null;
            state.loginRequests.push({method, credentials});
            state.loggedIn = method === 'POST' && credentials?.user_name === 'smoke_admin' &&
                credentials?.user_password === 'fixture-only-password';
            if (!state.loggedIn) {
                await route.fulfill({status: 401, contentType: 'application/json',
                    body: JSON.stringify({status: 401, message: 'Invalid fixture login'})});
                return;
            }
            data = { id: 'smoke-admin', user_name: 'smoke_admin', user_nickname: '测试管理员', is_superadmin: true };
        }
        else if (p === '/auth/context') {
            const admin = auth && state.loggedIn;
            data = { mode: auth ? 'AUTH' : 'NON_AUTH', role: admin ? 'SUPERADMIN' : 'ANONYMOUS', capabilities: auth && !admin ? [] : capabilities, actions: { graphspaces: admin ? ['create', 'read', 'update', 'delete'] : auth ? [] : ['read'] }, scopes: { all_graphspaces: admin } };
        }
        else if (p === '/auth/status')
            data = {};
        else if (p === '/auth/users/list')
            data = { users: [] };
        else if (p === '/graphspaces/list')
            data = { graphspaces: spaces.map(x => x.name) };
        else if (p === '/graphspaces')
            data = pageResult(spaces, url.searchParams.get('selected'));
        else if (/^\/graphspaces\/[^/]+$/.test(p)) {
            const s = spaces.find(x => x.name === p.split('/')[2]);
            if (method === 'PUT') {
                Object.assign(s, req.postDataJSON());
                data = s;
            }
            else if (state.pendingDetail && s.name === 'DEFAULT') {
                const pending = state.pendingDetail;
                state.pendingDetail = null;
                await pending;
                data = s;
            }
            else
                data = s;
        }
        else if (p.endsWith('/graphs')) {
            const graphSpace = p.split('/')[2];
            assert(graphsBySpace[graphSpace], `Unexpected graph-list scope: ${graphSpace}`);
            data = pageResult(graphsBySpace[graphSpace], url.searchParams.get('selected'));
        }
        else if (p.endsWith('/graph_read_mode'))
            data = { status: '1' };
        else if (p === '/vermeer')
            data = { enable: false };
        else if (p.endsWith('/text2gql/config'))
            data = { available: false, custom_available: true, reason: 'not_configured' };
        else if (p.endsWith('/schema/edgelabels') || p.endsWith('/schema/propertykeys'))
            data = { records: [] };
        else if (p.endsWith('/schema/vertexlabels'))
            data = { records: [{ name: 'Memory', properties: ['name'] }] };
        else if (p.endsWith('/execute-histories') || p.endsWith('/gremlin-collections'))
            data = { records: [], total: 0 };
        else if (p.endsWith('/gremlin-query/async-task'))
            data = { task_id: 80001 };
        else if (p.endsWith('/gremlin-query'))
            data = method === 'GET' ? { vertexcount: null, edgecount: null } : { type: 'VERTEX', graph_view: { vertices: [{ id: '1', label: 'Memory', properties: { name: 'hello' } }], edges: [] }, table_view: { header: ['id', 'label', 'properties'], rows: [] }, json_view: { data: [] }, default_limit_applied: true, default_limit: 300, ...(state.truncation ? { properties_truncated: true, property_preview_limit: 256, property_preview_vertex_ids: ['1'], graph_truncated: true } : {}) };
        else if (p.endsWith('/async-tasks/ids'))
            data = ++state.taskLookups < 3 ? [] : [task];
        else if (p.endsWith('/async-tasks/80001'))
            data = taskResult;
        else if (p.endsWith('/async-tasks'))
            data = pageResult([]); // The scheduler metadata must discover this task before the list does.
        else if (p === '/operations/nodes')
            data = { items: [node(1), node(2), node(3)], total: 3, page: 1, page_size: 20, observed_at: now, stale: false, sources: { pd: source } };
        else if (p === '/operations/overview')
            data = { sources: { pd: source }, observed_at: now, stale: false, nodes: [] };
        else if (p.endsWith('/is_auth'))
            data = { auth: false };
        else if (p.endsWith('/get'))
            data = { name: p.split('/')[4], nickname: '记忆图' };
        else {
            unexpected.add(`${method} ${p}`);
            data = {};
        }
        await route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify({ status: 200, data }) });
    });
    const page = await context.newPage();
    page.on('pageerror', e => errors.push(e.message));
    page.setDefaultTimeout(12000);
    return { context, page, state };
}
async function screenshot(page, name) {
    await page.mouse.move(20, 20);
    await page.evaluate(() => {
        document.activeElement?.blur();
        window.scrollTo(0, 0);
        document.querySelectorAll('main, [role=main], .ant-layout-content')
            .forEach(element => element.scrollTo(0, 0));
    });
    await page.waitForTimeout(250);
    if (name !== 'failure' && !name.startsWith('failure-')) {
        const toasts = await page.locator('.ant-message-error').allTextContents();
        assert.deepEqual(toasts, [], 'Unexpected error toast: ' + toasts.join('; '));
    }
    await page.screenshot({path: path.join(output, name + '.png'), fullPage: true});
}
async function check(name, fn) { try {
    await fn();
    checks.push({ name, status: 'passed' });
    console.log('PASS ' + name);
}
catch (e) {
    checks.push({ name, status: 'failed', error: e.message });
    console.log('FAIL ' + name + ': ' + e.message);
    throw e;
} }
(async () => {
    const executablePath = process.env.PLAYWRIGHT_CHROMIUM_EXECUTABLE_PATH ||
        (fs.existsSync('/usr/bin/chromium') ? '/usr/bin/chromium' : undefined);
    const browser = await chromium.launch({ headless: true, executablePath, args: ['--no-sandbox'] });
    try {
        const { page, state } = await setup(browser, true);
        await check('authenticated login and GraphSpace/account visibility', async () => {
            await page.goto(base + '/graphspace');
            await page.getByRole('textbox', { name: '用户名', exact: true }).fill('smoke_admin');
            await page.locator('input[type=password]').fill('fixture-only-password');
            const loginResponse = page.waitForResponse(response => new URL(response.url()).pathname.endsWith('/auth/login') && response.request().method() === 'POST');
            await page.getByRole('button', { name: /登\s*录/ }).click();
            const response = await loginResponse;
            await response.finished();
            assert.equal(response.status(), 200);
            assert.deepEqual(state.loginRequests, [{method: 'POST', credentials: {user_name: 'smoke_admin', user_password: 'fixture-only-password'}}]);
            assert.equal(state.loggedIn, true);
            await page.getByTestId('graphspace-page-title').waitFor();
            assert(await page.locator('a[href="/account"]').isVisible());
            await screenshot(page, 'graphspace-authenticated');
        });
        await check('GraphSpace title date and header whitespace navigate exactly to selected space', async () => {
            const card = () => page.locator('.ant-card').filter({ has: page.getByText('记忆库', { exact: true }) }).first();
            for (const target of ['date', 'title', 'whitespace', 'enter']) {
                await page.goto(base + '/graphspace');
                await card().waitFor();
                if (target === 'date')
                    await card().getByText(/2026-10-15/).click();
                else if (target === 'title')
                    await card().getByText('记忆库', { exact: true }).click();
                else if (target === 'enter')
                    await card().getByRole('button', { name: '进入图空间' }).click();
                else
                    await card().locator('.ant-card-head').click({ position: { x: 12, y: 12 } });
                await page.waitForURL('**/graphspace/soc_graph');
            }
        });
        await check('GraphSpace menu edit does not navigate and current quota is editable', async () => {
            await page.goto(base + '/graphspace');
            const card = page.locator('.ant-card').filter({ has: page.getByText('记忆库', { exact: true }) }).first();
            await card.getByRole('button', { name: '更多' }).click();
            assert(new URL(page.url()).pathname === '/graphspace');
            await page.getByRole('menuitem', { name: '编辑', exact: true }).click();
            await page.getByRole('dialog').waitFor();
            await page.locator('#control-hooks_storage_limit').waitFor({ state: 'attached' });
            await page.waitForFunction(() => document.querySelector('#control-hooks_storage_limit')?.value === '10');
            await page.getByRole('dialog').getByText('高级部署与资源限制', { exact: true }).click();
            const quota = page.locator('#control-hooks_storage_limit');
            assert(await quota.isEditable());
            await quota.fill('25');
            assert.equal(await quota.inputValue(), '25');
            assert(new URL(page.url()).pathname === '/graphspace');
            await screenshot(page, 'graphspace-edit');
            await page.getByRole('button', { name: /取\s*消/ }).click();
        });
        await check('late GraphSpace details cannot overwrite a newly opened editor', async () => {
            await page.goto(base + '/graphspace');
            let release;
            state.pendingDetail = new Promise(resolve => release = resolve);
            const staleResponse = page.waitForResponse(response => new URL(response.url()).pathname.endsWith('/graphspaces/DEFAULT') && response.request().method() === 'GET');
            const staleRequest = page.waitForRequest(request => new URL(request.url()).pathname.endsWith('/graphspaces/DEFAULT') && request.method() === 'GET');
            const defaultCard = page.locator('.ant-card').filter({ has: page.getByText('默认图空间', { exact: true }) }).first();
            await defaultCard.getByRole('button', { name: '更多' }).click();
            await page.getByRole('menuitem', { name: '编辑', exact: true }).click();
            await staleRequest;
            assert(await page.getByRole('button', { name: /保\s*存/ }).isDisabled());
            await page.getByRole('button', { name: /取\s*消/ }).click();
            const nextCard = page.locator('.ant-card').filter({ has: page.getByText('记忆库', { exact: true }) }).first();
            await nextCard.getByRole('button', { name: '更多' }).click();
            await page.getByRole('menuitem', { name: '编辑', exact: true }).click();
            await page.waitForFunction(() => document.querySelector('#control-hooks_name')?.value === 'soc_graph');
            release();
            await (await staleResponse).finished();
            await page.evaluate(() => new Promise(resolve => requestAnimationFrame(() => requestAnimationFrame(resolve))));
            assert.equal(await page.locator('#control-hooks_name').inputValue(), 'soc_graph');
            assert.equal(await page.locator('#control-hooks_storage_limit').inputValue(), '10');
            await page.getByRole('button', { name: /取\s*消/ }).click();
        });
        await check('Graph context switches via actual header selectors', async () => {
            await page.goto(base + '/gremlin/DEFAULT/hugegraph');
            await page.getByRole('button', { name: '执行查询', exact: true }).waitFor();
            await page.getByRole('combobox', { name: '当前 GraphSpace', exact: true }).click();
            await page.locator('.ant-select-item-option').filter({ hasText: '记忆库' }).click();
            await page.getByRole('combobox', { name: '当前图', exact: true }).click();
            await page.locator('.ant-select-item-option').filter({ hasText: '测试图' }).click();
            await page.waitForURL('**/gremlin/soc_graph/second');
            await screenshot(page, 'graph-context-switch');
        });
        await check('routine query limit is quiet canvas note without unknown totals', async () => {
            await page.goto(base + '/gremlin/DEFAULT/hugegraph');
            await page.locator('.cm-content').fill('g.V()');
            await page.getByRole('button', { name: '执行查询', exact: true }).click();
            await page.getByText('已自动限制为 300 条', { exact: true }).waitFor();
            await page.getByRole('group', { name: '节点：当前结果 1；全图总数未获取', exact: true }).waitFor();
            await page.getByRole('group', { name: '边：当前结果 0；全图总数未获取', exact: true }).waitFor();
            assert.equal(await page.getByRole('alert').count(), 0);
            assert.equal(await page.getByText('/不可用', { exact: false }).count(), 0);
            await screenshot(page, 'query-routine-limit');
        });
        await check('truncation is compact with details on demand', async () => {
            state.truncation = true;
            await page.getByRole('button', { name: '执行查询', exact: true }).click();
            await page.getByText(/部分属性为只读预览/).waitFor();
            assert.equal(await page.getByRole('alert').count(), 0);
            await screenshot(page, 'query-truncated');
            await page.getByRole('button', { name: '预览说明', exact: true }).click();
            await page.getByText('可缩小查询范围或仅返回所需属性。异步任务也受 Server 结果大小限制', {exact:true}).waitFor();
            assert.equal(await page.getByRole('alert').count(), 0);
            await screenshot(page, 'query-preview-details');
            await page.getByText('表格', {exact:true}).click();
            assert.equal(await page.getByText(/关联图为部分预览/).isVisible(), false);
            assert.equal(await page.getByRole('alert').count(), 0);
        });
        await check('async task ID remains visible in the selected graph and failure is actionable', async () => {
            await page.goto(base + '/gremlin/soc_graph/hugegraph');
            await page.getByRole('button', { name: '切换异步任务', exact: true }).click();
            await page.locator('.cm-content').fill('g.V().limit(100002)');
            await page.getByRole('button', { name: '提交任务', exact: true }).click();
            await page.getByRole('button', { name: '查看任务进度', exact: true }).click();
            await page.waitForURL('**/asyncTasks/soc_graph/hugegraph?taskId=80001&submitted=1');
            await page.getByText('任务 80001 已提交，正在同步状态', { exact: true }).waitFor();
            await page.getByText(/服务端已接受任务，正在每秒查询任务状态/).waitFor();
            assert.equal(await page.getByText('80001', { exact: true }).count(), 0);
            await screenshot(page, 'async-task-submitted');
            await page.getByText('80001', { exact: true }).waitFor();
            assert(state.taskLookups >= 3, `Expected delayed exact-ID polling, got ${state.taskLookups} lookups`);
            await page.getByText('查看原因', { exact: true }).click();
            await page.getByText('任务执行失败', { exact: true }).waitFor();
            assert.equal(await page.getByText('查看完整结果', { exact: true }).count(), 0);
            await page.getByText('任务结果超过服务端限制', { exact: true }).waitFor();
            await page.getByText(/结果大小为 74\.24 MiB，服务端上限为 16\.00 MiB/).waitFor();
            await page.getByText(/减少返回条数或仅返回需要的属性后重新提交/).waitFor();
            await page.getByRole('link', { name: '调整查询', exact: true }).waitFor();
            await screenshot(page, 'async-task-result-limit');
        });
        await check('PD unmapped metrics have localized three-node source diagnostics', async () => {
            await page.goto(base + '/operations/nodes?type=PD&source=pd#operations-source-diagnostics');
            await page.locator('summary').filter({hasText: '检查地址与修复方法'}).click();
            await page.getByText('最近拓扑采集成功', { exact: false }).waitFor();
            const repair = await page.getByText(/hugegraph-hubble\.properties/).innerText();
            for (const detail of ['pd.server', 'server.protocol', 'restUrl', '修改 Hubble 配置后需重启']) {
                assert(repair.includes(detail), `Missing actionable PD repair detail: ${detail}`);
            }
            await page.getByText('尚未采集系统指标', { exact: false }).first().waitFor();
            assert.equal(await page.getByText(/来源可能在发现节点前/).count(), 0);
            await page.getByText('http://pd-service:8620', { exact: true }).waitFor();
            await page.getByText(/本次快照受影响节点:\s*3/).waitFor();
            await page.getByText('没有匹配节点', { exact: true }).waitFor();
            await page.getByText('0 / 3', { exact: true }).waitFor();
            for (const member of source.diagnostics.pd_metric_mapping.members) {
                await page.getByText(member.origin, { exact: true }).waitFor();
            }
            await page.setViewportSize({width: 1440, height: 1600});
            await screenshot(page, 'pd-unmapped-diagnostics');
            await page.setViewportSize({width: 1440, height: 1000});
        });
        const anonymous = await setup(browser, false);
        await check('anonymous GraphSpaces visible with account and mutation actions hidden', async () => {
            await anonymous.page.goto(base + '/graphspace');
            await anonymous.page.getByTestId('graphspace-page-title').waitFor();
            assert.equal(await anonymous.page.locator('a[href="/account"]').count(), 0);
            assert.equal(await anonymous.page.getByRole('button', { name: '创建图空间', exact: true }).count(), 0);
            for (const mode of ['图模式', '列表模式']) {
                await anonymous.page.getByText(mode, { exact: true }).click();
                const container = mode === '图模式' ? '.ant-card' : 'tbody tr';
                for (const nickname of ['默认图空间', '记忆库']) {
                    const item = anonymous.page.locator(container).filter({has: anonymous.page.getByText(nickname, {exact: true})});
                    await item.waitFor({state: 'visible'});
                }
                assert.equal(await anonymous.page.getByRole('button', { name: '更多', exact: true }).count(), 0);
                assert.equal(await anonymous.page.getByText('编辑', { exact: true }).count(), 0);
                assert.equal(await anonymous.page.getByText('删除', { exact: true }).count(), 0);
            }
            await anonymous.page.getByText('图模式', { exact: true }).click();
            await screenshot(anonymous.page, 'graphspace-anonymous');
        });
    }
    catch (e) {
        failure = e.message;
        console.error(e.stack);
        for (const [contextIndex, ctx] of browser.contexts().entries()) {
            for (const [pageIndex, p] of ctx.pages().entries()) {
                console.log('PAGE', p.url(), await p.locator('body').innerText());
                await screenshot(p, `failure-context-${contextIndex}-page-${pageIndex}`);
            }
        }
    }
    finally {
        fs.writeFileSync(path.join(output, 'browser-report.json'), JSON.stringify({ status: !failure && checks.every(x => x.status === 'passed') && !errors.length && !unexpected.size ? 'passed' : 'failed', observedAt: new Date().toISOString(), browserVersion: browser.version(), timezone: 'Asia/Taipei', scope: 'Actual Chromium UI against mocked API responses; not real-cluster acceptance', checks, unexpectedApi: [...unexpected], pageErrors: errors, failure }, null, 2) + '\n');
        await browser.close();
    }
    if (failure || checks.some(x => x.status === 'failed') || errors.length || unexpected.size)
        process.exitCode = 1;
})();
