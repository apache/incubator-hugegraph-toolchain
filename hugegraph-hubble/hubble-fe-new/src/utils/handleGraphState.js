/**
 * 清除所有边特定状态
 * @param {*} graph
 * @param {*} stateList
 */
const clearEdgesStates  = (graph, stateList) => {
    graph.getEdges().forEach(edge => {
        graph.clearItemStates(edge, stateList);
    });
};

/**
 * 清除所有点特定状态
 * @param {*} graph
 * @param {*} stateList
 */
const clearNodesStates  = (graph, stateList) => {
    graph.getNodes().forEach(node => {
        graph.clearItemStates(node, stateList);
    });
};


/**
 * 清除特定边特定状态
 * @param {*} graph
 * @param {*} item
 * @param {*} stateList
 */
const clearItemStates  = (graph, item, stateList) => {
    graph.clearItemStates(item, stateList);
};

/**
 * 清除特定边特定状态
 * @param {*} graph
 */
const clearSelectedStates  = graph => {
    graph.getEdges().forEach(edge => {
        graph.clearItemStates(edge, ['edgeSelected']);
    });
    graph.getNodes().forEach(node => {
        graph.clearItemStates(node, ['customSelected']);
    });
};

/**
 * 高亮节点的关联边
 * @param {*} graph
 */
const highLightRelatedEdges = (graph, node) => {
    graph.getEdges().forEach(edge => {
        if (edge.getSource() === node || edge.getTarget() === node) {
            graph.setItemState(edge, 'edgeActive', true);
        }
    });
};

/**
 * 高亮或普通标签
 * @param {*} graph
 * @param {*} item
 * @param {*} labelType
 */
const setItemLabelState = (graph, item, labelType) => {
    const model = item.getModel();
    const newModel = {
        ...model,
        labelCfg: {
            ...model.labelCfg,
            style: {
                ...model.labelCfg.style,
                fontWeight: labelType,
            },
        },
    };
    graph.updateItem(item, newModel, false);
};

export {
    clearEdgesStates,
    clearNodesStates,
    clearItemStates,
    clearSelectedStates,
    highLightRelatedEdges,
    setItemLabelState,
};