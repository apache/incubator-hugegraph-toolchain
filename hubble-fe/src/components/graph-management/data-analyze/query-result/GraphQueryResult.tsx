import React, {
  useState,
  useContext,
  useEffect,
  useRef,
  useCallback
} from 'react';
import { observer } from 'mobx-react';
import { isUndefined, isEmpty } from 'lodash-es';
import { saveAs } from 'file-saver';
import vis from 'vis-network';
import 'vis-network/styles/vis-network.min.css';
import { Message } from '@baidu/one-ui';

import QueryFilterOptions from './QueryFilterOptions';
import { DataAnalyzeStoreContext } from '../../../../stores';
import ZoomInIcon from '../../../../assets/imgs/ic_fangda_16.svg';
import ZoomOutIcon from '../../../../assets/imgs/ic_suoxiao_16.svg';
import CenterIcon from '../../../../assets/imgs/ic_middle_16.svg';
import DownloadIcon from '../../../../assets/imgs/ic_xiazai_16.svg';
import FullScreenIcon from '../../../../assets/imgs/ic_quanping_16.svg';
import ResetScreenIcon from '../../../../assets/imgs/ic_tuichuquanping_16.svg';
import LoadingBackIcon from '../../../../assets/imgs/ic_loading_back.svg';
import LoadingFrontIcon from '../../../../assets/imgs/ic_loading_front.svg';

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
  const [nodeTooltipX, setNodeToolTipX] = useState(0);
  const [nodeTooltipY, setNodeToolTipY] = useState(0);
  const [legendStep, setLegendStep] = useState(0);
  const [legendWidth, setlegendWitdh] = useState(0);

  const [graph, setGraph] = useState<vis.Network | null>(null);
  const [visGraphNodes, setVisGraphNodes] = useState<vis.DataSetNodes | null>(
    null
  );
  const [visGraphEdges, setVisGraphEdges] = useState<vis.DataSetNodes | null>(
    null
  );

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

    if (!graph) {
      const graphNodes = new vis.DataSet(dataAnalyzeStore.graphNodes);
      const graphEdges = new vis.DataSet(dataAnalyzeStore.graphEdges);

      const data = {
        nodes: graphNodes,
        edges: graphEdges
      };

      setVisGraphNodes(graphNodes);
      setVisGraphEdges(graphEdges);

      const layout: vis.Options = {
        width,
        height,
        nodes: {
          shape: 'dot'
        },
        edges: {
          arrows: 'to',
          arrowStrikethrough: false,
          width: 1.5,
          color: {
            color: 'rgba(92, 115, 230, 0.8)',
            hover: 'rgba(92, 115, 230, 1)',
            highlight: 'rgba(92, 115, 230, 1)'
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

              const node = dataAnalyzeStore.graphData.data.graph_view.edges.find(
                ({ id }) => id === edgeId
              );

              if (isUndefined(node)) {
                return;
              }

              dataAnalyzeStore.changeSelectedGraphLinkData({
                id: node.id,
                label: node.label,
                properties: node.properties,
                source: node.source,
                target: node.target
              });

              if (
                dataAnalyzeStore.graphInfoDataSet !== 'edge' ||
                !dataAnalyzeStore.isShowGraphInfo
              ) {
                dataAnalyzeStore.switchShowScreeDataSet('edge');
                dataAnalyzeStore.switchShowScreenInfo(true);
              }

              // close filter board after click on node
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
              await dataAnalyzeStore.expandGraphNode(node.id, node.label);

              if (
                dataAnalyzeStore.requestStatus.expandGraphNode === 'success'
              ) {
                dataAnalyzeStore.expandedGraphData.data.graph_view.vertices.forEach(
                  ({ id, label, properties }) => {
                    graphNodes.add({
                      id,
                      label: id.length <= 15 ? id : id.slice(0, 15) + '...',
                      vLabel: label,
                      properties,
                      title: `
                          <div class="tooltip-fields">
                            <div>顶点类型：</div>
                            <div>${label}</div>
                          </div>
                          <div class="tooltip-fields">
                            <div>顶点ID：</div>
                            <div>${id}</div>
                          </div>
                          ${Object.entries(properties)
                            .map(([key, value]) => {
                              return `<div class="tooltip-fields">
                                        <div>${key}: </div>
                                        <div>${value}</div>
                                      </div>`;
                            })
                            .join('')}
                        `,
                      color: {
                        background:
                          dataAnalyzeStore.colorMappings[label] || '#5c73e6',
                        border:
                          dataAnalyzeStore.colorMappings[label] || '#5c73e6',
                        highlight: {
                          background: '#fb6a02',
                          border: '#fb6a02'
                        },
                        hover: { background: '#ec3112', border: '#ec3112' }
                      },
                      chosen: {
                        node(
                          values: any,
                          id: string,
                          selected: boolean,
                          hovering: boolean
                        ) {
                          if (hovering || selected) {
                            values.shadow = true;
                            values.shadowColor = 'rgba(0, 0, 0, 0.6)';
                            values.shadowX = 0;
                            values.shadowY = 0;
                            values.shadowSize = 25;
                          }

                          if (selected) {
                            values.size = 30;
                          }
                        }
                      }
                    });
                  }
                );

                dataAnalyzeStore.expandedGraphData.data.graph_view.edges.forEach(
                  edge => {
                    graphEdges.add({
                      ...edge,
                      from: edge.source,
                      to: edge.target,
                      font: {
                        color: '#666'
                      },
                      title: `
                          <div class="tooltip-fields">
                            <div>边类型：</div>
                            <div>${edge.label}</div>
                          </div>
                          <div class="tooltip-fields">
                            <div>边ID：</div>
                            <div>${edge.id}</div>
                          </div>
                          ${Object.entries(edge.properties)
                            .map(([key, value]) => {
                              return `<div class="tooltip-fields">
                                        <div>${key}: </div>
                                        <div>${value}</div>
                                      </div>`;
                            })
                            .join('')}
                        `,
                      color: {
                        color: dataAnalyzeStore.edgeColorMappings[edge.label],
                        highlight:
                          dataAnalyzeStore.edgeColorMappings[edge.label],
                        hover: dataAnalyzeStore.edgeColorMappings[edge.label]
                      }
                    });
                  }
                );
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

        network.on('oncontext', async e => {
          // disable default context menu
          e.event.preventDefault();

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

            await dataAnalyzeStore.fetchRelatedVertex();
            dataAnalyzeStore.fetchFilteredPropertyOptions(
              dataAnalyzeStore.graphDataEdgeTypes[0]
            );
          }
        });

        network.on('dragEnd', e => {
          if (!isEmpty(e.nodes)) {
            network.unselectAll();
          }
        });

        network.once('stabilizationIterationsDone', () => {
          switchShowLoadingGraphs(false);
        });

        setGraph(network);
      }
    } else {
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
          margin: '-9px'
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
              ).map(label => {
                return (
                  <div
                    style={{
                      display: 'flex',
                      marginRight: 14,
                      alignItems: 'center'
                    }}
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
          <GraphPopover
            x={nodeTooltipX}
            y={nodeTooltipY}
            switchIsPopover={switchIsPopover}
            visNetwork={graph}
            visGraphNodes={visGraphNodes}
            visGraphEdges={visGraphEdges}
          />
        )}
      </div>
      {dataAnalyzeStore.isShowFilterBoard && (
        <QueryFilterOptions
          visNetwork={graph}
          visGraphNodes={visGraphNodes}
          visGraphEdges={visGraphEdges}
        />
      )}
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

const GraphPopover: React.FC<{
  x: number;
  y: number;
  switchIsPopover: (state: boolean) => void;
  visNetwork: vis.Network | null;
  visGraphNodes: vis.DataSetNodes;
  visGraphEdges: vis.DataSetEdges;
}> = observer(
  ({ x, y, switchIsPopover, visNetwork, visGraphNodes, visGraphEdges }) => {
    const dataAnalyzeStore = useContext(DataAnalyzeStoreContext);
    const popoverWrapperRef = useRef<HTMLDivElement>(null);

    const handleOutSideClick = useCallback(
      (e: MouseEvent) => {
        // if clicked element is not on dropdown, collpase it
        if (
          popoverWrapperRef.current &&
          !popoverWrapperRef.current.contains(e.target as Element)
        ) {
          switchIsPopover(false);
        }
      },
      [switchIsPopover]
    );

    useEffect(() => {
      document.addEventListener('click', handleOutSideClick, false);

      return () => {
        document.removeEventListener('click', handleOutSideClick, false);
      };
    }, [handleOutSideClick]);

    return (
      <div
        className="graph-pop-over"
        onContextMenu={e => e.preventDefault()}
        style={{ top: y, left: x }}
        ref={popoverWrapperRef}
      >
        <div
          className="graph-pop-over-item"
          onClick={async () => {
            await dataAnalyzeStore.expandGraphNode();

            if (dataAnalyzeStore.requestStatus.expandGraphNode === 'success') {
              dataAnalyzeStore.expandedGraphData.data.graph_view.vertices.forEach(
                ({ id, label, properties }) => {
                  visGraphNodes.add({
                    id,
                    label: id.length <= 15 ? id : id.slice(0, 15) + '...',
                    vLabel: label,
                    properties,
                    title: `
                      <div class="tooltip-fields">
                        <div>顶点类型：</div>
                        <div>${label}</div>
                      </div>
                      <div class="tooltip-fields">
                        <div>顶点ID：</div>
                        <div>${id}</div>
                      </div>
                      ${Object.entries(properties)
                        .map(([key, value]) => {
                          return `<div class="tooltip-fields">
                                    <div>${key}: </div>
                                    <div>${value}</div>
                                  </div>`;
                        })
                        .join('')}
                    `,
                    color: {
                      background:
                        dataAnalyzeStore.colorMappings[label] || '#5c73e6',
                      border:
                        dataAnalyzeStore.colorMappings[label] || '#5c73e6',
                      highlight: {
                        background: '#fb6a02',
                        border: '#fb6a02'
                      },
                      hover: { background: '#ec3112', border: '#ec3112' }
                    },
                    chosen: {
                      node(
                        values: any,
                        id: string,
                        selected: boolean,
                        hovering: boolean
                      ) {
                        if (hovering || selected) {
                          values.shadow = true;
                          values.shadowColor = 'rgba(0, 0, 0, 0.6)';
                          values.shadowX = 0;
                          values.shadowY = 0;
                          values.shadowSize = 25;
                        }

                        if (selected) {
                          values.size = 30;
                        }
                      }
                    }
                  });
                }
              );

              dataAnalyzeStore.expandedGraphData.data.graph_view.edges.forEach(
                edge => {
                  visGraphEdges.add({
                    ...edge,
                    from: edge.source,
                    to: edge.target,
                    font: {
                      color: '#666'
                    },
                    title: `
                      <div class="tooltip-fields">
                        <div>边类型：</div>
                        <div>${edge.label}</div>
                      </div>
                      <div class="tooltip-fields">
                        <div>边ID：</div>
                        <div>${edge.id}</div>
                      </div>
                      ${Object.entries(edge.properties)
                        .map(([key, value]) => {
                          return `<div class="tooltip-fields">
                                    <div>${key}: </div>
                                    <div>${value}</div>
                                  </div>
                                `;
                        })
                        .join('')}
                    `,
                    color: {
                      color: dataAnalyzeStore.edgeColorMappings[edge.label],
                      highlight: dataAnalyzeStore.edgeColorMappings[edge.label],
                      hover: dataAnalyzeStore.edgeColorMappings[edge.label]
                    }
                  });
                }
              );

              dataAnalyzeStore.resetRightClickedGraphData();
              switchIsPopover(false);
            } else {
              Message.error({
                content: dataAnalyzeStore.errorInfo.expandGraphNode.message,
                size: 'medium',
                showCloseIcon: false
              });
            }
          }}
        >
          展开
        </div>
        <div
          className="graph-pop-over-item"
          onClick={() => {
            dataAnalyzeStore.switchShowFilterBoard(true);
            switchIsPopover(false);
          }}
        >
          查询
        </div>
        <div
          className="graph-pop-over-item"
          onClick={() => {
            if (visNetwork !== null) {
              visGraphNodes.remove([dataAnalyzeStore.rightClickedGraphData.id]);
              dataAnalyzeStore.hideGraphNode(
                dataAnalyzeStore.rightClickedGraphData.id
              );
              dataAnalyzeStore.resetRightClickedGraphData();
              switchIsPopover(false);
            }
          }}
        >
          隐藏
        </div>
      </div>
    );
  }
);

export default GraphQueryResult;
