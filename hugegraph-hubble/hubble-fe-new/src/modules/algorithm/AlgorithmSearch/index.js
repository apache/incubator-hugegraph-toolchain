/**
 * @file 图算法 搜索
 * @author
 */

import React, {useMemo} from 'react';
import {Input} from 'antd';
import _ from 'lodash';
import c from './index.module.scss';

const AlgorithmSearch = props => {

    const {onSearch} = props;

    const deboucedOnChange = useMemo(
        () => {
            return _.debounce(e => {
                const {value} = e.target;
                onSearch(value);
            }, 200);
        },
        [onSearch]
    );

    return (
        <div className={c.algorithmSearch}>
            <Input
                placeholder='算法查询'
                onChange={deboucedOnChange}
                allowClear
            />
        </div>
    );
};

export default AlgorithmSearch;
