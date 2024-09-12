/**
 * @file 画布Context
 * @author gouzixing@
 */

import React from 'react';

const defaultContext = {
    graph: {},
    layout: {},
};

const GraphContext = React.createContext(defaultContext);
const ToolBarContext = React.createContext(null);

export {GraphContext, ToolBarContext};
