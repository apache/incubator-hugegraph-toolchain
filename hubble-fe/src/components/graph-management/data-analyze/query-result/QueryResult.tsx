import React, { useContext } from 'react';
import { observer } from 'mobx-react';
import classnames from 'classnames';

import GraphQueryResult from './GraphQueryResult';
import TableQueryResult from './TableQueryResult';
import JSONQueryResult from './JSONQueryResult';
import { DataAnalyzeStoreContext } from '../../../../stores';
import EmptyIcon from '../../../../assets/imgs/ic_sousuo_empty.svg';
import LoadingBackIcon from '../../../../assets/imgs/ic_loading_back.svg';
import LoadingFrontIcon from '../../../../assets/imgs/ic_loading_front.svg';
import FailedIcon from '../../../../assets/imgs/ic_fail.svg';

export interface QueryResultProps {
  sidebarIndex: number;
  handleSetSidebarIndex: (index: number) => void;
}

const dataAnalyzeContentSidebarOptions = ['图', '表格', 'Json'];

const QueryResult: React.FC<QueryResultProps> = observer(
  ({ sidebarIndex, handleSetSidebarIndex }) => {
    const dataAnalyzeStore = useContext(DataAnalyzeStoreContext);

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

    return (
      <div className={queryResutlClassName}>
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
            (dataAnalyzeStore.graphData.data.graph_view.vertices !== null &&
              dataAnalyzeStore.graphData.data.graph_view.edges !== null) && (
              <GraphQueryResult hidden={sidebarIndex !== 0} />
            )}

          {dataAnalyzeStore.requestStatus.fetchGraphs === 'success' &&
            sidebarIndex === 0 &&
            (dataAnalyzeStore.graphData.data.graph_view.vertices === null ||
              dataAnalyzeStore.graphData.data.graph_view.edges === null) && (
              <div className="query-result-content-empty">
                <img src={EmptyIcon} alt="无图结果，请查看表格或Json数据" />
                <span>无图结果，请查看表格或Json数据</span>
              </div>
            )}

          {dataAnalyzeStore.requestStatus.fetchGraphs !== 'success' && (
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
        </div>
      </div>
    );
  }
);

export default QueryResult;
