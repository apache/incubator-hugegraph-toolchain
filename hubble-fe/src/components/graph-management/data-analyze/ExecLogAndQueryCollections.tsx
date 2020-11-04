import React, { useState, useContext, useEffect, useCallback } from 'react';
import { observer } from 'mobx-react';
import classnames from 'classnames';
import { isUndefined } from 'lodash-es';
import Highlighter from 'react-highlight-words';
import { useRoute, useLocation } from 'wouter';
import { useTranslation } from 'react-i18next';
import { Table, Input, Button, Message } from '@baidu/one-ui';

import { Tooltip } from '../../common';
import Favorite from './common/Favorite';
import {
  DataAnalyzeStoreContext,
  AsyncTasksStoreContext
} from '../../../stores';

import type { TFunction } from 'i18next';
import type {
  ExecutionLogs,
  FavoriteQuery
} from '../../../stores/types/GraphManagementStore/dataAnalyzeStore';
import {Algorithm} from '../../../stores/factory/dataAnalyzeStore/algorithmStore';

import ArrowIcon from '../../../assets/imgs/ic_arrow_16.svg';
import EmptyIcon from '../../../assets/imgs/ic_sousuo_empty.svg';

const styles = {
  tableCenter: {
    display: 'flex',
    justifyContent: 'center'
  },
  favoriteQueriesWrapper: {
    display: 'flex',
    flexDirection: 'column',
    justifyContent: 'flex-end'
  }
};

const ExecLogAndQueryCollections: React.FC = observer(() => {
  const asyncTasksStore = useContext(AsyncTasksStoreContext);
  const dataAnalyzeStore = useContext(DataAnalyzeStoreContext);
  const [tabIndex, setTabIndex] = useState(0);
  const [, params] = useRoute('/graph-management/:id/data-analyze');
  const { t } = useTranslation();
  const [, setLocation] = useLocation();

  // popovers
  const [isFavoritePop, switchFavoritePop] = useState(false);
  const [currentPopInTable, setCurrentPopInTable] = useState<
    'execLogs' | 'favoriteQueries' | 'deleteQueries'
  >('execLogs');

  const [currentFavoritePop, setCurrentFavoritePop] = useState<number | null>(
    null
  );

  // hack: @observable in columnConfigs cannot be observed
  dataAnalyzeStore.favoritePopUp.toUpperCase();

  const execLogsColumnConfigs = [
    {
      title: t('data-analyze.exec-logs.table-title.time'),
      dataIndex: 'create_time',
      width: '20%'
    },
    {
      title: t('data-analyze.exec-logs.table-title.type'),
      dataIndex: 'type',
      width: '15%',
      render(type: string) {
        return t(`data-analyze.exec-logs.type.${type}`);
      }
    },
    {
      title: t('data-analyze.exec-logs.table-title.content'),
      dataIndex: 'content',
      width: '30%',
      render(text: string, rowData: ExecutionLogs) {
        return (
          <ExecutionContent
            type={rowData.type}
            content={text}
            highlightText=""
          />
        );
      }
    },
    {
      title: t('data-analyze.exec-logs.table-title.status'),
      dataIndex: 'status',
      width: '10%',
      align: 'center',
      render(text: string) {
        switch (text) {
          case 'SUCCESS':
            return (
              <div style={styles.tableCenter}>
                <div className="exec-log-status success">
                  {t('data-analyze.exec-logs.status.success')}
                </div>
              </div>
            );
          case 'ASYNC_TASK_SUCCESS':
            return (
              <div style={styles.tableCenter}>
                <div className="exec-log-status success">
                  {t('data-analyze.exec-logs.status.async-success')}
                </div>
              </div>
            );
          case 'RUNNING':
            return (
              <div style={styles.tableCenter}>
                <div className="exec-log-status running">
                  {t('data-analyze.exec-logs.status.running')}
                </div>
              </div>
            );
          case 'ASYNC_TASK_RUNNING':
            return (
              <div style={styles.tableCenter}>
                <div className="exec-log-status running">
                  {t('data-analyze.exec-logs.status.running')}
                </div>
              </div>
            );
          case 'FAILED':
            return (
              <div style={styles.tableCenter}>
                <div className="exec-log-status failed">
                  {t('data-analyze.exec-logs.status.failed')}
                </div>
              </div>
            );
          case 'ASYNC_TASK_FAILED':
            return (
              <div style={styles.tableCenter}>
                <div className="exec-log-status failed">
                  {t('data-analyze.exec-logs.status.async-failed')}
                </div>
              </div>
            );
        }
      }
    },
    {
      title: t('data-analyze.exec-logs.table-title.duration'),
      dataIndex: 'duration',
      align: 'right',
      width: '10%'
    },
    {
      title: t('data-analyze.exec-logs.table-title.manipulation'),
      dataIndex: 'manipulation',
      width: '15%',
      render(_: string, rowData: ExecutionLogs, index: number) {
        return (
          <div>
            {rowData.type === 'GREMLIN_ASYNC' && (
              <span
                className="exec-log-manipulation"
                onClick={() => {
                  // mock search
                  asyncTasksStore.mutateSearchWords(String(rowData.async_id));
                  asyncTasksStore.switchSearchedStatus(true);

                  setLocation(`/graph-management/${params!.id}/async-tasks`);
                }}
              >
                详情
              </span>
            )}
            <Tooltip
              tooltipShown={
                currentPopInTable === 'execLogs' &&
                dataAnalyzeStore.favoritePopUp === 'addFavoriteInExeclog' &&
                currentFavoritePop === index
              }
              placement="left"
              modifiers={{
                offset: {
                  offset: '0, 10'
                }
              }}
              tooltipWrapperProps={{
                className: 'tooltips'
              }}
              tooltipWrapper={
                <Favorite
                  handlePop={switchFavoritePop}
                  queryStatement={rowData.content}
                />
              }
              childrenProps={{
                className: 'exec-log-manipulation',
                onClick: () => {
                  dataAnalyzeStore.setFavoritePopUp('addFavoriteInExeclog');
                  dataAnalyzeStore.resetFavoriteRequestStatus('add');
                  dataAnalyzeStore.resetFavoriteRequestStatus('edit');
                  switchFavoritePop(true);
                  setCurrentFavoritePop(index);
                }
              }}
            >
              收藏
            </Tooltip>
            <span
              className="exec-log-manipulation"
              onClick={loadStatements(
                rowData.content,
                rowData.type === 'GREMLIN_ASYNC' ? 'task' : 'query'
              )}
            >
              加载语句
            </span>
          </div>
        );
      }
    }
  ];

  const queryFavoriteColumnConfigs = [
    {
      title: '时间',
      dataIndex: 'create_time',
      width: '25%',
      sorter: true
    },
    {
      title: '名称',
      dataIndex: 'name',
      width: '15%',
      sorter: true,
      render(text: string) {
        return (
          <Highlighter
            highlightClassName="graph-management-list-highlight"
            searchWords={[dataAnalyzeStore.isSearched.value]}
            autoEscape={true}
            textToHighlight={text}
          />
        );
      }
    },
    {
      title: '收藏语句',
      dataIndex: 'content',
      width: '40%',
      render(text: string, rowData: FavoriteQuery) {
        return (
          <ExecutionContent
            type={''}
            content={text}
            highlightText={dataAnalyzeStore.isSearched.value}
          />
        );
      }
    },
    {
      title: '操作',
      dataIndex: 'manipulation',
      width: '20%',
      render(_: string, rowData: FavoriteQuery, index: number) {
        return (
          <div>
            <span
              className="exec-log-manipulation"
              onClick={loadStatements(rowData.content)}
            >
              加载语句
            </span>
            <Tooltip
              placement="left"
              tooltipShown={
                currentPopInTable === 'favoriteQueries' &&
                dataAnalyzeStore.favoritePopUp === 'editFavorite' &&
                currentFavoritePop === index
              }
              modifiers={{
                offset: {
                  offset: '0, 10'
                }
              }}
              tooltipWrapperProps={{
                className: 'tooltips'
              }}
              tooltipWrapper={
                <Favorite
                  handlePop={switchFavoritePop}
                  isEdit={true}
                  id={rowData.id}
                  name={rowData.name}
                  queryStatement={rowData.content}
                />
              }
              childrenProps={{
                className: 'exec-log-manipulation',
                onClick: () => {
                  dataAnalyzeStore.setFavoritePopUp('editFavorite');
                  dataAnalyzeStore.resetFavoriteRequestStatus('add');
                  dataAnalyzeStore.resetFavoriteRequestStatus('edit');
                  setCurrentPopInTable('favoriteQueries');
                  switchFavoritePop(true);
                  setCurrentFavoritePop(index);
                }
              }}
            >
              修改名称
            </Tooltip>
            <Tooltip
              placement="left"
              tooltipShown={
                currentPopInTable === 'deleteQueries' &&
                isFavoritePop &&
                currentFavoritePop === index
              }
              modifiers={{
                offset: {
                  offset: '0, 10'
                }
              }}
              tooltipWrapperProps={{
                className: 'tooltips'
              }}
              tooltipWrapper={
                <DeleteConfirm id={rowData.id} handlePop={switchFavoritePop} />
              }
              childrenProps={{
                className: 'exec-log-manipulation',
                onClick: () => {
                  setCurrentPopInTable('deleteQueries');
                  switchFavoritePop(true);
                  setCurrentFavoritePop(index);
                }
              }}
            >
              删除
            </Tooltip>
          </div>
        );
      }
    }
  ];

  const handleExecLogPageNoChange = useCallback(
    (e: React.ChangeEvent<HTMLSelectElement>) => {
      dataAnalyzeStore.mutatePageNumber('executionLog', Number(e.target.value));
      dataAnalyzeStore.fetchExecutionLogs();
    },
    [dataAnalyzeStore]
  );

  const handleFavoriteQueriesPageNoChange = useCallback(
    (e: React.ChangeEvent<HTMLSelectElement>) => {
      dataAnalyzeStore.mutatePageNumber(
        'favoriteQueries',
        Number(e.target.value)
      );
      dataAnalyzeStore.fetchFavoriteQueries();
    },
    [dataAnalyzeStore]
  );

  const handleExecLogPageSizeChange = useCallback(
    (e: React.ChangeEvent<HTMLButtonElement>) => {
      dataAnalyzeStore.mutatePageSize('executionLog', Number(e.target.value));
      dataAnalyzeStore.mutatePageNumber('executionLog', 1);
      dataAnalyzeStore.fetchExecutionLogs();
    },
    [dataAnalyzeStore]
  );

  const handleFavoriteQueriesPageSizeChange = useCallback(
    (e: React.ChangeEvent<HTMLButtonElement>) => {
      dataAnalyzeStore.mutatePageSize(
        'favoriteQueries',
        Number(e.target.value)
      );
      dataAnalyzeStore.mutatePageNumber('favoriteQueries', 1);
      dataAnalyzeStore.fetchFavoriteQueries();
    },
    [dataAnalyzeStore]
  );

  const loadStatements = useCallback(
    (content: string, type?: 'query' | 'task') => () => {
      if (!dataAnalyzeStore.isLoadingGraph) {
        if (!isUndefined(type)) {
          type === 'task'
            ? dataAnalyzeStore.setQueryMode('task')
            : dataAnalyzeStore.setQueryMode('query');
        }

        switchFavoritePop(false);
        dataAnalyzeStore.mutateCodeEditorText(content);
        dataAnalyzeStore.triggerLoadingStatementsIntoEditor();
        window.scrollTo(0, 0);
      }
    },
    [dataAnalyzeStore]
  );

  const handleSearchChange = useCallback(
    (e: React.ChangeEvent<HTMLInputElement>) => {
      dataAnalyzeStore.mutateSearchText(e.target.value);
    },
    [dataAnalyzeStore]
  );

  const handleSearch = useCallback(() => {
    dataAnalyzeStore.mutatePageNumber('favoriteQueries', 1);
    dataAnalyzeStore.swtichIsSearchedStatus(true);
    dataAnalyzeStore.fetchFavoriteQueries();
  }, [dataAnalyzeStore]);

  const handleClearSearch = useCallback(() => {
    dataAnalyzeStore.mutateSearchText('');
    dataAnalyzeStore.mutatePageNumber('favoriteQueries', 1);
    dataAnalyzeStore.swtichIsSearchedStatus(false);
    dataAnalyzeStore.fetchFavoriteQueries();
  }, [dataAnalyzeStore]);

  const handleFavoriteSortClick = useCallback(
    ({ sortOrder, sortColumn }) => {
      if (sortColumn.dataIndex === 'create_time') {
        dataAnalyzeStore.changeFavoriteQueriesSortOrder('name', '');

        sortOrder === 'descend'
          ? dataAnalyzeStore.changeFavoriteQueriesSortOrder('time', 'desc')
          : dataAnalyzeStore.changeFavoriteQueriesSortOrder('time', 'asc');
      }

      if (sortColumn.dataIndex === 'name') {
        dataAnalyzeStore.changeFavoriteQueriesSortOrder('time', '');

        sortOrder === 'descend'
          ? dataAnalyzeStore.changeFavoriteQueriesSortOrder('name', 'desc')
          : dataAnalyzeStore.changeFavoriteQueriesSortOrder('name', 'asc');
      }

      dataAnalyzeStore.fetchFavoriteQueries();
    },
    [dataAnalyzeStore]
  );

  useEffect(() => {
    if (dataAnalyzeStore.currentId !== null) {
      dataAnalyzeStore.fetchExecutionLogs();
      dataAnalyzeStore.fetchFavoriteQueries();
    }
    // fetch execlogs & favorites after id changes
  }, [dataAnalyzeStore, dataAnalyzeStore.currentId]);

  return (
    <div className="data-analyze-logs-favorite">
      <div className="query-tab-index-wrapper">
        <div
          onClick={() => {
            setTabIndex(0);
            setCurrentPopInTable('execLogs');
          }}
          className={
            tabIndex === 0 ? 'query-tab-index active' : 'query-tab-index'
          }
        >
          执行记录
        </div>
        <div
          onClick={() => {
            setTabIndex(1);
            setCurrentPopInTable('favoriteQueries');
          }}
          className={
            tabIndex === 1 ? 'query-tab-index active' : 'query-tab-index'
          }
        >
          收藏的查询
        </div>
      </div>
      <div className="exec-log-favorite-tab-content-wrapper">
        <div className="exec-log-favorite-tab-content">
          <div style={{ width: '100%' }}>
            {tabIndex === 0 ? (
              <Table
                columns={execLogsColumnConfigs}
                dataSource={dataAnalyzeStore.executionLogData}
                pagination={{
                  showPageJumper: false,
                  pageSize: dataAnalyzeStore.pageConfigs.executionLog.pageSize,
                  pageSizeOptions: ['10', '20', '50'],
                  pageNo: dataAnalyzeStore.pageConfigs.executionLog.pageNumber,
                  total: dataAnalyzeStore.pageConfigs.executionLog.pageTotal,
                  onPageNoChange: handleExecLogPageNoChange,
                  onPageSizeChange: handleExecLogPageSizeChange
                }}
              />
            ) : (
              <div>
                <div style={{ marginBottom: 16, textAlign: 'right' }}>
                  <Input.Search
                    size="medium"
                    width={200}
                    placeholder="搜索收藏名称或语句"
                    value={dataAnalyzeStore.searchText}
                    onChange={handleSearchChange}
                    onSearch={handleSearch}
                    onClearClick={handleClearSearch}
                    isShowDropDown={false}
                  />
                </div>
                <Table
                  locale={{
                    emptyText:
                      dataAnalyzeStore.requestStatus.fetchFavoriteQueries ===
                      'success' ? (
                        <>
                          <img src={EmptyIcon} alt="无匹配结果" />
                          <div style={{ fontSize: 14, color: '#333' }}>
                            无匹配结果
                          </div>
                        </>
                      ) : (
                        '暂无数据'
                      )
                  }}
                  columns={queryFavoriteColumnConfigs}
                  dataSource={dataAnalyzeStore.favoriteQueryData}
                  onSortClick={handleFavoriteSortClick}
                  pagination={{
                    showPageJumper: false,
                    pageSize:
                      dataAnalyzeStore.pageConfigs.favoriteQueries.pageSize,
                    pageSizeOptions: ['10', '20', '50'],
                    total:
                      dataAnalyzeStore.pageConfigs.favoriteQueries.pageTotal,
                    pageNo:
                      dataAnalyzeStore.pageConfigs.favoriteQueries.pageNumber,
                    onPageNoChange: handleFavoriteQueriesPageNoChange,
                    onPageSizeChange: handleFavoriteQueriesPageSizeChange
                  }}
                />
              </div>
            )}
          </div>
        </div>
      </div>
    </div>
  );
});

// collpase and expand statement in table
const ExecutionContent: React.FC<{
  type: string;
  content: string;
  highlightText: string;
}> = observer(({ type, content, highlightText }) => {
  const dataAnalyzeStore = useContext(DataAnalyzeStoreContext);
  const { t } = useTranslation();
  const [isExpand, switchExpand] = useState(dataAnalyzeStore.isSearched.status);
  const statements =
    type === 'ALGORITHM'
      ? formatAlgorithmStatement(content, Algorithm.shortestPath, t)
      : content.split('\n').filter((statement) => statement !== '');

  const arrowIconClassName = classnames({
    'data-analyze-logs-favorite-content-icon': true,
    reverse: isExpand
  });

  const handleExpandClick = useCallback(() => {
    switchExpand(!isExpand);
  }, [isExpand]);

  if (statements.length <= 1) {
    return (
      <Highlighter
        highlightClassName="graph-management-list-highlight"
        searchWords={[highlightText]}
        autoEscape={true}
        textToHighlight={content}
      />
    );
  }

  const statementElement = statements.map((statement, index) => (
    <div className="data-analyze-logs-favorite-content-statement" key={index}>
      <Highlighter
        highlightClassName="graph-management-list-highlight"
        searchWords={[highlightText]}
        autoEscape={true}
        textToHighlight={statement}
      />
    </div>
  ));

  return (
    <div className="data-analyze-logs-favorite-content">
      <img
        src={ArrowIcon}
        alt="展开/收起"
        className={arrowIconClassName}
        onClick={handleExpandClick}
      />
      <div className="data-analyze-logs-favorite-content-statements-wrapper">
        {isExpand ? statementElement : statementElement[0]}
      </div>
    </div>
  );
});

export interface DeleteConfirmProps {
  id: number;
  handlePop: (flag: boolean) => void;
}

export const DeleteConfirm: React.FC<DeleteConfirmProps> = observer(
  ({ id, handlePop }) => {
    const dataAnalyzeStore = useContext(DataAnalyzeStoreContext);

    const handleDelete = useCallback(async () => {
      await dataAnalyzeStore.deleteQueryCollection(id);

      if (dataAnalyzeStore.requestStatus.editQueryCollection === 'failed') {
        Message.error({
          content: dataAnalyzeStore.errorInfo.editQueryCollection.message,
          size: 'medium',
          showCloseIcon: false
        });
      }

      dataAnalyzeStore.fetchFavoriteQueries();
      handlePop(false);
    }, [dataAnalyzeStore, handlePop, id]);

    const handleCancel = useCallback(() => {
      handlePop(false);
    }, [handlePop]);

    return (
      <div className="data-analyze">
        <div className="delete-confirm-wrapper">
          <span>确认删除</span>
          <span>是否确认删除该条收藏语句？</span>
          <div className="delete-confirm-footer">
            <Button
              type="primary"
              size="medium"
              style={{ width: 60 }}
              onClick={handleDelete}
            >
              删除
            </Button>
            <Button
              size="medium"
              style={{
                marginLeft: 12,
                width: 60
              }}
              onClick={handleCancel}
            >
              取消
            </Button>
          </div>
        </div>
      </div>
    );
  }
);

function formatAlgorithmStatement(
  content: string,
  algorithmType: string,
  translator: TFunction
) {
  const convertedString = content
    .replace(/^.*\(/, '')
    .replace(/\)$/, '')
    .replace(/ /g, '');
  const statements: string[] = [
    translator(`data-analyze.algorithm-list.${algorithmType}`)
  ];

  convertedString.split(',').forEach((item) => {
    const [key, value] = item.split('=');

    statements.push(
      `${translator(
        `data-analyze.algorithm-forms.shortest-path.options.${key}`
      )} ${value}`
    );
  });

  return statements;
}

export default ExecLogAndQueryCollections;
