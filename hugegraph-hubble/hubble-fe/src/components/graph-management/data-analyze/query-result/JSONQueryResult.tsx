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

import React, { useContext } from 'react';
import { observer } from 'mobx-react';
import ReactJsonView from 'react-json-view';
import 'codemirror/mode/javascript/javascript';

import { DataAnalyzeStoreContext } from '../../../../stores';

const JSONQueryResult: React.FC = observer(() => {
  const dataAnalyzeStore = useContext(DataAnalyzeStoreContext);

  return (
    <div style={{ position: 'absolute', top: 0, left: 0, padding: 8 }}>
      <ReactJsonView
        src={dataAnalyzeStore.originalGraphData.data.json_view.data}
        name={false}
        displayObjectSize={false}
        displayDataTypes={false}
        groupArraysAfterLength={50}
      />
    </div>
  );
});

export default JSONQueryResult;
