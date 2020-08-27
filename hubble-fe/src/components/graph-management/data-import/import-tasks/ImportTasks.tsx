import React, { useState, useContext, useMemo, useEffect } from 'react';
import { observer } from 'mobx-react';
import { useRoute, useLocation } from 'wouter';
import { size } from 'lodash-es';
import { useTranslation } from 'react-i18next';
import classnames from 'classnames';
import { Breadcrumb, Steps, Button } from '@baidu/one-ui';

import UploadEntry from './UploadEntry';
import { DataMapConfigs } from './datamap-configs';
import { ServerDataImport } from './server-data-import';
import {
  GraphManagementStoreContext,
  DataImportRootStoreContext
} from '../../../../stores';
import PassIcon from '../../../../assets/imgs/ic_pass.svg';

import './ImportTasks.less';

const ImportTasks: React.FC = observer(() => {
  const graphManagementStore = useContext(GraphManagementStoreContext);
  const dataImportRootStore = useContext(DataImportRootStoreContext);
  const { dataMapStore, serverDataImportStore } = dataImportRootStore;
  const [, params] = useRoute(
    '/graph-management/:id/data-import/:jobId/import-tasks'
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
    window.scrollTo(0, 0);
    dataImportRootStore.setCurrentJobId(Number(params!.jobId));

    graphManagementStore.fetchIdList();
    dataImportRootStore.setCurrentId(Number(params!.id));
    dataImportRootStore.fetchVertexTypeList();
    dataImportRootStore.fetchEdgeTypeList();
    dataMapStore.fetchDataMaps();

    return () => {
      dataImportRootStore.dispose();
      dataMapStore.dispose();
      serverDataImportStore.dispose();
    };
  }, []);

  return (
    <section className={wrapperClassName}>
      <div className="import-tasks-breadcrumb-wrapper">
        <Breadcrumb size="small">
          <Breadcrumb.Item
            onClick={() => {
              setLocation(
                `/graph-management/${params!.id}/data-import/import-manager`
              );
            }}
          >
            {t('breadcrumb.first')}
          </Breadcrumb.Item>
          <Breadcrumb.Item>
            {dataImportRootStore.currentJobName}
          </Breadcrumb.Item>
        </Breadcrumb>
      </div>
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
        {dataImportRootStore.currentStep === 4 && (
          <div className="import-tasks-complete-hint">
            <div className="import-tasks-complete-hint-description">
              <img src={PassIcon} alt="complete" />
              <div>
                <div>{t('data-import-status.finished')}</div>
                <div>
                  {t('data-import-status.success', {
                    number: serverDataImportStore.successImportFileStatusNumber
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

                  setLocation(
                    `/graph-management/${params!.id}/data-import/import-manager`
                  );
                }}
              >
                {t('data-import-status.move-to-import-manager')}
              </Button>
            </div>
          </div>
        )}
      </div>
    </section>
  );
});

export default ImportTasks;
