/**
 * @file  NeighborRankApi算法展示
 * @author
 */

import React from 'react';
import {colors} from '../../../../utils/constants';
import JaccRankView from '../../../component/JaccRankView';
import _ from 'lodash';
import c from './index.module.scss';

const NeighborRankApiView = props => {
    const {rankArray} = props;
    const colorsNum = colors.length;
    return (
        <div className={c.noneGraphContent}>
            {rankArray.map((item, index) => {
                return (
                    <div key={_.uniqueId()}>
                        <div className={c.noneGraphContentTitle}>分类{index + 1}</div>
                        {_.isEmpty(item) ? (
                            <div className={c.emptyDesc}>本场景下没有第{index + 1}度邻居</div>
                        ) : (Object.entries(item)?.map(item2 => {
                            const [key, value] = item2;
                            return (
                                <JaccRankView
                                    nodeInfo={{
                                        label: key,
                                        color: colors[index % colorsNum],
                                    }}
                                    key={key}
                                    title={'排名得分:'}
                                    value={value?.toString()}
                                />
                            );
                        })
                        )}
                    </div>
                );
            })}
        </div>
    );
};

export default NeighborRankApiView;