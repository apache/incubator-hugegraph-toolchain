import React, { useContext, useMemo, useEffect, useLayoutEffect } from 'react';
import { observer } from 'mobx-react';
import { useRoute, useLocation } from 'wouter';
import { isNull } from 'lodash-es';
import { useTranslation } from 'react-i18next';
import classnames from 'classnames';
import { Steps, Button } from '@baidu/one-ui';

import UploadEntry from './UploadEntry';
import { DataMapConfigs } from './datamap-configs';
import { ServerDataImport } from './server-data-import';
import ImportFinish from './ImportFinish';
import {
  ImportManagerStoreContext,
  GraphManagementStoreContext,
  DataImportRootStoreContext
} from '../../../../stores';

import PassIcon from '../../../../assets/imgs/ic_pass.svg';

import './ImportTasks.less';

const ImportTasks: React.FC = observer(() => {
  const graphManagementStore = useContext(GraphManagementStoreContext);
  const importManagerStore = useContext(ImportManagerStoreContext);
  const dataImportRootStore = useContext(DataImportRootStoreContext);
  const { dataMapStore, serverDataImportStore } = dataImportRootStore;
  const [, params] = useRoute(
    '/graph-management/:id/data-import/import-manager/:jobId/import-tasks/:status*'
  );
  const [, setLocation] = useLocation();
  const { t } = useTranslation();

  const steps = useMemo(
    () => [
      t('step.first'),
      t('step.second'),
      t('step.third'),
      t('step.fourth')
    ],
    []
  );

  const wrapperClassName = classnames({
    'import-tasks': true,
    'import-tasks-with-expand-sidebar': graphManagementStore.isExpanded
  });

  useEffect(() => {
    if (!isNull(params)) {
      switch (params.status) {
        case 'upload':
          dataImportRootStore.setCurrentStep(1);
          break;
        case 'mapping':
          dataImportRootStore.setCurrentStep(2);
          break;
        case 'loading':
          dataImportRootStore.setCurrentStep(3);
          break;
        case 'finish':
          dataImportRootStore.setCurrentStep(4);
          break;
      }
    }
  }, [params?.status]);

  useEffect(() => {
    window.scrollTo(0, 0);
    dataImportRootStore.setCurrentJobId(Number(params!.jobId));

    graphManagementStore.fetchIdList();
    dataImportRootStore.setCurrentId(Number(params!.id));
    dataImportRootStore.fetchVertexTypeList();
    dataImportRootStore.fetchEdgeTypeList();
    dataMapStore.fetchDataMaps();

    return () => {
      // no specific job here, solve the problem that click back button in browser
      // since <ImportManager /> relies on @selectedJob in useEffect()
      importManagerStore.setSelectedJob(null);
      dataImportRootStore.dispose();
      dataMapStore.dispose();
      serverDataImportStore.dispose();
    };
  }, []);

  return (
    <section className={wrapperClassName}>
      <div className="import-tasks-content-wrapper">
        <div style={{ padding: '16px 64px' }}>
          <Steps current={dataImportRootStore.currentStep}>
            {steps.map((title: string, index: number) => (
              <Steps.Step
                title={title}
                status={
                  dataImportRootStore.currentStep === index + 1
                    ? 'process'
                    : dataImportRootStore.currentStep > index + 1
                    ? 'finish'
                    : 'wait'
                }
                key={title}
              />
            ))}
          </Steps>
        </div>
        {dataImportRootStore.currentStep === 1 && <UploadEntry />}
        {dataImportRootStore.currentStep === 2 && <DataMapConfigs />}
        {dataImportRootStore.currentStep === 3 && <ServerDataImport />}
        {dataImportRootStore.currentStep === 4 && <ImportFinish />}
      </div>
    </section>
  );
});

export default ImportTasks;
