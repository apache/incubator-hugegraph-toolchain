/**
 * 从参数中筛选节点
 * @param {*} verticesParams
 * @param {*} nodes
 */

export default function getNodesFromParams(verticesParams, nodes) {
    const {ids, label, properties} = verticesParams;
    if (ids) {
        return ids;
    }
    const result = nodes.filter(node => {
        const {label: nLabel, properties: nProperties} = node;
        if (!nLabel || !nProperties) {
            return false;
        }
        if (label !== nLabel) {
            return false;
        }
        for (const [key, value] of Object.entries(properties)) {
            if (nProperties[key] !== value) {
                return false;
            }
        }
        return true;
    }).map(item => item.id);
    return result;
}