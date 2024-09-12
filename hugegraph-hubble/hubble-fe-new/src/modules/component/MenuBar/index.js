/**
 * @file  MenuBar
 * @author
 */

import React from 'react';
import {Space} from 'antd';
import c from './index.module.scss';

const MenuBar = props => {
    const {
        content,
        extra,
    } = props;

    return (
        <div className={c.menuBar}>
            <Space>
                {content?.map(
                    item => {
                        const {key, content} = item;
                        return (
                            <div key={key}>{content}</div>
                        );
                    }
                )}
            </Space>
            <Space>
                {extra?.map(
                    item => {
                        const {key, content} = item;
                        return (
                            <div key={key}>{content}</div>
                        );
                    }
                )}
            </Space>
        </div>
    );
};


export default MenuBar;
