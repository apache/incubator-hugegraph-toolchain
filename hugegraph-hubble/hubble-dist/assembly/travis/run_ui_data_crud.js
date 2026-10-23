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
const assert = require('node:assert/strict');
const hubbleRoot = path.resolve(__dirname, '../../..');
const { chromium } = require(path.join(hubbleRoot, 'hubble-fe/node_modules/playwright'));
const base = process.env.HUBBLE_URL || 'http://127.0.0.1:3000';
const output = process.env.HUBBLE_UI_EVIDENCE_DIR || path.join(require('os').tmpdir(), 'hubble-data-crud-20261023');
fs.mkdirSync(output, { recursive: true });
const checks = [];
const mutations = [];
const performanceSamples = [];
const properties = [{name:'name',data_type:'TEXT',cardinality:'SINGLE'}];
const label = {name:'Memory',id_strategy:'CUSTOMIZE_STRING',primary_keys:[],properties:[{name:'name',nullable:false}],property_indexes:[],open_label_index:true};
const unexpected = new Set();
const errors = [];
let failure = null;
const spaces = [
    { name: 'DEFAULT', nickname: '默认图空间', create_time: '2026-10-13', auth: false, max_graph_number: 100, cpu_limit: 64, memory_limit: 128, compute_cpu_limit: 64, compute_memory_limit: 128, storage_limit: 100, storage_used_kb: 10752 },
    { name: 'soc_graph', nickname: '记忆库', create_time: '2026-10-15', auth: false, max_graph_number: 100, cpu_limit: 64, memory_limit: 128, compute_cpu_limit: 64, compute_memory_limit: 128, storage_limit: 10, storage_used_kb: 252590 },
];
const graphs = [{ name: 'hugegraph', nickname: '记忆图', status: 'CREATED' }, { name: 'second', nickname: '测试图', status: 'CREATED' }];
function pageResult(records, selected) { return { records, total: records.length, current: 1, size: 100, pages: 1, selected: records.find(x => x.name === selected) ?? null }; }
async function setup(browser, auth) {
    const context = await browser.newContext({ viewport: { width: 1440, height: 1000 }, timezoneId: 'Asia/Taipei' });
    await context.addInitScript(() => localStorage.setItem('languageType', 'zh-CN'));
    const state = { auth, truncation: false, count:100, empty:false, queryError:false, deleteError:false, deleteMode:'normal', usageNames:new Set(), calls:[], values:new Map(), vertexlabels:[{...label,name:'AuditVertex'}], edgelabels:[{name:'AuditEdge',edgelabel_type:'NORMAL',source_label:'Memory',target_label:'Memory',properties:[],sort_keys:[],property_indexes:[]}] };
    const capabilities = ['graphspaces_read', 'operations_health_read', 'operations_topology_read', ...(auth ? ['accounts_manage', 'model_settings_manage'] : [])];
    await context.route('**/api/v*/**', async (route) => {
        const req = route.request();
        const url = new URL(req.url());
        const p = url.pathname.replace(/^\/api\/v[\d.]+/, '');
        const method = req.method();
        let data;
        state.calls.push({method, path: p, names: method === 'DELETE' ? url.searchParams.getAll('names') : undefined});
        if (p === '/config')
            data = { pd_enabled: true, auth_enabled: auth, server_capabilities_verified: true, graph_create_enabled: true, cypher_enabled: false };
        else if (p === '/auth/login')
            data = { id: 'smoke-admin', user_name: 'smoke_admin', user_nickname: '测试管理员', is_superadmin: true };
        else if (p === '/auth/context')
            data = { mode: auth ? 'AUTH' : 'NON_AUTH', role: auth ? 'SUPERADMIN' : 'ANONYMOUS', capabilities, actions: { graphspaces: auth ? ['create', 'read', 'update', 'delete'] : ['read'] }, scopes: { all_graphspaces: auth } };
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
            else
                data = s;
        }
        else if (p.endsWith('/graphs'))
            data = pageResult(graphs, url.searchParams.get('selected'));
        else if (p.endsWith('/graph_read_mode'))
            data = { status: '1' };
        else if (p === '/vermeer')
            data = { enable: false };
        else if (p.endsWith('/text2gql/config'))
            data = { available: false, custom_available: true, reason: 'not_configured' };
        else if (p.endsWith('/schema/propertykeys/check_using') || p.endsWith('/schema/vertexlabels/check_using'))
            data = Object.fromEntries(req.postDataJSON().names.map(name => [name,state.usageNames.has(name)]));
        else if (p.endsWith('/schema/propertykeys')) {
            if (method === 'POST') { const value=req.postDataJSON(); properties.push(value); mutations.push({operation:'property-create',value}); data=value; }
            else if (method === 'DELETE') {
                if(state.deleteError) return route.fulfill({status:500,contentType:'application/json',body:JSON.stringify({status:500,message:'Temporary schema failure'})});
                const requested = url.searchParams.getAll('names');
                assert.equal(url.searchParams.get('skip_using'), 'false');
                const names = state.deleteMode === 'partial-lost' ? requested.slice(0, 1) : requested;
                if (!state.deleteMode.startsWith('pending')) for(const name of names) {const index=properties.findIndex(x=>x.name===name);if(index>=0)properties.splice(index,1);}
                mutations.push({operation:'property-delete',names}); data={};
                if (state.deleteMode.endsWith('lost')) return route.abort('failed');
            } else data=pageResult(properties);
        }
        else if(p.endsWith('/schema/graphview')) data={vertices:[],edges:[]};
        else if (p.endsWith('/schema/edgelabels') || p.endsWith('/schema/vertexlabels')) {
            const isVertex = p.endsWith('/vertexlabels');
            const key = isVertex ? 'vertexlabels' : 'edgelabels';
            if (method === 'DELETE') {
                if (state.deleteError) return route.fulfill({status:500,contentType:'application/json',body:JSON.stringify({status:500,message:'Temporary schema failure'})});
                const names = url.searchParams.getAll('names');
                if (!state.deleteMode.startsWith('pending')) state[key] = state[key].filter(x => !names.includes(x.name));
                mutations.push({operation: key + '-delete', names});
                if (state.deleteMode.endsWith('lost')) return route.abort('failed');
                data = {};
            } else data = pageResult(isVertex ? [label, ...state.vertexlabels] : state.edgelabels);
        }
        else if (/\/schema\/vertexlabels\/[^/]+$/.test(p)) data=label;
        else if (/\/vertexlabel\//.test(p)) data={nullableProps:[],nonNullableProps:['name'],primaryKeys:[]};
        else if (p.endsWith('/vertex') && method==='POST') {const value=req.postDataJSON();state.values.set(value.id,value);mutations.push({operation:'vertex-create',value});data=value;}
        else if (/\/vertex\//.test(p) && method==='PUT') {const value=req.postDataJSON();state.values.set(value.id,value);mutations.push({operation:'vertex-update',value});data=value;}
        else if (p.endsWith('/execute-histories') || p.endsWith('/gremlin-collections'))
            data = { records: [], total: 0 };
        else if (p.endsWith('/gremlin-query')) {
            if(state.queryError && method==='POST') return route.fulfill({status:500,contentType:'application/json',body:JSON.stringify({status:500,message:'Fixture query unavailable'})});
            data = method === 'GET' ? { vertexcount: null, edgecount: null } : { type: 'VERTEX', graph_view: {vertices:state.empty?[]:Array.from({length:state.count},(_,i)=>state.values.get(String(i+1)) || {id:String(i+1),label:'Memory',properties:{name:'Node '+(i+1)}}),edges:[]}, table_view: { header: ['id', 'label', 'properties'], rows: [] }, json_view: { data: [] }, default_limit_applied: true, default_limit: 300, ...(state.truncation ? { properties_truncated: true, property_preview_limit: 256, property_preview_vertex_ids: ['1'], graph_truncated: true } : {}) };}
        else if (p.endsWith('/schema') || p.endsWith('/schema/all')) data={vertexlabels:[label],edgelabels:[],propertykeys:properties,indexlabels:[]};
        else if(p.includes('/schema/indexlabels')) data={records:[]};
        else if (p.endsWith('/is_auth'))
            data = { auth: false };
        else if (p.endsWith('/get'))
            data = { name: p.split('/')[4], nickname: '记忆图' };
        else {
            unexpected.add(`${method} ${p}`);
            data = {};
        }
        if (state.invalidSchemaList && method === 'GET' && /\/schema\/(propertykeys|vertexlabels|edgelabels)$/.test(p)) {
            data = {records: [{name: 'other'}, {name: 'other'}], total: 2};
        }
        await route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify({ status: 200, data }) });
    });
    const page = await context.newPage();
    page.on('pageerror', e => errors.push(e.message));
    page.setDefaultTimeout(12000);
    return { context, page, state };
}
async function screenshot(page, name, allowErrors = false) {
    await page.mouse.move(20, 20);
    await page.evaluate(() => {
        document.activeElement?.blur();
        window.scrollTo(0, 0);
        document.querySelectorAll('main, [role=main], .ant-layout-content')
            .forEach(element => element.scrollTo(0, 0));
    });
    await page.waitForTimeout(250);
    if (name !== 'failure' && !allowErrors) {
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
    const browser = await chromium.launch({executablePath: process.env.PLAYWRIGHT_CHROMIUM_EXECUTABLE_PATH || '/usr/bin/chromium', headless: true, args: ['--no-sandbox']});
    let page;
    try {
        const fixture = await setup(browser, true);
        page = fixture.page;
        const {state} = fixture;
        await page.goto(base + '/gremlin/DEFAULT/hugegraph');
        await page.getByRole('textbox', {name: '用户名', exact: true}).fill('smoke_admin');
        await page.locator('input[type=password]').fill('fixture-only-password');
        await page.getByRole('button', {name: /登\s*录/}).click();
        await page.getByRole('button', {name: '执行查询', exact: true}).waitFor();

        if (!process.env.PERF_ONLY) {
            await check('00 authenticated GraphSpace/account visibility and graph context switching', async () => {
                await page.goto(base + '/graphspace');
                await page.getByTestId('graphspace-page-title').waitFor();
                assert(await page.locator('a[href="/account"]').isVisible());
                await screenshot(page, '00-authenticated-graphspaces');
                await page.goto(base + '/gremlin/DEFAULT/hugegraph');
                await page.getByRole('combobox', {name: '当前 GraphSpace', exact: true}).click();
                await page.locator('.ant-select-item-option').filter({hasText: '记忆库'}).click();
                await page.getByRole('combobox', {name: '当前图', exact: true}).click();
                await page.locator('.ant-select-item-option').filter({hasText: '测试图'}).click();
                await page.waitForURL('**/gremlin/soc_graph/second');
                state.count = 1;
                await query(page, state, 'g.V().limit(1)');
                assert(state.calls.some(x => x.method === 'POST' && x.path === '/graphspaces/soc_graph/graphs/second/gremlin-query'));
                await screenshot(page, '00-graph-context-switched');
            });
            await check('01 schema create validates required name and supports keyboard submission', async () => {
                await page.goto(base + '/graphspace/DEFAULT/graph/hugegraph/meta');
                await page.getByText('列表模式', {exact: true}).click();
                await page.getByRole('button', {name: /创\s*建/, exact: true}).click();
                const dialog = page.getByRole('dialog');
                await dialog.getByRole('button', {name: '创建属性', exact: true}).click();
                assert.equal(mutations.filter(x => x.operation === 'property-create').length, 0);
                await page.locator('#name_help').waitFor();
                await screenshot(page, '01-schema-validation');
                await page.locator('#name').fill('ui_audit_property');
                await dialog.getByRole('button', {name: '创建属性', exact: true}).focus();
                await page.keyboard.press('Enter');
                await dialog.waitFor({state: 'hidden'});
                await page.getByRole('row').filter({hasText: 'ui_audit_property'}).waitFor();
                assert.equal(mutations.filter(x => x.operation === 'property-create').length, 1);
                await screenshot(page, '02-schema-created');
            });
            await check('02 schema delete cancel does not mutate; failed delete stays retryable', async () => {
                const row = page.getByRole('row').filter({hasText: 'ui_audit_property'});
                await row.getByRole('button', {name: '删除', exact: true}).click();
                await page.getByRole('dialog').getByRole('button', {name: /取\s*消/}).click();
                assert.equal(mutations.filter(x => x.operation === 'property-delete').length, 0);
                state.deleteError = true;
                await row.getByRole('button', {name: '删除', exact: true}).click();
                await page.getByRole('dialog').getByRole('button', {name: /确\s*定/}).click();
                await page.locator('.ant-message-error').waitFor();
                assert.equal(await page.getByRole('dialog').count(), 1);
                assert.equal(await page.getByRole('dialog').getByRole('button', {name: /确\s*定/}).isEnabled(), true);
                await page.getByRole('dialog').getByText('DEFAULT/hugegraph', {exact: true}).waitFor();
                await page.getByRole('dialog').getByText('ui_audit_property', {exact: true}).waitFor();
                await screenshot(page, '03-schema-delete-retry', true);
                state.deleteError = false;
                await page.getByRole('dialog').getByRole('button', {name: /确\s*定/}).click();
                await page.getByRole('dialog').waitFor({state: 'hidden'});
                await row.waitFor({state: 'hidden'});
                assert.equal(mutations.filter(x => x.operation === 'property-delete').length, 1);
                await page.waitForFunction(() => !document.querySelector('.ant-message-error'));
                await screenshot(page, '04-schema-delete-complete');
            });
            await check('02 duplicate schema snapshots keep deletion pending until a trusted retry', async () => {
                properties.push({name: 'UntrustedProperty', data_type: 'TEXT', cardinality: 'SINGLE'});
                await page.getByRole('button', {name: /刷\s*新/, exact: true}).click();
                const row = page.getByRole('row').filter({hasText: 'UntrustedProperty'});
                await row.getByRole('button', {name: '删除', exact: true}).click();
                const before = state.calls.filter(x => x.method === 'DELETE').length;
                await page.waitForFunction(() => !document.querySelector('.ant-message-success'));
                state.invalidSchemaList = true;
                await page.getByRole('dialog').getByRole('button', {name: /确\s*定/}).click();
                await page.locator('.ant-message-error').waitFor();
                assert.equal(state.calls.filter(x => x.method === 'DELETE').length, before);
                assert.equal(await page.getByRole('dialog').count(), 1);
                await screenshot(page, '04-untrusted-schema-list', true);
                state.invalidSchemaList = false;
                await page.getByRole('dialog').getByRole('button', {name: /确\s*定/}).click();
                await page.getByRole('dialog').waitFor({state: 'hidden'});
                await row.waitFor({state: 'hidden'});
                assert.equal(state.calls.filter(x => x.method === 'DELETE').length, before + 1);
                await page.waitForFunction(() => !document.querySelector('.ant-message-error'));
            });
            for (const [tab, labelName, operation, name] of [
                ['顶点类型', 'AuditVertex', 'vertexlabels-delete', 'vertex'],
                ['边类型', 'AuditEdge', 'edgelabels-delete', 'edge'],
            ]) {
                await check(`02 ${name} schema delete failure stays open and retries successfully`, async () => {
                    await page.getByRole('tab', {name: tab, exact: true}).click();
                    const row = page.getByRole('row').filter({hasText: labelName});
                    await row.getByRole('button', {name: '删除', exact: true}).click();
                    state.deleteError = true;
                    await page.getByRole('dialog').getByRole('button', {name: /确\s*定/}).click();
                    await page.locator('.ant-message-error').waitFor();
                    assert.equal(await page.getByRole('dialog').count(), 1);
                    await page.getByRole('dialog').getByText('DEFAULT/hugegraph', {exact: true}).waitFor();
                    await page.getByRole('dialog').getByText(labelName, {exact: true}).waitFor();
                    await screenshot(page, `03-${name}-schema-delete-retry`, true);
                    state.deleteError = false;
                    await page.getByRole('dialog').getByRole('button', {name: '重试删除', exact: true}).click();
                    await page.getByRole('dialog').waitFor({state: 'hidden'});
                    await row.waitFor({state: 'hidden'});
                    assert.equal(mutations.filter(x => x.operation === operation).length, 1);
                    await page.waitForFunction(() => !document.querySelector('.ant-message-error'));
                    await screenshot(page, `04-${name}-schema-delete-complete`);
                });
            }
            for (const [tab, name, kind] of [['属性', 'LostProperty', 'propertykeys'],
                ['顶点类型', 'LostVertex', 'vertexlabels'], ['边类型', 'LostEdge', 'edgelabels']]) {
                await check(`02 ${kind} lost response reconciles completed deletion without repeat`, async () => {
                    if (kind === 'propertykeys') properties.push({name, data_type: 'TEXT', cardinality: 'SINGLE'});
                    else if (kind === 'vertexlabels') state.vertexlabels.push({...label, name});
                    else state.edgelabels.push({name, properties: [], sort_keys: [], property_indexes: []});
                    await page.getByRole('tab', {name: tab, exact: true}).click();
                    await page.getByRole('button', {name: /刷\s*新/, exact: true}).click();
                    const row = page.getByRole('row').filter({hasText: name});
                    const before = state.calls.filter(x => x.method === 'DELETE').length;
                    state.deleteMode = 'lost';
                    await row.getByRole('button', {name: '删除', exact: true}).click();
                    await page.getByRole('dialog').getByRole('button', {name: /确\s*定/}).click();
                    await page.getByRole('dialog').waitFor({state: 'hidden'});
                    await row.waitFor({state: 'hidden'});
                    assert.equal(state.calls.filter(x => x.method === 'DELETE').length, before + 1);
                    await screenshot(page, `10-${kind}-lost-response-reconciled`);
                    state.deleteMode = 'normal';
                });
            }
            await check('02 partial batch retries remaining names and revalidates newly used properties', async () => {
                const names = ['PartialA', 'PartialB'];
                properties.push(...names.map(name => ({name, data_type: 'TEXT', cardinality: 'SINGLE'})));
                await page.getByRole('tab', {name: '属性', exact: true}).click();
                await page.getByRole('button', {name: /刷\s*新/, exact: true}).click();
                for (const name of names) await page.getByRole('row').filter({hasText: name}).getByRole('checkbox').check();
                await page.getByRole('button', {name: '批量删除', exact: true}).click();
                state.deleteMode = 'partial-lost';
                await page.getByRole('dialog').getByRole('button', {name: /确\s*定/}).click();
                await page.locator('.ant-message-error').waitFor();
                const before = state.calls.filter(x => x.method === 'DELETE').length;
                state.deleteMode = 'normal';
                state.usageNames.add('PartialB');
                await page.getByRole('dialog').getByRole('button', {name: /确\s*定/}).click();
                await page.getByText(/PartialB.*使用|PartialB.*占用/).waitFor();
                assert.equal(state.calls.filter(x => x.method === 'DELETE').length, before);
                await screenshot(page, '11-batch-retry-usage-changed', true);
                state.usageNames.clear();
                await page.getByRole('dialog').getByRole('button', {name: /确\s*定/}).click();
                await page.getByRole('dialog').waitFor({state: 'hidden'});
                assert.deepEqual(state.calls.filter(x => x.method === 'DELETE').at(-1).names, ['PartialB']);
                await page.waitForFunction(() => !document.querySelector('.ant-message-error'));
            });
            for (const mode of ['pending', 'pending-lost']) await check(`02 asynchronous ${mode} deletion checks status and requires explicit retry`, async () => {
                state.vertexlabels.push({...label, name: 'PendingVertex'});
                await page.getByRole('tab', {name: '顶点类型', exact: true}).click();
                await page.getByRole('button', {name: /刷\s*新/, exact: true}).click();
                await page.getByRole('row').filter({hasText: 'PendingVertex'}).getByRole('button', {name: '删除', exact: true}).click();
                state.deleteMode = mode;
                await page.getByRole('dialog').getByRole('button', {name: /确\s*定/}).click();
                const checkStatus = page.getByRole('dialog').getByRole('button', {name: /检查状态$/});
                await checkStatus.waitFor();
                const before = state.calls.filter(x => x.method === 'DELETE').length;
                await checkStatus.click();
                await page.waitForTimeout(200);
                assert.equal(state.calls.filter(x => x.method === 'DELETE').length, before);
                await page.waitForFunction(() => !document.querySelector('.ant-message-error'));
                await screenshot(page, `12-asynchronous-delete-${mode}`);
                state.deleteMode = 'normal';
                await page.getByRole('dialog').getByRole('button', {name: '重试删除', exact: true}).click();
                await page.getByRole('dialog').waitFor({state: 'hidden'});
                assert.equal(state.calls.filter(x => x.method === 'DELETE').length, before + 1);
            });
            await check('02 anonymous mode keeps GraphSpaces and schema CRUD, hides accounts', async () => {
                const anonymous = await setup(browser, false);
                const ap = anonymous.page;
                await ap.goto(base + '/graphspace');
                await ap.getByTestId('graphspace-page-title').waitFor();
                assert.equal(await ap.locator('a[href="/account"]').count(), 0);
                assert.equal(await ap.getByRole('button', {name: '创建图空间', exact: true}).count(), 0);
                await screenshot(ap, '13-anonymous-graphspaces');
                await ap.goto(base + '/graphspace/DEFAULT/graph/hugegraph/meta');
                await ap.getByText('列表模式', {exact: true}).click();
                await ap.getByRole('button', {name: /创\s*建/, exact: true}).click();
                await ap.locator('#name').fill('AnonymousProperty');
                await ap.getByRole('dialog').getByRole('button', {name: '创建属性', exact: true}).click();
                await ap.getByRole('dialog').waitFor({state: 'hidden'});
                await ap.getByRole('row').filter({hasText: 'AnonymousProperty'}).getByRole('button', {name: '删除', exact: true}).click();
                await screenshot(ap, '14-anonymous-schema-delete');
                await ap.getByRole('dialog').getByRole('button', {name: /确\s*定/}).click();
                await ap.getByRole('dialog').waitFor({state: 'hidden'});
                await ap.getByRole('row').filter({hasText: 'AnonymousProperty'}).waitFor({state: 'hidden'});
                await anonymous.context.close();
            });
            if (!process.env.SCHEMA_ONLY) await check('03 canvas vertex create and property update persist to API fixture', async () => {
                await page.goto(base + '/gremlin/DEFAULT/hugegraph');
                state.count = 1;
                await query(page, state, 'g.V().limit(1)');
                await page.getByText('新建', {exact: true}).hover();
                await page.locator('.ant-dropdown-menu').getByRole('menuitem', {name: '新增顶点', exact: true}).click();
                await page.getByRole('dialog').getByRole('combobox').click();
                await page.locator('.ant-select-item-option').filter({hasText: 'Memory'}).click();
                await page.getByPlaceholder('请输入ID值').fill('2');
                await page.getByPlaceholder('请输入属性值').fill('Browser created');
                await screenshot(page, '05-vertex-create');
                await page.getByRole('dialog').getByRole('button', {name: '添加', exact: true}).click();
                await page.getByRole('dialog').waitFor({state: 'hidden'});
                assert.equal(state.values.get('2').properties.name, 'Browser created');
                await query(page, state, 'g.V().limit(1)');
                const box = await page.locator('.graph-canvas-surface').boundingBox();
                await page.mouse.click(box.x + box.width / 2, box.y + box.height / 2);
                await page.getByRole('dialog').waitFor();
                await page.getByRole('dialog').getByRole('button', {name: /编\s*辑/, exact: true}).click();
                await page.getByRole('dialog').getByPlaceholder('请输入属性值').fill('Browser updated');
                await page.getByRole('dialog').getByRole('button', {name: /保\s*存/, exact: true}).click();
                await page.getByRole('dialog').waitFor({state: 'hidden'});
                assert.equal(state.values.get('1').properties.name, 'Browser updated');
                await query(page, state, 'g.V().limit(1)');
                const current = await page.locator('.graph-canvas-surface').boundingBox();
                await page.mouse.click(current.x + current.width / 2, current.y + current.height / 2);
                await page.getByRole('dialog').getByText('Browser updated', {exact: true}).waitFor();
                await screenshot(page, '06-vertex-update-persisted');
                await page.getByRole('dialog').getByRole('button', {name: /关\s*闭/, exact: true}).last().click();

            });
        }
        if (!process.env.SCHEMA_ONLY) await check('04 repeated 100 and 300 vertex query rendering and responsiveness', async () => {
            await page.goto(base + '/gremlin/DEFAULT/hugegraph');
            await page.getByRole('button', {name: '执行查询', exact: true}).waitFor();
            await page.evaluate(() => {
                window.__auditLongTasks = [];
                new PerformanceObserver(list => window.__auditLongTasks.push(...list.getEntries().map(x => ({startTime: x.startTime, duration: x.duration}))))
                    .observe({type: 'longtask', buffered: false});
            });
            // Warm renderer and font caches before measured repeated actions.
            state.count = 100;
            await query(page, state, 'g.V().limit(100)');
            for (const count of [100, 300]) {
                state.count = count;
                for (let repeat = 0; repeat < 5; repeat++) {
                    const sample = await query(page, state, `g.V().limit(${count})`, true);
                    performanceSamples.push({count, repeat: repeat + 1, ...sample});
                }
                await screenshot(page, `perf-${count}-vertices`);
            }
        });
        if (!process.env.PERF_ONLY && !process.env.SCHEMA_ONLY) {
            await check('05 empty result and failed query can be retried', async () => {
                state.empty = true;
                await page.locator('.cm-content').fill('g.V().hasId("missing")');
                await page.getByRole('button', {name: '执行查询', exact: true}).click();
                await page.getByText('查询成功，结果为空', {exact: true}).waitFor();
                await screenshot(page, '07-query-empty');
                state.empty = false;
                state.queryError = true;
                await page.getByRole('button', {name: '执行查询', exact: true}).click();
                await page.waitForTimeout(300);
                await page.getByText('查询失败', {exact: true}).waitFor();
                await screenshot(page, '08-query-failed', true);
                state.queryError = false;
                state.count = 1;
                await query(page, state, 'g.V().limit(1)');
                await page.waitForFunction(() => !document.querySelector('.ant-message-error'));
                await screenshot(page, '09-query-recovered');
            });
        }
    } catch (error) {
        failure = error.message;
        console.error(error.stack);
        if (page) {
            console.log('BODY', await page.locator('body').innerText());
            await page.screenshot({path: path.join(output, 'failure.png'), fullPage: true});
        }
    } finally {
        const metrics = [100, 300].map(count => {
            const values = performanceSamples.filter(x => x.count === count);
            const sorted = values.map(x => x.actionMs).sort((a, b) => a - b);
            return {count, samples: values.length, p50ActionMs: sorted[Math.floor(sorted.length / 2)], maxActionMs: sorted.at(-1), maxLongTaskMs: Math.max(0, ...values.map(x => x.maxLongTaskMs))};
        });
        fs.writeFileSync(path.join(output, 'browser-report.json'), JSON.stringify({status: !failure && !errors.length && !unexpected.size ? 'passed' : 'failed', observedAt: new Date().toISOString(), base, browserVersion: browser.version(), scope: 'Actual Chromium UI with intercepted in-memory API fixtures. Performance measures warm UI action through response completion and the current query/canvas busy-to-idle cycle plus two animation frames; does not measure Server or cluster throughput.', checks, mutations, metrics, performanceSamples, pageErrors: errors, unexpectedApi: [...unexpected], failure}, null, 2) + '\n');
        console.log('METRICS', JSON.stringify(metrics));
        await browser.close();
    }
    if (failure || errors.length || unexpected.size) process.exitCode = 1;
})();

async function query(page, state, script, measured = false) {
    await page.locator('.cm-content').fill(script);
    const start = await page.evaluate(() => {
        window.__perfObserver?.disconnect();
        window.__perfCycle = {queryBusy: false, canvasBusy: false};
        window.__perfObserver = new MutationObserver(records => {
            for (const record of records) {
                const busy = record.oldValue === 'true' || record.target.getAttribute('aria-busy') === 'true';
                if (busy && record.target.id === 'queryBar') window.__perfCycle.queryBusy = true;
                if (busy && record.target.matches('.graph-canvas')) window.__perfCycle.canvasBusy = true;
            }
        });
        window.__perfObserver.observe(document.body, {subtree: true, attributes: true,
            attributeOldValue: true, attributeFilter: ['aria-busy']});
        return performance.now();
    });
    const response = page.waitForResponse(r => r.url().endsWith('/gremlin-query') && r.request().method() === 'POST');
    await page.getByRole('button', {name: '执行查询', exact: true}).click();
    assert.equal(await (await response).finished(), null);
    await page.waitForFunction(() => window.__perfCycle.queryBusy && window.__perfCycle.canvasBusy
        && document.querySelector('#queryBar')?.getAttribute('aria-busy') === 'false'
        && document.querySelector('.graph-canvas')?.getAttribute('aria-busy') === 'false');
    await page.getByRole('group', {name: new RegExp('节点：当前结果 ' + state.count + '(?:，|；)')}).waitFor();
    await page.locator('.graph-canvas[aria-busy="false"]').waitFor();
    assert.equal(await page.getByRole('button', {name: '执行查询', exact: true}).isEnabled(), true);
    return page.evaluate(async ({start, measured}) => {
        await new Promise(resolve => requestAnimationFrame(() => requestAnimationFrame(resolve)));
        const end = performance.now();
        window.__perfObserver.disconnect();
        return {actionMs: Math.round((end - start) * 10) / 10, maxLongTaskMs: measured ? Math.max(0, ...window.__auditLongTasks.filter(x => x.startTime >= start && x.startTime < end).map(x => x.duration)) : 0};
    }, {start, measured});
}
