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
import { isEmpty } from 'lodash-es';
import { Menu } from 'hubble-ui';

import {
  ImportManagerStoreContext,
  DataImportRootStoreContext
} from '../../../../../stores';
import FileConfigs from './FileConfigs';
import TypeConfigs from './TypeConfigs';
import { useInitDataImport } from '../../../../../hooks';

import './DataMapConfigs.less';

export interface DataMapConfigsProps {
  height?: string;
}

const DataMapConfigs: React.FC<DataMapConfigsProps> = observer(({ height }) => {
  const dataImportRootStore = useContext(DataImportRootStoreContext);
  const { dataMapStore, serverDataImportStore } = dataImportRootStore;
  const isInitReady = useInitDataImport();
  const realHeight = height ? height : 'calc(100vh - 194px)';

  return isInitReady ? (
    <div className="import-tasks-step-wrapper" style={{ height: realHeight }}>
      <Menu
        mode="inline"
        needBorder={true}
        style={{ width: 200, height: realHeight }}
        selectedKeys={[String(dataMapStore.selectedFileId)]}
        onClick={(e: any) => {
          // reset state from the previous file
          dataMapStore.resetDataMaps();

          // if data import starts, do not expand collpase
          if (!serverDataImportStore.isServerStartImport) {
            dataMapStore.switchExpand('file', true);
          }

          dataMapStore.setSelectedFileId(Number(e.key));
          dataMapStore.setSelectedFileInfo();
          serverDataImportStore.switchImporting(false);
        }}
      >
        {dataMapStore.fileMapInfos
          .filter(({ file_status }) => file_status === 'COMPLETED')
          .map(({ id, name }) => (
            <Menu.Item key={id}>
              <span>{name}</span>
            </Menu.Item>
          ))}
      </Menu>
      <div className="import-tasks-data-map-configs">
        <FileConfigs />
        <TypeConfigs />
      </div>
    </div>
  ) : null;
});

export default DataMapConfigs;
