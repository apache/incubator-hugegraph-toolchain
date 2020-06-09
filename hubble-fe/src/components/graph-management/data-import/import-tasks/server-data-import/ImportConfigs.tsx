import React, { useContext, useCallback } from 'react';
import { observer } from 'mobx-react';
import { isEmpty, size } from 'lodash-es';
import { useTranslation } from 'react-i18next';
import classnames from 'classnames';
import { Switch, Input, Button, Table, Tooltip, Message } from '@baidu/one-ui';

import { DataImportRootStoreContext } from '../../../../../stores';

import ArrowIcon from '../../../../../assets/imgs/ic_arrow_16.svg';
import HintIcon from '../../../../../assets/imgs/ic_question_mark.svg';
import {
  ImportTasks,
  LoadParameter
} from '../../../../../stores/types/GraphManagementStore/dataImportStore';

const importStatusColorMapping: Record<string, string> = {
  RUNNING: '#2b65ff',
  SUCCEED: '#39bf45',
  FAILED: '#e64552',
  PAUSED: '#a9cbfb',
  STOPPED: '#ccc'
};

const commonInputProps = {
  size: 'medium',
  width: 100,
  errorLocation: 'layer'
};

const ImportConfigs: React.FC = observer(() => {
  const dataImportRootStore = useContext(DataImportRootStoreContext);
  const { serverDataImportStore, dataMapStore } = dataImportRootStore;
  const { t } = useTranslation();

  const columnConfigs = [
    {
      title: t('server-data-import.import-details.column-titles.file-name'),
      dataIndex: 'file_name',
      render(text: string) {
        return <div className="no-line-break">{text}</div>;
      }
    },
    {
      title: t('server-data-import.import-details.column-titles.type'),
      dataIndex: 'type',
      width: '15%',
      render(_: any, rowData: ImportTasks) {
        const title = (
          <div className="import-tasks-server-data-import-table-tooltip-title">
            {!isEmpty(rowData.vertices) && (
              <div>
                {t('server-data-import.import-details.content.vertex')}：
                {rowData.vertices.join(' ')}
              </div>
            )}
            {!isEmpty(rowData.edges) && (
              <div>
                {t('server-data-import.import-details.content.edge')}：
                {rowData.edges.join(' ')}
              </div>
            )}
            {isEmpty(rowData.vertices) && isEmpty(rowData.edges) && '-'}
          </div>
        );

        return (
          <Tooltip placement="bottomLeft" title={title} type="dark">
            <div className="no-line-break">
              {!isEmpty(rowData.vertices) &&
                `${t(
                  'server-data-import.import-details.content.vertex'
                )}：${rowData.vertices.join(' ')} `}
              {!isEmpty(rowData.edges) &&
                `${t(
                  'server-data-import.import-details.content.edge'
                )}：${rowData.edges.join(' ')}`}
              {isEmpty(rowData.vertices) && isEmpty(rowData.edges) && '-'}
            </div>
          </Tooltip>
        );
      }
    },
    {
      title: t('server-data-import.import-details.column-titles.import-speed'),
      dataIndex: 'load_rate',
      align: 'center',
      render(text: string) {
        return <div className="no-line-break">{text}</div>;
      }
    },
    {
      title: t(
        'server-data-import.import-details.column-titles.import-progress'
      ),
      dataIndex: 'load_progress',
      width: '40%',
      render(progress: number, rowData: Record<string, any>) {
        return (
          <div
            className="no-line-break"
            style={{ display: 'flex', alignItems: 'center' }}
          >
            <div className="import-tasks-server-data-import-table-progress">
              <div
                className="import-tasks-server-data-import-table-progress"
                style={{
                  width: `${progress}%`,
                  background: importStatusColorMapping[rowData.status],
                  transition: 'width 1s ease-in-out'
                }}
              ></div>
            </div>
            <div>{`${progress}%`}</div>
          </div>
        );
      }
    },
    {
      title: t('server-data-import.import-details.column-titles.status'),
      dataIndex: 'status',
      width: '5%',
      render(text: string) {
        return (
          <div className="no-line-break">
            {t(`server-data-import.import-details.status.${text}`)}
          </div>
        );
      }
    },
    {
      title: t('server-data-import.import-details.column-titles.time-consumed'),
      dataIndex: 'duration',
      align: 'right',
      render(text: string) {
        return <div className="no-line-break">{text}</div>;
      }
    },
    {
      title: t('server-data-import.import-details.column-titles.manipulations'),
      width: '15%',
      render(_: never, rowData: Record<string, any>, taskIndex: number) {
        return (
          <div className="no-line-break">
            <ImportManipulations
              importStatus={rowData.status}
              taskIndex={taskIndex}
              loopQuery={loopQueryImportData}
            />
          </div>
        );
      }
    }
  ];

  const handleInputChange = (key: keyof LoadParameter) => (e: any) => {
    serverDataImportStore.mutateImportConfigs(key, e.value);
    serverDataImportStore.validateImportConfigs(key);
  };

  const handleBlur = (key: keyof LoadParameter) => () => {
    serverDataImportStore.validateImportConfigs(key);
  };

  const loopQueryImportData = useCallback(() => {
    const loopId = window.setInterval(async () => {
      if (serverDataImportStore.isImportFinished) {
        window.clearInterval(loopId);
        return;
      }

      serverDataImportStore.fetchImportTasks(
        serverDataImportStore.fileImportTaskIds
      );
    }, 500);
  }, []);

  const expandClassName = classnames({
    'import-tasks-step-content-header-expand':
      serverDataImportStore.isExpandImportConfig,
    'import-tasks-step-content-header-collpase': !serverDataImportStore.isExpandImportConfig
  });

  return (
    <div className="import-tasks-server-data-import-configs-wrapper">
      <div className="import-tasks-step-content-header">
        <span>{t('server-data-import.import-settings.title')}</span>
        <img
          src={ArrowIcon}
          alt="collpaseOrExpand"
          className={expandClassName}
          onClick={() => {
            serverDataImportStore.switchExpandImportConfig(
              !serverDataImportStore.isExpandImportConfig
            );
          }}
        />
      </div>

      {serverDataImportStore.isExpandImportConfig && (
        <div className="import-tasks-server-data-import-configs">
          <div className="import-tasks-server-data-import-config">
            <div className="import-tasks-server-data-import-config-option">
              <div
                style={{
                  display: 'flex',
                  alignItems: 'center',
                  height: 32,
                  justifyContent: 'flex-end'
                }}
              >
                <span style={{ marginRight: 5 }}>
                  {t('server-data-import.import-settings.checkIfExist')}:
                </span>
                <Tooltip
                  placement="right"
                  title={t('server-data-import.hint.check-vertex')}
                  type="dark"
                >
                  <img src={HintIcon} alt="hint" />
                </Tooltip>
              </div>
              <div style={{ width: 100 }}>
                <Switch
                  checked={serverDataImportStore.importConfigs?.check_vertex}
                  size="large"
                  onChange={(checked: boolean) => {
                    serverDataImportStore.mutateImportConfigs(
                      'check_vertex',
                      checked
                    );
                  }}
                />
              </div>
            </div>
            <div className="import-tasks-server-data-import-config-option">
              <span>
                {t(
                  'server-data-import.import-settings.maximumAnalyzedErrorRow'
                )}
                :
              </span>
              <Input
                {...commonInputProps}
                value={serverDataImportStore.importConfigs?.max_parse_errors}
                onChange={handleInputChange('max_parse_errors')}
                errorMessage={
                  serverDataImportStore.validateImportConfigErrorMessage
                    .max_parse_errors
                }
                originInputProps={{
                  onBlur: handleBlur('max_parse_errors')
                }}
              />
            </div>
            <div className="import-tasks-server-data-import-config-option">
              <span>
                {t(
                  'server-data-import.import-settings.maxiumInterpolateErrorRow'
                )}
                :
              </span>
              <Input
                {...commonInputProps}
                value={serverDataImportStore.importConfigs?.max_insert_errors}
                onChange={handleInputChange('max_insert_errors')}
                errorMessage={
                  serverDataImportStore.validateImportConfigErrorMessage
                    .max_insert_errors
                }
                originInputProps={{
                  onBlur: handleBlur('max_insert_errors')
                }}
              />
            </div>
          </div>
          <div className="import-tasks-server-data-import-config">
            <div className="import-tasks-server-data-import-config-option">
              <span>
                {t(
                  'server-data-import.import-settings.requestTimesWhenInterpolationFailed'
                )}
                :
              </span>
              <Input
                {...commonInputProps}
                value={serverDataImportStore.importConfigs?.retry_times}
                onChange={handleInputChange('retry_times')}
                errorMessage={
                  serverDataImportStore.validateImportConfigErrorMessage
                    .retry_times
                }
                originInputProps={{
                  onBlur: handleBlur('retry_times')
                }}
              />
            </div>
            <div className="import-tasks-server-data-import-config-option">
              <span>
                {t(
                  'server-data-import.import-settings.requestTicksWhenInterpolationFailed'
                )}
                :
              </span>
              <Input
                {...commonInputProps}
                value={serverDataImportStore.importConfigs?.retry_interval}
                onChange={handleInputChange('retry_interval')}
                errorMessage={
                  serverDataImportStore.validateImportConfigErrorMessage
                    .retry_interval
                }
                originInputProps={{
                  onBlur: handleBlur('retry_interval')
                }}
              />
            </div>
            <div className="import-tasks-server-data-import-config-option">
              <span>
                {t('server-data-import.import-settings.InterpolationTimeout')}:
              </span>
              <Input
                {...commonInputProps}
                value={serverDataImportStore.importConfigs?.insert_timeout}
                onChange={handleInputChange('insert_timeout')}
                errorMessage={
                  serverDataImportStore.validateImportConfigErrorMessage
                    .insert_timeout
                }
                originInputProps={{
                  onBlur: handleBlur('retry_interval')
                }}
              />
            </div>
          </div>
        </div>
      )}

      {(serverDataImportStore.isImporting ||
        size(serverDataImportStore.importTasks) !== 0) && (
        <>
          <div
            className="import-tasks-step-content-header"
            style={{ marginTop: 16 }}
          >
            <span>{t('server-data-import.import-details.title')}</span>
          </div>
          <div className="import-tasks-server-data-import-table-wrapper">
            <Table
              columns={columnConfigs}
              dataSource={serverDataImportStore.importTasks}
              locale={{
                emptyText: t('server-data-import.hint.no-data') + '...'
              }}
              pagination={false}
            />
          </div>
        </>
      )}

      <div className="import-tasks-server-data-import-manipulations">
        <Button
          size="medium"
          style={{
            width: 74,
            marginRight: 16
          }}
          onClick={() => {
            if (serverDataImportStore.isServerStartImport) {
              dataMapStore.switchExpand('file', false);
            }

            dataImportRootStore.setCurrentStep(2);
          }}
        >
          {t('server-data-import.manipulations.previous')}
        </Button>
        <Button
          type="primary"
          size="medium"
          style={{
            width: 88
          }}
          disabled={
            Object.values(serverDataImportStore.importConfigs!).some(
              (value) => value === ''
            ) ||
            Object.values(
              serverDataImportStore.validateImportConfigErrorMessage
            ).some((value) => !isEmpty(value))
          }
          onClick={async () => {
            if (serverDataImportStore.isImportFinished) {
              dataImportRootStore.setCurrentStep(4);
              return;
            }

            if (serverDataImportStore.isImporting) {
              await Promise.all(
                serverDataImportStore.importTasks
                  .filter(
                    ({ status }) => status === 'RUNNING' || status === 'PAUSED'
                  )
                  .map(({ id }) => serverDataImportStore.abortImport(id))
              );

              await serverDataImportStore.fetchImportTasks(
                serverDataImportStore.fileImportTaskIds
              );
              serverDataImportStore.switchFetchImportStatus('standby');
              serverDataImportStore.switchImporting(false);
              serverDataImportStore.switchImportFinished(true);
              return;
            }

            serverDataImportStore.switchExpandImportConfig(false);
            serverDataImportStore.switchImporting(true);

            await serverDataImportStore.startImport(
              dataMapStore.fileMapInfos
                .filter(({ name }) =>
                  dataImportRootStore.successFileUploadTaskNames.includes(name)
                )
                .map(({ id }) => id)
            );

            if (serverDataImportStore.requestStatus.startImport === 'failed') {
              Message.error({
                content: serverDataImportStore.errorInfo.startImport.message,
                size: 'medium',
                showCloseIcon: false
              });

              return;
            }

            loopQueryImportData();
          }}
        >
          {serverDataImportStore.isImportFinished
            ? t('server-data-import.manipulations.finished')
            : serverDataImportStore.requestStatus.startImport === 'standby'
            ? t('server-data-import.manipulations.start')
            : t('server-data-import.manipulations.cancel')}
        </Button>
      </div>
    </div>
  );
});

export interface ImportManipulationsProps {
  importStatus: string;
  taskIndex: number;
  loopQuery: () => void;
}

const ImportManipulations: React.FC<ImportManipulationsProps> = observer(
  ({ importStatus, taskIndex, loopQuery }) => {
    const dataImportRootStore = useContext(DataImportRootStoreContext);
    const { serverDataImportStore } = dataImportRootStore;
    const { t } = useTranslation();
    const manipulations: string[] = [];

    const handleClickManipulation = async (manipulation: string) => {
      switch (manipulation) {
        case t('server-data-import.import-details.manipulations.pause'):
          serverDataImportStore.pauseImport(
            serverDataImportStore.importTasks[taskIndex].id
          );
          break;
        case t('server-data-import.import-details.manipulations.abort'):
          serverDataImportStore.abortImport(
            serverDataImportStore.importTasks[taskIndex].id
          );

          break;
        case t('server-data-import.import-details.manipulations.resume'):
          await serverDataImportStore.resumeImport(
            serverDataImportStore.importTasks[taskIndex].id
          );

          serverDataImportStore.switchImporting(true);
          serverDataImportStore.switchImportFinished(false);
          loopQuery();
          break;
        case t('server-data-import.import-details.manipulations.failed-cause'):
          serverDataImportStore.checkErrorLogs(
            serverDataImportStore.importTasks[taskIndex].id
          );
          break;
        case t('server-data-import.import-details.manipulations.retry'):
          await serverDataImportStore.retryImport(
            serverDataImportStore.importTasks[taskIndex].id
          );

          serverDataImportStore.switchImporting(true);
          serverDataImportStore.switchImportFinished(false);
          loopQuery();
          break;
      }
    };

    switch (importStatus) {
      case 'RUNNING':
        manipulations.push(
          t('server-data-import.import-details.manipulations.pause'),
          t('server-data-import.import-details.manipulations.abort')
        );
        break;
      case 'FAILED':
        manipulations.push(
          t('server-data-import.import-details.manipulations.resume'),
          t('server-data-import.import-details.manipulations.failed-cause')
        );
        break;
      case 'PAUSED':
        manipulations.push(
          t('server-data-import.import-details.manipulations.resume'),
          t('server-data-import.import-details.manipulations.abort')
        );
        break;
      case 'STOPPED':
        manipulations.push(
          t('server-data-import.import-details.manipulations.retry')
        );
        break;
      case 'SUCCEED':
        break;
      default:
        throw new Error('Wrong status received from server');
    }

    return (
      <div className="import-tasks-server-data-import-table-manipulations">
        {manipulations.map((manipulation) => {
          if (
            manipulation ===
            t('server-data-import.import-details.manipulations.failed-cause')
          ) {
            return (
              <a
                target="_blank"
                className="import-tasks-manipulation"
                key={manipulation}
                href={`/graph-management/${dataImportRootStore.currentId}/data-import/import-tasks/${serverDataImportStore.importTasks[taskIndex].id}/error-log`}
                style={{ marginRight: 8, textDecoration: 'none' }}
                onClick={() => {
                  handleClickManipulation(manipulation);
                }}
              >
                {manipulation}
              </a>
            );
          }

          return (
            <span
              className="import-tasks-manipulation"
              key={manipulation}
              style={{ marginRight: 8 }}
              onClick={() => {
                serverDataImportStore.switchImportFinished(false);
                handleClickManipulation(manipulation);
              }}
            >
              {manipulation}
            </span>
          );
        })}
      </div>
    );
  }
);

export default ImportConfigs;
