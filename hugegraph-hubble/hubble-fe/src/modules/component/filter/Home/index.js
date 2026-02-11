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
 * @file 筛选
 * @author
 */

import React, {useCallback, useState} from 'react';
import {Button, Tooltip} from 'antd';
import {FilterOutlined} from '@ant-design/icons';
import Filter from '../FilterDrawer';
import {formatToStyleData, formatToDownloadData} from '../../../../utils/formatGraphResultData';

const FilterHome = props => {
    const {
        graphData,
        onChange,
        buttonEnable,
        tooltip,
    } = props;

    const [isFilterModalVisible, setIsFilterModalVisible] = useState(false);
    const [changeData, setChangeData] = useState({});

    const style = formatToStyleData(graphData);
    const dataSouce = formatToDownloadData(graphData);

    const handleClickFilterConfig = useCallback(
        () => {
            setIsFilterModalVisible(true);
        },
        []
    );

    const handleClickFilterCancel = useCallback(
        () => {
            setIsFilterModalVisible(false);
        },
        []
    );

    const handleFilter = useCallback(value => {
        const newData = {...changeData, filter: value};
        setChangeData(newData);
        onChange?.call(null, newData);
    }, [onChange, changeData]);

    return (
        <>
            <Tooltip placement="bottom" title={!buttonEnable ? tooltip : ''}>
                <Button
                    onClick={handleClickFilterConfig}
                    icon={<FilterOutlined />}
                    type={'text'}
                    disabled={!buttonEnable}
                >
                    筛选
                </Button>
            </Tooltip>
            <Filter
                open={isFilterModalVisible}
                onCancel={handleClickFilterCancel}
                rawTypeInfo={style}
                dataSource={dataSouce}
                onChange={handleFilter}
            />
        </>
    );
};

export default FilterHome;
