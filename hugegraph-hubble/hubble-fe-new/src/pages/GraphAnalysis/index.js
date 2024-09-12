import GraphAnalysisHome from '../../modules/GraphAnalysis';
// import {useParams, useLocation, use} from 'react-router-dom';

const GraphAnalysis = ({moduleName}) => {

    // const {moduleName} = useParams();

    return (
        <GraphAnalysisHome moduleName={moduleName} />
    );
};

export default GraphAnalysis;