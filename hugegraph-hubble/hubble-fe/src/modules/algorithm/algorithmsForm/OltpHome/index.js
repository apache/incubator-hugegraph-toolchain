/*
 *
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. The ASF
 * licenses this file to You under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

/**
 * @file Oltp图算法表单列表
 * @author
 */

import React from 'react';
import {Collapse} from 'antd';
import _ from 'lodash';
import {ALGORITHM_NAME, useTranslatedConstants} from '../../../../utils/constants';
import OltpItem from '../Oltp/OltpItem';
import c from './index.module.scss';

const {
    K_NEIGHBOR,
    K_OUT,
    SAME_NEIGHBORS,
    RINGS,
    SHORTEST_PATH,
    ALLPATHS,
    JACCARD_SIMILARITY,
    CROSSPOINTS,
    CUSTOMIZED_CROSSPOINTS,
    KOUT_POST,
    KNEIGHBOR_POST,
    FINDSHORTESTPATH,
    FINDSHORTESTPATHWITHWEIGHT,
    SINGLESOURCESHORTESTPATH,
    MULTINODESSHORTESTPATH,
    CUSTOMIZEDPATHS,
    TEMPLATEPATHS,
    RAYS,
    FUSIFORM_SIMILARITY,
    ADAMIC_ADAR,
    RESOURCE_ALLOCATION,
    SAME_NEIGHBORS_BATCH,
    EGONET,
    JACCARD_SIMILARITY_POST,
    RANK_API,
    NEIGHBOR_RANK_API, PATHS,
} = ALGORITHM_NAME;

const oltpListRaw = [
    K_OUT,
    K_NEIGHBOR,
    SAME_NEIGHBORS,
    RINGS,
    SHORTEST_PATH,
    ALLPATHS,
    JACCARD_SIMILARITY,
    KOUT_POST,
    KNEIGHBOR_POST,
    JACCARD_SIMILARITY_POST,
    RANK_API,
    NEIGHBOR_RANK_API,
    FINDSHORTESTPATH,
    FINDSHORTESTPATHWITHWEIGHT,
    SINGLESOURCESHORTESTPATH,
    MULTINODESSHORTESTPATH,
    CUSTOMIZEDPATHS,
    TEMPLATEPATHS,
    CROSSPOINTS,
    CUSTOMIZED_CROSSPOINTS,
    RAYS,
    FUSIFORM_SIMILARITY,
    ADAMIC_ADAR,
    RESOURCE_ALLOCATION,
    SAME_NEIGHBORS_BATCH,
    EGONET,
    PATHS,
];



const OltpFormHome = props => {
    const {
        onOltpFormSubmit,
        search,
        currentAlgorithm,
        updateCurrentAlgorithm,
    } =  props;
    const {ALGORITHM_MODE} = useTranslatedConstants();
    const {OLTP} = ALGORITHM_MODE;
    const getSearchedList = (arr, value) => {
        return arr.filter(item => item.includes(value));
    };

    const basicOltpList = getSearchedList(oltpListRaw, search);
    const isEmptyBasicOltp = _.isEmpty(basicOltpList);

    return (
        <div>
            {!isEmptyBasicOltp && (<div className={c.algorithmCatagery}>{OLTP}</div>)}
            <Collapse ghost accordion className={c.sideBarCollapse}>
                {
                    basicOltpList.map(item =>
                        (
                            <OltpItem
                                key={item}
                                handleFormSubmit={onOltpFormSubmit}
                                algorithmName={item}
                                searchValue={search}
                                currentAlgorithm={currentAlgorithm}
                                updateCurrentAlgorithm={updateCurrentAlgorithm}
                            />
                        )
                    )
                }
            </Collapse>
        </div>
    );
};

export default OltpFormHome;
