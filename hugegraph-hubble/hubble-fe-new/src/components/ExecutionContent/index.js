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