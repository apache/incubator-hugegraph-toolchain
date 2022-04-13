import React, { useState, useContext, useEffect } from 'react';
import { observer } from 'mobx-react';
import { useRoute, useLocation } from 'wouter';
import { useTranslation } from 'react-i18next';
import { isEmpty, size } from 'lodash-es';
import classnames from 'classnames';
import { Button, Input, Table, Modal, Message } from 'hubble-ui';

import LoadingDataView from '../../../common/LoadingDataView';
import { Tooltip as CustomTooltip } from '../../../common';
import {
  DataImportRootStoreContext,
  ImportManagerStoreContext
} from '../../../../stores';

import AddIcon from '../../../../assets/imgs/ic_add.svg';
import HintIcon from '../../../../assets/imgs/ic_question_mark.svg';

const styles = {
  button: {
    width: 78,
    marginLeft: 12
  }
};

const ImportTaskList: React.FC = observer(() => {
  const dataImportRootStore = useContext(DataImportRootStoreContext);
  const importManagerStore = useContext(ImportManagerStoreContext);
  const { dataMapStore, serverDataImportStore } = dataImportRootStore;
  const [preLoading, switchPreLoading] = useState(true);
  const [isPopCreateModal, switchCreatePopModal] = useState(false);
  const [, params] = useRoute(
    '/graph-management/:id/data-import/import-manager'
  );
  const [, setLocation] = useLocation();
  const { t } = useTranslation();

  const isLoading =
    preLoading ||
    importManagerStore.requestStatus.fetchImportJobList === 'pending';

  const handleSearchChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    importManagerStore.mutateSearchWords(e.target.value);
  };

  const handleSearch = async () => {
    importManagerStore.mutateImportJobListPageNumber(1);
    importManagerStore.switchSearchedStatus(true);
    await importManagerStore.fetchImportJobList();
  };

  const handleClearSearch = () => {
    importManagerStore.mutateSearchWords('');
    importManagerStore.mutateImportJobListPageNumber(1);
    importManagerStore.switchSearchedStatus(false);
    importManagerStore.fetchImportJobList();
  };

  const handlePageChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    importManagerStore.mutateImportJobListPageNumber(Number(e.target.value));
    importManagerStore.fetchImportJobList();
  };

  const columnConfigs = [
    {
      title: t('import-manager.list-column-title.job-name'),
      dataIndex: 'job_name',
      width: '20%',
      render(name: string, rowData: any) {
        const readyToJump =
          rowData.job_status === 'SUCCESS' || rowData.job_status === 'FAILED';

        const wrapperClassName = classnames({
          'no-line-break': true,
          link: readyToJump
        });

        return (
          <div className="import-manager-table-job-name">
            <div
              className={wrapperClassName}
              title={name}
              onClick={async () => {
                if (readyToJump) {
                  importManagerStore.setCurrentJobDetailStep('basic');
                  importManagerStore.setSelectedJob(rowData.id);

                  // fill in essential data in import-task stores
                  dataImportRootStore.setCurrentId(Number(params!.id));
                  dataImportRootStore.setCurrentJobId(rowData.id);

                  dataImportRootStore.fetchVertexTypeList();
                  dataImportRootStore.fetchEdgeTypeList();

                  setLocation(
                    `/graph-management/${
                      params!.id
                    }/data-import/import-manager/${rowData.id}/details`
                  );

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
              }}
            >
              {name}
            </div>
            {!isEmpty(rowData.job_remarks) && (
              <CustomTooltip
                trigger="hover"
                placement="bottom-start"
                modifiers={{
                  offset: {
                    offset: '0, 3'
                  }
                }}
                tooltipWrapperProps={{
                  className: 'tooltips-dark',
                  style: {
                    zIndex: 7
                  }
                }}
                tooltipWrapper={rowData.job_remarks}
                childrenWrapperElement="img"
                childrenProps={{
                  src: HintIcon,
                  alt: 'hint'
                }}
              />
            )}
          </div>
        );
      }
    },
    {
      title: t('import-manager.list-column-title.size'),
      dataIndex: 'job_size',
      width: '15%',
      render(text: any) {
        return <div className="no-line-break">{text}</div>;
      }
    },
    {
      title: t('import-manager.list-column-title.create-time'),
      dataIndex: 'create_time',
      render(text: string) {
        return <div className="no-line-break">{text}</div>;
      }
    },
    {
      title: t('import-manager.list-column-title.status'),
      dataIndex: 'job_status',
      render(text: string) {
        let specificClassName = '';

        switch (text) {
          case 'DEFAULT':
          case 'UPLOADING':
            specificClassName = 'import-manager-table-status-pending';
            break;
          case 'MAPPING':
          case 'SETTING':
          case 'LOADING':
            specificClassName = 'import-manager-table-status-process';
            break;
          case 'FAILED':
            specificClassName = 'import-manager-table-status-failed';
            break;
          case 'SUCCESS':
            specificClassName = 'import-manager-table-status-success';
            break;
        }

        return (
          <div
            className={
              'import-manager-table-status-wrapper ' + specificClassName
            }
          >
            {t(`import-manager.list-column-status.${text}`)}
          </div>
        );
      }
    },
    {
      title: t('import-manager.list-column-title.time-consuming'),
      dataIndex: 'job_duration',
      render(text: string) {
        return <div className="no-line-break">{text}</div>;
      }
    },
    {
      title: t('import-manager.list-column-title.manipulation'),
      render(_: any, rowData: any) {
        return (
          <div className="no-line-break">
            <ImportManagerManipulation
              jobId={rowData.id}
              jobName={rowData.job_name}
              status={rowData.job_status}
            />
          </div>
        );
      }
    }
  ];

  useEffect(() => {
    if (importManagerStore.currentId !== null) {
      setTimeout(() => {
        switchPreLoading(false);
      }, 800);

      importManagerStore.fetchImportJobList();
    }
  }, [importManagerStore.currentId]);

  return (
    <div className="import-manager-content-wrapper">
      <div className="import-manager-content-header">
        <Input.Search
          size="medium"
          width={200}
          placeholder={t('import-manager.placeholder.input-job-name')}
          value={importManagerStore.searchWords}
          onChange={handleSearchChange}
          onSearch={handleSearch}
          onClearClick={handleClearSearch}
          isShowDropDown={false}
          disabled={isLoading}
        />
        <Button
          type="primary"
          size="medium"
          style={styles.button}
          disabled={isLoading || size(importManagerStore.importJobList) >= 500}
          onClick={() => {
            switchCreatePopModal(true);
          }}
        >
          {t('import-manager.manipulation.create')}
        </Button>
      </div>
      <div>
        <Table
          columns={columnConfigs}
          locale={{
            emptyText: (
              <LoadingDataView
                isLoading={isLoading}
                emptyView={
                  importManagerStore.isSearched.status ? (
                    <span>{t('import-manager.hint.no-result')}</span>
                  ) : (
                    <EmptyImportHints />
                  )
                }
              />
            )
          }}
          dataSource={isLoading ? [] : importManagerStore.importJobList}
          pagination={
            isLoading
              ? null
              : {
                  hideOnSinglePage: false,
                  pageNo: importManagerStore.importJobListPageConfig.pageNumber,
                  pageSize: 10,
                  showSizeChange: false,
                  showPageJumper: false,
                  total: importManagerStore.importJobListPageConfig.pageTotal,
                  onPageNoChange: handlePageChange
                }
          }
        />
      </div>
      <Modal
        title={t('import-manager.modal.create-job.title')}
        visible={isPopCreateModal}
        width={499}
        footer={[
          <Button
            size="medium"
            type="primary"
            style={{ width: 60 }}
            disabled={
              isEmpty(importManagerStore.newJob.name) ||
              !isEmpty(importManagerStore.validateNewJobErrorMessage.name) ||
              !isEmpty(
                importManagerStore.validateNewJobErrorMessage.description
              )
            }
            onClick={async () => {
              switchCreatePopModal(false);
              await importManagerStore.createNewJob();
              importManagerStore.resetJob('new');

              if (importManagerStore.requestStatus.createNewJob === 'success') {
                Message.success({
                  content: t('import-manager.hint.creation-succeed'),
                  size: 'medium',
                  showCloseIcon: false
                });

                importManagerStore.fetchImportJobList();
                return;
              }

              if (importManagerStore.requestStatus.createNewJob === 'failed') {
                Message.error({
                  content: importManagerStore.errorInfo.createNewJob.message,
                  size: 'medium',
                  showCloseIcon: false
                });
              }
            }}
          >
            {t('import-manager.modal.manipulations.create')}
          </Button>,
          <Button
            size="medium"
            style={{ width: 60 }}
            onClick={() => {
              switchCreatePopModal(false);
              importManagerStore.resetJob('new');
            }}
          >
            {t('import-manager.modal.manipulations.cancel')}
          </Button>
        ]}
        destroyOnClose
        needCloseIcon={true}
        onCancel={() => {
          switchCreatePopModal(false);
          importManagerStore.resetJob('new');
        }}
      >
        <div>
          <div className="import-manager-create-job-option">
            <div>
              <span className="import-manager-create-job-option-required-mark">
                *
              </span>
              <span>{t('import-manager.modal.create-job.job-name')}</span>
            </div>
            <Input
              size="medium"
              width={349}
              maxLen={48}
              countMode="en"
              placeholder={t('import-manager.placeholder.input-valid-job-name')}
              errorLocation="layer"
              errorMessage={importManagerStore.validateNewJobErrorMessage.name}
              value={importManagerStore.newJob.name}
              onChange={(e: any) => {
                importManagerStore.mutateNewJob('name', e.value);
                importManagerStore.validateJob('new', 'name');
              }}
              originInputProps={{
                onBlur: () => {
                  importManagerStore.validateJob('new', 'name');
                }
              }}
            />
          </div>
          <div className="import-manager-create-job-option">
            <div>{t('import-manager.modal.create-job.job-description')}</div>
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
                  importManagerStore.validateNewJobErrorMessage.description
                }
                value={importManagerStore.newJob.description}
                onChange={(e: any) => {
                  importManagerStore.mutateNewJob('description', e.value);
                  importManagerStore.validateJob('new', 'description');
                }}
                originInputProps={{
                  onBlur: () => {
                    importManagerStore.validateJob('new', 'description');
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

export interface ImportManagerManipulationProps {
  jobId: number;
  jobName: string;
  status: string;
}

export const ImportManagerManipulation: React.FC<ImportManagerManipulationProps> = observer(
  ({ jobId, jobName, status }) => {
    const importManagerStore = useContext(ImportManagerStoreContext);
    const dataImportRootStore = useContext(DataImportRootStoreContext);
    const { dataMapStore, serverDataImportStore } = dataImportRootStore;
    const [isPopDeleteModal, switchPopDeleteModal] = useState(false);
    const [, params] = useRoute(
      '/graph-management/:id/data-import/import-manager'
    );
    const [, setLocation] = useLocation();
    const { t } = useTranslation();

    const jumpToLoaction = (step: number, jobName: string) => async () => {
      importManagerStore.setSelectedJob(jobId);

      dataImportRootStore.setCurrentId(Number(params!.id));
      dataImportRootStore.setCurrentJobId(jobId);
      dataImportRootStore.setCurrentStatus(status);

      let route = '';

      if (step === 1) {
        await dataMapStore.fetchDataMaps();
        route = 'upload';
      }

      if (step === 2) {
        // users may browse from <JobDetails />
        dataMapStore.switchReadOnly(false);

        await dataMapStore.fetchDataMaps();
        dataMapStore.setSelectedFileId(dataMapStore.fileMapInfos[0].id);
        dataMapStore.setSelectedFileInfo();
        dataMapStore.switchIrregularProcess(true);
        serverDataImportStore.syncImportConfigs(
          dataMapStore.selectedFileInfo!.load_parameter
        );

        route = 'mapping';
      }

      if (step === 3) {
        // users may browse from <JobDetails />
        serverDataImportStore.switchReadOnly(false);

        await dataMapStore.fetchDataMaps();

        // need to set default selected file
        dataMapStore.setSelectedFileId(dataMapStore.fileMapInfos[0].id);
        dataMapStore.setSelectedFileInfo();
        dataMapStore.switchIrregularProcess(true);

        serverDataImportStore.syncImportConfigs(
          dataMapStore.selectedFileInfo!.load_parameter
        );
        serverDataImportStore.switchIrregularProcess(true);

        if (status === 'SETTING') {
          // user may browse from <JobDeatils /> to <ImportTask />
          dataMapStore.switchReadOnly(false);

          serverDataImportStore.resetImportTasks();
          serverDataImportStore.switchFetchImportStatus('standby');
          serverDataImportStore.switchImportFinished(false);
        }

        if (status === 'LOADING') {
          // reveal previous & next button in <DataMap />
          // users may browse from <JobDetails /> which set @readonly true
          dataMapStore.switchReadOnly(false);
          dataMapStore.switchLock(true);
          serverDataImportStore.switchImportConfigReadOnly(true);
          // users may browse from <JobDetails />, let store fetches
          // for one time and decide whether import is finished
          serverDataImportStore.resetImportTasks();
          serverDataImportStore.switchImportFinished(false);
        }

        route = 'loading';
      }

      dataImportRootStore.setCurrentStep(step);

      setLocation(
        `/graph-management/${
          params!.id
        }/data-import/import-manager/${jobId}/import-tasks/${route}`
      );
    };

    return (
      <div className="import-manager-table-manipulations">
        {(status === 'DEFAULT' || status === 'UPLOADING') && (
          <span onClick={jumpToLoaction(1, jobName)}>
            {t('import-manager.list-column-manipulations.start')}
          </span>
        )}
        {status === 'MAPPING' && (
          <span onClick={jumpToLoaction(2, jobName)}>
            {t('import-manager.list-column-manipulations.resume-setting')}
          </span>
        )}
        {(status === 'SETTING' || status === 'LOADING') && (
          <span onClick={jumpToLoaction(3, jobName)}>
            {t('import-manager.list-column-manipulations.resume-importing')}
          </span>
        )}
        {status === 'FAILED' && (
          // <span
          //   onClick={() => {
          //     setLocation(
          //       `/graph-management/${
          //         params!.id
          //       }/data-import/job-error-log/${jobId}`
          //     );
          //   }}
          // >
          //   {t('import-manager.list-column-manipulations.check-error-log')}
          // </span>
          <a
            target="_blank"
            className="import-manager-table-manipulations-outlink"
            href={`/graph-management/${
              params!.id
            }/data-import/job-error-log/${jobId}`}
          >
            {t('import-manager.list-column-manipulations.check-error-log')}
          </a>
        )}
        <span
          onClick={() => {
            switchPopDeleteModal(true);
          }}
        >
          {t('import-manager.list-column-manipulations.delete')}
        </span>
        <Modal
          title={t('import-manager.modal.delete-job.title')}
          width={400}
          visible={isPopDeleteModal}
          footer={[
            <Button
              size="medium"
              type="primary"
              style={{ width: 60 }}
              onClick={async () => {
                switchPopDeleteModal(false);
                await importManagerStore.deleteJob(jobId);
                importManagerStore.fetchImportJobList();
              }}
              key="delete"
            >
              {t('import-manager.modal.manipulations.delete')}
            </Button>,
            <Button
              size="medium"
              style={{ width: 60 }}
              onClick={() => {
                switchPopDeleteModal(false);
              }}
              key="cancel"
            >
              {t('import-manager.modal.manipulations.cancel')}
            </Button>
          ]}
          destroyOnClose
          needCloseIcon={true}
          onCancel={() => {
            switchPopDeleteModal(false);
          }}
        >
          <div className="import-manager-delete-job-option">
            <span>
              {t('import-manager.modal.delete-job.hint', {
                name: jobName
              })}
            </span>
            <span>{t('import-manager.modal.delete-job.sub-hint')}</span>
          </div>
        </Modal>
      </div>
    );
  }
);

export const EmptyImportHints: React.FC = observer(() => {
  const importManagerStore = useContext(ImportManagerStoreContext);
  const [isPopCreateModal, switchCreatePopModal] = useState(false);
  const { t } = useTranslation();

  return (
    <div className="import-manager-empty-list">
      <img src={AddIcon} alt={t('import-manager.manipulation.create')} />
      <div>{t('import-manager.hint.empty-task')}</div>
      <Button
        type="primary"
        size="xlarge"
        style={{
          width: 112
        }}
        onClick={() => {
          switchCreatePopModal(true);
        }}
      >
        {t('import-manager.manipulation.create')}
      </Button>
      <Modal
        title={t('import-manager.modal.create-job.title')}
        visible={isPopCreateModal}
        width={499}
        footer={[
          <Button
            size="medium"
            type="primary"
            style={{ width: 60 }}
            disabled={
              isEmpty(importManagerStore.newJob.name) ||
              !isEmpty(importManagerStore.validateNewJobErrorMessage.name) ||
              !isEmpty(
                importManagerStore.validateNewJobErrorMessage.description
              )
            }
            onClick={async () => {
              switchCreatePopModal(false);
              await importManagerStore.createNewJob();
              importManagerStore.resetJob('new');

              if (importManagerStore.requestStatus.createNewJob === 'success') {
                Message.success({
                  content: t('import-manager.hint.creation-succeed'),
                  size: 'medium',
                  showCloseIcon: false
                });

                importManagerStore.fetchImportJobList();
                return;
              }

              if (importManagerStore.requestStatus.createNewJob === 'failed') {
                Message.error({
                  content: importManagerStore.errorInfo.createNewJob.message,
                  size: 'medium',
                  showCloseIcon: false
                });
              }
            }}
          >
            {t('import-manager.modal.manipulations.create')}
          </Button>,
          <Button
            size="medium"
            style={{ width: 60 }}
            onClick={() => {
              switchCreatePopModal(false);
              importManagerStore.resetJob('new');
            }}
          >
            {t('import-manager.modal.manipulations.cancel')}
          </Button>
        ]}
        destroyOnClose
        needCloseIcon={true}
        onCancel={() => {
          switchCreatePopModal(false);
          importManagerStore.resetJob('new');
        }}
      >
        <div>
          <div className="import-manager-create-job-option">
            <div>
              <span className="import-manager-create-job-option-required-mark">
                *
              </span>
              <span>{t('import-manager.modal.create-job.job-name')}</span>
            </div>
            <Input
              size="medium"
              width={349}
              maxLen={48}
              countMode="en"
              placeholder={t('import-manager.placeholder.input-valid-job-name')}
              errorLocation="layer"
              errorMessage={importManagerStore.validateNewJobErrorMessage.name}
              value={importManagerStore.newJob.name}
              onChange={(e: any) => {
                importManagerStore.mutateNewJob('name', e.value);
                importManagerStore.validateJob('new', 'name');
              }}
              onBlur={() => {
                importManagerStore.validateJob('new', 'name');
              }}
            />
          </div>
          <div className="import-manager-create-job-option">
            <div>{t('import-manager.modal.create-job.job-description')}</div>
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
                  importManagerStore.validateNewJobErrorMessage.description
                }
                value={importManagerStore.newJob.description}
                onChange={(e: any) => {
                  importManagerStore.mutateNewJob('description', e.value);
                  importManagerStore.validateJob('new', 'description');
                }}
                onBlur={() => {
                  importManagerStore.validateJob('new', 'description');
                }}
              />
            </div>
          </div>
        </div>
      </Modal>
    </div>
  );
});

export default ImportTaskList;
