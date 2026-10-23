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
 const bottom = await page.locator('.operations-page').evaluate(el => el.getBoundingClientRect().bottom);
 await page.setViewportSize({width:1440,height:Math.max(1100,Math.ceil(bottom+80))});
 await page.screenshot({path:path.join(output,name+'.png'),fullPage:true,animations:'disabled'});
}
(async()=>{
 fs.mkdirSync(output,{recursive:true});
 const browser=await chromium.launch({executablePath:process.env.PLAYWRIGHT_CHROMIUM_EXECUTABLE_PATH || '/usr/bin/chromium',headless:true,args:['--no-sandbox']});
 const context=await browser.newContext({viewport:{width:1440,height:1100}});
 await context.addInitScript(()=>localStorage.setItem('languageType','zh-CN'));
 const pageErrors=[];const unexpected=[];const checks=[];
 await context.route('**/api/v*/**',async route=>{
  const url=new URL(route.request().url());const p=url.pathname.replace(/^\/api\/v[\d.]+/,'');let data;
  if(p==='/config') data={pd_enabled:true,auth_enabled:false,server_capabilities_verified:true};
  else if(p==='/auth/context') data={mode:'NON_AUTH',role:'ANONYMOUS',capabilities:['graphspaces_read','operations_health_read','operations_topology_read'],actions:{graphspaces:['read']},scopes:{all_graphspaces:true}};
  else if(p==='/graphspaces') data={records:[{name:'DEFAULT',nickname:'默认图空间'}],total:1};
  else if(p.endsWith('/graphs'))data={records:[{name:'hugegraph',nickname:'hugegraph'}],total:1};
  else if(p==='/auth/status')data={};
  else if(p==='/vermeer')data={enable:false};
  else if(p==='/dashboard')data={configured:false,available:false};
  else if(p==='/operations/overview')data={status:'DEGRADED',reason:'service_not_ready',observed_at:now,stale:false,sources,nodes,facts:{stores:1,stores_up:1}};
  else if(p==='/operations/nodes')data={items:nodes,total:nodes.length,page:1,page_size:20,observed_at:now,stale:false,sources};
  else if(p.startsWith('/operations/nodes/'))data={node:nodes.find(n=>n.id===p.split('/').at(-1)),observed_at:now,stale:false,sources};
  else{unexpected.push(p);data={};}
  await route.fulfill({status:200,contentType:'application/json',body:JSON.stringify({status:200,data})});
 });
 const page=await context.newPage();page.on('pageerror',e=>pageErrors.push(e.message));
 try{
  for(const [id,heading] of [['pd-1','PD 1'],['store-1','Store 1']]){
   await page.goto(base+'/operations/nodes/'+id);await page.getByRole('heading',{name:heading,exact:true}).waitFor();
   {const panel=page.getByRole('region',{name:'服务就绪状态'});await panel.waitFor();await panel.getByText(id==='pd-1'?'/v1/ready':'/-/state',{exact:true}).waitFor();assert(await page.getByText('UP',{exact:true}).first().isVisible());}
   await capture(page,id);checks.push(id);
  }
  await page.goto(base+'/operations/nodes');await page.getByRole('heading',{name:'节点详情',exact:true}).waitFor();
  await page.getByText('服务: 未就绪',{exact:true}).first().waitFor();
  await capture(page,'nodes');checks.push('nodes');
  await page.goto(base+'/operations/overview');await page.getByRole('heading',{name:'集群概览',exact:true}).waitFor();
  await page.getByText('服务: 未就绪',{exact:true}).first().waitFor();
  await capture(page,'overview');checks.push('overview');
  assert.deepEqual(pageErrors,[]);assert.deepEqual(unexpected,[]);
  fs.writeFileSync(output+'/report.json',JSON.stringify({status:'passed',scope:'Chromium UI with deterministic API fixtures; not live-cluster acceptance',checks,pageErrors,unexpected},null,2));
  console.log(JSON.stringify({status:'passed',checks,output}));
 }finally{await browser.close();}
})().catch(error=>{console.error(error);process.exitCode=1});
