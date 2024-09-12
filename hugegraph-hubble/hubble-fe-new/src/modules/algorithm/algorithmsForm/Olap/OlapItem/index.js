/**
 * @file OLAP算法item
 * @author zhanghao14@
 */

import React, {useContext} from 'react';
import {ALGORITHM_NAME} from '../../../../../utils/constants';
import GraphAnalysisContext from '../../../../Context';
import PageRank from '../PageRank';
import PageRankVermeer from '../PageRankVermeer';
import WeaklyConnectedComponent from '../WeaklyConnectedComponent';
import WeaklyConnectedComponentVermeer from '../WeaklyConnectedComponentVermeer';
import DegreeCentrality from '../DegreeCentrality';
import DegreeCentralityVermeer from '../DegreeCentralityVermeer';
import ClosenessCentrality from '../ClosenessCentrality';
import ClosenessCentralityVermeer from '../ClosenessCentralityVermeer';
import TriangleCount from '../TriangleCount';
import TriangleCountVermeer from '../TriangleCountVermeer';
import RingsDetection from '../RingsDetection';
import FilteredRingsDetection from '../FilteredRingsDetection';
import Links from '../Links';
import ClusterCoefficient from '../ClusterCoefficient';
import BetweennessCentrality from '../BetweennessCentrality';
import BetweennessCentralityVermeer from '../BetweennessCentralityVermeer';
import LabelPropagationAlgorithm from '../LabelPropagationAlgorithm';
import LabelPropagationAlgorithmVermeer from '../LabelPropagationAlgorithmVermeer';
import Louvain from '../Louvain';
import FilterSubGraphMatching from '../FilterSubGraphMatching';
import KCore from '../KCore';
import KCoreVermeer from '../KCoreVermeer';
import PersonalPageRank from '../PersonalPageRank';
import SSSPVermeer from '../SSSPVermeer';

const {
    PAGE_RANK,
    WEAKLY_CONNECTED_COMPONENT,
    DEGREE_CENTRALIT,
    CLOSENESS_CENTRALITY,
    TRIANGLE_COUNT,
    RINGS_DETECTION,
    FILTERED_RINGS_DETECTION,
    LINKS,
    CLUSTER_COEFFICIENT,
    BETWEENNESS_CENTRALITY,
    LABEL_PROPAGATION_ALGORITHM,
    LOUVAIN,
    FILTER_SUBGRAPH_MATCHING,
    K_CORE,
    PERSONAL_PAGE_RANK,
    SSSP,
} = ALGORITHM_NAME;

const OlapItem = props => {
    const {
        algorithmName,
        ...args
    } = props;

    const {isVermeer} = useContext(GraphAnalysisContext);

    const renderItem = () => {
        switch (algorithmName) {
            case PAGE_RANK:
                return (
                    isVermeer
                        ? (<PageRankVermeer {...args} />)
                        : (<PageRank {...args} />)
                );
            case WEAKLY_CONNECTED_COMPONENT:
                return (
                    isVermeer
                        ? (<WeaklyConnectedComponentVermeer {...args} />)
                        : (<WeaklyConnectedComponent {...args} />)
                );
            case DEGREE_CENTRALIT:
                return (
                    isVermeer
                        ? (<DegreeCentralityVermeer {...args} />)
                        : (<DegreeCentrality {...args} />)
                );
            case CLOSENESS_CENTRALITY:
                return (
                    isVermeer
                        ? (<ClosenessCentralityVermeer {...args} />)
                        : (<ClosenessCentrality {...args} />)
                );
            case TRIANGLE_COUNT:
                return (
                    isVermeer
                        ? (<TriangleCountVermeer {...args} />)
                        : (<TriangleCount {...args} />)
                );
            case RINGS_DETECTION:
                return <RingsDetection {...args} />;
            case FILTERED_RINGS_DETECTION:
                return <FilteredRingsDetection {...args} />;
            case LINKS:
                return <Links {...args} />;
            case CLUSTER_COEFFICIENT:
                return <ClusterCoefficient {...args} />;
            case BETWEENNESS_CENTRALITY:
                return (
                    isVermeer
                        ? (<BetweennessCentralityVermeer {...args} />)
                        : (<BetweennessCentrality {...args} />)
                );
            case LABEL_PROPAGATION_ALGORITHM:
                return (
                    isVermeer
                        ? (<LabelPropagationAlgorithmVermeer {...args} />)
                        : (<LabelPropagationAlgorithm {...args} />)
                );
            case LOUVAIN:
                return <Louvain {...args} />;
            case FILTER_SUBGRAPH_MATCHING:
                return <FilterSubGraphMatching {...args} />;
            case K_CORE:
                return (
                    isVermeer ? <KCoreVermeer {...args} /> : <KCore {...args} />
                );
            case PERSONAL_PAGE_RANK:
                return <PersonalPageRank {...args} />;
            case SSSP:
                return <SSSPVermeer {...args} />;
        }
    };

    return renderItem();
};

export default OlapItem;
