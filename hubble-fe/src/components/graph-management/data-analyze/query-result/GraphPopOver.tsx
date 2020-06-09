import React, { useContext, useEffect, useRef, useCallback } from 'react';
import { observer } from 'mobx-react';
import vis from 'vis-network';
import 'vis-network/styles/vis-network.min.css';
import { Message } from '@baidu/one-ui';

import { DataAnalyzeStoreContext } from '../../../../stores';
import { convertArrayToString } from '../../../../stores/utils';

interface GraphPopoverProps {
  x: number;
  y: number;
  switchIsPopover: (state: boolean) => void;
  isAfterDragging: boolean;
  switchAfterDragging: (state: boolean) => void;
  visNetwork: vis.Network | null;
  visGraphNodes: vis.DataSetNodes;
  visGraphEdges: vis.DataSetEdges;
}

const GraphPopover: React.FC<GraphPopoverProps> = observer(
  ({
    x,
    y,
    isAfterDragging,
    switchAfterDragging,
    switchIsPopover,
    visNetwork,
    visGraphNodes,
    visGraphEdges
  }) => {
    const dataAnalyzeStore = useContext(DataAnalyzeStoreContext);
    const popoverWrapperRef = useRef<HTMLDivElement>(null);

    const handleOutSideClick = useCallback(
      (e: MouseEvent) => {
        // if clicked element is not on dropdown, collpase it
        if (
          popoverWrapperRef.current &&
          !popoverWrapperRef.current.contains(e.target as Element)
        ) {
          if (isAfterDragging) {
            switchAfterDragging(false);
            return;
          }

          switchIsPopover(false);
        }
      },
      [switchIsPopover, isAfterDragging]
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
        onContextMenu={(e) => e.preventDefault()}
        style={{ top: y, left: x }}
        ref={popoverWrapperRef}
      >
        {dataAnalyzeStore.rightClickedGraphData.id === '' ? (
          <div
            className="graph-pop-over-item"
            onClick={() => {
              switchIsPopover(false);
              dataAnalyzeStore.setDynamicAddGraphDataStatus('vertex');
            }}
          >
            添加顶点
          </div>
        ) : (
          <>
            <div
              className="graph-pop-over-item"
              onClick={async () => {
                await dataAnalyzeStore.expandGraphNode();

                if (
                  dataAnalyzeStore.requestStatus.expandGraphNode === 'success'
                ) {
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
                                    <div>${convertArrayToString(
                                      value,
                                      '，'
                                    )}</div>
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
                    (edge) => {
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
                                    <div>${convertArrayToString(
                                      value,
                                      '，'
                                    )}</div>
                                  </div>
                                `;
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
                  visGraphNodes.remove([
                    dataAnalyzeStore.rightClickedGraphData.id
                  ]);
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
            <div
              className="graph-pop-over-item"
              onClick={() => {
                dataAnalyzeStore.setDynamicAddGraphDataStatus('outEdge');
                dataAnalyzeStore.fetchRelatedEdges();
                switchIsPopover(false);
              }}
            >
              添加出边
            </div>
            <div
              className="graph-pop-over-item"
              onClick={() => {
                dataAnalyzeStore.setDynamicAddGraphDataStatus('inEdge');
                dataAnalyzeStore.fetchRelatedEdges();
                switchIsPopover(false);
              }}
            >
              添加入边
            </div>
          </>
        )}
      </div>
    );
  }
);

export default GraphPopover;
