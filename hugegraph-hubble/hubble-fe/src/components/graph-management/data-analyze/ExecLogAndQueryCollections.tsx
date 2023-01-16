import React, { useState, useContext, useEffect, useCallback } from 'react';
import { observer } from 'mobx-react';
import classnames from 'classnames';
import { isUndefined, cloneDeep, isEmpty, isNull } from 'lodash-es';
import Highlighter from 'react-highlight-words';
import { useRoute, useLocation } from 'wouter';
import { useTranslation } from 'react-i18next';

import { v4 } from 'uuid';
import { Table, Input, Button, Message } from 'hubble-ui';

import { Tooltip } from '../../common';
import Favorite from './common/Favorite';
import {
  DataAnalyzeStoreContext,
  AsyncTasksStoreContext
} from '../../../stores';
import { formatAlgorithmStatement } from '../../../utils';

import type {
  ExecutionLogs,
  FavoriteQuery,
  LoopDetectionParams,
  FocusDetectionParams,
  ShortestPathAlgorithmParams,
  ShortestPathAllAlgorithmParams,
  AllPathAlgorithmParams,
  KStepNeighbor,
  KHop,
  RadiographicInspection,
  SameNeighbor,
  Jaccard,
  WeightedShortestPath,
  SingleSourceWeightedShortestPath,
  PersonalRank,
  ModelSimilarityParams,
  NeighborRankParams,
  CustomPathParams,
  CustomPathRule
} from '../../../stores/types/GraphManagementStore/dataAnalyzeStore';
import { Algorithm } from '../../../stores/factory/dataAnalyzeStore/algorithmStore';

import ArrowIcon from '../../../assets/imgs/ic_arrow_16.svg';
import EmptyIcon from '../../../assets/imgs/ic_sousuo_empty.svg';
import { toJS } from 'mobx';

export const AlgorithmInternalNameMapping: Record<string, string> = {
  rings: 'loop-detection',
  crosspoints: 'focus-detection',
  shortpath: 'shortest-path',
  allshortpath: 'shortest-path-all',
  paths: 'all-path',
  fsimilarity: 'model-similarity',
  neighborrank: 'neighbor-rank',
  kneighbor: 'k-step-neighbor',
  kout: 'k-hop',
  customizedpaths: 'custom-path',
  rays: 'radiographic-inspection',
  sameneighbors: 'same-neighbor',
  weightedshortpath: 'weighted-shortest-path',
  singleshortpath: 'single-source-weighted-shortest-path',
  jaccardsimilarity: 'jaccard',
  personalrank: 'personal-rank'
};

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
  const { algorithmAnalyzerStore } = dataAnalyzeStore;
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
      className: 'sql-content-clazz',
      width: '30%',
      render(text: string, rowData: ExecutionLogs) {
        return (
          <ExecutionContent
            type={rowData.type}
            content={text}
            algorithmName={rowData.algorithm_name}
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
                {t('addition.operate.detail')}
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
              {t('addition.operate.favorite')}
            </Tooltip>
            <span
              className="exec-log-manipulation"
              onClick={loadStatements(
                rowData.content,
                rowData.type === 'GREMLIN_ASYNC'
                  ? 'task'
                  : rowData.type === 'GREMLIN'
                  ? 'query'
                  : 'algorithm',
                rowData.algorithm_name
              )}
            >
              {t('addition.operate.load-statement')}
            </span>
          </div>
        );
      }
    }
  ];

  const queryFavoriteColumnConfigs = [
    {
      title: t('addition.operate.time'),
      dataIndex: 'create_time',
      width: '25%',
      sorter: true
    },
    {
      title: t('addition.operate.name'),
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
      title: t('addition.operate.favorite-statement'),
      dataIndex: 'content',
      width: '40%',
      className: 'sql-content-clazz',
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
      title: t('addition.operate.operate'),
      dataIndex: 'manipulation',
      width: '20%',
      render(_: string, rowData: FavoriteQuery, index: number) {
        return (
          <div>
            <span
              className="exec-log-manipulation"
              onClick={loadStatements(rowData.content)}
            >
              {t('addition.operate.load-statement')}
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
              {t('addition.operate.modify-name')}
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
              {t('addition.common.del')}
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
    (
      content: string,
      type?: 'query' | 'task' | 'algorithm',
      algorithmName?: string
    ) => () => {
      if (!dataAnalyzeStore.isLoadingGraph) {
        if (type === 'algorithm') {
          const params: Record<string, any> = JSON.parse(content);
          dataAnalyzeStore.setCurrentTab('algorithm-analyze');

          if (params.hasOwnProperty('direction')) {
            params.direction = params.direction.toUpperCase();
          }

          if (params.hasOwnProperty('label')) {
            params['label'] === null && (params['label'] = '__all__');
          }

          window.scrollTo(0, 0);

          switch (algorithmName) {
            case 'rings':
              algorithmAnalyzerStore.changeCurrentAlgorithm(
                Algorithm.loopDetection
              );

              Object.keys(params).forEach((key) => {
                algorithmAnalyzerStore.mutateLoopDetectionParams(
                  key as keyof LoopDetectionParams,
                  params[key]
                );
              });

              return;
            case 'crosspoints':
              algorithmAnalyzerStore.changeCurrentAlgorithm(
                Algorithm.focusDetection
              );

              Object.keys(params).forEach((key) => {
                algorithmAnalyzerStore.mutateFocusDetectionParams(
                  key as keyof FocusDetectionParams,
                  params[key]
                );
              });

              return;
            case 'shortpath':
              algorithmAnalyzerStore.changeCurrentAlgorithm(
                Algorithm.shortestPath
              );

              Object.keys(params).forEach((key) => {
                algorithmAnalyzerStore.mutateShortestPathParams(
                  key as keyof ShortestPathAlgorithmParams,
                  params[key]
                );
              });

              return;
            case 'allshortpath':
              algorithmAnalyzerStore.changeCurrentAlgorithm(
                Algorithm.shortestPathAll
              );

              Object.keys(params).forEach((key) => {
                algorithmAnalyzerStore.mutateShortestPathAllParams(
                  key as keyof ShortestPathAllAlgorithmParams,
                  params[key]
                );
              });

              return;
            case 'paths':
              algorithmAnalyzerStore.changeCurrentAlgorithm(Algorithm.allPath);

              Object.keys(params).forEach((key) => {
                algorithmAnalyzerStore.mutateAllPathParams(
                  key as keyof AllPathAlgorithmParams,
                  params[key]
                );
              });

              return;
            case 'fsimilarity': {
              const clonedParams = cloneDeep(params);

              if (isEmpty(params.sources.ids)) {
                clonedParams.method = 'property';
              } else {
                clonedParams.method = 'id';
              }

              clonedParams.source = clonedParams.sources.ids.join(',');
              clonedParams.vertexType = clonedParams.sources.label;
              clonedParams.vertexProperty = Object.entries(
                clonedParams.sources.properties
              );

              if (isEmpty(clonedParams.vertexProperty)) {
                clonedParams.vertexProperty = [['', '']];
              } else {
                clonedParams.vertexProperty = clonedParams.vertexProperty.map(
                  ([key, value]: [string, string[]]) => [key, value.join(',')]
                );
              }

              clonedParams.least_neighbor = clonedParams.min_neighbors;
              clonedParams.similarity = clonedParams.alpha;
              clonedParams.least_similar = clonedParams.min_similars;
              clonedParams.max_similar = clonedParams.top;
              clonedParams.property_filter = clonedParams.group_property;
              clonedParams.least_property_number = clonedParams.min_groups;
              clonedParams.return_common_connection =
                clonedParams.with_intermediary;
              clonedParams.return_complete_info = clonedParams.with_vertex;

              delete clonedParams.sources;
              delete clonedParams.min_neighbors;
              delete clonedParams.alpha;
              delete clonedParams.min_similars;
              delete clonedParams.top;
              delete clonedParams.group_property;
              delete clonedParams.min_groups;
              delete clonedParams.with_intermediary;
              delete clonedParams.with_vertex;

              Object.keys(clonedParams).forEach((key) => {
                algorithmAnalyzerStore.mutateModelSimilarityParams(
                  key as keyof ModelSimilarityParams,
                  clonedParams[key]
                );
              });

              algorithmAnalyzerStore.changeCurrentAlgorithm(
                Algorithm.modelSimilarity
              );

              return;
            }
            case 'neighborrank': {
              algorithmAnalyzerStore.changeCurrentAlgorithm(
                Algorithm.neighborRank
              );

              const clonedParams = cloneDeep(params) as NeighborRankParams;

              // remove neighborrank validate rules first;
              algorithmAnalyzerStore.removeValidateNeighborRankRule(0);
              clonedParams.steps.forEach(({ labels }, index) => {
                clonedParams.steps[index].uuid = v4();
                // add neighborrank rule validation per each
                algorithmAnalyzerStore.addValidateNeighborRankRule();

                if (isEmpty(labels)) {
                  clonedParams.steps[index].labels = ['__all__'];
                }
              });

              Object.keys(clonedParams).forEach((key) => {
                algorithmAnalyzerStore.mutateNeighborRankParams(
                  key as keyof NeighborRankParams,
                  // @ts-ignore
                  clonedParams[key]
                );
              });

              return;
            }
            case 'kneighbor':
              algorithmAnalyzerStore.changeCurrentAlgorithm(
                Algorithm.kStepNeighbor
              );

              Object.keys(params).forEach((key) => {
                algorithmAnalyzerStore.mutateKStepNeighborParams(
                  key as keyof KStepNeighbor,
                  params[key]
                );
              });

              return;
            case 'kout':
              algorithmAnalyzerStore.changeCurrentAlgorithm(Algorithm.kHop);

              Object.keys(params).forEach((key) => {
                algorithmAnalyzerStore.mutateKHopParams(
                  key as keyof KHop,
                  params[key]
                );
              });

              return;
            case 'customizedpaths':
              const clonedParams = cloneDeep(params);

              if (isEmpty(params.sources.ids)) {
                clonedParams.method = 'property';
              } else {
                clonedParams.method = 'id';
              }
              clonedParams.source = clonedParams.sources.ids.join(',');
              clonedParams.vertexType = clonedParams.sources.label;
              clonedParams.vertexProperty = Object.entries(
                clonedParams.sources.properties
              );

              if (isEmpty(clonedParams.vertexProperty)) {
                clonedParams.vertexProperty = [['', '']];
              } else {
                clonedParams.vertexProperty = clonedParams.vertexProperty.map(
                  ([key, value]: [string, string[]]) => [key, value.join(',')]
                );

                clonedParams.vertexProperty = Object.keys(
                  clonedParams.vertexProperty
                ).map((key) => [
                  key,
                  clonedParams.vertexProperty[key].join(',')
                ]);
              }

              clonedParams.steps.forEach((step: any, index: number) => {
                const { labels, properties, weight_by, default_weight } = step;
                clonedParams.steps[index].uuid = v4();

                if (isEmpty(labels)) {
                  step.labels = dataAnalyzeStore.edgeTypes.map(
                    ({ name }) => name
                  );
                }

                if (isEmpty(properties)) {
                  step.properties = [['', '']];
                } else {
                  step.properties = Object.keys(step.properties).map((key) => [
                    key,
                    step.properties[key].join(',')
                  ]);
                }

                if (clonedParams.sort_by === 'NONE') {
                  step.weight_by = '';
                  step.default_weight = '';
                } else {
                  if (!isNull(weight_by)) {
                    step.default_weight = '';
                  } else {
                    // custom weight
                    step.weight_by = '__CUSTOM_WEIGHT__';
                    step.default_weight = default_weight;
                  }
                }
              });

              delete clonedParams.sources;

              Object.keys(clonedParams).forEach((key) => {
                algorithmAnalyzerStore.mutateCustomPathParams(
                  key as keyof CustomPathParams,
                  clonedParams[key]
                );
              });

              algorithmAnalyzerStore.changeCurrentAlgorithm(
                Algorithm.customPath
              );

              return;
            case 'rays':
              algorithmAnalyzerStore.changeCurrentAlgorithm(
                Algorithm.radiographicInspection
              );

              Object.keys(params).forEach((key) => {
                algorithmAnalyzerStore.mutateRadiographicInspectionParams(
                  key as keyof RadiographicInspection,
                  params[key]
                );
              });

              return;
            case 'sameneighbors':
              algorithmAnalyzerStore.changeCurrentAlgorithm(
                Algorithm.sameNeighbor
              );

              Object.keys(params).forEach((key) => {
                algorithmAnalyzerStore.mutateSameNeighborParams(
                  key as keyof SameNeighbor,
                  params[key]
                );
              });

              return;
            case 'weightedshortpath':
              algorithmAnalyzerStore.changeCurrentAlgorithm(
                Algorithm.weightedShortestPath
              );

              Object.keys(params).forEach((key) => {
                algorithmAnalyzerStore.mutateWeightedShortestPathParams(
                  key as keyof WeightedShortestPath,
                  params[key]
                );
              });

              return;
            case 'singleshortpath':
              algorithmAnalyzerStore.changeCurrentAlgorithm(
                Algorithm.singleSourceWeightedShortestPath
              );

              Object.keys(params).forEach((key) => {
                algorithmAnalyzerStore.mutateSingleSourceWeightedShortestPathParams(
                  key as keyof SingleSourceWeightedShortestPath,
                  params[key]
                );
              });

              return;
            case 'jaccardsimilarity':
              algorithmAnalyzerStore.changeCurrentAlgorithm(Algorithm.jaccard);

              Object.keys(params).forEach((key) => {
                algorithmAnalyzerStore.mutateJaccardParams(
                  key as keyof Jaccard,
                  params[key]
                );
              });

              return;
            case 'personalrank':
              algorithmAnalyzerStore.changeCurrentAlgorithm(
                Algorithm.personalRankRecommendation
              );

              Object.keys(params).forEach((key) => {
                algorithmAnalyzerStore.mutatePersonalRankParams(
                  key as keyof PersonalRank,
                  params[key]
                );
              });

              return;
            default:
              return;
          }
        }

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
          {t('addition.operate.execution-record')}
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
          {t('addition.operate.favorite-queries')}
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
                    placeholder={t('addition.operate.favorite-search-desc')}
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
                          <img
                            src={EmptyIcon}
                            alt={t('addition.common.no-matching-results')}
                          />
                          <div style={{ fontSize: 14, color: '#333' }}>
                            {t('addition.common.no-matching-results')}
                          </div>
                        </>
                      ) : (
                        t('addition.common.no-data')
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
  algorithmName?: string;
}> = observer(({ type, content, highlightText, algorithmName }) => {
  const dataAnalyzeStore = useContext(DataAnalyzeStoreContext);
  const { t, i18n } = useTranslation();
  const [isExpand, switchExpand] = useState(dataAnalyzeStore.isSearched.status);

  const statements =
    type === 'ALGORITHM'
      ? formatAlgorithmStatement(content, algorithmName, t, i18n)
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
        alt={t('addition.operate.expand-collapse')}
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
    const { t } = useTranslation();
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
          <span>{t('addition.common.del-comfirm')}</span>
          <span>{t('addition.operate.favorite-del-desc')}</span>
          <div className="delete-confirm-footer">
            <Button
              type="primary"
              size="medium"
              style={{ width: 60 }}
              onClick={handleDelete}
            >
              {t('addition.common.del')}
            </Button>
            <Button
              size="medium"
              style={{
                marginLeft: 12,
                width: 60
              }}
              onClick={handleCancel}
            >
              {t('addition.common.cancel')}
            </Button>
          </div>
        </div>
      </div>
    );
  }
);

export default ExecLogAndQueryCollections;
