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
import { useRoute, useLocation, Params } from 'wouter';
import { Modal, Button } from 'hubble-ui';

import DataAnalyzeContent from './DataAnalyzeContent';
import DataAnalyzeInfoDrawer from './DataAnalyzeInfoDrawer';
import DynamicAddNode from './DynamicAddNode';
import DynamicAddEdge from './DynamicAddEdge';
import {
  AppStoreContext,
  GraphManagementStoreContext,
  DataAnalyzeStoreContext
} from '../../../stores';
import './DataAnalyze.less';
import { useTranslation } from 'react-i18next';

const DataAnalyze: React.FC = observer(() => {
  const { t } = useTranslation();
  const graphManagementStore = useContext(GraphManagementStoreContext);
  const appStore = useContext(AppStoreContext);
  const dataAnalyzeStore = useContext(DataAnalyzeStoreContext);
  const [match, params] = useRoute('/graph-management/:id/data-analyze');
  const [_, setLocation] = useLocation();

  useEffect(() => {
    window.scrollTo(0, 0);

    if (graphManagementStore.requestStatus.fetchIdList !== 'success') {
      graphManagementStore.fetchIdList();
    }

    return () => {
      dataAnalyzeStore.dispose();
    };
  }, [dataAnalyzeStore, graphManagementStore]);

  // Caution: Preitter will automatically add 'params' behind 'match' in array,
  // which is not equal each time
  /* eslint-disable */
  useEffect(() => {
    if (match && params !== null) {
      appStore.setCurrentId(Number(params.id));
      dataAnalyzeStore.setCurrentId(Number(params.id));
      dataAnalyzeStore.fetchValueTypes();
      dataAnalyzeStore.fetchVertexTypes();
      dataAnalyzeStore.fetchAllPropertyIndexes('vertex');
      dataAnalyzeStore.fetchEdgeTypes();
      dataAnalyzeStore.fetchAllNodeStyle();
      dataAnalyzeStore.fetchAllEdgeStyle();
    }
  }, [dataAnalyzeStore, match, params?.id]);

  return (
    <section className="data-analyze">
      <DataAnalyzeContent />
      <DataAnalyzeInfoDrawer />
      <DynamicAddNode />
      <DynamicAddEdge />
      <Modal
        title={t('addition.dataAnalyze.cannot-access')}
        footer={[
          <Button
            size="medium"
            type="primary"
            style={{ width: 88 }}
            onClick={() => {
              setLocation('/');
            }}
          >
            {t('addition.dataAnalyze.return-home')}
          </Button>
        ]}
        visible={graphManagementStore.graphData.some(
          ({ id, enabled }) => dataAnalyzeStore.currentId === id && !enabled
        )}
        destroyOnClose
        needCloseIcon={false}
      >
        <div style={{ color: '#333' }}>
          {dataAnalyzeStore.errorInfo.fetchExecutionLogs.message}
        </div>
      </Modal>
    </section>
  );
});

export default DataAnalyze;
