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
 * @file Gremlin语法分析 Header
 * @author huangqiuyu
 */

import React, {useCallback, useState} from 'react';
import {Tabs} from 'antd';
import CodeEditor from '../../../../components/CodeEditor';
import {ANALYSIS_TYPE} from '../../../../utils/constants';
import ContentCommon from '../ContentCommon';
import c from './index.module.scss';

const {GREMLIN, CYPHER} = ANALYSIS_TYPE;

const QueryBar = props => {
    const {...args} = props;

    const {codeEditorContent, setCodeEditorContent, activeTab, onTabsChange} = args;

    const [isEmptyQuery, setIsEmptyQuery] = useState(true);
    const [favoriteCardVisible, setFavoriteCardVisible] = useState(false);

    const handleCodeEditorChange = useCallback(
        value => {
            const existQuery = Boolean(value);
            setCodeEditorContent(value);
            setIsEmptyQuery(!existQuery);
            if (!existQuery) {
                setFavoriteCardVisible(false);
            }
        },
        [setCodeEditorContent]
    );

    const tabItems = [
        {
            label: 'Gremlin分析',
            key: GREMLIN,
            children: (
                <ContentCommon
                    {...args}
                    isEmptyQuery={isEmptyQuery}
                    favoriteCardVisible={favoriteCardVisible}
                    setFavoriteCardVisible={setFavoriteCardVisible}
                >
                    <CodeEditor
                        value={codeEditorContent}
                        onChange={handleCodeEditorChange}
                        lang={'gremlin'}
                    />
                </ContentCommon>
            ),
        },
        {
            label: 'Cypher分析',
            key: CYPHER,
            children: (
                <ContentCommon
                    {...args}
                    isEmptyQuery={isEmptyQuery}
                    favoriteCardVisible={favoriteCardVisible}
                    setFavoriteCardVisible={setFavoriteCardVisible}
                >
                    <CodeEditor
                        value={codeEditorContent}
                        onChange={handleCodeEditorChange}
                        lang={'cypher'}
                    />
                </ContentCommon>
            ),
        },
    ];

    return (
        <div className={c.queryBar} id='queryBar'>
            <Tabs
                defaultActiveKey={GREMLIN}
                activeKey={activeTab}
                type="card"
                onChange={onTabsChange}
                items={tabItems}
                size='small'
            />
        </div>
    );
};

export default QueryBar;
