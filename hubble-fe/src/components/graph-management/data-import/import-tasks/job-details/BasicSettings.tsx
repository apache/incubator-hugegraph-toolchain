import React, { useState, useContext, useEffect } from 'react';
import { observer } from 'mobx-react';
import { isEmpty, isNull, isUndefined } from 'lodash-es';
import { useRoute } from 'wouter';
import { motion } from 'framer-motion';
import { useTranslation } from 'react-i18next';
import { Button, Modal, Input, Message } from '@baidu/one-ui';

import { ImportManagerStoreContext } from '../../../../../stores';
import {
  GraphManagementStoreContext,
  DataImportRootStoreContext
} from '../../../../../stores';

const BasicSettings: React.FC = observer(() => {
  const graphManagementStore = useContext(GraphManagementStoreContext);
  const importManagerStore = useContext(ImportManagerStoreContext);
  const dataImportRootStore = useContext(DataImportRootStoreContext);
  const { dataMapStore, serverDataImportStore } = dataImportRootStore;
  const [isPopEditModal, switchPopEditModal] = useState(false);
  const [, params] = useRoute(
    '/graph-management/:id/data-import/import-manager/:jobId/details'
  );
  const { t } = useTranslation();

  useEffect(() => {
    const init = async () => {
      if (isEmpty(importManagerStore.importJobList)) {
        graphManagementStore.fetchIdList();
        importManagerStore.setCurrentId(Number(params!.id));
        await importManagerStore.fetchImportJobList();
      }

      if (isNull(importManagerStore.selectedJob)) {
        if (
          !isUndefined(
            importManagerStore.importJobList.find(
              ({ id }) => String(id) === (params && params.jobId)
            )
          ) &&
          params !== null
        ) {
          importManagerStore.setSelectedJob(Number(params.jobId));

          // duplicate logic in <Importmanager />
          // fill in essential data in import-task stores
          dataImportRootStore.setCurrentId(Number(params!.id));
          dataImportRootStore.setCurrentJobId(Number(params.jobId));

          dataImportRootStore.fetchVertexTypeList();
          dataImportRootStore.fetchEdgeTypeList();

          // fetch related data
          await Promise.all([
            dataMapStore.fetchDataMaps(),
            serverDataImportStore.fetchAllImportTasks()
          ]);

          dataMapStore.setSelectedFileId(
            Number(dataMapStore.fileMapInfos[0].id)
          );
          dataMapStore.setSelectedFileInfo();

          // set flags about readonly and irregular process in <DataMap />
          dataMapStore.switchReadOnly(true);
          dataMapStore.switchIrregularProcess(true);

          // set flags about readonly and irregular process in <ServerDataImport />
          serverDataImportStore.switchExpandImportConfig(true);
          serverDataImportStore.switchReadOnly(true);
          serverDataImportStore.switchIrregularProcess(true);
          serverDataImportStore.syncImportConfigs(
            dataMapStore.selectedFileInfo!.load_parameter
          );
        }
      }
    };

    init();
  }, []);

  return (
    <div
      className="import-job-details-content-wrapper"
      style={{ display: 'flex', justifyContent: 'space-between' }}
    >
      <div className="import-job-details-basic-text">
        <div>
          <span>{t('import-job-details.basic.job-name')}</span>
          <span>{importManagerStore.selectedJob?.job_name}</span>
        </div>
        <div>
          <span>{t('import-job-details.basic.job-description')}</span>
          <span>{importManagerStore.selectedJob?.job_remarks}</span>
        </div>
      </div>
      <Button
        size="medium"
        style={{
          width: 88
        }}
        onClick={() => {
          importManagerStore.mutateEditJob(
            'name',
            importManagerStore.selectedJob!.job_name
          );
          importManagerStore.mutateEditJob(
            'description',
            importManagerStore.selectedJob!.job_remarks
          );
          switchPopEditModal(true);
        }}
      >
        {t('import-job-details.manipulations.edit')}
      </Button>
      <Modal
        title={t('import-job-details.basic.modal.edit-job.title')}
        visible={isPopEditModal}
        width={499}
        footer={[
          <Button
            size="medium"
            type="primary"
            style={{ width: 60 }}
            disabled={
              isEmpty(importManagerStore.editJob?.name) ||
              !isEmpty(importManagerStore.validateEditJobErrorMessage.name) ||
              !isEmpty(
                importManagerStore.validateEditJobErrorMessage.description
              )
            }
            onClick={async () => {
              switchPopEditModal(false);
              await importManagerStore.updateJobInfo();

              if (importManagerStore.requestStatus.updateJobInfo === 'failed') {
                Message.error({
                  content: importManagerStore.errorInfo.updateJobInfo.message,
                  size: 'medium',
                  showCloseIcon: false
                });

                return;
              }

              if (
                importManagerStore.requestStatus.updateJobInfo === 'success'
              ) {
                Message.success({
                  content: t('import-manager.hint.update-succeed'),
                  size: 'medium',
                  showCloseIcon: false
                });
              }
            }}
          >
            {t('import-job-details.basic.modal.manipulations.save')}
          </Button>,
          <Button
            size="medium"
            style={{ width: 60 }}
            onClick={() => {
              switchPopEditModal(false);
            }}
          >
            {t('import-job-details.basic.modal.manipulations.cancel')}
          </Button>
        ]}
        destroyOnClose
        needCloseIcon
        onCancel={() => {
          switchPopEditModal(false);
        }}
      >
        <div>
          <div className="import-manager-create-job-option">
            <div>
              <span className="import-manager-create-job-option-required-mark">
                *
              </span>
              <span>
                {t('import-job-details.basic.modal.edit-job.job-name')}
              </span>
            </div>
            <Input
              size="medium"
              width={349}
              maxLen={48}
              countMode="en"
              placeholder={t('import-manager.placeholder.input-valid-job-name')}
              errorLocation="layer"
              errorMessage={importManagerStore.validateEditJobErrorMessage.name}
              value={importManagerStore.editJob!.name}
              onChange={(e: any) => {
                importManagerStore.mutateEditJob('name', e.value);
                importManagerStore.validateJob('edit', 'name');
              }}
              originInputProps={{
                onBlur: () => {
                  importManagerStore.validateJob('edit', 'name');
                }
              }}
            />
          </div>
          <div className="import-manager-create-job-option">
            <div>
              {t('import-job-details.basic.modal.edit-job.job-description')}
            </div>
            <div>
              <Input
                size="medium"
                width={349}
                maxLen={200}
                countMode="en"
                placeholder={t(
                  'import-manager.placeholder.input-job-description'
                )}
                errorLocation="layer"
                errorMessage={
                  importManagerStore.validateEditJobErrorMessage.description
                }
                value={importManagerStore.editJob!.description}
                onChange={(e: any) => {
                  importManagerStore.mutateEditJob('description', e.value);
                  importManagerStore.validateJob('edit', 'description');
                }}
                originInputProps={{
                  onBlur: () => {
                    importManagerStore.validateJob('edit', 'description');
                  }
                }}
              />
            </div>
          </div>
        </div>
      </Modal>
    </div>
  );
});

export default BasicSettings;
