/**
 * @file 算法表单目录Home
 * @author
 */

import React, {useCallback, useState} from 'react';
import {Empty} from 'antd';
import OlapFormHome from '../OlapHome';
import OltpFormHome from '../OltpHome';
import AlgorithmSearch from '../../AlgorithmSearch';
import {useTranslatedConstants} from '../../../../utils/constants';
import c from './index.module.scss';
import _ from 'lodash';

const AlgorithmFormHome = props => {
    const {
        handleOltpFormSubmit,
        handleOlapFormSubmit,
        currentAlgorithm,
        updateCurrentAlgorithm,
    } =  props;
    const {ALGORITHM_NAME} = useTranslatedConstants();
    const [search, setSearch] = useState('');

    const handleSearch = useCallback(value => {
        setSearch(value);
    }, []);

    const isListEmpty = _.isEmpty(Object.values(ALGORITHM_NAME).filter(item => item.includes(search)));

    return (
        <div className={c.algorithmSidebar}>
            <AlgorithmSearch onSearch={handleSearch} />
            {isListEmpty && (<Empty className={c.listEmpty} />)}
            <OltpFormHome
                onOltpFormSubmit={handleOltpFormSubmit}
                search={search}
                currentAlgorithm={currentAlgorithm}
                updateCurrentAlgorithm={updateCurrentAlgorithm}
            />
            <OlapFormHome
                onOlapFormSubmit={handleOlapFormSubmit}
                search={search}
                currentAlgorithm={currentAlgorithm}
                updateCurrentAlgorithm={updateCurrentAlgorithm}
            />
        </div>
    );
};

export default AlgorithmFormHome;
