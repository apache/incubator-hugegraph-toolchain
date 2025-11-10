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
import { useRoute, useLocation } from 'wouter';
import { isEmpty } from 'lodash-es';
import { useTranslation } from 'react-i18next';
import { Button } from 'hubble-ui';

import {
  ImportManagerStoreContext,
  GraphManagementStoreContext,
  DataImportRootStoreContext
} from '../../../../stores';

import { useInitDataImport } from '../../../../hooks';

import PassIcon from '../../../../assets/imgs/ic_pass.svg';

const ImportFinish: React.FC = observer(() => {
  const importManagerStore = useContext(ImportManagerStoreContext);
  const dataImportRootStore = useContext(DataImportRootStoreContext);
  const { dataMapStore, serverDataImportStore } = dataImportRootStore;
  const [, params] = useRoute(
    '/graph-management/:id/data-import/import-manager/:jobId/import-tasks/:status*'
  );
  const [, setLocation] = useLocation();
  const { t } = useTranslation();

  useEffect(() => {
    if (isEmpty(serverDataImportStore.importTasks) && params !== null) {
      dataImportRootStore.setCurrentId(Number(params.id));
      dataImportRootStore.setCurrentJobId(Number(params.jobId));

      serverDataImportStore.fetchAllImportTasks();
    }
  }, [params?.id, params?.jobId]);

  return (
    <div className="import-tasks-complete-hint">
      <div className="import-tasks-complete-hint-description">
        <img src={PassIcon} alt="complete" />
        <div>
          <div>{t('data-import-status.finished')}</div>
          <div>
            {t('data-import-status.success', {
              number:
                serverDataImportStore.successImportFileStatusNumber !== 0
                  ? serverDataImportStore.successImportFileStatusNumber
                  : '-'
            })}
            {serverDataImportStore.pausedImportFileNumber !== 0 &&
              `，${t('data-import-status.pause', {
                number: serverDataImportStore.pausedImportFileNumber
              })}`}
            {serverDataImportStore.abortImportFileNumber !== 0 &&
              `，${t('data-import-status.abort', {
                number: serverDataImportStore.abortImportFileNumber
              })}`}
          </div>
        </div>
      </div>
      <div className="import-tasks-complete-hint-manipulations">
        <Button
          type="primary"
          size="large"
          onClick={() => {
            dataImportRootStore.resetAllFileInfos();
            dataMapStore.dispose();
            serverDataImportStore.dispose();
            dataImportRootStore.dispose();

            importManagerStore.setSelectedJob(null);
            setLocation(
              `/graph-management/${params!.id}/data-import/import-manager`
            );
            importManagerStore.fetchImportJobList();
          }}
        >
          {t('data-import-status.move-to-import-manager')}
        </Button>
      </div>
    </div>
  );
});

export default ImportFinish;
