import React, { useContext, useCallback, useEffect, useRef } from 'react';
import { observer } from 'mobx-react';
import { useLocation } from 'wouter';
import { isEmpty } from 'lodash-es';
import { useTranslation } from 'react-i18next';
import classnames from 'classnames';
import { Switch, Input, Button, Table, Tooltip, Message } from '@baidu/one-ui';

import { DataImportRootStoreContext } from '../../../../../stores';
import { useInitDataImport } from '../../../../../hooks';

import type {
  ImportTasks,
  LoadParameter
} from '../../../../../stores/types/GraphManagementStore/dataImportStore';

import ArrowIcon from '../../../../../assets/imgs/ic_arrow_16.svg';
import HintIcon from '../../../../../assets/imgs/ic_question_mark.svg';

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

export interface ImportConfigsProps {
  height?: string;
}

const ImportConfigs: React.FC<ImportConfigsProps> = observer(({ height }) => {
  const dataImportRootStore = useContext(DataImportRootStoreContext);
  const { serverDataImportStore, dataMapStore } = dataImportRootStore;
  const timerId = useRef(NaN);
  const isInitReady = useInitDataImport();
  const [, setLocation] = useLocation();
  const { t } = useTranslation();

  const columnConfigs = [
    {
      title: t('server-data-import.import-details.column-titles.file-name'),
      dataIndex: 'file_name',
      width: '10%',
      render(text: string) {
        return (
          <div className="no-line-break" title={text}>
            {text}
          </div>
        );
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
      width: '12%',
      align: 'center',
      render(text: string) {
        return (
          <div className="no-line-break" title={text}>
            {text}
          </div>
        );
      }
    },
    {
      title: t(
        'server-data-import.import-details.column-titles.import-progress'
      ),
      dataIndex: 'load_progress',
      width: '35%',
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
      width: '10%',
      render(text: string) {
        return (
          <div
            className="no-line-break"
            title={t(`server-data-import.import-details.status.${text}`)}
          >
            {t(`server-data-import.import-details.status.${text}`)}
          </div>
        );
      }
    },
    {
      title: t('server-data-import.import-details.column-titles.time-consumed'),
      dataIndex: 'duration',
      align: 'right',
      width: '8%',
      render(text: string) {
        return (
          <div className="no-line-break" title={text}>
            {text}
          </div>
        );
      }
    },
    {
      title: !serverDataImportStore.readOnly
        ? t('server-data-import.import-details.column-titles.manipulations')
        : '',
      width: '15%',
      render(_: never, rowData: Record<string, any>, taskIndex: number) {
        return !serverDataImportStore.readOnly ? (
          <div className="no-line-break">
            <ImportManipulations
              importStatus={rowData.status}
              taskIndex={taskIndex}
              loopQuery={loopQueryImportData}
            />
          </div>
        ) : null;
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

      if (
        serverDataImportStore.requestStatus.fetchImportTasks === 'failed' ||
        serverDataImportStore.requestStatus.fetchAllImportTasks === 'failed'
      ) {
        Message.error({
          content:
            serverDataImportStore.errorInfo.fetchImportTasks.message ||
            serverDataImportStore.errorInfo.fetchAllImportTasks.message,
          size: 'medium',
          showCloseIcon: false
        });

        window.clearInterval(loopId);
        return;
      }

      if (serverDataImportStore.isIrregularProcess) {
        serverDataImportStore.fetchAllImportTasks();
      } else {
        // stop loops when users click sidebar icon
        // (dispose called in <ImportTasks />, [ids] resets to empty array)
        if (isEmpty(serverDataImportStore.fileImportTaskIds)) {
          window.clearInterval(loopId);
          return;
        }

        serverDataImportStore.fetchImportTasks(
          serverDataImportStore.fileImportTaskIds
        );
      }
    }, 1000);

    timerId.current = loopId;
  }, []);

  const expandClassName = classnames({
    'import-tasks-step-content-header-expand':
      serverDataImportStore.isExpandImportConfig,
    'import-tasks-step-content-header-collpase': !serverDataImportStore.isExpandImportConfig
  });

  useEffect(() => {
    // if comes from import manager or refresh
    if (
      (!serverDataImportStore.readOnly ||
        serverDataImportStore.isIrregularProcess) &&
      dataImportRootStore.currentStatus === 'LOADING'
    ) {
      loopQueryImportData();
    }

    return () => {
      if (!Object.is(NaN, timerId.current)) {
        window.clearInterval(timerId.current);
      }
    };
  }, [
    isInitReady,
    serverDataImportStore.readOnly,
    serverDataImportStore.isIrregularProcess,
    dataImportRootStore.currentStatus
  ]);

  return isInitReady ? (
    <div
      className="import-tasks-server-data-import-configs-wrapper"
      style={{
        height: height ? height : 'calc(100vh - 194px)',
        overflow: 'auto'
      }}
    >
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
                  disabled={
                    serverDataImportStore.readOnly ||
                    serverDataImportStore.importConfigReadOnly
                  }
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
              {!serverDataImportStore.readOnly &&
              !serverDataImportStore.importConfigReadOnly ? (
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
              ) : (
                <div className="import-tasks-server-data-import-config-option-readonly-data">
                  {serverDataImportStore.importConfigs?.max_parse_errors}
                </div>
              )}
            </div>
            <div className="import-tasks-server-data-import-config-option">
              <span>
                {t(
                  'server-data-import.import-settings.maxiumInterpolateErrorRow'
                )}
                :
              </span>
              {!serverDataImportStore.readOnly &&
              !serverDataImportStore.importConfigReadOnly ? (
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
              ) : (
                <div className="import-tasks-server-data-import-config-option-readonly-data">
                  {serverDataImportStore.importConfigs?.max_insert_errors}
                </div>
              )}
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
              {!serverDataImportStore.readOnly &&
              !serverDataImportStore.importConfigReadOnly ? (
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
              ) : (
                <div className="import-tasks-server-data-import-config-option-readonly-data">
                  {serverDataImportStore.importConfigs?.retry_times}
                </div>
              )}
            </div>
            <div className="import-tasks-server-data-import-config-option">
              <span>
                {t(
                  'server-data-import.import-settings.requestTicksWhenInterpolationFailed'
                )}
                :
              </span>
              {!serverDataImportStore.readOnly &&
              !serverDataImportStore.importConfigReadOnly ? (
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
              ) : (
                <div className="import-tasks-server-data-import-config-option-readonly-data">
                  {serverDataImportStore.importConfigs?.retry_interval}
                </div>
              )}
            </div>
            <div className="import-tasks-server-data-import-config-option">
              <span>
                {t('server-data-import.import-settings.InterpolationTimeout')}:
              </span>
              {!serverDataImportStore.readOnly &&
              !serverDataImportStore.importConfigReadOnly ? (
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
              ) : (
                <div className="import-tasks-server-data-import-config-option-readonly-data">
                  {serverDataImportStore.importConfigs?.insert_timeout}
                </div>
              )}
            </div>
          </div>
        </div>
      )}

      {(dataImportRootStore.currentStatus === 'LOADING' ||
        dataImportRootStore.currentStatus === 'SUCCESS' ||
        dataImportRootStore.currentStatus === 'FAILED' ||
        (serverDataImportStore.isIrregularProcess &&
          serverDataImportStore.readOnly)) && (
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

      {!serverDataImportStore.readOnly && (
        <div className="import-tasks-server-data-import-manipulations">
          <Button
            size="medium"
            style={{
              width: 74,
              marginRight: 16
            }}
            disabled={
              dataImportRootStore.currentStatus === 'SUCCESS' ||
              dataImportRootStore.currentStatus === 'FAILED'
            }
            onClick={() => {
              if (serverDataImportStore.isServerStartImport) {
                dataMapStore.switchExpand('file', false);
              }

              setLocation(
                `/graph-management/${dataImportRootStore.currentId}/data-import/import-manager/${dataImportRootStore.currentJobId}/import-tasks/mapping`
              );

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
              Object.values(serverDataImportStore.importConfigs ?? {}).some(
                (value) => value === ''
              ) ||
              Object.values(
                serverDataImportStore.validateImportConfigErrorMessage
              ).some((value) => !isEmpty(value)) ||
              // (serverDataImportStore.isIrregularProcess &&
              //   isEmpty(serverDataImportStore.importTasks))
              (dataImportRootStore.currentStatus === 'LOADING' &&
                isEmpty(serverDataImportStore.importTasks))
            }
            onClick={async () => {
              if (
                dataImportRootStore.currentStatus === 'SUCCESS' ||
                dataImportRootStore.currentStatus === 'FAILED' ||
                (!isEmpty(serverDataImportStore.importTasks) &&
                  !serverDataImportStore.importTasks.some(
                    ({ status }) => status === 'RUNNING'
                  ))
              ) {
                setLocation(
                  `/graph-management/${dataImportRootStore.currentId}/data-import/import-manager/${dataImportRootStore.currentJobId}/import-tasks/finish`
                );

                dataImportRootStore.setCurrentStatus('SUCCESS');
                dataImportRootStore.setCurrentStep(4);
                return;
              }

              if (dataImportRootStore.currentStatus === 'LOADING') {
                await Promise.all(
                  serverDataImportStore.importTasks
                    .filter(
                      ({ status }) =>
                        status === 'RUNNING' || status === 'PAUSED'
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

              if (
                !dataMapStore.isIrregularProcess &&
                dataImportRootStore.currentStatus === 'SETTING'
              ) {
                // forbid editings on previous step
                dataMapStore.switchLock(true);
                serverDataImportStore.switchImportConfigReadOnly(true);
                dataImportRootStore.setCurrentStatus('LOADING');

                await serverDataImportStore.setConfigParams();

                if (
                  serverDataImportStore.requestStatus.setConfigParams ===
                  'failed'
                ) {
                  Message.error({
                    content:
                      serverDataImportStore.errorInfo.setConfigParams.message,
                    size: 'medium',
                    showCloseIcon: false
                  });
                }

                await serverDataImportStore.startImport(
                  dataMapStore.fileMapInfos
                    .filter(({ name }) =>
                      dataImportRootStore.successFileUploadTaskNames.includes(
                        name
                      )
                    )
                    .map(({ id }) => id)
                );

                if (
                  serverDataImportStore.requestStatus.startImport === 'failed'
                ) {
                  Message.error({
                    content:
                      serverDataImportStore.errorInfo.startImport.message,
                    size: 'medium',
                    showCloseIcon: false
                  });
                }
              } else {
                dataMapStore.switchLock(true);
                serverDataImportStore.switchImportConfigReadOnly(true);
                dataImportRootStore.setCurrentStatus('LOADING');

                await serverDataImportStore.setConfigParams();

                if (
                  serverDataImportStore.requestStatus.setConfigParams ===
                  'failed'
                ) {
                  Message.error({
                    content:
                      serverDataImportStore.errorInfo.setConfigParams.message,
                    size: 'medium',
                    showCloseIcon: false
                  });
                }

                await serverDataImportStore.startImport(
                  dataMapStore.fileMapInfos.map(({ id }) => id)
                );

                if (
                  serverDataImportStore.requestStatus.startImport === 'failed'
                ) {
                  Message.error({
                    content:
                      serverDataImportStore.errorInfo.startImport.message,
                    size: 'medium',
                    showCloseIcon: false
                  });
                }
              }

              if (
                serverDataImportStore.requestStatus.startImport === 'failed'
              ) {
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
            {dataImportRootStore.currentStatus === 'SUCCESS' ||
            dataImportRootStore.currentStatus === 'FAILED' ||
            (!isEmpty(serverDataImportStore.importTasks) &&
              !serverDataImportStore.importTasks.some(
                ({ status }) => status === 'RUNNING'
              ))
              ? t('server-data-import.manipulations.finished')
              : dataImportRootStore.currentStatus === 'SETTING'
              ? t('server-data-import.manipulations.start')
              : t('server-data-import.manipulations.cancel')}
          </Button>
        </div>
      )}
    </div>
  ) : null;
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
          await serverDataImportStore.pauseImport(
            serverDataImportStore.importTasks[taskIndex].id
          );

          if (serverDataImportStore.requestStatus.pauseImport === 'failed') {
            Message.error({
              content: serverDataImportStore.errorInfo.pauseImport.message,
              size: 'medium',
              showCloseIcon: false
            });
          }

          break;
        case t('server-data-import.import-details.manipulations.abort'):
          await serverDataImportStore.abortImport(
            serverDataImportStore.importTasks[taskIndex].id
          );

          if (serverDataImportStore.requestStatus.abortImport === 'failed') {
            Message.error({
              content: serverDataImportStore.errorInfo.abortImport.message,
              size: 'medium',
              showCloseIcon: false
            });
          }

          serverDataImportStore.fetchAllImportTasks();

          break;
        case t('server-data-import.import-details.manipulations.resume'):
          await serverDataImportStore.resumeImport(
            serverDataImportStore.importTasks[taskIndex].id
          );

          if (serverDataImportStore.requestStatus.resumeImport === 'failed') {
            Message.error({
              content: serverDataImportStore.errorInfo.resumeImport.message,
              size: 'medium',
              showCloseIcon: false
            });
          }

          serverDataImportStore.switchImporting(true);
          serverDataImportStore.switchImportFinished(false);
          loopQuery();
          break;
        case t('server-data-import.import-details.manipulations.failed-cause'):
          serverDataImportStore.checkErrorLogs(
            dataImportRootStore.currentId!,
            dataImportRootStore.currentJobId!,
            serverDataImportStore.importTasks[taskIndex].id
          );
          break;
        case t('server-data-import.import-details.manipulations.retry'):
          await serverDataImportStore.retryImport(
            serverDataImportStore.importTasks[taskIndex].id
          );

          if (serverDataImportStore.requestStatus.retryImport === 'failed') {
            Message.error({
              content: serverDataImportStore.errorInfo.retryImport.message,
              size: 'medium',
              showCloseIcon: false
            });
          }

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
                href={`/graph-management/${dataImportRootStore.currentId}/data-import/${dataImportRootStore.currentJobId}/task-error-log/${serverDataImportStore.importTasks[taskIndex].id}`}
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
