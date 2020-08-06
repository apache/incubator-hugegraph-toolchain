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
import classnames from 'classnames';
import { Button, Tooltip, Alert, Dropdown } from '@baidu/one-ui';
import TooltipTrigger from 'react-popper-tooltip';

import 'codemirror/lib/codemirror.css';
import 'react-popper-tooltip/dist/styles.css';
import 'codemirror/addon/display/placeholder';

import { Tooltip as CustomTooltip } from '../../common';
import Favorite from './common/Favorite';
import { DataAnalyzeStoreContext } from '../../../stores';
import { useMultiKeyPress } from '../../../hooks';

import ArrowIcon from '../../../assets/imgs/ic_arrow_16.svg';
import QuestionMarkIcon from '../../../assets/imgs/ic_question_mark.svg';

const styles = {
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
  const [tabIndex, setTabIndex] = useState(0);
  const [isCodeExpand, switchCodeExpand] = useState(true);
  const [isFavoritePop, switchFavoritePop] = useState(false);
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
    if (keyPressed.has('Tab') && codeEditor.current) {
      codeEditor.current.focus();
    }

    if (keyPressed.size === 2 && keyPressed.has('Enter') && !isDisabledExec) {
      if (isQueryShortcut()) {
        handleQueryExecution();
      }
    }
  }, [keyPressed]);

  const codeEditWrapperClassName = classnames({
    'query-tab-code-edit': true,
    hide: !isCodeExpand,
    isLoading: dataAnalyzeStore.requestStatus.fetchGraphs === 'pending'
  });

  return (
    <>
      <div className="query-tab-index-wrapper">
        <div
          onClick={() => {
            setTabIndex(0);
          }}
          className={
            tabIndex === 0 ? 'query-tab-index active' : 'query-tab-index'
          }
        >
          Gremlin分析
        </div>
      </div>
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
                    ? '执行中'
                    : dataAnalyzeStore.queryMode === 'query'
                    ? '执行查询'
                    : '执行任务'
                }
                onHandleMenuClick={(e: any) => {
                  dataAnalyzeStore.setQueryMode(e.key);
                }}
                onClickButton={handleQueryExecution}
                size="small"
                type="primary"
                primaryType="primary"
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
              disabled={
                dataAnalyzeStore.requestStatus.fetchGraphs === 'pending'
              }
            >
              清空
            </Button>
          </div>
        )}
      </div>
    </>
  );
});

export default QueryAndAlgorithmLibrary;
