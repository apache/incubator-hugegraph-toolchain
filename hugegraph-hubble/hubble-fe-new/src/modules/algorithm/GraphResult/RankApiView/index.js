/**
 * @file  RankApi算法展示
 * @author
 */

import React from 'react';
import {GRAPH_STATUS} from '../../../../utils/constants';
import JaccRankView from '../../../component/JaccRankView';
import GraphStatusView from '../../../component/GraphStatusView';
import _ from 'lodash';
import c from './index.module.scss';

const RankApiView = props => {
    const {rankObj} = props;
    if (_.isEmpty(rankObj)) {
        return (
            <GraphStatusView status={GRAPH_STATUS.SUCCESS} message={'无图结果'} />
        );
    }
    return (
        <div className={c.noneGraphContent}>
            {
                Object.entries(rankObj)?.map(
                    item => {
                        const [key, value] = item;
                        return (
                            <JaccRankView
                                nodeInfo={{
                                    label: key,
                                    color: '#42B3E5',
                                }}
                                key={key}
                                title={'相似度的值'}
                                value={value?.toString()}
                            />
                        );
                    }
                )
            }
        </div>
    );

};

export default RankApiView;