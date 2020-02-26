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
import { Button, Tooltip, Alert } from '@baidu/one-ui';
import TooltipTrigger from 'react-popper-tooltip';

import 'codemirror/lib/codemirror.css';
import 'react-popper-tooltip/dist/styles.css';
import 'codemirror/addon/display/placeholder';

import Favorite from './common/Favorite';
import { DataAnalyzeStoreContext } from '../../../stores';
import ArrowIcon from '../../../assets/imgs/ic_arrow_16.svg';

const styles = {
  primaryButton: {
    width: 72,
    marginRight: 12
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

  const handleCodeExpandChange = useCallback(
    (flag: boolean) => () => {
      switchCodeExpand(flag);
    },
    []
  );

  const handleQueryExecution = useCallback(async () => {
    if (codeEditor.current) {
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
          Gremlin 查询
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
            <Tooltip
              placement="bottom"
              title={
                dataAnalyzeStore.codeEditorText.length === 0
                  ? '查询语句不能为空'
                  : ''
              }
              type="dark"
            >
              <Button
                type="primary"
                style={styles.primaryButton}
                disabled={
                  dataAnalyzeStore.codeEditorText.length === 0 ||
                  !codeRegexp.test(dataAnalyzeStore.codeEditorText) ||
                  dataAnalyzeStore.requestStatus.fetchGraphs === 'pending'
                }
                onClick={handleQueryExecution}
              >
                {dataAnalyzeStore.requestStatus.fetchGraphs === 'pending'
                  ? '执行中'
                  : '执行'}
              </Button>
            </Tooltip>
            {dataAnalyzeStore.codeEditorText.length !== 0 ? (
              <TooltipTrigger
                tooltipShown={dataAnalyzeStore.favoritePopUp === 'addFavorite'}
                placement="bottom-start"
                tooltip={({
                  arrowRef,
                  tooltipRef,
                  getArrowProps,
                  getTooltipProps,
                  placement
                }) => (
                  <div
                    {...getTooltipProps({
                      ref: tooltipRef,
                      className: 'tooltips',
                      style: {
                        zIndex: 7
                      }
                    })}
                  >
                    <div
                      {...getArrowProps({
                        ref: arrowRef,
                        className: 'tooltip-arrow',
                        'data-placement': placement
                      })}
                    />
                    <Favorite handlePop={switchFavoritePop} />
                  </div>
                )}
              >
                {({ getTriggerProps, triggerRef }) => (
                  <div
                    {...getTriggerProps({
                      ref: triggerRef
                    })}
                  >
                    <Button
                      style={styles.primaryButton}
                      disabled={
                        !codeRegexp.test(dataAnalyzeStore.codeEditorText)
                      }
                      onClick={() => {
                        dataAnalyzeStore.setFavoritePopUp('addFavorite');
                        dataAnalyzeStore.resetFavoriteRequestStatus('add');
                        dataAnalyzeStore.resetFavoriteRequestStatus('edit');
                        switchFavoritePop(true);
                      }}
                    >
                      收藏
                    </Button>
                  </div>
                )}
              </TooltipTrigger>
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
