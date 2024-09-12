/**
 * @file  画布数据统计
 * @author
 */

import React from 'react';
import c from './index.module.scss';
import classnames from 'classnames';

const NumberCard = props => {
    const {pathNum, data, hasPadding} = props;
    const {currentGraphNodesNum, currentGraphEdgesNum, allGraphNodesNum, allGraphEdgesNum} = data;

    let showLoading = false;
    if (currentGraphNodesNum < 0 || currentGraphEdgesNum < 0 || +allGraphNodesNum < 0 || +allGraphEdgesNum < 0) {
        showLoading = true;
    }

    const allGraphNodes = showLoading ? 'loading...' : allGraphNodesNum;
    const allGraphEdges = showLoading ? 'loading...' : allGraphEdgesNum;

    const graphClassName = classnames(
        c.numberCard,
        {[c.numberCardWithLayoutPanel]: hasPadding}
    );

    return (
        <div className={graphClassName}>
            {pathNum && (
                <div className={c.numberCardItem}>
                    <div className={c.numberCardTitle}>Paths</div>
                    <div className={c.numberCardInfo}>
                        <span className={c.numberCur}>{pathNum}</span>
                    </div>
                </div>
            )}
            <div className={c.numberCardItem}>
                <div className={c.numberCardTitle}>Nodes</div>
                <div className={c.numberCardInfo}>
                    <span className={c.numberCur}>{currentGraphNodesNum}</span>
                    <span>/</span>
                    <span className={c.numberAll}>
                        {allGraphNodes}
                    </span>
                </div>
            </div>
            <div className={c.numberCardItem}>
                <div className={c.numberCardTitle}>Edges</div>
                <div className={c.numberCardInfo}>
                    <span className={c.numberCur}>{currentGraphEdgesNum}</span>
                    <span>/</span>
                    <span className={c.numberAll}>
                        {allGraphEdges}
                    </span>
                </div>
            </div>
        </div>
    );
};

export default NumberCard;
