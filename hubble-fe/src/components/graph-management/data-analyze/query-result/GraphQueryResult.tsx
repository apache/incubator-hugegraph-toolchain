import React, {
  useState,
  useContext,
  useEffect,
  useRef,
  useCallback
} from 'react';
import { observer } from 'mobx-react';
import { size, isUndefined, isEmpty } from 'lodash-es';
import { saveAs } from 'file-saver';
import vis from 'vis-network';
import 'vis-network/styles/vis-network.min.css';
import { Message } from '@baidu/one-ui';

import QueryFilterOptions from './QueryFilterOptions';
import GraphPopOver from './GraphPopOver';
import { DataAnalyzeStoreContext } from '../../../../stores';
import { addGraphNodes, addGraphEdges } from '../../../../stores/utils';

import ZoomInIcon from '../../../../assets/imgs/ic_fangda_16.svg';
import ZoomOutIcon from '../../../../assets/imgs/ic_suoxiao_16.svg';
import CenterIcon from '../../../../assets/imgs/ic_middle_16.svg';
import DownloadIcon from '../../../../assets/imgs/ic_xiazai_16.svg';
import FullScreenIcon from '../../../../assets/imgs/ic_quanping_16.svg';
import ResetScreenIcon from '../../../../assets/imgs/ic_tuichuquanping_16.svg';
import LoadingBackIcon from '../../../../assets/imgs/ic_loading_back.svg';
import LoadingFrontIcon from '../../../../assets/imgs/ic_loading_front.svg';
import AddNodeIcon from '../../../../assets/imgs/ic_add_node.svg';

export interface GraphQueryResult {
  hidden: boolean;
}

const GraphQueryResult: React.FC<GraphQueryResult> = observer(({ hidden }) => {
  const dataAnalyzeStore = useContext(DataAnalyzeStoreContext);
  const graphWrapper = useRef<HTMLDivElement>(null);
  const resultWrapper = useRef<HTMLDivElement>(null);
  const legendViewPointWrapper = useRef<HTMLDivElement>(null);
  const legendWrapper = useRef<HTMLDivElement>(null);
  const [showLoadingGraphs, switchShowLoadingGraphs] = useState(true);
  const [isPopover, switchIsPopover] = useState(false);
  const [isAfterDragging, switchAfterDragging] = useState(false);
  const [nodeTooltipX, setNodeToolTipX] = useState(0);
  const [nodeTooltipY, setNodeToolTipY] = useState(0);
  const [legendStep, setLegendStep] = useState(0);
  const [legendWidth, setlegendWitdh] = useState(0);

  const [graph, setGraph] = useState<vis.Network | null>(null);

  const redrawGraphs = useCallback(() => {
    if (graph) {
      const width = getComputedStyle(resultWrapper.current!).width as string;
      const height = getComputedStyle(resultWrapper.current!).height as string;

      graph.setSize(width, height);
      graph.redraw();
    }
  }, [graph]);

  useEffect(() => {
    const width = getComputedStyle(resultWrapper.current!).width as string;
    const height = getComputedStyle(resultWrapper.current!).height as string;

    const graphNodes = new vis.DataSet(dataAnalyzeStore.graphNodes);
    const graphEdges = new vis.DataSet(dataAnalyzeStore.graphEdges);

    if (!graph) {
      const data = {
        nodes: graphNodes,
        edges: graphEdges
      };

      const layout: vis.Options = {
        width,
        height,
        nodes: {
          shape: 'dot'
        },
        edges: {
          arrowStrikethrough: false,
          color: {
            color: 'rgba(92, 115, 230, 0.8)',
            hover: 'rgba(92, 115, 230, 1)',
            highlight: 'rgba(92, 115, 230, 1)'
          },
          scaling: {
            min: 1,
            max: 3,
            label: {
              enabled: false
            }
          }
        },
        interaction: {
          hover: true
        },
        physics: {
          maxVelocity: 50,
          solver: 'forceAtlas2Based',
          timestep: 0.3,
          stabilization: { iterations: 150 }
        }
      };

      // initialize your network!
      if (graphWrapper.current !== null) {
        const network = new vis.Network(graphWrapper!.current, data, layout);

        let timer: number | undefined = undefined;

        network.on('click', ({ nodes, edges }) => {
          // click on node, note that edges(related) also has value
          if (!isEmpty(nodes)) {
            // note: cannot abstract switchClickOn...() and clearTimeout
            // as common callings with node and edge, since click event
            // would be dispatched even if click is not on node and edge
            dataAnalyzeStore.switchClickOnNodeOrEdge(true);
            clearTimeout(timer);

            timer = window.setTimeout(() => {
              const nodeId = nodes[0];
              const node = dataAnalyzeStore.graphData.data.graph_view.vertices.find(
                ({ id }) => id === nodeId
              );

              if (isUndefined(node)) {
                return;
              }

              dataAnalyzeStore.changeSelectedGraphData({
                id: node.id,
                label: node.label,
                properties: node.properties
              });

              dataAnalyzeStore.syncGraphEditableProperties('vertex');
              dataAnalyzeStore.initValidateEditGraphDataPropertiesErrorMessage();

              if (
                dataAnalyzeStore.graphInfoDataSet !== 'node' ||
                !dataAnalyzeStore.isShowGraphInfo
              ) {
                dataAnalyzeStore.switchShowScreeDataSet('node');
                dataAnalyzeStore.switchShowScreenInfo(true);
              }

              // close filter board after click on node
              dataAnalyzeStore.switchShowFilterBoard(false);
              // reset status, or click blank area won't collpase the drawer
              dataAnalyzeStore.switchClickOnNodeOrEdge(false);
            }, 200);

            return;
          }

          // click on edge
          if (!isEmpty(edges)) {
            dataAnalyzeStore.switchClickOnNodeOrEdge(true);
            clearTimeout(timer);

            timer = window.setTimeout(() => {
              const edgeId = edges[0];

              const edge = dataAnalyzeStore.graphData.data.graph_view.edges.find(
                ({ id }) => id === edgeId
              );

              if (isUndefined(edge)) {
                return;
              }

              dataAnalyzeStore.changeSelectedGraphLinkData({
                id: edge.id,
                label: edge.label,
                properties: edge.properties,
                source: edge.source,
                target: edge.target
              });

              dataAnalyzeStore.syncGraphEditableProperties('edge');
              dataAnalyzeStore.initValidateEditGraphDataPropertiesErrorMessage();

              if (
                dataAnalyzeStore.graphInfoDataSet !== 'edge' ||
                !dataAnalyzeStore.isShowGraphInfo
              ) {
                dataAnalyzeStore.switchShowScreeDataSet('edge');
                dataAnalyzeStore.switchShowScreenInfo(true);
              }

              // close filter board after click on edge
              dataAnalyzeStore.switchShowFilterBoard(false);
              // reset status, or click blank area won't collpase the drawer
              dataAnalyzeStore.switchClickOnNodeOrEdge(false);
            }, 200);
          }
        });

        network.on('doubleClick', async ({ nodes }) => {
          clearTimeout(timer);
          dataAnalyzeStore.switchClickOnNodeOrEdge(false);
          if (!isEmpty(nodes)) {
            const nodeId = nodes[0];
            const node = dataAnalyzeStore.graphData.data.graph_view.vertices.find(
              ({ id }) => id === nodeId
            );

            if (!isUndefined(node)) {
              // specific symbol (~undefined) in schema
              if (node.label === '~undefined') {
                Message.info({
                  content: '该顶点是非法顶点，可能是由悬空边导致',
                  size: 'medium',
                  showCloseIcon: false,
                  duration: 1
                });
              }

              if (
                isUndefined(
                  dataAnalyzeStore.vertexTypes.find(
                    ({ name }) => name === node.label
                  )
                )
              ) {
                return;
              }

              await dataAnalyzeStore.expandGraphNode(node.id, node.label);

              if (
                dataAnalyzeStore.requestStatus.expandGraphNode === 'success'
              ) {
                // prompt if there's no extra node
                if (
                  size(
                    dataAnalyzeStore.expandedGraphData.data.graph_view.vertices
                  ) === 0
                ) {
                  if (isEmpty(network.getConnectedNodes(nodeId))) {
                    Message.info({
                      content: '不存在邻接点',
                      size: 'medium',
                      showCloseIcon: false,
                      duration: 1
                    });
                  } else {
                    Message.info({
                      content: '不存在更多邻接点',
                      size: 'medium',
                      showCloseIcon: false,
                      duration: 1
                    });
                  }

                  return;
                }

                addGraphNodes(
                  dataAnalyzeStore.expandedGraphData.data.graph_view.vertices,
                  dataAnalyzeStore.visDataSet?.nodes,
                  dataAnalyzeStore.vertexSizeMappings,
                  dataAnalyzeStore.colorMappings,
                  dataAnalyzeStore.vertexWritingMappings
                );

                addGraphEdges(
                  dataAnalyzeStore.expandedGraphData.data.graph_view.edges,
                  dataAnalyzeStore.visDataSet?.edges,
                  dataAnalyzeStore.edgeColorMappings,
                  dataAnalyzeStore.edgeThicknessMappings,
                  dataAnalyzeStore.edgeWithArrowMappings,
                  dataAnalyzeStore.edgeWritingMappings
                );
              }

              if (dataAnalyzeStore.requestStatus.expandGraphNode === 'failed') {
                Message.error({
                  content: dataAnalyzeStore.errorInfo.expandGraphNode.message,
                  size: 'medium',
                  showCloseIcon: false
                });
              }
            } else {
              Message.error({
                content: dataAnalyzeStore.errorInfo.expandGraphNode.message,
                size: 'medium',
                showCloseIcon: false
              });
            }
          }
        });

        network.on('oncontext', async (e) => {
          // disable default context menu
          e.event.preventDefault();

          // It's weird that sometimes e.nodes is empty when right click on node
          // thus using coordinate to work as expect
          const nodeId = network.getNodeAt(e.pointer.DOM);
          const node = dataAnalyzeStore.graphData.data.graph_view.vertices.find(
            ({ id }) => id === nodeId
          );

          if (!isUndefined(node)) {
            dataAnalyzeStore.changeRightClickedGraphData({
              id: node.id,
              label: node.label,
              properties: node.properties
            });

            switchIsPopover(true);

            network.selectNodes([nodeId]);
            setNodeToolTipX(e.pointer.DOM.x);
            setNodeToolTipY(e.pointer.DOM.y);
            dataAnalyzeStore.setVisCurrentCoordinates({
              domX: e.pointer.DOM.x,
              domY: e.pointer.DOM.y,
              canvasX: e.pointer.canvas.x,
              canvasY: e.pointer.canvas.y
            });

            await dataAnalyzeStore.fetchRelatedVertex();

            if (size(dataAnalyzeStore.graphDataEdgeTypes) !== 0) {
              dataAnalyzeStore.fetchFilteredPropertyOptions(
                dataAnalyzeStore.graphDataEdgeTypes[0]
              );
            }
          } else {
            const edgeId = network.getEdgeAt(e.pointer.DOM);

            // if not click on edge
            if (isUndefined(edgeId)) {
              dataAnalyzeStore.resetRightClickedGraphData();
              setNodeToolTipX(e.pointer.DOM.x);
              setNodeToolTipY(e.pointer.DOM.y);
              dataAnalyzeStore.setVisCurrentCoordinates({
                domX: e.pointer.DOM.x,
                domY: e.pointer.DOM.y,
                canvasX: e.pointer.canvas.x,
                canvasY: e.pointer.canvas.y
              });
              switchIsPopover(true);
            }
          }
        });

        network.on('dragging', () => {
          const node = dataAnalyzeStore.visDataSet?.nodes.get(
            dataAnalyzeStore.rightClickedGraphData.id
          );

          if (node !== null) {
            const position = network.getPositions(node.id);
            setNodeToolTipX(network.canvasToDOM(position[node.id]).x);
            setNodeToolTipY(network.canvasToDOM(position[node.id]).y);
            switchAfterDragging(true);
          }
        });

        network.on('zoom', () => {
          const node = dataAnalyzeStore.visDataSet?.nodes.get(
            dataAnalyzeStore.rightClickedGraphData.id
          );

          if (node !== null) {
            const position = network.getPositions(node.id);
            setNodeToolTipX(network.canvasToDOM(position[node.id]).x);
            setNodeToolTipY(network.canvasToDOM(position[node.id]).y);
          }
        });

        network.on('dragEnd', (e) => {
          if (!isEmpty(e.nodes)) {
            network.unselectAll();
          }
        });

        network.once('stabilizationIterationsDone', () => {
          switchShowLoadingGraphs(false);
        });

        setGraph(network);
        dataAnalyzeStore.setVisNetwork(network);
      }
    } else {
      if (!dataAnalyzeStore.isGraphLoaded) {
        graph.setData({
          nodes: graphNodes,
          edges: graphEdges
        });

        dataAnalyzeStore.setVisDataSet({
          nodes: graphNodes,
          edges: graphEdges
        });

        dataAnalyzeStore.switchGraphLoaded(true);
      }

      redrawGraphs();
    }
  }, [
    dataAnalyzeStore,
    dataAnalyzeStore.originalGraphData,
    graph,
    dataAnalyzeStore.isFullScreenReuslt,
    redrawGraphs
  ]);

  useEffect(() => {
    if (legendWrapper.current) {
      const legendWidth = getComputedStyle(legendWrapper.current).width!.split(
        'px'
      )[0];

      setlegendWitdh(Number(legendWidth));
    }
  }, []);

  useEffect(() => {
    window.addEventListener('resize', redrawGraphs, false);

    return () => {
      window.removeEventListener('resize', redrawGraphs);
    };
  }, [redrawGraphs]);

  return (
    <>
      <div
        className={
          dataAnalyzeStore.isFullScreenReuslt
            ? 'full-screen-graph-wrapper'
            : 'graph-wrapper'
        }
        ref={resultWrapper}
        style={{
          zIndex: hidden ? -1 : 0,
          margin: '-9px',
          overflow: 'hidden'
        }}
      >
        <div
          className={
            dataAnalyzeStore.isFullScreenReuslt
              ? 'query-result-content-manipulations fullscreen'
              : 'query-result-content-manipulations'
          }
        >
          <div
            style={{
              position: 'relative',
              width: '50%',
              overflow: 'auto',
              height: '20px',
              display: 'flex',
              justifyContent: 'space-around',
              alignItems: 'center',
              color: '#666',
              fontSize: 12
            }}
            ref={legendViewPointWrapper}
          >
            <div
              style={{
                display: 'flex',
                position: 'absolute',
                top: 0,
                left: legendStep
              }}
              ref={legendWrapper}
            >
              {Array.from(
                new Set(
                  dataAnalyzeStore.graphData.data.graph_view.vertices.map(
                    ({ label }) => label
                  )
                )
              ).map((label) => {
                return (
                  <div
                    style={{
                      display: 'flex',
                      marginRight: 14,
                      alignItems: 'center'
                    }}
                    key={label}
                  >
                    <div
                      style={{
                        width: 10,
                        height: 10,
                        borderRadius: 5,
                        marginRight: 5,
                        backgroundColor:
                          dataAnalyzeStore.colorMappings[label] || '#5c73e6'
                      }}
                    ></div>
                    <div>{label}</div>
                  </div>
                );
              })}
            </div>
          </div>

          <div>
            <img
              src={AddNodeIcon}
              alt="添加顶点"
              title="添加顶点"
              onClick={() => {
                if (graph) {
                  dataAnalyzeStore.setDynamicAddGraphDataStatus('vertex');
                }
              }}
            />
            <img
              src={ZoomInIcon}
              alt="放大"
              title="放大"
              onClick={() => {
                if (graph) {
                  const currentScale = graph.getScale();

                  graph.moveTo({
                    scale: currentScale + 0.1
                  });
                }
              }}
            />
            <img
              src={ZoomOutIcon}
              alt="缩小"
              title="缩小"
              onClick={() => {
                if (graph) {
                  const currentScale = graph.getScale();

                  graph.moveTo({
                    scale:
                      currentScale - 0.1 > 0 ? currentScale - 0.1 : currentScale
                  });
                }
              }}
            />
            <img
              src={CenterIcon}
              alt="居中"
              title="居中"
              onClick={() => {
                if (graph) {
                  graph.moveTo({
                    position: {
                      x: 0,
                      y: 0
                    }
                  });
                }
              }}
            />
            <img
              src={DownloadIcon}
              alt="下载"
              title="下载"
              onClick={() => {
                const blob = new Blob(
                  [
                    JSON.stringify(
                      dataAnalyzeStore.originalGraphData.data.json_view.data,
                      null,
                      4
                    )
                  ],
                  { type: 'text/plain;charset=utf-8' }
                );

                saveAs(blob, 'gremlin-data.json');
              }}
            />
            {dataAnalyzeStore.isFullScreenReuslt ? (
              <img
                src={ResetScreenIcon}
                alt="退出全屏"
                title="退出全屏"
                onClick={() => {
                  dataAnalyzeStore.setFullScreenReuslt(false);
                }}
              />
            ) : (
              <img
                src={FullScreenIcon}
                alt="全屏"
                title="全屏"
                onClick={() => {
                  dataAnalyzeStore.setFullScreenReuslt(true);
                }}
              />
            )}
          </div>
        </div>
        <div ref={graphWrapper}></div>
        {isPopover && (
          <GraphPopOver
            x={nodeTooltipX}
            y={nodeTooltipY}
            isAfterDragging={isAfterDragging}
            switchAfterDragging={switchAfterDragging}
            switchIsPopover={switchIsPopover}
          />
        )}
      </div>
      {dataAnalyzeStore.isShowFilterBoard && <QueryFilterOptions />}
      {showLoadingGraphs && (
        <div className="graph-loading-placeholder">
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
          <span>正在渲染...</span>
        </div>
      )}
    </>
  );
});

export default GraphQueryResult;
