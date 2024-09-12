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
