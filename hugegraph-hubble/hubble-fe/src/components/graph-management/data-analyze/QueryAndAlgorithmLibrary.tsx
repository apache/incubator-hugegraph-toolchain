import React, {
  useState,
  useRef,
  useEffect,
  useContext,
  useCallback
} from 'react';
import { reaction } from 'mobx';
import { observer } from 'mobx-react';
import CodeMirror from 'codemirror';
import { useTranslation } from 'react-i18next';
import classnames from 'classnames';
import { Button, Tooltip, Alert, Dropdown } from 'hubble-ui';

import 'codemirror/lib/codemirror.css';
import 'react-popper-tooltip/dist/styles.css';
import 'codemirror/addon/display/placeholder';

import { Tooltip as CustomTooltip } from '../../common';
import Favorite from './common/Favorite';
import { DataAnalyzeStoreContext } from '../../../stores';
import { useMultiKeyPress } from '../../../hooks';

import LoopDetection from './algorithm/LoopDetection';
import FocusDetection from './algorithm/FocusDetection';
import ShortestPath from './algorithm/ShortestPath';
import ShortestPathAll from './algorithm/ShortestPathAll';
import AllPath from './algorithm/AllPath';
import ModelSimilarity from './algorithm/ModelSimilarity';
import NeighborRank from './algorithm/NeighborRank';
import KStepNeighbor from './algorithm/KStepNeighbor';
import KHop from './algorithm/KHop';
import CustomPath from './algorithm/CustomPath';
import RadiographicInspection from './algorithm/RadiographicInspection';
import SameNeighbor from './algorithm/SameNeighbor';
import WeightedShortestPath from './algorithm/WeightedShortestPath';
import SingleSourceWeightedShortestPath from './algorithm/SingleSourceWeightedShortestPath';
import Jaccard from './algorithm/Jaccard';
import PersonalRank from './algorithm/PersonalRank';

import ArrowIcon from '../../../assets/imgs/ic_arrow_16.svg';
import QuestionMarkIcon from '../../../assets/imgs/ic_question_mark.svg';
import { Algorithm } from '../../../stores/factory/dataAnalyzeStore/algorithmStore';

export const styles = {
  primaryButton: {
    width: 72,
    marginLeft: 12
  },
  alert: {
    margin: '16px 0'
  }
};

const codeRegexp = /[A-Za-z0-9]+/;

const QueryAndAlgorithmLibrary: React.FC = observer(() => {
  const dataAnalyzeStore = useContext(DataAnalyzeStoreContext);
  const { algorithmAnalyzerStore } = dataAnalyzeStore;
  const { t } = useTranslation();

  const handleTabChange = (tab: string) => () => {
    dataAnalyzeStore.resetSwitchTabState();

    if (tab !== dataAnalyzeStore.currentTab) {
      // reset codeEditor value
      dataAnalyzeStore.mutateCodeEditorText('');

      // reset default selection of edge labels
      algorithmAnalyzerStore.mutateCustomPathRuleParams(
        'labels',
        dataAnalyzeStore.edgeTypes.map(({ name }) => name),
        0
      );
    }

    dataAnalyzeStore.setCurrentTab(tab);
    // need manually set codeMirror text value to empty here
    dataAnalyzeStore.codeEditorInstance?.setValue('');
    // reset algorithm tab to list
    algorithmAnalyzerStore.changeCurrentAlgorithm('');
    algorithmAnalyzerStore.switchCollapse(false);
  };

  return (
    <>
      <div className="query-tab-index-wrapper">
        <div
          onClick={handleTabChange('gremlin-analyze')}
          className={
            dataAnalyzeStore.currentTab === 'gremlin-analyze'
              ? 'query-tab-index active'
              : 'query-tab-index'
          }
        >
          {t('data-analyze.category.gremlin-analyze')}
        </div>
        <div
          onClick={handleTabChange('algorithm-analyze')}
          className={
            dataAnalyzeStore.currentTab === 'algorithm-analyze'
              ? 'query-tab-index active'
              : 'query-tab-index'
          }
        >
          {t('data-analyze.category.algorithm-analyze')}
        </div>
      </div>
      {dataAnalyzeStore.currentTab === 'gremlin-analyze' && <GremlinQuery />}
      {dataAnalyzeStore.currentTab === 'algorithm-analyze' && (
        <AlgorithmQuery />
      )}
    </>
  );
});

export const GremlinQuery: React.FC = observer(() => {
  const dataAnalyzeStore = useContext(DataAnalyzeStoreContext);
  const [isFavoritePop, switchFavoritePop] = useState(false);
  const [isCodeExpand, switchCodeExpand] = useState(true);
  const codeContainer = useRef<HTMLTextAreaElement>(null);
  const codeEditor = useRef<CodeMirror.Editor>();
  const keyPressed = useMultiKeyPress();
  const { t } = useTranslation();
  const isDisabledExec =
    dataAnalyzeStore.codeEditorText.length === 0 ||
    !codeRegexp.test(dataAnalyzeStore.codeEditorText) ||
    dataAnalyzeStore.requestStatus.fetchGraphs === 'pending';

  const isQueryShortcut = () => {
    const isMacOS = navigator.platform.includes('Mac');

    if (isMacOS) {
      return keyPressed.has('MetaLeft') || keyPressed.has('MetaRight');
    } else {
      return (
        keyPressed.has('Control') ||
        keyPressed.has('ControlLeft') ||
        keyPressed.has('ControlRight')
      );
    }
  };

  const handleCodeExpandChange = useCallback(
    (flag: boolean) => () => {
      switchCodeExpand(flag);
    },
    []
  );

  const handleQueryExecution = useCallback(async () => {
    if (codeEditor.current) {
      if (dataAnalyzeStore.queryMode === 'query') {
        // graph reload
        dataAnalyzeStore.switchGraphLoaded(false);
        // remove graph data filter board
        dataAnalyzeStore.switchShowFilterBoard(false);
        dataAnalyzeStore.clearFilteredGraphQueryOptions();
        // forbid edit when exec a query
        codeEditor.current.setOption('readOnly', 'nocursor');
        // add temp log into exec log
        const timerId = dataAnalyzeStore.addTempExecLog();

        await dataAnalyzeStore.fetchGraphs();
        codeEditor.current.setOption('readOnly', false);

        // fetch execution logs after query
        await dataAnalyzeStore.fetchExecutionLogs();
        // clear timer after fetching new exec logs
        window.clearTimeout(timerId);
      } else {
        await dataAnalyzeStore.createAsyncTask();
        dataAnalyzeStore.fetchExecutionLogs();
      }
    }
  }, [dataAnalyzeStore]);

  const resetCodeEditorText = useCallback(() => {
    switchFavoritePop(false);
    dataAnalyzeStore.resetFavoriteRequestStatus('add');

    if (codeEditor.current) {
      codeEditor.current.setValue('');
      dataAnalyzeStore.mutateCodeEditorText('');
    }
  }, [dataAnalyzeStore]);

  const codeEditWrapperClassName = classnames({
    'query-tab-code-edit': true,
    hide: !isCodeExpand,
    isLoading: dataAnalyzeStore.requestStatus.fetchGraphs === 'pending'
  });

  useEffect(() => {
    codeEditor.current = CodeMirror.fromTextArea(
      codeContainer.current as HTMLTextAreaElement,
      {
        lineNumbers: true,
        lineWrapping: true,
        placeholder: t('addition.operate.input-query-statement')
      }
    );

    if (codeEditor.current) {
      const handleCodeEditorChange = () => {
        dataAnalyzeStore.mutateCodeEditorText(
          (codeEditor.current as CodeMirror.Editor).getValue()
        );
      };

      dataAnalyzeStore.assignCodeEditorInstance(codeEditor.current);
      codeEditor.current.on('change', handleCodeEditorChange);

      reaction(
        () => dataAnalyzeStore.currentId,
        () => {
          (codeEditor.current as CodeMirror.Editor).setValue('');
        }
      );

      return () => {
        (codeEditor.current as CodeMirror.Editor).off(
          'change',
          handleCodeEditorChange
        );
      };
    }
  }, [dataAnalyzeStore]);

  // weird, mobx@reaction is not working when puluse changed, setValue()
  // has no influence
  useEffect(() => {
    if (codeEditor?.current && dataAnalyzeStore.codeEditorText !== '') {
      (codeEditor.current as CodeMirror.Editor).setValue(
        dataAnalyzeStore.codeEditorText
      );
    }
  }, [dataAnalyzeStore.pulse]);
  // }, [dataAnalyzeStore.pulse, codeEditor?.current]);

  useEffect(() => {
    if (
      keyPressed.has('Tab') &&
      codeEditor.current &&
      // disable shortcut when drawer pops
      !dataAnalyzeStore.isShowGraphInfo &&
      dataAnalyzeStore.dynamicAddGraphDataStatus === ''
    ) {
      codeEditor.current.focus();
    }

    if (
      keyPressed.size === 2 &&
      keyPressed.has('Enter') &&
      !isDisabledExec &&
      // disable shortcut when drawer pops
      !dataAnalyzeStore.isShowGraphInfo &&
      dataAnalyzeStore.dynamicAddGraphDataStatus === ''
    ) {
      if (isQueryShortcut()) {
        handleQueryExecution();
      }
    }
  }, [keyPressed]);

  return (
    <div className="query-tab-content-wrapper">
      <div className="query-tab-content">
        <Tooltip placement="bottomLeft" title="" type="dark">
          <div className={codeEditWrapperClassName}>
            <textarea
              className="query-tab-code-editor"
              ref={codeContainer}
            ></textarea>
          </div>
        </Tooltip>

        {isCodeExpand ? (
          <div
            className="query-tab-expand"
            onClick={handleCodeExpandChange(false)}
          >
            <img src={ArrowIcon} alt={t('addition.operate.expand')} />
            <span>{t('addition.operate.collapse')}</span>
          </div>
        ) : (
          <div
            className="query-tab-collpase"
            onClick={handleCodeExpandChange(true)}
          >
            <div>
              <img src={ArrowIcon} alt={t('addition.operate.collapse')} />
              <span>{t('addition.operate.expand')}</span>
            </div>
          </div>
        )}
      </div>

      {isCodeExpand &&
        dataAnalyzeStore.requestStatus.fetchGraphs === 'failed' &&
        dataAnalyzeStore.errorInfo.fetchGraphs.code === 460 && (
          <Alert
            content={dataAnalyzeStore.errorInfo.fetchGraphs.message}
            type="error"
            showIcon
            style={styles.alert}
          />
        )}

      {isCodeExpand && (
        <div className="query-tab-manipulations">
          <CustomTooltip
            trigger="hover"
            placement="bottom"
            modifiers={{
              offset: {
                offset: '0, 8'
              }
            }}
            tooltipWrapperProps={{
              className: 'tooltips-dark',
              style: {
                zIndex: 7
              }
            }}
            tooltipWrapper={
              dataAnalyzeStore.codeEditorText.length === 0
                ? t('addition.operate.execute-query')
                : '⌘ + Enter'
            }
          >
            <Dropdown.Button
              options={[
                { label: t('addition.operate.execute-query'), value: 'query' },
                { label: t('addition.operate.execute-task'), value: 'task' }
              ]}
              trigger={['click']}
              title={
                dataAnalyzeStore.requestStatus.fetchGraphs === 'pending'
                  ? t('addition.operate.execute-ing')
                  : dataAnalyzeStore.queryMode === 'query'
                  ? t('addition.operate.execute-query')
                  : t('addition.operate.execute-task')
              }
              onHandleMenuClick={(e: any) => {
                dataAnalyzeStore.setQueryMode(e.key);
              }}
              overlayClassName="improve-zindex"
              onClickButton={handleQueryExecution}
              size="small"
              type="primary"
              disabled={
                dataAnalyzeStore.codeEditorText.length === 0 ||
                !codeRegexp.test(dataAnalyzeStore.codeEditorText) ||
                dataAnalyzeStore.requestStatus.fetchGraphs === 'pending'
              }
            />
          </CustomTooltip>
          <Tooltip
            placement="bottomLeft"
            title={t('addition.operate.query-result-desc')}
            type="dark"
          >
            <div style={{ marginLeft: 6, paddingTop: 6 }}>
              <img src={QuestionMarkIcon} alt="tips" />
            </div>
          </Tooltip>
          {dataAnalyzeStore.codeEditorText.length !== 0 ? (
            <CustomTooltip
              placement="bottom-start"
              tooltipShown={dataAnalyzeStore.favoritePopUp === 'addFavorite'}
              modifiers={{
                offset: {
                  offset: '0, 10'
                }
              }}
              tooltipWrapperProps={{
                className: 'tooltips',
                style: {
                  zIndex: 7
                }
              }}
              tooltipWrapper={<Favorite handlePop={switchFavoritePop} />}
              childrenWrapperElement="div"
            >
              <Button
                style={styles.primaryButton}
                disabled={!codeRegexp.test(dataAnalyzeStore.codeEditorText)}
                onClick={() => {
                  dataAnalyzeStore.setFavoritePopUp('addFavorite');
                  dataAnalyzeStore.resetFavoriteRequestStatus('add');
                  dataAnalyzeStore.resetFavoriteRequestStatus('edit');
                  switchFavoritePop(true);
                }}
              >
                {t('addition.operate.favorite')}
              </Button>
            </CustomTooltip>
          ) : (
            <Tooltip
              placement="bottom"
              title={
                dataAnalyzeStore.codeEditorText.length === 0
                  ? t('addition.operate.query-statement-required')
                  : ''
              }
              type="dark"
            >
              <Button style={styles.primaryButton} disabled={true}>
                {t('addition.operate.favorite')}
              </Button>
            </Tooltip>
          )}
          <Button
            style={styles.primaryButton}
            onClick={resetCodeEditorText}
            disabled={dataAnalyzeStore.requestStatus.fetchGraphs === 'pending'}
          >
            {t('addition.operate.clean')}
          </Button>
        </div>
      )}
    </div>
  );
});

export const AlgorithmQuery: React.FC = observer(() => {
  const dataAnalyzeStore = useContext(DataAnalyzeStoreContext);
  const { algorithmAnalyzerStore } = dataAnalyzeStore;
  const { t } = useTranslation();

  const isValidExec =
    Object.values(
      algorithmAnalyzerStore.validateShortestPathParamsErrorMessage
    ).every((value) => value === '') &&
    algorithmAnalyzerStore.shortestPathAlgorithmParams.source !== '' &&
    algorithmAnalyzerStore.shortestPathAlgorithmParams.target !== '' &&
    algorithmAnalyzerStore.shortestPathAlgorithmParams.max_depth !== '';

  const handleChangeAlgorithm = (algorithm: string) => () => {
    algorithmAnalyzerStore.changeCurrentAlgorithm(algorithm);
  };

  const handleExpandClick = () => {
    algorithmAnalyzerStore.switchCollapse(!algorithmAnalyzerStore.isCollapse);
  };

  const renderForms = () => {
    switch (algorithmAnalyzerStore.currentAlgorithm) {
      case Algorithm.loopDetection:
        return <LoopDetection />;
      case Algorithm.focusDetection:
        return <FocusDetection />;
      case Algorithm.shortestPath:
        return <ShortestPath />;
      case Algorithm.shortestPathAll:
        return <ShortestPathAll />;
      case Algorithm.allPath:
        return <AllPath />;
      case Algorithm.modelSimilarity:
        return <ModelSimilarity />;
      case Algorithm.neighborRank:
        return <NeighborRank />;
      case Algorithm.kStepNeighbor:
        return <KStepNeighbor />;
      case Algorithm.kHop:
        return <KHop />;
      case Algorithm.customPath:
        return <CustomPath />;
      case Algorithm.radiographicInspection:
        return <RadiographicInspection />;
      case Algorithm.sameNeighbor:
        return <SameNeighbor />;
      case Algorithm.weightedShortestPath:
        return <WeightedShortestPath />;
      case Algorithm.singleSourceWeightedShortestPath:
        return <SingleSourceWeightedShortestPath />;
      case Algorithm.jaccard:
        return <Jaccard />;
      case Algorithm.personalRankRecommendation:
        return <PersonalRank />;
    }
  };

  useEffect(() => {
    return () => {
      algorithmAnalyzerStore.dispose();
    };
  }, []);

  return (
    <div className="query-tab-algorithm-wrapper">
      <div className="query-tab-content-title">
        {algorithmAnalyzerStore.currentAlgorithm === '' ? (
          <span>{t('data-analyze.algorithm-list.title')}</span>
        ) : (
          <div style={{ display: 'flex', alignItems: 'center' }}>
            <img
              src={ArrowIcon}
              alt="go-back"
              style={{
                transform: 'rotate(90deg)',
                marginRight: 12
              }}
              onClick={() => {
                algorithmAnalyzerStore.switchCollapse(false);
                algorithmAnalyzerStore.changeCurrentAlgorithm('');
              }}
            />
            <span onClick={handleExpandClick} style={{ cursor: 'pointer' }}>
              {t(
                `data-analyze.algorithm-list.${algorithmAnalyzerStore.currentAlgorithm}`
              )}
            </span>
          </div>
        )}
        <div
          style={{ flex: 'auto', height: 24, cursor: 'pointer' }}
          onClick={handleExpandClick}
        ></div>
        <img
          src={ArrowIcon}
          alt="expand-collpase"
          style={{
            transform: algorithmAnalyzerStore.isCollapse
              ? 'rotate(0deg)'
              : 'rotate(180deg)'
          }}
          onClick={handleExpandClick}
        />
      </div>
      {algorithmAnalyzerStore.isCollapse ? null : algorithmAnalyzerStore.currentAlgorithm ===
        '' ? (
        <>
          <div className="query-tab-content-menu">
            {[
              Algorithm.loopDetection,
              Algorithm.focusDetection,
              Algorithm.shortestPath,
              Algorithm.shortestPathAll,
              Algorithm.allPath
            ].map((algorithm) => (
              <span onClick={handleChangeAlgorithm(algorithm)} key={algorithm}>
                {t(`data-analyze.algorithm-list.${algorithm}`)}
              </span>
            ))}
          </div>
          <div className="query-tab-content-menu">
            {[
              Algorithm.modelSimilarity,
              Algorithm.neighborRank,
              Algorithm.kStepNeighbor,
              Algorithm.kHop,
              Algorithm.customPath
            ].map((algorithm) => (
              <span onClick={handleChangeAlgorithm(algorithm)} key={algorithm}>
                {t(`data-analyze.algorithm-list.${algorithm}`)}
              </span>
            ))}
          </div>
          <div className="query-tab-content-menu">
            {[
              Algorithm.radiographicInspection,
              Algorithm.sameNeighbor,
              Algorithm.weightedShortestPath,
              Algorithm.singleSourceWeightedShortestPath,
              Algorithm.jaccard
            ].map((algorithm) => (
              <span onClick={handleChangeAlgorithm(algorithm)} key={algorithm}>
                {t(`data-analyze.algorithm-list.${algorithm}`)}
              </span>
            ))}
          </div>
          <div className="query-tab-content-menu">
            {[Algorithm.personalRankRecommendation].map((algorithm) => (
              <span onClick={handleChangeAlgorithm(algorithm)} key={algorithm}>
                {t(`data-analyze.algorithm-list.${algorithm}`)}
              </span>
            ))}
          </div>
        </>
      ) : (
        renderForms()
      )}
    </div>
  );
});

export default QueryAndAlgorithmLibrary;
