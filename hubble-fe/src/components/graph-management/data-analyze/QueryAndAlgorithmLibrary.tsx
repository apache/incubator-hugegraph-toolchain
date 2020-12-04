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
import {
  Button,
  Tooltip,
  Alert,
  Dropdown,
  Input,
  Radio,
  Select
} from '@baidu/one-ui';

import 'codemirror/lib/codemirror.css';
import 'react-popper-tooltip/dist/styles.css';
import 'codemirror/addon/display/placeholder';

import { Tooltip as CustomTooltip } from '../../common';
import Favorite from './common/Favorite';
import { DataAnalyzeStoreContext } from '../../../stores';
import { useMultiKeyPress } from '../../../hooks';

import LoopDetection from './algorithm/LoopDetection';
import FocusDetection from './algorithm/FocusDetection';
import ShortestPathAll from './algorithm/ShortestPathAll';
import AllPath from './algorithm/AllPath';
import ModelSimilarity from './algorithm/ModelSimilarity';
import NeighborRank from './algorithm/NeighborRank';

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

const algorithmWhiteList: string[] = [
  Algorithm.shortestPath,
  Algorithm.loopDetection,
  Algorithm.focusDetection,
  Algorithm.shortestPathAll,
  Algorithm.allPath,
  Algorithm.modelSimilarity,
  Algorithm.neighborRankRecommendation
];

const QueryAndAlgorithmLibrary: React.FC = observer(() => {
  const dataAnalyzeStore = useContext(DataAnalyzeStoreContext);
  const { t } = useTranslation();

  const handleTabChange = (tab: string) => () => {
    dataAnalyzeStore.resetSwitchTabState();

    if (tab !== dataAnalyzeStore.currentTab) {
      dataAnalyzeStore.mutateCodeEditorText('');
    }

    dataAnalyzeStore.setCurrentTab(tab);
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
        placeholder: '请输入查询语句'
      }
    );

    if (codeEditor.current) {
      const handleCodeEditorChange = () => {
        dataAnalyzeStore.mutateCodeEditorText(
          (codeEditor.current as CodeMirror.Editor).getValue()
        );
      };

      codeEditor.current.on('change', handleCodeEditorChange);

      reaction(
        () => dataAnalyzeStore.currentId,
        () => {
          (codeEditor.current as CodeMirror.Editor).setValue('');
        }
      );

      reaction(
        () => dataAnalyzeStore.pulse,
        () => {
          (codeEditor.current as CodeMirror.Editor).setValue(
            dataAnalyzeStore.codeEditorText
          );
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
            <img src={ArrowIcon} alt="展开" />
            <span>收起</span>
          </div>
        ) : (
          <div
            className="query-tab-collpase"
            onClick={handleCodeExpandChange(true)}
          >
            <div>
              <img src={ArrowIcon} alt="展开" />
              <span>展开</span>
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
                ? '查询语句不能为空'
                : '⌘ + Enter'
            }
          >
            <Dropdown.Button
              options={[
                { label: '执行查询', value: 'query' },
                { label: '执行任务', value: 'task' }
              ]}
              trigger={['click']}
              title={
                dataAnalyzeStore.requestStatus.fetchGraphs === 'pending'
                  ? '执行中　'
                  : dataAnalyzeStore.queryMode === 'query'
                  ? '执行查询'
                  : '执行任务'
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
            title="查询模式适合30秒内可返回结果的小规模分析；任务模式适合较长时间返回结果的大规模分析，任务详情可在任务管理中查看"
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
                收藏
              </Button>
            </CustomTooltip>
          ) : (
            <Tooltip
              placement="bottom"
              title={
                dataAnalyzeStore.codeEditorText.length === 0
                  ? '查询语句不能为空'
                  : ''
              }
              type="dark"
            >
              <Button style={styles.primaryButton} disabled={true}>
                收藏
              </Button>
            </Tooltip>
          )}
          <Button
            style={styles.primaryButton}
            onClick={resetCodeEditorText}
            disabled={dataAnalyzeStore.requestStatus.fetchGraphs === 'pending'}
          >
            清空
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
    // disable other algorithm now
    if (algorithmWhiteList.includes(algorithm)) {
      algorithmAnalyzerStore.changeCurrentAlgorithm(algorithm);
    }
  };

  const renderForms = () => {
    switch (algorithmAnalyzerStore.currentAlgorithm) {
      case Algorithm.shortestPath:
        return (
          <div className="query-tab-content-form">
            <div className="query-tab-content-form-row">
              <div className="query-tab-content-form-item">
                <div className="query-tab-content-form-item-title">
                  <i>*</i>
                  <span>
                    {t(
                      'data-analyze.algorithm-forms.shortest-path.options.source'
                    )}
                  </span>
                </div>
                <Input
                  width={400}
                  size="medium"
                  disabled={
                    dataAnalyzeStore.requestStatus.fetchGraphs === 'pending'
                  }
                  placeholder={t(
                    'data-analyze.algorithm-forms.shortest-path.placeholder.input-source-id'
                  )}
                  errorLocation="layer"
                  errorMessage={
                    algorithmAnalyzerStore
                      .validateShortestPathParamsErrorMessage.source
                  }
                  value={
                    algorithmAnalyzerStore.shortestPathAlgorithmParams.source
                  }
                  onChange={(e: any) => {
                    algorithmAnalyzerStore.mutateShortestPathParams(
                      'source',
                      e.value as string
                    );

                    algorithmAnalyzerStore.validateShortestPathParams('source');
                  }}
                  originInputProps={{
                    onBlur() {
                      algorithmAnalyzerStore.validateShortestPathParams(
                        'source'
                      );
                    }
                  }}
                />
              </div>
              <div className="query-tab-content-form-item">
                <div className="query-tab-content-form-item-title">
                  <span>
                    {t(
                      'data-analyze.algorithm-forms.shortest-path.options.label'
                    )}
                  </span>
                </div>
                <Select
                  size="medium"
                  trigger="click"
                  value={
                    algorithmAnalyzerStore.shortestPathAlgorithmParams.label
                  }
                  notFoundContent={t(
                    'data-analyze.algorithm-forms.shortest-path.placeholder.no-edge-types'
                  )}
                  disabled={
                    dataAnalyzeStore.requestStatus.fetchGraphs === 'pending'
                  }
                  width={400}
                  onChange={(value: string) => {
                    algorithmAnalyzerStore.mutateShortestPathParams(
                      'label',
                      value
                    );
                  }}
                >
                  <Select.Option value="__all__" key="__all__">
                    {t('data-analyze.algorithm-forms.shortest-path.pre-value')}
                  </Select.Option>
                  {dataAnalyzeStore.edgeTypes.map(({ name }) => (
                    <Select.Option value={name} key={name}>
                      {name}
                    </Select.Option>
                  ))}
                </Select>
              </div>
            </div>
            <div className="query-tab-content-form-row">
              <div className="query-tab-content-form-item">
                <div className="query-tab-content-form-item-title">
                  <i>*</i>
                  <span>
                    {t(
                      'data-analyze.algorithm-forms.shortest-path.options.target'
                    )}
                  </span>
                </div>
                <Input
                  width={400}
                  size="medium"
                  disabled={
                    dataAnalyzeStore.requestStatus.fetchGraphs === 'pending'
                  }
                  placeholder={t(
                    'data-analyze.algorithm-forms.shortest-path.placeholder.input-target-id'
                  )}
                  errorLocation="layer"
                  errorMessage={
                    algorithmAnalyzerStore
                      .validateShortestPathParamsErrorMessage.target
                  }
                  value={
                    algorithmAnalyzerStore.shortestPathAlgorithmParams.target
                  }
                  onChange={(e: any) => {
                    algorithmAnalyzerStore.mutateShortestPathParams(
                      'target',
                      e.value as string
                    );

                    algorithmAnalyzerStore.validateShortestPathParams('target');
                  }}
                  originInputProps={{
                    onBlur() {
                      algorithmAnalyzerStore.validateShortestPathParams(
                        'target'
                      );
                    }
                  }}
                />
              </div>
              <div className="query-tab-content-form-item">
                <div className="query-tab-content-form-item-title">
                  <span>
                    {t(
                      'data-analyze.algorithm-forms.shortest-path.options.max_degree'
                    )}
                  </span>
                </div>
                <Input
                  width={400}
                  size="medium"
                  disabled={
                    dataAnalyzeStore.requestStatus.fetchGraphs === 'pending'
                  }
                  placeholder={t(
                    'data-analyze.algorithm-forms.shortest-path.placeholder.input-positive-integer'
                  )}
                  errorLocation="layer"
                  errorMessage={
                    algorithmAnalyzerStore
                      .validateShortestPathParamsErrorMessage.max_degree
                  }
                  value={
                    algorithmAnalyzerStore.shortestPathAlgorithmParams
                      .max_degree
                  }
                  onChange={(e: any) => {
                    algorithmAnalyzerStore.mutateShortestPathParams(
                      'max_degree',
                      e.value as string
                    );

                    algorithmAnalyzerStore.validateShortestPathParams(
                      'max_degree'
                    );
                  }}
                  originInputProps={{
                    onBlur() {
                      algorithmAnalyzerStore.validateShortestPathParams(
                        'max_degree'
                      );
                    }
                  }}
                />
              </div>
            </div>
            <div className="query-tab-content-form-row">
              <div className="query-tab-content-form-item">
                <div className="query-tab-content-form-item-title">
                  <i>*</i>
                  <span>
                    {t(
                      'data-analyze.algorithm-forms.shortest-path.options.direction'
                    )}
                  </span>
                </div>
                <Radio.Group
                  disabled={
                    dataAnalyzeStore.requestStatus.fetchGraphs === 'pending'
                  }
                  value={
                    algorithmAnalyzerStore.shortestPathAlgorithmParams.direction
                  }
                  onChange={(e: React.ChangeEvent<HTMLSelectElement>) => {
                    algorithmAnalyzerStore.mutateShortestPathParams(
                      'direction',
                      e.target.value
                    );
                  }}
                >
                  <Radio value="BOTH">both</Radio>
                  <Radio value="OUT">out</Radio>
                  <Radio value="IN">in</Radio>
                </Radio.Group>
              </div>
              <div className="query-tab-content-form-item">
                <div className="query-tab-content-form-item-title">
                  <span>
                    {t(
                      'data-analyze.algorithm-forms.shortest-path.options.skip_degree'
                    )}
                  </span>
                  <CustomTooltip
                    trigger="hover"
                    placement="bottom-start"
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
                    tooltipWrapper={t(
                      'data-analyze.algorithm-forms.shortest-path.hint.skip-degree'
                    )}
                    childrenProps={{
                      src: QuestionMarkIcon,
                      alt: 'hint',
                      style: {
                        marginLeft: 5
                      }
                    }}
                    childrenWrapperElement="img"
                  />
                </div>
                <Input
                  width={400}
                  size="medium"
                  disabled={
                    dataAnalyzeStore.requestStatus.fetchGraphs === 'pending'
                  }
                  placeholder={t(
                    'data-analyze.algorithm-forms.shortest-path.placeholder.input-integer'
                  )}
                  errorLocation="layer"
                  errorMessage={
                    algorithmAnalyzerStore
                      .validateShortestPathParamsErrorMessage.skip_degree
                  }
                  value={
                    algorithmAnalyzerStore.shortestPathAlgorithmParams
                      .skip_degree
                  }
                  onChange={(e: any) => {
                    algorithmAnalyzerStore.mutateShortestPathParams(
                      'skip_degree',
                      e.value as string
                    );

                    algorithmAnalyzerStore.validateShortestPathParams(
                      'skip_degree'
                    );
                  }}
                  originInputProps={{
                    onBlur() {
                      algorithmAnalyzerStore.validateShortestPathParams(
                        'skip_degree'
                      );
                    }
                  }}
                />
              </div>
            </div>
            <div className="query-tab-content-form-row">
              <div className="query-tab-content-form-item">
                <div className="query-tab-content-form-item-title">
                  <i>*</i>
                  <span>
                    {t(
                      'data-analyze.algorithm-forms.shortest-path.options.max_depth'
                    )}
                  </span>
                  <CustomTooltip
                    trigger="hover"
                    placement="bottom-start"
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
                    tooltipWrapper={t(
                      'data-analyze.algorithm-forms.shortest-path.hint.max-depth'
                    )}
                    childrenProps={{
                      src: QuestionMarkIcon,
                      alt: 'hint',
                      style: {
                        marginLeft: 5
                      }
                    }}
                    childrenWrapperElement="img"
                  />
                </div>
                <Input
                  width={400}
                  size="medium"
                  disabled={
                    dataAnalyzeStore.requestStatus.fetchGraphs === 'pending'
                  }
                  placeholder={t(
                    'data-analyze.algorithm-forms.shortest-path.placeholder.input-positive-integer'
                  )}
                  errorLocation="layer"
                  errorMessage={
                    algorithmAnalyzerStore
                      .validateShortestPathParamsErrorMessage.max_depth
                  }
                  value={
                    algorithmAnalyzerStore.shortestPathAlgorithmParams.max_depth
                  }
                  onChange={(e: any) => {
                    algorithmAnalyzerStore.mutateShortestPathParams(
                      'max_depth',
                      e.value as string
                    );

                    algorithmAnalyzerStore.validateShortestPathParams(
                      'max_depth'
                    );
                  }}
                  originInputProps={{
                    onBlur() {
                      algorithmAnalyzerStore.validateShortestPathParams(
                        'max_depth'
                      );
                    }
                  }}
                />
              </div>
              <div className="query-tab-content-form-item">
                <div className="query-tab-content-form-item-title">
                  <span>
                    {t(
                      'data-analyze.algorithm-forms.shortest-path.options.capacity'
                    )}
                  </span>
                </div>
                <Input
                  width={400}
                  size="medium"
                  disabled={
                    dataAnalyzeStore.requestStatus.fetchGraphs === 'pending'
                  }
                  placeholder={t(
                    'data-analyze.algorithm-forms.shortest-path.placeholder.input-positive-integer'
                  )}
                  errorLocation="layer"
                  errorMessage={
                    algorithmAnalyzerStore
                      .validateShortestPathParamsErrorMessage.capacity
                  }
                  value={
                    algorithmAnalyzerStore.shortestPathAlgorithmParams.capacity
                  }
                  onChange={(e: any) => {
                    algorithmAnalyzerStore.mutateShortestPathParams(
                      'capacity',
                      e.value as string
                    );

                    algorithmAnalyzerStore.validateShortestPathParams(
                      'capacity'
                    );
                  }}
                  originInputProps={{
                    onBlur() {
                      algorithmAnalyzerStore.validateShortestPathParams(
                        'capacity'
                      );
                    }
                  }}
                />
              </div>
            </div>
            <div
              className="query-tab-content-form-row"
              style={{ marginLeft: 92, justifyContent: 'flex-start' }}
            >
              <Button
                type="primary"
                style={styles.primaryButton}
                disabled={
                  !isValidExec ||
                  dataAnalyzeStore.requestStatus.fetchGraphs === 'pending'
                }
                onClick={async () => {
                  algorithmAnalyzerStore.switchCollapse(true);
                  dataAnalyzeStore.switchGraphLoaded(false);

                  const timerId = dataAnalyzeStore.addTempExecLog();
                  await dataAnalyzeStore.fetchGraphs({
                    url: 'shortpath',
                    type: Algorithm.shortestPath
                  });
                  await dataAnalyzeStore.fetchExecutionLogs();
                  window.clearTimeout(timerId);
                }}
              >
                {t('data-analyze.manipulations.execution')}
              </Button>
              <Button
                style={styles.primaryButton}
                disabled={
                  dataAnalyzeStore.requestStatus.fetchGraphs === 'pending'
                }
                onClick={() => {
                  algorithmAnalyzerStore.resetShortestPathParams();
                  // temp solution
                  algorithmAnalyzerStore.mutateShortestPathParams(
                    'label',
                    '__all__'
                  );
                }}
              >
                {t('data-analyze.manipulations.reset')}
              </Button>
            </div>
          </div>
        );
      case Algorithm.loopDetection:
        return <LoopDetection />;
      case Algorithm.focusDetection:
        return <FocusDetection />;
      case Algorithm.shortestPathAll:
        return <ShortestPathAll />;
      case Algorithm.allPath:
        return <AllPath />;
      case Algorithm.modelSimilarity:
        return <ModelSimilarity />;
      case Algorithm.neighborRankRecommendation:
        return <NeighborRank />;
    }
  };

  useEffect(() => {
    return () => {
      algorithmAnalyzerStore.dispose();
    };
  }, []);

  return (
    <div className="query-tab-content-wrapper">
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
                algorithmAnalyzerStore.dispose();
              }}
            />
            <span>
              {t(
                `data-analyze.algorithm-list.${algorithmAnalyzerStore.currentAlgorithm}`
              )}
            </span>
          </div>
        )}
        <img
          src={ArrowIcon}
          alt="expand-collpase"
          style={{
            transform: algorithmAnalyzerStore.isCollapse
              ? 'rotate(0deg)'
              : 'rotate(180deg)'
          }}
          onClick={() => {
            algorithmAnalyzerStore.switchCollapse(
              !algorithmAnalyzerStore.isCollapse
            );
          }}
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
              <span
                className={
                  algorithmWhiteList.includes(algorithm)
                    ? ''
                    : 'query-tab-content-menu-item-disabled'
                }
                onClick={handleChangeAlgorithm(algorithm)}
              >
                {t(`data-analyze.algorithm-list.${algorithm}`)}
              </span>
            ))}
          </div>
          <div className="query-tab-content-menu">
            {[
              Algorithm.modelSimilarity,
              Algorithm.neighborRankRecommendation,
              Algorithm.realTimeRecommendation,
              Algorithm.kStepNeighbor,
              Algorithm.kHop
            ].map((algorithm) => (
              <span
                className={
                  algorithmWhiteList.includes(algorithm)
                    ? ''
                    : 'query-tab-content-menu-item-disabled'
                }
                onClick={handleChangeAlgorithm(algorithm)}
              >
                {t(`data-analyze.algorithm-list.${algorithm}`)}
              </span>
            ))}
          </div>
          <div className="query-tab-content-menu">
            {[
              Algorithm.customPath,
              Algorithm.customIntersectionDetection,
              Algorithm.radiographicInspection,
              Algorithm.commonNeighbor,
              Algorithm.weightedShortestPath
            ].map((algorithm) => (
              <span
                className={
                  algorithmWhiteList.includes(algorithm)
                    ? ''
                    : 'query-tab-content-menu-item-disabled'
                }
                onClick={handleChangeAlgorithm(algorithm)}
              >
                {t(`data-analyze.algorithm-list.${algorithm}`)}
              </span>
            ))}
          </div>
          <div className="query-tab-content-menu">
            {[
              Algorithm.singleSourceWeightedPath,
              Algorithm.jaccardSimilarity,
              Algorithm.personalRankRecommendation
            ].map((algorithm) => (
              <span
                className={
                  algorithmWhiteList.includes(algorithm)
                    ? ''
                    : 'query-tab-content-menu-item-disabled'
                }
                onClick={() => {}}
              >
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
