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