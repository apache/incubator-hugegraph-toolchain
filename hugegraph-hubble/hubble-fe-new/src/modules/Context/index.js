/**
 * @file 图分析部分Context
 * @author gouzixing@
 */


import React from 'react';

const defaultContext = {
    graphSpace: null,
    graph: null,
    graphLoadTime: null,
    graphStatus: null,
    isVermeer: false,
};

const GraphAnalysisContext = React.createContext(defaultContext);

export default GraphAnalysisContext;