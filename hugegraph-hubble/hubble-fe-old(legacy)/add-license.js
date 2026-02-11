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

var fs = require('fs')
var path = require('path')
const license = `/*
 * Licensed to the Apache Software Foundation (ASF) under one or more 
 * contributor license agreements; and to You under the Apache License, Version 2.0.
 */
`
const license_html = `<!--
 Licensed to the Apache Software Foundation (ASF) under one or more 
 contributor license agreements; and to You under the Apache License, Version 2.0.
 -->`

const full_license = `/*
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
`
// str 以 key 结尾
function _endWith(str, key){    
  var reg = new RegExp(key + '$')  
  return reg.test(str)
} 
function walkSync(currentDirPath, callback){
  fs.readdirSync(currentDirPath, { withFileTypes: true }).forEach(function(dirent) {
    var filePath = path.join(currentDirPath, dirent.name)
    if (dirent.isFile()) {
      callback(filePath, dirent)
    } else if (dirent.isDirectory()) {
      walkSync(filePath, callback)
    }
  });
}
walkSync('build', function(a, b) {
  if (_endWith(b.name, '.js') ||  _endWith(b.name, '.html') || _endWith(b.name, '.css')) {
    var data = fs.readFileSync(a).toString().split('\n')
    let _license =  _endWith(b.name, '.html') ? license_html : license
    if (b.name === 'service-worker.js') {
      _license = full_license
    }
    data.splice(0, 0, _license)
    var text = data.join("\n")
    fs.writeFileSync(a, text)
  }
})

