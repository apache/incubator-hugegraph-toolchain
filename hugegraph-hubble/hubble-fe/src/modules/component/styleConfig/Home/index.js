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
 * @file 外观设置
 * @author
 */

import React, {useCallback, useState} from 'react';
import {Button, Tooltip} from 'antd';
import ConfigModal from '../ConfigModal';
import {ExperimentOutlined} from '@ant-design/icons';

const StyleConfig = props => {
    const {
        styleConfig,
        onChange,
        buttonEnable,
        refreshExcuteCount,
        tooltip,
    } = props;

    const [isModalVisible, setModalVisible] = useState(false);

    const handleClickStyleConfig = useCallback(
        () => {
            setModalVisible(true);
        },
        []
    );

    const handleStyleModalOk = useCallback(
        styleConfig => {
            onChange(styleConfig);
            setModalVisible(false);
            refreshExcuteCount && refreshExcuteCount();
        },
        [onChange, refreshExcuteCount]
    );

    const handleStyleModalCancel = useCallback(
        () => {
            setModalVisible(false);
        },
        []
    );

    return (
        <>
            <Tooltip placement="bottom" title={!buttonEnable ? tooltip : ''}>
                <Button
                    onClick={handleClickStyleConfig}
                    icon={<ExperimentOutlined />}
                    type={'text'}
                    disabled={!buttonEnable}
                >
                    外观设置
                </Button>
            </Tooltip>
            <ConfigModal
                style={styleConfig}
                visible={isModalVisible}
                onOk={handleStyleModalOk}
                onCancel={handleStyleModalCancel}
            />
        </>
    );
};

export default StyleConfig;
