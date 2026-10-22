# Licensed to the Apache Software Foundation (ASF) under one or more
# contributor license agreements. See the NOTICE file distributed with
# this work for additional information regarding copyright ownership.
# The ASF licenses this file to You under the Apache License, Version 2.0
# (the "License"); you may not use this file except in compliance with
# the License. You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

import pathlib, subprocess, os, sys
ROOT=pathlib.Path('/workspace/hubble-perf')
name=sys.argv[1]
java='/workspace/.onboarding/tools/jdk17/bin/java'
args={
 'pd':['-Dspring.config.location=conf/application.yml','-jar','lib/hg-pd-service-1.8.0.jar'],
 'store':['-Dspring.config.location=conf/application.yml','-jar','lib/hg-store-node-1.8.0.jar'],
 'server':['-Dlog4j.configurationFile=conf/log4j2.xml','-cp','conf:lib/*:ext/*:plugins/*','org.apache.hugegraph.bootstrap.HugeGraphServerBootstrap','false','conf/gremlin-server.yaml','conf/rest-server.properties'],
 'hubble':['--add-opens=java.base/java.net=ALL-UNNAMED','-Dhubble.home.path='+str(ROOT/'runtime/hubble'),'-cp','/workspace/hugegraph-toolchain/hugegraph-hubble/hubble-be/target/classes:'+ (ROOT/'hubble.classpath').read_text().strip() if (ROOT/'hubble.classpath').exists() else '', 'org.apache.hugegraph.HugeGraphHubble',str(ROOT/'runtime/hubble/conf/hugegraph-hubble.properties')]
}[name]
heap={'pd':'1024m','store':'2048m','server':'3072m','hubble':'3072m'}[name]
cwd=ROOT/'runtime'/name
(cwd/'logs').mkdir(exist_ok=True, parents=True)
modules=['@bin/jvm-module.options'] if name!='hubble' else []
log=(ROOT/'logs'/f'{name}-stdout.log').open('a')
p=subprocess.Popen([java,*modules,'-XX:ActiveProcessorCount=4','-Xms256m','-Xmx'+heap,'-Xlog:gc:file='+str(ROOT/'logs'/f'{name}-gc.log'),*args],cwd=cwd,stdout=log,stderr=subprocess.STDOUT,start_new_session=True)
(ROOT/f'{name}.pid').write_text(str(p.pid))
print(name,p.pid)
