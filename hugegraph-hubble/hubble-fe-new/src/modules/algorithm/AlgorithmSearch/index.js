/**
 * @file 图算法 搜索
 * @author
 */

import React, {useMemo} from 'react';
import {Input} from 'antd';
import _ from 'lodash';
import c from './index.module.scss';
import {useTranslation} from 'react-i18next';

const AlgorithmSearch = props => {
    const {t} = useTranslation();
    const {onSearch} = props;

    const debounceOnChange = useMemo(
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
                placeholder={t('analysis.algorithm.placeholder')}
                onChange={debounceOnChange}
                allowClear
            />
        </div>
    );
};

export default AlgorithmSearch;
