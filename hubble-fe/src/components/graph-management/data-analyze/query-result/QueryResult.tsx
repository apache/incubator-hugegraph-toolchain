import React, { useContext } from 'react';
import { observer } from 'mobx-react';
import { isEmpty } from 'lodash-es';
import { useLocation } from 'wouter';
import classnames from 'classnames';

import GraphQueryResult from './GraphQueryResult';
import TableQueryResult from './TableQueryResult';
import JSONQueryResult from './JSONQueryResult';
import {
  DataAnalyzeStoreContext,
  AsyncTasksStoreContext
} from '../../../../stores';
import {Algorithm} from '../../../../stores/factory/dataAnalyzeStore/algorithmStore';
import EmptyIcon from '../../../../assets/imgs/ic_sousuo_empty.svg';
import LoadingBackIcon from '../../../../assets/imgs/ic_loading_back.svg';
import LoadingFrontIcon from '../../../../assets/imgs/ic_loading_front.svg';
import FinishedIcon from '../../../../assets/imgs/ic_done_144.svg';
import FailedIcon from '../../../../assets/imgs/ic_fail.svg';

export interface QueryResultProps {
  sidebarIndex: number;
  handleSetSidebarIndex: (index: number) => void;
}

const dataAnalyzeContentSidebarOptions = ['图', '表格', 'Json'];

const QueryResult: React.FC<QueryResultProps> = observer(
  ({ sidebarIndex, handleSetSidebarIndex }) => {
    const dataAnalyzeStore = useContext(DataAnalyzeStoreContext);
    const { algorithmAnalyzerStore } = dataAnalyzeStore;
    const asyncTasksStore = useContext(AsyncTasksStoreContext);
    const [, setLocation] = useLocation();

    const renderReuslt = (index: number) => {
      switch (index) {
        case 1:
          return <TableQueryResult />;
        case 2:
          return <JSONQueryResult />;
      }
    };

    const queryResutlClassName = classnames({
      'query-result': true,
      'query-result-fullscreen': dataAnalyzeStore.isFullScreenReuslt
    });

    const dynHeightStyle: Record<string, string> = {};

    if (
      dataAnalyzeStore.currentTab === 'algorithm-analyze' &&
      !algorithmAnalyzerStore.isCollapse
    ) {
      if (algorithmAnalyzerStore.currentAlgorithm === '') {
        dynHeightStyle.height = 'calc(100vh - 441px)';
      }

      if (algorithmAnalyzerStore.currentAlgorithm === Algorithm.shortestPath) {
        dynHeightStyle.height = 'calc(100vh - 555px)';
      }
    }

    return (
      <div className={queryResutlClassName} style={{ ...dynHeightStyle }}>
        {!dataAnalyzeStore.isFullScreenReuslt && (
          <div className="query-result-sidebar">
            {dataAnalyzeContentSidebarOptions.map((text, index) => (
              <div className="query-result-sidebar-options" key={text}>
                <div
                  onClick={() => {
                    handleSetSidebarIndex(index);
                  }}
                  className={
                    sidebarIndex === index
                      ? 'query-result-sidebar-options-active'
                      : ''
                  }
                >
                  <i className={sidebarIndex === index ? 'selected' : ''}></i>
                  <span>{text}</span>
                </div>
              </div>
            ))}
          </div>
        )}
        <div className="query-result-content">
          {dataAnalyzeStore.requestStatus.fetchGraphs === 'success' &&
            renderReuslt(sidebarIndex)}

          {dataAnalyzeStore.requestStatus.fetchGraphs === 'success' &&
            dataAnalyzeStore.graphData.data.graph_view.vertices !== null &&
            dataAnalyzeStore.graphData.data.graph_view.edges !== null &&
            !isEmpty(dataAnalyzeStore.graphData.data.graph_view.vertices) && (
              <GraphQueryResult hidden={sidebarIndex !== 0} />
            )}

          {dataAnalyzeStore.requestStatus.fetchGraphs === 'success' &&
            sidebarIndex === 0 &&
            (dataAnalyzeStore.graphData.data.graph_view.vertices === null ||
              dataAnalyzeStore.graphData.data.graph_view.edges === null ||
              isEmpty(dataAnalyzeStore.graphData.data.graph_view.vertices)) && (
              <div className="query-result-content-empty">
                <img src={EmptyIcon} alt="无图结果，请查看表格或Json数据" />
                <span>无图结果，请查看表格或Json数据</span>
              </div>
            )}

          {dataAnalyzeStore.requestStatus.fetchGraphs !== 'success' &&
            dataAnalyzeStore.requestStatus.createAsyncTask === 'standby' && (
              <div className="query-result-content-empty">
                {dataAnalyzeStore.requestStatus.fetchGraphs === 'standby' && (
                  <>
                    <img src={EmptyIcon} alt="暂无数据结果" />
                    <span>暂无数据结果</span>
                  </>
                )}
                {dataAnalyzeStore.requestStatus.fetchGraphs === 'pending' && (
                  <>
                    <div className="query-result-loading-bg">
                      <img
                        className="query-result-loading-back"
                        src={LoadingBackIcon}
                        alt="加载背景"
                      />
                      <img
                        className="query-result-loading-front"
                        src={LoadingFrontIcon}
                        alt="加载 spinner"
                      />
                    </div>
                    <span>数据加载中...</span>
                  </>
                )}
                {dataAnalyzeStore.requestStatus.fetchGraphs === 'failed' && (
                  <>
                    <img src={FailedIcon} alt="" />

                    {dataAnalyzeStore.errorInfo.fetchGraphs.code !== 460 && (
                      <span>
                        {dataAnalyzeStore.errorInfo.fetchGraphs.message}
                      </span>
                    )}
                  </>
                )}
              </div>
            )}

          {/* exec async tasks */}
          {dataAnalyzeStore.requestStatus.createAsyncTask !== 'standby' && (
            <div className="query-result-content-empty">
              {dataAnalyzeStore.requestStatus.createAsyncTask === 'pending' && (
                <>
                  <div className="query-result-loading-bg">
                    <img
                      className="query-result-loading-back"
                      src={LoadingBackIcon}
                      alt="加载背景"
                    />
                    <img
                      className="query-result-loading-front"
                      src={LoadingFrontIcon}
                      alt="加载 spinner"
                    />
                  </div>
                  <span>提交异步任务中...</span>
                </>
              )}
              {dataAnalyzeStore.requestStatus.createAsyncTask === 'success' && (
                <>
                  <img src={FinishedIcon} alt="提交成功" />
                  <span style={{ marginBottom: 4 }}>提交成功</span>
                  <span style={{ marginBottom: 4 }}>
                    任务ID：{dataAnalyzeStore.executionLogData[0]?.async_id}
                  </span>
                  <span
                    style={{ cursor: 'pointer', color: '#2b65ff' }}
                    onClick={() => {
                      // mock search
                      asyncTasksStore.mutateSearchWords(
                        String(dataAnalyzeStore.executionLogData[0].async_id)
                      );
                      asyncTasksStore.switchSearchedStatus(true);

                      setLocation(
                        `/graph-management/${dataAnalyzeStore.currentId}/async-tasks`
                      );
                    }}
                  >
                    查看
                  </span>
                </>
              )}
              {dataAnalyzeStore.requestStatus.createAsyncTask === 'failed' && (
                <>
                  <img src={FailedIcon} alt="提交失败" />
                  <span>任务提交失败</span>
                </>
              )}
            </div>
          )}
        </div>
      </div>
    );
  }
);

export default QueryResult;
