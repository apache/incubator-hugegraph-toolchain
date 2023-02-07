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

import React, { useContext, useEffect } from 'react';
import { observer } from 'mobx-react';

import { GraphManagementStoreContext } from '../../stores';
import GraphManagementLimitHint from './GraphManagementLimitHint';
import GraphManagementHeader from './GraphManagementHeader';
import NewGraphConfig from './NewGraphConfig';
import GraphManagementList from './GraphManagementList';
import GraphManagementEmptyList from './GraphManagementEmptyList';

import './GraphManagement.less';

const GraphManagement: React.FC = observer(() => {
  const graphManagementStore = useContext(GraphManagementStoreContext);

  useEffect(() => {
    graphManagementStore.fetchLicenseInfo();
    graphManagementStore.fetchGraphDataList();

    return () => {
      graphManagementStore.dispose();
    };
  }, [graphManagementStore]);

  return (
    <section className="graph-management">
      <GraphManagementLimitHint />
      <GraphManagementHeader />
      <NewGraphConfig />
      <GraphManagementEmptyList />
      <GraphManagementList />
    </section>
  );
});

export default GraphManagement;
