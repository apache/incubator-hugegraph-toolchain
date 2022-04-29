import React, {
  useContext,
  useCallback,
  useEffect,
  useState,
  useRef
} from 'react';
import { observer } from 'mobx-react';
import classnames from 'classnames';
import { useRoute } from 'wouter';
import { useTranslation } from 'react-i18next';
import { isEmpty, size, intersection, without } from 'lodash-es';
import { Breadcrumb, Input, Button, Message, Table, Loading } from 'hubble-ui';

import {
  GraphManagementStoreContext,
  AsyncTasksStoreContext
} from '../../../stores';
import { LoadingDataView, Tooltip } from '../../common';
import AddIcon from '../../../assets/imgs/ic_add.svg';
import WhiteCloseIcon from '../../../assets/imgs/ic_close_white.svg';

import type { AsyncTask } from '../../../stores/types/GraphManagementStore/asyncTasksStore';

import './AsyncTaskList.less';
import { isUndefined } from 'util';

const AsyncTaskList: React.FC = observer(() => {
  const graphManagementStore = useContext(GraphManagementStoreContext);
  const asyncTasksStore = useContext(AsyncTasksStoreContext);
  const [, params] = useRoute('/graph-management/:id/async-tasks');
  const [selectedRowKeys, mutateSelectedRowKeys] = useState<number[]>([]);
  const [isShowTypeFilter, switchShowTypeFilter] = useState(false);
  const [isShowStatusFilter, switchShowStatusFilter] = useState(false);
  const [isShowBatchDeleteModal, switchShowBatchDeleteModal] = useState(false);
  const [preLoading, switchPreLoading] = useState(true);
  const [isInLoop, switchInLoop] = useState(false);

  const deleteWrapperRef = useRef<HTMLDivElement>(null);

  const { t } = useTranslation();

  const currentSelectedRowKeys = intersection(
    selectedRowKeys,
    asyncTasksStore.asyncTaskList.map(({ id }) => id)
  );

  const handleSelectedTableRow = (newSelectedRowKeys: number[]) => {
    mutateSelectedRowKeys(newSelectedRowKeys);
  };

  const handleSearchChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    asyncTasksStore.mutateSearchWords(e.target.value);
  };

  const handleSearch = () => {
    asyncTasksStore.mutateAsyncTasksPageNumber(1);
    asyncTasksStore.switchSearchedStatus(true);
    asyncTasksStore.fetchAsyncTaskList();
  };

  const handlePageChange = (e: React.ChangeEvent<HTMLSelectElement>) => {
    switchPreLoading(true);

    asyncTasksStore.mutateAsyncTasksPageNumber(Number(e.target.value));
    asyncTasksStore.fetchAsyncTaskList();
  };

  const handleClearSearch = () => {
    asyncTasksStore.mutateSearchWords('');
    asyncTasksStore.mutateAsyncTasksPageNumber(1);
    asyncTasksStore.switchSearchedStatus(false);
    asyncTasksStore.fetchAsyncTaskList();
  };

  const handleFilterOptions = (
    categroy: 'type' | 'status',
    option: string
  ) => () => {
    asyncTasksStore.mutateFilterOptions(categroy, option);
    // reset page number to beginning
    asyncTasksStore.mutateAsyncTasksPageNumber(1);
    asyncTasksStore.fetchAsyncTaskList();

    categroy === 'type'
      ? switchShowTypeFilter(false)
      : switchShowStatusFilter(false);
  };

  const handleOutSideClick = useCallback(
    (e: MouseEvent) => {
      if (
        isShowBatchDeleteModal !== null &&
        deleteWrapperRef &&
        deleteWrapperRef.current &&
        !deleteWrapperRef.current.contains(e.target as Element)
      ) {
        switchShowBatchDeleteModal(false);
      }
    },
    [deleteWrapperRef, isShowBatchDeleteModal]
  );

  const columnConfigs = [
    {
      title: t('async-tasks.table-column-title.task-id'),
      dataIndex: 'id',
      render(text: string) {
        return (
          <div className="no-line-break" title={text}>
            {text}
          </div>
        );
      }
    },
    {
      title: t('async-tasks.table-column-title.task-name'),
      dataIndex: 'task_name',
      width: '20%',
      render(text: string) {
        return (
          <div className="no-line-break" title={text}>
            {text}
          </div>
        );
      }
    },
    {
      title: t('async-tasks.table-column-title.task-type'),
      dataIndex: 'task_type',
      width: '13%',
      onFilterDropdownVisibleChange(visible: boolean) {
        switchShowTypeFilter(visible);
      },
      filterDropdownVisible: isShowTypeFilter,
      filterDropdown: (
        <div
          className={`async-task-list-table-filters-wrapper type-${asyncTasksStore.filterOptions.type}`}
        >
          <div onClick={handleFilterOptions('type', '')}>
            {t('async-tasks.table-filters.task-type.all')}
          </div>
          <div onClick={handleFilterOptions('type', 'gremlin')}>
            {t('async-tasks.table-filters.task-type.gremlin')}
          </div>
          <div onClick={handleFilterOptions('type', 'algorithm')}>
            {t('async-tasks.table-filters.task-type.algorithm')}
          </div>
          <div onClick={handleFilterOptions('type', 'remove_schema')}>
            {t('async-tasks.table-filters.task-type.remove-schema')}
          </div>
          <div onClick={handleFilterOptions('type', 'create_index')}>
            {t('async-tasks.table-filters.task-type.create-index')}
          </div>
          <div onClick={handleFilterOptions('type', 'rebuild_index')}>
            {t('async-tasks.table-filters.task-type.rebuild-index')}
          </div>
        </div>
      ),
      render(text: string) {
        return (
          <div
            className="no-line-break"
            title={t(
              `async-tasks.table-filters.task-type.${text.replace('_', '-')}`
            )}
          >
            {t(`async-tasks.table-filters.task-type.${text.replace('_', '-')}`)}
          </div>
        );
      }
    },
    {
      title: t('async-tasks.table-column-title.create-time'),
      dataIndex: 'task_create',
      width: '18%',
      render(timeStamp: string) {
        const date = new Date(timeStamp);
        const convertedDate = `${date.toISOString().split('T')[0]} ${
          date.toTimeString().split(' ')[0]
        }`;

        return (
          <div className="no-line-break" title={convertedDate}>
            {convertedDate}
          </div>
        );
      }
    },
    {
      title: t('async-tasks.table-column-title.time-consuming'),
      width: '12%',
      render(rowData: AsyncTask) {
        const duration =
          Number(rowData.task_update) - Number(rowData.task_create);
        let restTime = duration;
        let timeString = '';

        if (Math.floor(duration / 1000 / 60 / 60 / 24) > 0) {
          const dayDiffernce = Math.floor(duration / 1000 / 60 / 60 / 24);
          timeString += dayDiffernce + 'd';
          restTime -= dayDiffernce * 1000 * 60 * 60 * 24;
        }

        if (Math.floor(restTime / 1000 / 60 / 60) > 0) {
          const dayDiffernce = Math.floor(restTime / 1000 / 60 / 60);
          timeString += ' ' + dayDiffernce + 'h';
          restTime -= dayDiffernce * 1000 * 60 * 60;
        }

        if (Math.floor(restTime / 1000 / 60) > 0) {
          const dayDiffernce = Math.floor(restTime / 1000 / 60);
          timeString += ' ' + dayDiffernce + 'm';
          restTime -= dayDiffernce * 1000 * 60;
        }

        if (Math.floor(restTime / 1000) > 0) {
          const dayDiffernce = Math.floor(restTime / 1000);
          timeString += ' ' + dayDiffernce + 's';
          restTime -= dayDiffernce * 1000;
        }

        if (restTime > 0) {
          timeString += ' ' + restTime + 'ms';
        }

        if (restTime <= 0) {
          timeString = '0s';
        }

        return <div className="no-line-break">{timeString}</div>;
      }
    },
    {
      title: t('async-tasks.table-column-title.status'),
      dataIndex: 'task_status',
      onFilterDropdownVisibleChange(visible: boolean) {
        switchShowStatusFilter(visible);
      },
      filterDropdownVisible: isShowStatusFilter,
      filterDropdown: (
        <div
          className={`async-task-list-table-filters-wrapper status-${asyncTasksStore.filterOptions.status}`}
        >
          <div onClick={handleFilterOptions('status', '')}>
            {t('async-tasks.table-filters.status.all')}
          </div>
          <div onClick={handleFilterOptions('status', 'scheduling')}>
            {t('async-tasks.table-filters.status.scheduling')}
          </div>
          <div onClick={handleFilterOptions('status', 'queued')}>
            {t('async-tasks.table-filters.status.queued')}
          </div>
          <div onClick={handleFilterOptions('status', 'running')}>
            {t('async-tasks.table-filters.status.running')}
          </div>
          <div onClick={handleFilterOptions('status', 'restoring')}>
            {t('async-tasks.table-filters.status.restoring')}
          </div>
          <div onClick={handleFilterOptions('status', 'success')}>
            {t('async-tasks.table-filters.status.success')}
          </div>
          <div onClick={handleFilterOptions('status', 'failed')}>
            {t('async-tasks.table-filters.status.failed')}
          </div>
          <div onClick={handleFilterOptions('status', 'cancelled')}>
            {t('async-tasks.table-filters.status.cancelled')}
          </div>
        </div>
      ),
      width: '8%',
      render(text: string) {
        return (
          <div className={`async-task-list-table-status-wrapper ${text}`}>
            {t(`async-tasks.table-filters.status.${text}`)}
          </div>
        );
      }
    },
    {
      title: t('async-tasks.table-column-title.manipulation'),
      width: '15%',
      render(rowData: AsyncTask) {
        return (
          <div className="no-line-break">
            <AsyncTaskListManipulation
              id={rowData.id}
              type={rowData.task_type}
              status={rowData.task_status}
            />
          </div>
        );
      }
    }
  ];

  const isLoading =
    preLoading ||
    asyncTasksStore.requestStatus.fetchAsyncTaskList === 'pending';

  const wrapperClassName = classnames({
    'async-task-list': true,
    'async-task-list-with-expand-sidebar': graphManagementStore.isExpanded
  });

  useEffect(() => {
    if (params !== null) {
      graphManagementStore.fetchIdList();
      asyncTasksStore.setCurrentId(Number(params!.id));
      let startTime = new Date().getTime();
      let timerId: number;

      const loopFetchAsyncTaskList = async () => {
        const currentTime = new Date().getTime();

        if (currentTime - startTime > 1000 * 60 * 10) {
          switchInLoop(false);
          return;
        }

        await asyncTasksStore.fetchAsyncTaskList();

        if (asyncTasksStore.requestStatus.fetchAsyncTaskList === 'failed') {
          Message.error({
            content: asyncTasksStore.errorInfo.fetchAsyncTaskList.message,
            size: 'medium',
            showCloseIcon: false
          });

          return;
        }

        if (
          !isUndefined(
            asyncTasksStore.asyncTaskList.find(
              ({ task_status }) =>
                task_status === 'scheduling' ||
                task_status === 'scheduled' ||
                task_status === 'queued' ||
                task_status === 'running' ||
                task_status === 'restoring'
            )
          )
        ) {
          timerId = window.setTimeout(() => {
            switchInLoop(true);
            loopFetchAsyncTaskList();
          }, 5000);
        } else {
          switchInLoop(false);
          return;
        }
      };

      loopFetchAsyncTaskList();

      return () => {
        switchInLoop(false);
        window.clearTimeout(timerId);
      };
    }
    // when page number changed, dispatch current useEffect to loop again
  }, [params?.id, asyncTasksStore.asyncTasksPageConfig.pageNumber]);

  useEffect(() => {
    document.addEventListener('click', handleOutSideClick, false);

    return () => {
      document.removeEventListener('click', handleOutSideClick, false);
    };
  }, [handleOutSideClick]);

  useEffect(() => {
    setTimeout(() => {
      switchPreLoading(false);
    }, 800);
  }, [asyncTasksStore.asyncTasksPageConfig.pageNumber]);

  // dynamic edit table border-radius when selected row changes
  useEffect(() => {
    const tableWrapperRef = document.querySelector('.new-fc-one-table');
    const tableRef = document.querySelector('.new-fc-one-table table');

    if (tableRef !== null) {
      if (size(selectedRowKeys) !== 0) {
        (tableWrapperRef as HTMLDivElement).style.borderRadius = '0';
        (tableRef as HTMLTableElement).style.borderRadius = '0';
      } else {
        (tableWrapperRef as HTMLDivElement).style.borderRadius = '2px 2px 0 0';
        (tableRef as HTMLTableElement).style.borderRadius = '2px 2px 0 0';
      }
    }
  }, [selectedRowKeys]);

  useEffect(() => {
    return () => {
      asyncTasksStore.dispose();
    };
  }, []);

  return (
    <section className={wrapperClassName}>
      <div className="async-task-list-breadcrumb-wrapper">
        <Breadcrumb>
          <Breadcrumb.Item>{t('async-tasks.title')}</Breadcrumb.Item>
        </Breadcrumb>
      </div>
      <div className="async-task-list-content-wrapper">
        <div className="async-task-list-content-header">
          <Input.Search
            size="medium"
            width={215}
            placeholder={t('async-tasks.placeholders.search')}
            value={asyncTasksStore.searchWords}
            onChange={handleSearchChange}
            onSearch={handleSearch}
            onClearClick={handleClearSearch}
            isShowDropDown={false}
            disabled={isLoading || size(currentSelectedRowKeys) !== 0}
          />
        </div>
        {size(currentSelectedRowKeys) !== 0 && (
          <div className="async-task-list-table-selected-reveals">
            <div>
              {t('async-tasks.table-selection.selected', {
                number: size(currentSelectedRowKeys)
              })}
            </div>
            <Tooltip
              tooltipShown={isShowBatchDeleteModal}
              placement="bottom-start"
              modifiers={{
                offset: {
                  offset: '0, 10'
                }
              }}
              tooltipWrapperProps={{
                className: 'async-task-list-tooltip'
              }}
              tooltipWrapper={
                <div ref={deleteWrapperRef}>
                  <>
                    <p className="async-task-list-tooltip-title">
                      {t('async-tasks.hint.delete-batch-confirm')}
                    </p>
                    <p className="async-task-list-tooltip-description">
                      {t('async-tasks.hint.delete-batch-description')}
                    </p>
                    <div
                      style={{
                        display: 'flex',
                        marginTop: 12,
                        color: '#2b65ff',
                        cursor: 'pointer'
                      }}
                    >
                      <Button
                        type="primary"
                        size="medium"
                        style={{ width: 60 }}
                        onClick={async () => {
                          switchShowBatchDeleteModal(false);
                          await asyncTasksStore.deleteAsyncTask(
                            currentSelectedRowKeys
                          );

                          if (
                            asyncTasksStore.requestStatus.deleteAsyncTask ===
                            'success'
                          ) {
                            Message.success({
                              content: t('async-tasks.hint.delete-succeed'),
                              size: 'medium',
                              showCloseIcon: false
                            });

                            mutateSelectedRowKeys(
                              without(
                                selectedRowKeys,
                                ...currentSelectedRowKeys
                              )
                            );

                            await asyncTasksStore.fetchAsyncTaskList();

                            // fetch previous page data if it's empty
                            if (
                              asyncTasksStore.requestStatus
                                .fetchAsyncTaskList === 'success' &&
                              size(asyncTasksStore.asyncTaskList) === 0 &&
                              asyncTasksStore.asyncTasksPageConfig.pageNumber >
                                1
                            ) {
                              asyncTasksStore.mutateAsyncTasksPageNumber(
                                asyncTasksStore.asyncTasksPageConfig
                                  .pageNumber - 1
                              );

                              asyncTasksStore.fetchAsyncTaskList();
                            }
                          }

                          if (
                            asyncTasksStore.requestStatus.deleteAsyncTask ===
                            'failed'
                          ) {
                            Message.error({
                              content: <AsyncTaskDeleteErrorLayout />,
                              size: 'medium',
                              showCloseIcon: false
                            });
                          }
                        }}
                      >
                        {t('async-tasks.hint.delete')}
                      </Button>
                      <Button
                        size="medium"
                        style={{
                          marginLeft: 12,
                          width: 60
                        }}
                        onClick={() => {
                          switchShowBatchDeleteModal(false);
                        }}
                      >
                        {t('async-tasks.hint.cancel')}
                      </Button>
                    </div>
                  </>
                </div>
              }
              childrenWrapperElement="div"
            >
              <Button
                onClick={() => {
                  switchShowBatchDeleteModal(true);
                }}
              >
                {t('async-tasks.table-selection.delete-batch')}
              </Button>
            </Tooltip>
            <img
              src={WhiteCloseIcon}
              alt="close"
              onClick={() => {
                mutateSelectedRowKeys([]);
              }}
            />
          </div>
        )}
        <Table
          columns={columnConfigs}
          rowKey={(rowData: AsyncTask) => rowData.id}
          rowSelection={{
            selectedRowKeys,
            onChange: handleSelectedTableRow,
            getCheckboxProps(records: AsyncTask) {
              const { task_status } = records;
              return {
                disabled:
                  task_status === 'scheduling' ||
                  task_status === 'scheduled' ||
                  task_status === 'queued' ||
                  task_status === 'running' ||
                  task_status === 'restoring' ||
                  (!isEmpty(selectedRowKeys) &&
                    asyncTasksStore.requestStatus.deleteAsyncTask === 'pending')
              };
            }
          }}
          locale={{
            emptyText: (
              <LoadingDataView
                isLoading={isLoading}
                emptyView={
                  asyncTasksStore.isSearched.status ? (
                    <span>{t('async-tasks.hint.no-data')}</span>
                  ) : (
                    <EmptyAsyncTaskHints />
                  )
                }
              />
            )
          }}
          dataSource={
            isLoading && !isInLoop ? [] : asyncTasksStore.asyncTaskList
          }
          pagination={{
            hideOnSinglePage: false,
            pageNo: asyncTasksStore.asyncTasksPageConfig.pageNumber,
            pageSize: 10,
            showSizeChange: false,
            showPageJumper: false,
            total: asyncTasksStore.asyncTasksPageConfig.pageTotal,
            onPageNoChange: handlePageChange
          }}
        />
      </div>
    </section>
  );
});

export interface AsyncTaskListManipulationProps {
  id: number;
  type: string;
  status: string;
}

export const AsyncTaskListManipulation: React.FC<AsyncTaskListManipulationProps> = observer(
  ({ id, type, status }) => {
    const asyncTasksStore = useContext(AsyncTasksStoreContext);
    const [isPopDeleteModal, switchPopDeleteModal] = useState(false);
    const deleteWrapperRef = useRef<HTMLDivElement>(null);
    const [, params] = useRoute('/graph-management/:id/async-tasks');
    const { t } = useTranslation();

    const handleOutSideClick = useCallback(
      (e: MouseEvent) => {
        if (
          isPopDeleteModal !== null &&
          deleteWrapperRef &&
          deleteWrapperRef.current &&
          !deleteWrapperRef.current.contains(e.target as Element)
        ) {
          switchPopDeleteModal(false);
        }
      },
      [deleteWrapperRef, isPopDeleteModal]
    );

    // some status only has one manipulation, if all of them in such status
    // no margin left needed (also flex-end)
    const shouldLeftMargin = !isEmpty(
      asyncTasksStore.asyncTaskList.filter(
        ({ task_status, task_type }) =>
          task_status === 'failed' ||
          (task_status === 'success' &&
            (task_type === 'gremlin' || task_type === 'algorithm'))
      )
    );

    useEffect(() => {
      document.addEventListener('click', handleOutSideClick, false);

      return () => {
        document.removeEventListener('click', handleOutSideClick, false);
      };
    }, [handleOutSideClick]);

    return (
      <div
        className="async-task-list-table-manipulations"
        style={{ justifyContent: shouldLeftMargin ? 'flex-end' : 'flex-start' }}
      >
        {status === 'success' && (type === 'gremlin' || type === 'algorithm') && (
          <a
            target="_blank"
            className="async-task-list-table-outlink"
            href={`/graph-management/${params!.id}/async-tasks/${id}/result`}
          >
            {t('async-tasks.manipulations.check-result')}
          </a>
        )}
        {status === 'failed' && (
          <a
            target="_blank"
            className="async-task-list-table-outlink"
            href={`/graph-management/${params!.id}/async-tasks/${id}/result`}
          >
            {t('async-tasks.manipulations.check-reason')}
          </a>
        )}
        {status !== 'scheduling' &&
          status !== 'scheduled' &&
          status !== 'queued' &&
          status !== 'running' &&
          status !== 'restoring' && (
            <Tooltip
              tooltipShown={isPopDeleteModal}
              placement="bottom-end"
              modifiers={{
                offset: {
                  offset: '0, 5'
                }
              }}
              tooltipWrapperProps={{
                className: 'async-task-list-tooltip'
              }}
              tooltipWrapper={
                <div ref={deleteWrapperRef}>
                  <p className="async-task-list-tooltip-title">
                    {t('async-tasks.hint.delete-confirm')}
                  </p>
                  <p className="async-task-list-tooltip-description">
                    {t('async-tasks.hint.delete-description')}
                  </p>
                  <div
                    style={{
                      display: 'flex',
                      marginTop: 12,
                      color: '#2b65ff',
                      cursor: 'pointer'
                    }}
                  >
                    <Button
                      type="primary"
                      size="medium"
                      style={{ width: 60 }}
                      onClick={async () => {
                        switchPopDeleteModal(false);

                        await asyncTasksStore.deleteAsyncTask([id]);

                        if (
                          asyncTasksStore.requestStatus.deleteAsyncTask ===
                          'success'
                        ) {
                          Message.success({
                            content: t('async-tasks.hint.delete-succeed'),
                            size: 'medium',
                            showCloseIcon: false
                          });

                          asyncTasksStore.fetchAsyncTaskList();
                        }

                        if (
                          asyncTasksStore.requestStatus.deleteAsyncTask ===
                          'failed'
                        ) {
                          Message.error({
                            content: <AsyncTaskDeleteErrorLayout />,
                            size: 'medium',
                            showCloseIcon: false
                          });
                        }
                      }}
                    >
                      {t('async-tasks.hint.delete')}
                    </Button>
                    <Button
                      size="medium"
                      style={{
                        marginLeft: 12,
                        width: 60
                      }}
                      onClick={() => {
                        switchPopDeleteModal(false);
                      }}
                    >
                      {t('async-tasks.hint.cancel')}
                    </Button>
                  </div>
                </div>
              }
              childrenProps={{
                style: {
                  marginLeft: shouldLeftMargin ? '16px' : 0
                },
                onClick() {
                  switchPopDeleteModal(true);
                }
              }}
            >
              {t('async-tasks.manipulations.delete')}
            </Tooltip>
          )}
        {(status === 'scheduling' ||
          status === 'scheduled' ||
          status === 'queued' ||
          status === 'running' ||
          status === 'restoring') && (
          <span
            onClick={async () => {
              await asyncTasksStore.abortAsyncTask(id);
              asyncTasksStore.fetchAsyncTaskList();
            }}
            style={{ marginLeft: shouldLeftMargin ? '16px' : 0 }}
          >
            {t('async-tasks.manipulations.abort')}
          </span>
        )}
        {status === 'cancelling' && (
          <div style={{ marginLeft: shouldLeftMargin ? '16px' : 0 }}>
            <Loading type="strong" style={{ padding: 0 }} />
            <span style={{ marginLeft: 4 }}>
              {t('async-tasks.manipulations.aborting')}
            </span>
          </div>
        )}
      </div>
    );
  }
);

export const EmptyAsyncTaskHints: React.FC = observer(() => {
  const { t } = useTranslation();

  return (
    <div className="import-manager-empty-list">
      <img src={AddIcon} alt={t('async-tasks.hint.empty')} />
      <div>{t('async-tasks.hint.empty')}</div>
    </div>
  );
});

export const AsyncTaskDeleteErrorLayout: React.FC = observer(() => {
  const asyncTasksStore = useContext(AsyncTasksStoreContext);
  const { t } = useTranslation();
  const [isShowDetails, switchShowDetails] = useState(false);

  const errorLayoutWrapper = classnames({
    'async-task-list-error-layout': true,
    'async-task-list-error-layout-expand': isShowDetails
  });

  return (
    <div className={errorLayoutWrapper}>
      <div className="async-task-list-error-layout-title">
        {t('async-tasks.hint.creation-failed')}
      </div>
      {isShowDetails ? (
        <p>{asyncTasksStore.errorInfo.deleteAsyncTask.message}</p>
      ) : (
        <p
          onClick={() => {
            switchShowDetails(true);
          }}
        >
          {t('async-tasks.hint.check-details')}
        </p>
      )}
    </div>
  );
});

export default AsyncTaskList;
