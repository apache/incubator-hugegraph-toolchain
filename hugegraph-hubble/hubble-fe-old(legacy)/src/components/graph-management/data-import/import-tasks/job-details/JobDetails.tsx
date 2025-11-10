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
import { AnimatePresence } from 'framer-motion';
import { useTranslation } from 'react-i18next';
import { isEmpty } from 'lodash-es';
import { Menu } from 'hubble-ui';

import {
  DataImportRootStoreContext,
  ImportManagerStoreContext
} from '../../../../../stores';

import BasicSettings from './BasicSettings';
import UploadedFiles from './UploadedFiles';
import DataMaps from './DataMaps';
import DataImportDetails from './DataImportDetails';

import './JobDetails.less';

const JobDetails: React.FC = observer(() => {
  const importManagerStore = useContext(ImportManagerStoreContext);
  const { dataMapStore, serverDataImportStore } = useContext(
    DataImportRootStoreContext
  );
  const { t } = useTranslation();

  const handleMenuItemChange = ({ key }: { key: string }) => {
    importManagerStore.setCurrentJobDetailStep(key);
  };

  const renderListView = () => {
    switch (importManagerStore.jobDetailsStep) {
      case 'basic':
        return <BasicSettings />;
      case 'upload':
        return <UploadedFiles />;
      case 'data-map':
        return <DataMaps />;
      case 'import-details':
        return <DataImportDetails />;
    }
  };

  return (
    <>
      <Menu
        mode="horizontal"
        menuLevel={2}
        selectedKeys={[importManagerStore.jobDetailsStep]}
        onClick={handleMenuItemChange}
      >
        <Menu.Item key="basic">
          {t('import-job-details.tabs.basic-settings')}
        </Menu.Item>
        <Menu.Item key="upload" disabled={isEmpty(dataMapStore.fileMapInfos)}>
          {t('import-job-details.tabs.uploaded-files')}
        </Menu.Item>
        <Menu.Item key="data-map" disabled={isEmpty(dataMapStore.fileMapInfos)}>
          {t('import-job-details.tabs.data-maps')}
        </Menu.Item>
        <Menu.Item
          key="import-details"
          disabled={isEmpty(serverDataImportStore.importTasks)}
        >
          {t('import-job-details.tabs.import-details')}
        </Menu.Item>
      </Menu>
      <AnimatePresence exitBeforeEnter>{renderListView()}</AnimatePresence>
    </>
  );
});

export default JobDetails;
