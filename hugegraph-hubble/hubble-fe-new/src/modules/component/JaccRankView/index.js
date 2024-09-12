import React from 'react';
import c from './index.module.scss';

const JaccRankView = props => {
    const {
        nodeInfo,
        title,
        value,
    } = props;

    const {label, color} = nodeInfo || {};
    const nodeStyle = `linear-gradient(-70deg, ${color},70%,#fff)`;

    const formatedValue = Math.floor(value * 1000000000000000) / 1000000000000000;

    return (
        <div className={c.jaccViewContent}>
            {nodeInfo && (
                <div className={c.circleContent}>
                    <div className={c.singleCircle} style={{backgroundImage: nodeStyle}}></div>
                    <div className={c.circleLabel} style={{color: color}}>{label}</div>
                </div>
            )}
            <div>
                <div className={c.jaccardCanvasTitle}>{title}</div>
                <div className={c.jaccardCanvasData}>{formatedValue}</div>
            </div>
        </div>
    );

};

export default JaccRankView;
