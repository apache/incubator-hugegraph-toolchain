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
 * @file 折叠组件
 * @author
 */

import React, {useState, useCallback} from 'react';
import Highlighter from 'react-highlight-words';

import {UpOutlined, DownOutlined} from '@ant-design/icons';
import c from './index.module.scss';

const ExecutionContent = ({content, highlightText}) => {

    const [isExpand, switchExpand] = useState(false);

    const onToggleCollapse = useCallback(() => {
        switchExpand(prev => !prev);
    }, []);

    const icon = isExpand ? <UpOutlined /> : <DownOutlined />;
    const contentElement = isExpand ? (
        <>
            <Highlighter
                highlightClassName={c.highlight}
                searchWords={[highlightText]}
                autoEscape
                textToHighlight={content}
            />
        </>
    ) : (
        <>
            <Highlighter
                highlightClassName={c.highlight}
                searchWords={[highlightText]}
                autoEscape
                textToHighlight={content.split('\n')[0]}
            />
        </>
    );

    return (
        <div
            onClick={onToggleCollapse}
            className={c.breakWord}
        >
            {icon}
            {contentElement}
        </div>
    );
};

export default ExecutionContent;
