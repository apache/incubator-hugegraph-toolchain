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

const {chromium} = require('../../../hubble-fe/node_modules/playwright');
const fs = require('fs');
const path = require('path');
const assert = require('node:assert/strict');
const {execFileSync} = require('node:child_process');
const codeCommit = execFileSync('git', ['rev-parse', 'HEAD'], {
 cwd: path.resolve(__dirname, '../../../..'), encoding: 'utf8',
}).trim();
const base = process.env.HUBBLE_UI_BASE || 'http://127.0.0.1:3004';
const output = process.env.HUBBLE_UI_EVIDENCE_DIR || '/tmp/hubble-vermeer-first-evidence';
fs.mkdirSync(output, {recursive:true});
const checks = [];
let runnerError = null;
const unexpected = new Set();
const errors = [];
const spaces = [
 {name:'DEFAULT',nickname:'默认图空间',create_time:'2026-10-13',auth:false,max_graph_number:100,cpu_limit:64,memory_limit:128,compute_cpu_limit:64,compute_memory_limit:128,storage_limit:100,storage_used_kb:10752},
 {name:'soc_graph',nickname:'记忆库',create_time:'2026-10-15',auth:false,max_graph_number:100,cpu_limit:64,memory_limit:128,compute_cpu_limit:64,compute_memory_limit:128,storage_limit:10,storage_used_kb:252590},
];
const graphs = [{name:'hugegraph',nickname:'记忆图',status:'loaded'}, {name:'second',nickname:'测试图',status:'loaded'}];
const now = 1792757451000;
const tasks = [
 {id:-801,task_name:'PageRank',task_type:'vermeer-task:compute',task_status:'success',task_create:now-5000,task_update:now},
 {id:802,task_name:'PageRank',task_type:'computer-dis',task_status:'success',task_create:now-5000,task_update:now},
];
function pageResult(records, selected) {return {records,total:records.length,current:1,size:100,pages:1,selected:records.find(x=>x.name===selected)??null};}
async function setup(browser, auth) {
 const context=await browser.newContext({viewport:{width:1440,height:1000}});
 await context.addInitScript(()=>localStorage.setItem('languageType','zh-CN'));
 const state={auth, enabled:true, loaded:true, computerAvailable:true, capabilityCalls:0, submissions:[]};
 const capabilities=['graphspaces_read','operations_health_read','operations_topology_read',...(auth?['accounts_manage','model_settings_manage']:[])];
 await context.route('**/api/v*/**', async route=>{
  const req=route.request(); const url=new URL(req.url()); const p=url.pathname.replace(/^\/api\/v[\d.]+/,''); const method=req.method(); let data;
  if(p==='/config')data={pd_enabled:true,auth_enabled:auth,server_capabilities_verified:true,graph_create_enabled:true,cypher_enabled:false};
  else if(p==='/auth/login')data={id:'smoke-admin',user_name:'smoke_admin',user_nickname:'测试管理员',is_superadmin:true};
  else if(p==='/auth/context')data={mode:auth?'AUTH':'NON_AUTH',role:auth?'SUPERADMIN':'ANONYMOUS',capabilities,actions:{graphspaces:auth?['create','read','update','delete']:['read']},scopes:{all_graphspaces:auth}};
  else if(p==='/auth/status')data={};
  else if(p==='/auth/users/list')data={users:[]};
  else if(p==='/graphspaces/list')data={graphspaces:spaces.map(x=>x.name)};
  else if(p==='/graphspaces')data=pageResult(spaces,url.searchParams.get('selected'));
  else if(/^\/graphspaces\/[^/]+$/.test(p)) {
   const s=spaces.find(x=>x.name===p.split('/')[2]);
   data=s;
  }
  else if(p.endsWith('/graphs'))data=pageResult(graphs.map(g=>({...g,status:state.loaded?'loaded':'created'})),url.searchParams.get('selected'));
  else if(p.endsWith('/graph_read_mode'))data={status:'1'};
  else if(p==='/vermeer')data={enable:state.enabled};
  else if(p.endsWith('/algorithms/olap/capability')) {state.capabilityCalls++; data={available:state.computerAvailable};}
  else if(p.endsWith('/algorithms/vermeer') && method==='POST') {state.submissions.push({path:p,body:req.postDataJSON()}); data={task_id:-801};}
  else if(p.endsWith('/text2gql/config'))data={available:false,custom_available:true,reason:'not_configured'};
  else if(p.endsWith('/schema/edgelabels')||p.endsWith('/schema/propertykeys'))data={records:[]};
  else if(p.endsWith('/schema/vertexlabels'))data={records:[{name:'Memory',properties:['name']}]};
  else if(p.endsWith('/execute-histories')||p.endsWith('/gremlin-collections'))data={records:[],total:0};
  else if(p.endsWith('/gremlin-query'))data={vertexcount:100,edgecount:100};
  else if(p.endsWith('/async-tasks/ids'))data=tasks;
  else if(p.endsWith('/async-tasks'))data=pageResult(tasks);
  else if(p.endsWith('/is_auth'))data={auth:false};
  else if(p.endsWith('/get'))data={name:p.split('/')[4],nickname:'记忆图',status:state.loaded?'loaded':'created'};
  else {unexpected.add(`${method} ${p}`); data={};}
  await route.fulfill({status:200,contentType:'application/json',body:JSON.stringify({status:200,data})});
 });
 const page=await context.newPage();
 page.on('pageerror',e=>errors.push(e.message));
 page.setDefaultTimeout(12000);
 return {context,page,state};
}
async function screenshot(page,name){assert.equal(await page.locator('.ant-message-error').count(),0,'no caught error toasts'); await page.screenshot({path:path.join(output,name+'.png'),fullPage:true});}
async function check(name,fn){try{await fn();checks.push({name,status:'passed'}); console.log('PASS '+name);}catch(e){checks.push({name,status:'failed',error:e.message}); console.log('FAIL '+name+': '+e.message);throw e;}}
(async()=>{
 const browser=await chromium.launch({headless:true,executablePath:process.env.CHROMIUM_PATH||'/usr/bin/chromium',args:['--no-sandbox']});
 try {
 const {page,state}=await setup(browser,true);
 const goAlgorithms=async()=>{await page.goto(base+'/algorithms/DEFAULT/hugegraph'); await page.getByText('Vermeer · OLAP 全图计算',{exact:true}).waitFor();};
 const search=()=>page.getByPlaceholder("算法查询");
 const searchFor=async value=>{await search().fill(value); await page.getByText('K-out API（GET，基础版）',{exact:true}).waitFor({state:'hidden'});};
 const pagerank=()=>page.locator('.ant-collapse-item').filter({has:page.getByRole('link',{name:/^PageRank /})});
 await check('authenticated login and GraphSpace/account visibility',async()=>{
  await page.goto(base+'/graphspace'); await page.getByRole('textbox',{name:'用户名',exact:true}).fill('smoke_admin'); await page.locator('input[type=password]').fill('fixture-only-password'); await page.getByRole('button',{name:/登\s*录/}).click();
  await page.getByTestId('graphspace-page-title').waitFor(); assert(await page.locator('a[href="/account"]').isVisible());
  await screenshot(page,'01-authenticated');
 });
 await check('Vermeer loaded: correct docs and PageRank submission route',async()=>{
  await goAlgorithms();
  await page.getByText('通过 Vermeer 执行；图已加载，可运行下方算法',{exact:true}).waitFor();
  assert.equal(state.capabilityCalls,0);
  assert.equal(await page.getByText('Vermeer · OLAP 全图计算',{exact:true}).locator('..').locator('.ant-collapse-item').count(),9);
  assert.equal(await page.getByText('Computer 兼容入口（按需展开）',{exact:true}).count(),0);
  assert.equal(await page.getByRole('link',{name:'Vermeer 图计算指南',exact:true}).getAttribute('href'),'https://github.com/apache/hugegraph-computer/tree/master/vermeer');
  await searchFor('PageRank');
  await pagerank().waitFor();
  assert.equal(await pagerank().getByRole('link').getAttribute('href'),'https://github.com/apache/hugegraph-computer/tree/master/vermeer');
  const run=pagerank().locator('.ant-collapse-header button');
  assert.equal(await run.isDisabled(),false);
  await pagerank().scrollIntoViewIfNeeded(); await screenshot(page,'02-vermeer-ready');
  await run.click(); await page.getByText('查看任务进度',{exact:true}).waitFor();
  assert.equal(state.submissions.length,1);
  assert.equal(state.submissions[0].path,'/graphspaces/DEFAULT/graphs/hugegraph/algorithms/vermeer');
  assert.equal(state.submissions[0].body.params['compute.algorithm'],'pagerank');
  await screenshot(page,'03-vermeer-submitted');
 });
 await check('native-only Louvain is not presented as a supported Vermeer algorithm',async()=>{
  await search().fill('Louvain');
  await page.getByText('当前 Hubble 的 Vermeer 接口未提供匹配的算法',{exact:true}).waitFor();
  assert.equal(await page.locator('.ant-collapse-item').count(),0);
  await screenshot(page,'04-unsupported-vermeer');
 });
 await check('Vermeer unloaded graph explains loading and disables execution',async()=>{
  state.loaded=false; await goAlgorithms(); await searchFor('PageRank');
  await page.getByText('通过 Vermeer 执行。请先在顶部将当前图加载到 Vermeer',{exact:true}).waitFor();
  assert(await pagerank().locator('.ant-collapse-header button').isDisabled());
  await pagerank().scrollIntoViewIfNeeded(); await screenshot(page,'05-vermeer-unloaded');
 });
 await check('unavailable Vermeer never automatically probes or exposes Computer',async()=>{
  state.enabled=false; await goAlgorithms(); await searchFor('PageRank');
  const toggle=page.getByRole('button',{name:'Computer 兼容入口（按需展开）',exact:true});
  assert.equal(await toggle.getAttribute('aria-expanded'),'false');
  assert.equal(state.capabilityCalls,0); assert.equal(await pagerank().count(),0);
  await screenshot(page,'06-computer-collapsed');
  await toggle.click(); await pagerank().waitFor();
  await page.getByText('下方算法使用 Computer 兼容接口，任务不会发送到 Vermeer',{exact:false}).waitFor();
  assert.equal(state.capabilityCalls,1);
  assert((await pagerank().getByRole('link').getAttribute('href')).includes('/quickstart/computing/hugegraph-computer/'));
  await screenshot(page,'07-computer-opt-in');
 });
 await check('Computer opt-in is cleared after graph A to B to A',async()=>{
  const calls=state.capabilityCalls;
  const toggle=page.getByRole('button',{name:'Computer 兼容入口（按需展开）',exact:true});
  for(const [label,graph] of [['测试图','second'],['记忆图','hugegraph']]) {
   await page.getByRole('combobox',{name:'当前图',exact:true}).click();
   await page.locator('.ant-select-item-option').filter({hasText:label}).click();
   await page.waitForURL('**/algorithms/DEFAULT/'+graph);
   await page.getByRole('button',{name:'Computer 兼容入口（按需展开）',exact:true,expanded:false}).waitFor();
   assert.equal(await toggle.getAttribute('aria-expanded'),'false');
   assert.equal(await pagerank().count(),0);
   assert.equal(state.capabilityCalls,calls);
  }
  await searchFor('PageRank');
  await toggle.scrollIntoViewIfNeeded();
  await screenshot(page,'12-computer-return-collapsed');
  await toggle.click(); await pagerank().waitFor();
  assert.equal(state.capabilityCalls,calls+1);
  assert.equal(await page.getByRole('link',{name:'Computer 配置文档',exact:true}).getAttribute('href'),
   'https://hugegraph.apache.org/docs/quickstart/computing/hugegraph-computer/#6-built-in-algorithms-and-development');
 });
 await check('unavailable Computer capability stays non-runnable after opt-in',async()=>{
  state.computerAvailable=false; await goAlgorithms(); await searchFor('PageRank');
  await page.getByRole('button',{name:'Computer 兼容入口（按需展开）',exact:true}).click();
  await page.getByText('当前图无法使用 Computer 兼容接口。请检查已有 Computer 部署',{exact:false}).waitFor();
  assert.equal(await pagerank().count(),0); await screenshot(page,'08-computer-unavailable');
 });
 await check('task types identify the real Vermeer and compatibility Computer engines',async()=>{
  state.enabled=true; await page.goto(base+'/asyncTasks/DEFAULT/hugegraph');
  await page.getByText('Vermeer 图计算',{exact:true}).waitFor();
  await page.getByText('Computer 图计算（兼容）',{exact:true}).waitFor();
  await screenshot(page,'09-task-engines');
 });
 const anonymous=await setup(browser,false);
 await check('anonymous GraphSpaces remain visible while account and mutations are hidden',async()=>{
  await anonymous.page.goto(base+'/graphspace'); await anonymous.page.getByTestId('graphspace-page-title').waitFor();
  assert.equal(await anonymous.page.locator('a[href="/account"]').count(),0);
  assert.equal(await anonymous.page.getByRole('button',{name:'创建图空间',exact:true}).count(),0);
  await screenshot(anonymous.page,'10-anonymous');
 });
 await check('graph context selectors switch Vermeer scope without probing Computer',async()=>{
  await anonymous.page.goto(base+'/algorithms/DEFAULT/hugegraph');
  await anonymous.page.getByRole('combobox',{name:'当前 GraphSpace',exact:true}).click();
  await anonymous.page.locator('.ant-select-item-option').filter({hasText:'记忆库'}).click();
  await anonymous.page.getByRole('combobox',{name:'当前图',exact:true}).click();
  await anonymous.page.locator('.ant-select-item-option').filter({hasText:'测试图'}).click();
  await anonymous.page.waitForURL('**/algorithms/soc_graph/second');
  await anonymous.page.getByText('通过 Vermeer 执行；图已加载，可运行下方算法',{exact:true}).waitFor();
  assert.equal(anonymous.state.capabilityCalls,0); await screenshot(anonymous.page,'11-context-switched');
 });
 }catch(e){runnerError=e.message;process.exitCode=1;console.error(e.stack);for(const ctx of browser.contexts()){for(const p of ctx.pages()){console.log('PAGE',p.url(),await p.locator('body').innerText());await p.screenshot({path:path.join(output,'failure.png'),fullPage:true});}}}finally{
  fs.writeFileSync(path.join(output,'browser-report.json'),JSON.stringify({scope:'Actual Chromium UI with explicit API fixtures; not live-engine end-to-end acceptance',codeCommit,screenshotLocation:'Outside Git; see PR description for screenshot evidence status',checks,runnerError,unexpectedApi:[...unexpected],pageErrors:errors},null,2)+'\n');
  await browser.close();
 }
 if(checks.some(x=>x.status==='failed')||unexpected.size||errors.length)process.exitCode=1;
})();
