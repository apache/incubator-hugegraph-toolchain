/*
 *
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

// Deterministic Chromium fixtures; no request reaches a deployed cluster.
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const hubbleRoot = path.resolve(__dirname, '../../..');
const {chromium} = require(path.join(hubbleRoot, 'hubble-fe/node_modules/playwright'));
const output = process.env.HUBBLE_UI_EVIDENCE_DIR;
assert(output, 'Set HUBBLE_UI_EVIDENCE_DIR to a directory outside the repository');
const base = process.env.HUBBLE_URL || 'http://127.0.0.1:3000';
const now = Date.now();
const acceptedScreenshots = [];
const available = {availability:'AVAILABLE',observed_at:now,last_success_at:now,fresh:true,stale:false};
const nodes = [
{id:'server-1',name:'Server 1',type:'SERVER',status:'UP',observed_at:now,metrics:{system:{basic:{processors:8}},backend:{graphs:2}},metric_statuses:{system:available,backend:available}},
{id:'pd-1',name:'PD 1',type:'PD',role:'FOLLOWER',status:'UP',observed_at:now,metrics:{system:{}},metric_statuses:{system:available},readiness:{status:'NOT_READY',state:'STATE_FOLLOWER',reason:'pd_quorum_not_ready',observed_at:now,diagnostics:{request_path:'/v1/ready',http_status:503}}},
{id:'pd-2',name:'PD 2',type:'PD',role:'LEADER',status:'UP',observed_at:now,metrics:{},metric_statuses:{system:{availability:'UNSUPPORTED',reason:'metrics_not_collected'}},readiness:{status:'UNKNOWN',reason:'readiness_not_probed',diagnostics:{}}},
{id:'store-1',name:'Store 1',type:'STORE',status:'UP',observed_at:now,metrics:{system:{},drive:{},raft:{},backend:{leaders:3}},metric_statuses:{system:available,drive:available,raft:available,backend:available},readiness:{status:'NOT_READY',state:'STARTING',reason:'store_starting',observed_at:now,diagnostics:{request_path:'/-/state',http_status:200}}}
];
const sources={server:{...available,status:'UP'},pd:{...available,status:'UP',cluster_state:'Cluster_OK'},stores:{...available,status:'UP'}};
async function capture(page, name) {
 await page.waitForFunction(() => !document.querySelector('.ant-spin-spinning, .ant-spin-blur'));
 await page.mouse.move(20,20);
 const surface = page.locator('.operations-page');
 const bottom = await surface.count() ? await surface.evaluate(el => el.getBoundingClientRect().bottom) : 1020;
 await page.setViewportSize({width:1440,height:Math.max(1100,Math.ceil(bottom+80))});
 await page.screenshot({path:path.join(output,name+'.png'),fullPage:true,animations:'disabled'});
 acceptedScreenshots.push(name+'.png');
}

const spaces = ['DEFAULT','MEMORY'].map(name => ({name,nickname:name,default:name === 'DEFAULT',
 create_time:now,max_graph_number:100,cpu_limit:8,memory_limit:16,compute_cpu_limit:0,compute_memory_limit:0,
 storage_limit:100,storage_used_kb:1024,storage_percent:0.00001,auth:false}));
const graphOptions = {
 DEFAULT: [{name:'hugegraph',nickname:'hugegraph'}, {name:'archive',nickname:'archive'}],
 MEMORY: [{name:'memory',nickname:'memory'}, {name:'history',nickname:'history'}],
};
const operationsCapabilities = ['operations_health_read','operations_topology_read','operations_metrics_read'];
const fixturePassword = 'fixture-only-password';

async function fixture(browser, name, role) {
 const authenticated = role !== 'ANONYMOUS';
 const username = role === 'SUPERADMIN' ? 'fixture-admin' : 'fixture-reader';
 const session = `readiness-${name}`;
 const user = {id:username,user_name:username,user_nickname:username,
  is_superadmin:role === 'SUPERADMIN',resSpaces:role === 'USER' ? ['MEMORY'] : ['DEFAULT','MEMORY']};
 const capabilities = ['graphspaces_read','graph_resources_access',
  ...(role !== 'USER' ? operationsCapabilities : []),
  ...(role === 'SUPERADMIN' ? ['accounts_manage','graphspaces_manage'] : [])];
 const authContext = {mode:authenticated ? 'PD' : 'NON_AUTH',role,
  ...(authenticated ? {username} : {}),capabilities,
  actions:{graphspaces:role === 'SUPERADMIN' ? ['read','create','update','delete'] : ['read']},
  scopes:{all_graphspaces:role !== 'USER',admin_graphspaces:[],write_graphspaces:[],
   graph_resources:authenticated ? 'SERVER_AUTHORIZED' : 'SERVER_ANONYMOUS'}};
 const result = {name,mode:authContext.mode,role,checks:[],requests:[],contexts:[],pageErrors:[],unexpected:[]};
 const context = await browser.newContext({viewport:{width:1440,height:1100}});
 // No user, config, token or graph context is preseeded: AUTH must use the login form.
 await context.addInitScript(() => localStorage.setItem('languageType','zh-CN'));
 await context.route('**/api/v*/**',async route => {
  const request = route.request();
  const url = new URL(request.url());
  const p = url.pathname.replace(/^\/api\/v[\d.]+/,'');
  const signedIn = (request.headers().cookie || '').split(';').some(
   value => value.trim() === `HUBBLE_READINESS_SESSION=${session}`);
  const record = {method:request.method(),path:p,session:authenticated ? signedIn : false};
  result.requests.push(record);
  let data;let status = 200;const headers = {};
  if(p === '/config') data = {pd_enabled:true,auth_enabled:authenticated,server_capabilities_verified:true};
  else if(p === '/auth/login' && request.method() === 'POST') {
   const credentials = request.postDataJSON();
   if(!authenticated || credentials.user_name !== username || credentials.user_password !== fixturePassword) {
    status = 401;data = null;result.unexpected.push('invalid fixture login');
   }
   else {data = user;headers['set-cookie'] = `HUBBLE_READINESS_SESSION=${session}; Path=/; HttpOnly; SameSite=Lax`;}
  }
  else if(authenticated && !signedIn) {status = 401;data = null;}
  else if(p === '/auth/context') {data = authContext;result.contexts.push(authContext);}
  else if(p === '/auth/status') data = authenticated ? (role === 'SUPERADMIN' ? 'ADMIN' : 'USER') : {};
  else if(p === '/graphspaces') {
   const records = role === 'USER' ? spaces.filter(space => space.name === 'MEMORY') : spaces;
   data = {records,total:records.length};
  }
  else if(p === '/vermeer') data = {enable:false};
  else if(p === '/dashboard') data = {configured:false,available:false};
  else if(p.startsWith('/graphspaces/')) {
   const parts = p.split('/');const space = decodeURIComponent(parts[2]);const graph = decodeURIComponent(parts[4] || '');
   record.graphspace = space;if(graph) record.graph = graph;
   if(role === 'USER' && space !== 'MEMORY') {status = 403;data = null;}
   else if(parts.length === 3) data = spaces.find(item => item.name === space);
   else if(parts.length === 4 && parts[3] === 'graphs') data = {records:(graphOptions[space] || []).map(graph => ({...graph,graphspace:space})),total:graphOptions[space]?.length || 0};
   else if(parts.length === 6 && parts[5] === 'get') data = graphOptions[space]?.find(item => item.name === graph);
   else if(parts.length === 6 && parts[5] === 'gremlin-query') data = {vertexcount:0,edgecount:0,date:now,approximate:true};
   else if(parts.length === 7 && parts[5] === 'schema' && parts[6] === 'graphview') data = {vertexlabels:[],edgelabels:[]};
   else {result.unexpected.push(`${request.method()} ${p}`);data = {};}
  }
  else if(p.startsWith('/operations/') && role === 'USER') {status = 403;data = null;}
  else if(p === '/operations/overview') data = {status:'DEGRADED',reason:'service_not_ready',observed_at:now,stale:false,sources,nodes,facts:{stores:1,stores_up:1}};
  else if(p === '/operations/nodes') data = {items:nodes,total:nodes.length,page:1,page_size:20,observed_at:now,stale:false,sources};
  else if(p.startsWith('/operations/nodes/')) data = {node:nodes.find(node => node.id === p.split('/').at(-1)),observed_at:now,stale:false,sources};
  else {result.unexpected.push(`${request.method()} ${p}`);data = {};}
  record.status = status;
  await route.fulfill({status,headers,contentType:'application/json',body:JSON.stringify({status,data})});
 });
 const page = await context.newPage();
 page.on('pageerror',error => result.pageErrors.push(error.message));
 page.setDefaultTimeout(15000);
 return {context,page,result,username,authenticated,authContext};
}

async function login(item, redirect) {
 const {page,result,username} = item;
 await page.goto(`${base}/login?redirect=${encodeURIComponent(redirect)}`);
 await page.getByRole('heading',{name:'登录 Hubble',exact:true}).waitFor();
 assert.equal(result.requests.some(request => request.path.startsWith('/operations/')),false,
  'Operations must not be requested before form login');
 await page.getByRole('textbox',{name:'用户名',exact:true}).fill(username);
 await page.getByLabel('密码',{exact:true}).fill(fixturePassword);
 await page.getByRole('button',{name:/登\s*录/}).click();
 await page.waitForURL(base + redirect);
 await page.locator('.workbench-topbar').getByRole('button',{name:`打开 ${username} 的账号菜单`,exact:true}).waitFor();
 const stored = await page.evaluate(() => JSON.parse(sessionStorage.getItem('user_') || '{}'));
 assert.equal(stored.user_name,username,'Form login must establish the UI actor');
 assert.equal(result.requests.filter(request => request.path === '/auth/login').length,1);
 assert(result.contexts.some(context => context.username === username && context.role === item.authContext.role));
 assert(result.requests.some(request => request.path === '/auth/context' && request.session && request.status === 200));
 assert(result.requests.filter(request => !['/config','/auth/login'].includes(request.path))
  .every(request => request.session),'Authenticated API calls must carry the form-established cookie');
 result.checks.push('form-login-and-session');
}

async function readinessFlows(item) {
 const {page,result,authenticated} = item;
 for(const [id,heading] of [['pd-1','PD 1'],['store-1','Store 1']]) {
  await page.goto(base+'/operations/nodes/'+id);
  await page.getByRole('heading',{name:heading,exact:true}).waitFor();
  const panel = page.getByRole('region',{name:'服务就绪状态'});
  await panel.waitFor();
  await panel.getByText(id === 'pd-1' ? '/v1/ready' : '/-/state',{exact:true}).waitFor();
  assert(await page.getByText('UP',{exact:true}).first().isVisible());
  await capture(page,(authenticated ? 'auth-' : '')+id);result.checks.push(id);
 }
 await page.goto(base+'/operations/nodes');
 await page.getByRole('heading',{name:'节点详情',exact:true}).waitFor();
 await page.getByText('服务: 未就绪',{exact:true}).first().waitFor();
 await capture(page,(authenticated ? 'auth-' : '')+'nodes');result.checks.push('nodes');
 await page.goto(base+'/operations/overview');
 await page.getByRole('heading',{name:'集群概览',exact:true}).waitFor();
 await page.getByText('服务: 未就绪',{exact:true}).first().waitFor();
 await capture(page,(authenticated ? 'auth-' : '')+'overview');result.checks.push('overview');
}

async function choose(page, label, value) {
 const select = page.getByRole('combobox',{name:label,exact:true});
 await select.waitFor();
 await select.click();
 await page.locator('.ant-select-dropdown:visible .ant-select-item-option-content')
  .filter({hasText:new RegExp('^'+value+'$')}).click();
}

async function switching(item) {
 const {page,result,authenticated,username} = item;
 const restricted = item.authContext.role === 'USER';
 await page.goto(base+(restricted ? '/navigation' : '/operations/nodes'));
 if(!restricted) await page.getByRole('heading',{name:'节点详情',exact:true}).waitFor();
 const expectedSpaces = restricted ? ['MEMORY'] : ['DEFAULT','MEMORY'];
 const spaceSelect = page.getByRole('combobox',{name:'当前 GraphSpace',exact:true});
 await spaceSelect.click();
 const options = page.locator('.ant-select-dropdown:visible .ant-select-item-option-content');
 await options.first().waitFor();
 await page.waitForFunction(expected => {
  const dropdown = Array.from(document.querySelectorAll('.ant-select-dropdown')).find(
   element => element.getClientRects().length && getComputedStyle(element).visibility !== 'hidden');
  const texts = Array.from(dropdown?.querySelectorAll('.ant-select-item-option-content') || []).map(
   element => element.textContent);
  return JSON.stringify(texts) === JSON.stringify(expected);
 },expectedSpaces);
 await page.keyboard.press('Escape');
 for(const space of expectedSpaces) {
  await choose(page,'当前 GraphSpace',space);
  await page.waitForURL(`${base}/graphspace/${space}`);
  for(const graph of graphOptions[space].map(option => option.name)) {
   const graphRead = page.waitForResponse(response => new URL(response.url()).pathname
    .endsWith(`/graphspaces/${space}/graphs/${graph}/gremlin-query`) && response.status() === 200);
   await choose(page,'当前图',graph);
   await page.waitForURL(`${base}/graphspace/${space}/graph/${graph}/detail`);
   await page.waitForFunction(({key,space,graph}) => {
    const stored = JSON.parse(localStorage.getItem(key) || '{}');
    return stored.graphspace === space && stored.graph === graph;
   },{key:'hubble_workbench_graph_context'+(authenticated ? '.'+encodeURIComponent(username) : ''),space,graph});
   await graphRead;
   assert(result.requests.some(request => request.graphspace === space && request.graph === graph && request.status === 200));
  }
 }
 await page.goto(base+'/graphspace');
 await page.getByRole('button',{name:'MEMORY',exact:true}).waitFor();
 assert.equal(await page.getByRole('button',{name:'默认图空间',exact:true}).count(),restricted ? 0 : 1);
 assert.equal(await page.getByRole('button',{name:'创建图空间',exact:true}).count(),item.authContext.role === 'SUPERADMIN' ? 1 : 0);
 const sidebar = page.locator('.ant-layout-sider');
 assert.equal(await sidebar.locator('a[href="/graphspace"]').isVisible(),true);
 assert.equal(await sidebar.locator('a[href="/account"]').count(),item.authContext.role === 'SUPERADMIN' ? 1 : 0);
 assert.equal(await page.locator('.workbench-topbar [aria-haspopup="menu"]').count(),authenticated ? 1 : 0);
 if(!authenticated) {
  assert.equal(await page.evaluate(() => sessionStorage.getItem('user_')),null);
  assert.equal(result.requests.some(request => request.path === '/auth/login'),false);
 }
 await capture(page,item.result.name+'-context');
 if(restricted) {
  assert(result.contexts.every(context => context.role === 'USER' && context.scopes.all_graphspaces === false
   && context.scopes.graph_resources === 'SERVER_AUTHORIZED'));
  assert(result.requests.filter(request => request.graphspace).every(request => request.graphspace === 'MEMORY'));
  assert.equal(await sidebar.locator('a[href="/operations/nodes"]').count(),0);
  await page.goto(base+'/account');await page.waitForURL(base+'/navigation');
  assert.equal(result.requests.some(request => request.path.startsWith('/auth/users')),false,
   'Restricted account deep links must not mount account requests');
  await capture(page,item.result.name+'-account-guard');
  result.checks.push('restricted-scope-and-account-route');
 }
 result.checks.push('graphspace-and-graph-switching','account-and-graphspace-visibility');
}

(async () => {
 fs.mkdirSync(output,{recursive:true});
 const browser = await chromium.launch({executablePath:process.env.PLAYWRIGHT_CHROMIUM_EXECUTABLE_PATH || '/usr/bin/chromium',headless:true,args:['--no-sandbox']});
 const scenarios = [];let failure = null;
 try {
  for(const [name,role] of [['non-auth','ANONYMOUS'],['auth-admin','SUPERADMIN'],['auth-reader','USER']]) {
   const item = await fixture(browser,name,role);scenarios.push(item.result);
   try {
    if(item.authenticated) await login(item,role === 'USER' ? '/navigation' : '/operations/nodes/pd-1');
    if(role !== 'USER') await readinessFlows(item);
    await switching(item);
    assert.deepEqual(item.result.pageErrors,[]);assert.deepEqual(item.result.unexpected,[]);
   } catch(error) {
    await item.page.screenshot({path:path.join(output,name+'-failed.png'),fullPage:true});throw error;
   } finally {await item.context.close();}
  }
 } catch(error) {failure = error.message;throw error;}
 finally {
  await browser.close();
  const report = {status:failure ? 'failed' : 'passed',
   frontendTarget:base,frontendRevision:process.env.HUBBLE_UI_REVISION || 'unspecified; no exact-head claim',
   scope:'Actual Chromium UI with deterministic API fixtures, including form login and fixture session cookies; not live-backend authentication or deployed-cluster acceptance',
   checks:scenarios.flatMap(scenario => scenario.checks.map(check => `${scenario.name}:${check}`)),
   screenshots:acceptedScreenshots,scenarios,failure};
  fs.writeFileSync(path.join(output,'report.json'),JSON.stringify(report,null,2));
  console.log(JSON.stringify({status:report.status,checks:report.checks,output}));
 }
})().catch(error => {console.error(error);process.exitCode = 1;});
