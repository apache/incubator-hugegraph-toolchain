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
import { Algorithm } from '../../../../stores/factory/dataAnalyzeStore/algorithmStore';
import EmptyIcon from '../../../../assets/imgs/ic_sousuo_empty.svg';
import LoadingBackIcon from '../../../../assets/imgs/ic_loading_back.svg';
import LoadingFrontIcon from '../../../../assets/imgs/ic_loading_front.svg';
import FinishedIcon from '../../../../assets/imgs/ic_done_144.svg';
import FailedIcon from '../../../../assets/imgs/ic_fail.svg';
import i18next from '../../../../i18n';
import { useTranslation } from 'react-i18next';

export interface QueryResultProps {
  sidebarIndex: number;
  handleSetSidebarIndex: (index: number) => void;
}

const dataAnalyzeContentSidebarOptions = [
  i18next.t('addition.menu.chart'),
  i18next.t('addition.menu.table'),
  'Json'
];

const QueryResult: React.FC<QueryResultProps> = observer(
  ({ sidebarIndex, handleSetSidebarIndex }) => {
    const dataAnalyzeStore = useContext(DataAnalyzeStoreContext);
    const { algorithmAnalyzerStore } = dataAnalyzeStore;
    const asyncTasksStore = useContext(AsyncTasksStoreContext);
    const [, setLocation] = useLocation();
    const { t } = useTranslation();

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
                <img
                  src={EmptyIcon}
                  alt={t('addition.message.no-chart-desc')}
                />
                <span>{t('addition.message.no-chart-desc')}</span>
              </div>
            )}

          {dataAnalyzeStore.requestStatus.fetchGraphs !== 'success' &&
            dataAnalyzeStore.requestStatus.createAsyncTask === 'standby' && (
              <div className="query-result-content-empty">
                {dataAnalyzeStore.requestStatus.fetchGraphs === 'standby' && (
                  <>
                    <img
                      src={EmptyIcon}
                      alt={t('addition.message.no-data-desc')}
                    />
                    <span>{t('addition.message.no-data-desc')}</span>
                  </>
                )}
                {dataAnalyzeStore.requestStatus.fetchGraphs === 'pending' && (
                  <>
                    <div className="query-result-loading-bg">
                      <img
                        className="query-result-loading-back"
                        src={LoadingBackIcon}
                        alt={t('addition.operate.load-background')}
                      />
                      <img
                        className="query-result-loading-front"
                        src={LoadingFrontIcon}
                        alt={t('addition.operate.load-spinner')}
                      />
                    </div>
                    <span>{t('addition.message.data-loading')}...</span>
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
                      alt={t('addition.operate.load-background')}
                    />
                    <img
                      className="query-result-loading-front"
                      src={LoadingFrontIcon}
                      alt={t('addition.operate.load-spinner')}
                    />
                  </div>
                  <span>{t('addition.message.submit-async-task')}...</span>
                </>
              )}
              {dataAnalyzeStore.requestStatus.createAsyncTask === 'success' && (
                <>
                  <img
                    src={FinishedIcon}
                    alt={t('addition.message.submit-success')}
                  />
                  <span style={{ marginBottom: 4 }}>
                    {t('addition.message.submit-success')}
                  </span>
                  <span style={{ marginBottom: 4 }}>
                    {t('addition.menu.task-id')}：
                    {dataAnalyzeStore.executionLogData[0]?.async_id}
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
                    {t('addition.operate.look')}
                  </span>
                </>
              )}
              {dataAnalyzeStore.requestStatus.createAsyncTask === 'failed' && (
                <>
                  <img
                    src={FailedIcon}
                    alt={t('addition.message.submit-fail')}
                  />
                  <span>{t('addition.message.task-submit-fail')}</span>
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
