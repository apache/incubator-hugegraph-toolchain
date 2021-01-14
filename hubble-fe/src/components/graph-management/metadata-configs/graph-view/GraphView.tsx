import React, {
  useContext,
  useState,
  useEffect,
  useRef,
  useCallback
} from 'react';
import { observer } from 'mobx-react';
import { isEmpty } from 'lodash-es';
import vis from 'vis-network';
import 'vis-network/styles/vis-network.min.css';
import { Button } from '@baidu/one-ui';

import CreateProperty from './CreateProperty';
import CreateVertex from './CreateVertex';
import CreateEdge from './CreateEdge';
import CheckAndEditVertex from './CheckAndEditVertex';
import CheckAndEditEdge from './CheckAndEditEdge';
import CheckProperty from './CheckProperty';

import DataAnalyzeStore from '../../../../stores/GraphManagementStore/dataAnalyzeStore/dataAnalyzeStore';
import MetadataConfigsRootStore from '../../../../stores/GraphManagementStore/metadataConfigsStore/metadataConfigsStore';
import { generateGraphModeId } from '../../../../stores/utils';

import AddIcon from '../../../../assets/imgs/ic_add.svg';
import LoadingBackIcon from '../../../../assets/imgs/ic_loading_back.svg';
import LoadingFrontIcon from '../../../../assets/imgs/ic_loading_front.svg';

import '../../data-analyze/DataAnalyze.less';
import './GraphView.less';

const styles = {
  marginLeft: '12px'
};

const GraphView: React.FC = observer(() => {
  const {
    metadataPropertyStore,
    vertexTypeStore,
    edgeTypeStore,
    graphViewStore
  } = useContext(MetadataConfigsRootStore);

  useEffect(() => {
    metadataPropertyStore.fetchMetadataPropertyList({
      fetchAll: true
    });

    vertexTypeStore.fetchVertexTypeList({ fetchAll: true });
    edgeTypeStore.fetchEdgeTypeList({ fetchAll: true });

    return () => {
      metadataPropertyStore.dispose();
      vertexTypeStore.dispose();
      edgeTypeStore.dispose();
    };
  }, [edgeTypeStore, graphViewStore, metadataPropertyStore, vertexTypeStore]);

  return (
    <div className="metadata-configs-content-wrapper">
      <div className="metadata-configs-content-header">
        <Button
          size="medium"
          style={styles}
          onClick={() => {
            graphViewStore.setCurrentDrawer('create-property');
          }}
        >
          创建属性
        </Button>
        <Button
          size="medium"
          style={styles}
          onClick={() => {
            graphViewStore.setCurrentDrawer('create-vertex');
          }}
        >
          创建顶点类型
        </Button>
        <Button
          size="medium"
          style={styles}
          onClick={() => {
            graphViewStore.setCurrentDrawer('create-edge');
          }}
        >
          创建边类型
        </Button>
        {/* <CreateProperty />.outsideClick need id to specify logic here */}
        {!isEmpty(metadataPropertyStore.metadataProperties) && (
          <div id="metadata-graph-button-check-property">
            <Button
              size="medium"
              style={styles}
              onClick={() => {
                graphViewStore.setCurrentDrawer('check-property');
              }}
            >
              查看属性
            </Button>
          </div>
        )}
      </div>
      {/* note: components below all have graphView.currentDrawer in render
       * if use && at here it will dispatch re-render in this component
       * which cause all components below to re-render either
       */}
      <GraphDataView />
      <CreateProperty />
      <CreateVertex />
      <CreateEdge />
      <CheckAndEditVertex />
      <CheckAndEditEdge />
      <CheckProperty />
    </div>
  );
});

const GraphDataView: React.FC = observer(() => {
  const dataAnalyzeStore = useContext(DataAnalyzeStore);
  const {
    metadataPropertyStore,
    vertexTypeStore,
    edgeTypeStore,
    graphViewStore
  } = useContext(MetadataConfigsRootStore);
  const [graph, setGraph] = useState<vis.Network | null>(null);
  const [showLoadingGraphs, switchShowLoadingGraphs] = useState(true);
  const [isGraphDataLoaded, switchIsGraphLoaded] = useState(false);

  const graphWrapper = useRef<HTMLDivElement>(null);
  const resultWrapper = useRef<HTMLDivElement>(null);

  const redrawGraphs = useCallback(() => {
    if (graph) {
      // list mode may has scrollbar
      // when switch to graph mode, scrollbar hides quickly but resize event cannot dispatch
      // at this scenario we have to manually calculate the width
      // however when set resultWrapper style with overflow: hidden
      // window.innerWidth equals to document.body.scrollWidth
      // no need to write code below:

      // let width: string;
      // if (window.innerWidth - document.body.scrollWidth > 0) {
      //   width = String(window.innerWidth - 126) + 'px';
      // } else {
      //   width = getComputedStyle(resultWrapper.current!).width as string;
      // }

      if (
        graphViewStore.originalGraphViewData !== null &&
        graphViewStore.originalGraphViewData.vertices.length === 0 &&
        graphViewStore.originalGraphViewData.edges.length === 0
      ) {
        graph.setSize('0', '0');
        graph.redraw();
        return;
      }

      const width =
        String(
          Number(
            (getComputedStyle(resultWrapper.current!).width as string).split(
              'px'
            )[0]
          ) + 2
        ) + 'px';
      const height =
        String(
          Number(
            (getComputedStyle(resultWrapper.current!).height as string).split(
              'px'
            )[0]
          ) + 2
        ) + 'px';

      graph.setSize(width, height);
      graph.redraw();
    }
  }, [graph, graphViewStore.originalGraphViewData]);

  useEffect(() => {
    graphViewStore.fetchGraphViewData(
      dataAnalyzeStore.colorMappings,
      dataAnalyzeStore.vertexSizeMappings,
      dataAnalyzeStore.vertexWritingMappings,
      dataAnalyzeStore.edgeColorMappings,
      dataAnalyzeStore.edgeThicknessMappings,
      dataAnalyzeStore.edgeWithArrowMappings,
      dataAnalyzeStore.edgeWritingMappings
    );

    return () => {
      graphViewStore.dispose();
    };
  }, [
    dataAnalyzeStore.colorMappings,
    dataAnalyzeStore.vertexSizeMappings,
    dataAnalyzeStore.vertexWritingMappings,
    dataAnalyzeStore.edgeColorMappings,
    dataAnalyzeStore.edgeThicknessMappings,
    dataAnalyzeStore.edgeWithArrowMappings,
    dataAnalyzeStore.edgeWritingMappings,
    graphViewStore
  ]);

  useEffect(() => {
    const graphNodes = new vis.DataSet(graphViewStore.graphNodes);
    const graphEdges = new vis.DataSet(graphViewStore.graphEdges);

    if (!graph) {
      const data = {
        nodes: graphNodes,
        edges: graphEdges
      };

      const layout: vis.Options = {
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

      if (graphWrapper.current !== null) {
        const network = new vis.Network(graphWrapper!.current, data, layout);

        network.on('click', ({ nodes, edges }) => {
          // click on node, note that edges(related) also has value
          if (!isEmpty(nodes)) {
            // note: cannot abstract switchClickOn...() and clearTimeout
            // as common callings with node and edge, since click event
            // would be dispatched even if click is not on node and edge
            // dataAnalyzeStore.switchClickOnNodeOrEdge(true);
            // clearTimeout(timer);

            // caution: nodeId is automatically converted to number
            const nodeId = nodes[0];

            if (graphViewStore.graphViewData !== null) {
              const index = vertexTypeStore.vertexTypes.findIndex(
                (vertex) => vertex.name === String(nodeId)
              );

              if (index === -1) {
                return;
              }

              vertexTypeStore.selectVertexType(index);

              // check also needs style infos
              vertexTypeStore.mutateEditedSelectedVertexType({
                ...vertexTypeStore.editedSelectedVertexType,
                style: {
                  color: vertexTypeStore.selectedVertexType!.style.color,
                  icon: null,
                  size: vertexTypeStore.selectedVertexType!.style.size,
                  display_fields: vertexTypeStore.selectedVertexType!.style
                    .display_fields
                }
              });

              graphViewStore.setCurrentDrawer('check-vertex');
              graphViewStore.switchNodeOrEdgeClicked(true);

              // check if vertex type being used
              vertexTypeStore.checkIfUsing([
                vertexTypeStore.selectedVertexType!.name
              ]);
            }

            return;
          }

          if (!isEmpty(edges)) {
            const edgeId = edges[0];

            if (graphViewStore.graphViewData !== null) {
              const index = edgeTypeStore.edgeTypes.findIndex(
                (edge) =>
                  generateGraphModeId(
                    edge.name,
                    edge.source_label,
                    edge.target_label
                  ) === edgeId
              );

              if (index === -1) {
                return;
              }

              edgeTypeStore.selectEdgeType(index);
              // check also needs style infos
              edgeTypeStore.mutateEditedSelectedEdgeType({
                ...edgeTypeStore.editedSelectedEdgeType,
                style: {
                  color: edgeTypeStore.selectedEdgeType!.style.color,
                  icon: null,
                  with_arrow: edgeTypeStore.selectedEdgeType!.style.with_arrow,
                  thickness: edgeTypeStore.selectedEdgeType!.style.thickness,
                  display_fields: edgeTypeStore.selectedEdgeType!.style
                    .display_fields
                }
              });

              graphViewStore.setCurrentDrawer('check-edge');
              graphViewStore.switchNodeOrEdgeClicked(true);
            }
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
        graphViewStore.setVisNetwork(network);
      }
    } else {
      // if graph view data arrives, init to graph
      if (graphViewStore.originalGraphViewData !== null && !isGraphDataLoaded) {
        // switchIsGraphLoaded(true);

        graph.setData({
          nodes: graphNodes,
          edges: graphEdges
        });

        graphViewStore.setVisDataSet({
          nodes: graphNodes,
          edges: graphEdges
        });
      }

      redrawGraphs();
    }
  }, [
    graph,
    graphViewStore.graphEdges,
    graphViewStore.graphNodes,
    redrawGraphs,
    vertexTypeStore,
    edgeTypeStore,
    graphViewStore,
    isGraphDataLoaded
  ]);

  useEffect(() => {
    window.addEventListener('resize', redrawGraphs, false);

    return () => {
      window.removeEventListener('resize', redrawGraphs);
    };
  }, [redrawGraphs]);

  return (
    <>
      <div className="metadata-graph-view-wrapper" ref={resultWrapper}>
        <div className="metadata-graph-view" ref={graphWrapper}></div>
        {graphViewStore.requestStatus.fetchGraphViewData === 'pending' && (
          <div className="metadata-graph-loading">
            <div className="metadata-graph-loading-bg">
              <img
                className="metadata-graph-loading-back"
                src={LoadingBackIcon}
                alt="加载背景"
              />
              <img
                className="metadata-graph-loading-front"
                src={LoadingFrontIcon}
                alt="加载 spinner"
              />
            </div>
            <span>数据加载中...</span>
          </div>
        )}
        {graphViewStore.requestStatus.fetchGraphViewData === 'success' &&
          showLoadingGraphs &&
          (!isEmpty(graphViewStore.originalGraphViewData!.vertices) ||
            !isEmpty(graphViewStore.originalGraphViewData!.edges)) && (
            <div className="metadata-graph-loading">
              <div className="metadata-graph-loading-bg">
                <img
                  className="metadata-graph-loading-back"
                  src={LoadingBackIcon}
                  alt="加载背景"
                />
                <img
                  className="metadata-graph-loading-front"
                  src={LoadingFrontIcon}
                  alt="加载 spinner"
                />
              </div>
              <span>正在渲染...</span>
            </div>
          )}
        {graphViewStore.requestStatus.fetchGraphViewData === 'success' &&
          graphViewStore.isGraphVertexEmpty &&
          isEmpty(graphViewStore.originalGraphViewData!.vertices) &&
          isEmpty(graphViewStore.originalGraphViewData!.edges) && (
            <EmptyGraphDataView
              hasProeprties={!isEmpty(metadataPropertyStore.metadataProperties)}
            />
          )}
      </div>
    </>
  );
});

const EmptyGraphDataView: React.FC<{ hasProeprties: boolean }> = observer(
  ({ hasProeprties }) => {
    return (
      <div className="metadata-graph-view-empty-wrapper">
        <div className="metadata-graph-view-empty">
          <img src={AddIcon} alt="您暂时还没有任何元数据" />
          <div>
            {hasProeprties
              ? '您还未设置顶点类型或边类型'
              : '您暂时还没有任何元数据'}
          </div>
        </div>
      </div>
    );
  }
);

export default GraphView;
