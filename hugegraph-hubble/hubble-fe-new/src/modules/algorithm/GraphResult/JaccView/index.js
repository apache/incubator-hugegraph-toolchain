/**
 * @file  JACCARD_SIMILARITY等算法展示
 * @author
 */

import React from 'react';
import JaccRankView from '../../../component/JaccRankView';
import c from './index.module.scss';

const JaccView = props => {
    const {jaccardsimilarity} = props;
    return (
        <div className={c.noneGraphContent}>
            <JaccRankView
                title={'相似度的值'}
                value={jaccardsimilarity?.toString()}
            />
        </div>
    );

};

export default JaccView;