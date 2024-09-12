/**
 * @file 新建按钮
 * @author
 */

import React, {useCallback} from 'react';
import {Button, Tooltip, Dropdown, Menu} from 'antd';
import {PlusSquareOutlined} from '@ant-design/icons';

const NewConfig = props => {
    const {
        buttonEnable,
        onClickAddNode,
        onClickAddEdge,
        tooltip,
    } = props;

    const handleClickNewNode = useCallback(
        () => {
            onClickAddNode();
        },
        [onClickAddNode]
    );

    const handleClickNewEdge = useCallback(
        isOut => {
            onClickAddEdge(isOut);
        },
        [onClickAddEdge]
    );

    const newMenu = (
        <Menu
            items={[
                {
                    key: '1',
                    label: (<a onClick={handleClickNewNode}>添加顶点</a>),
                },
                {
                    key: '2',
                    label: (<a onClick={() => handleClickNewEdge(false)}>添加入边</a>),
                },
                {
                    key: '3',
                    label: (<a onClick={() => handleClickNewEdge(true)}>添加出边</a>),
                },
            ]}
        />
    );

    return (
        <Dropdown overlay={newMenu} placement="bottomLeft" disabled={!buttonEnable}>
            <Tooltip placement="bottom" title={!buttonEnable ? tooltip : ''}>
                <Button
                    type='text'
                    icon={<PlusSquareOutlined />}
                    disabled={!buttonEnable}
                >
                    新建
                </Button>
            </Tooltip>
        </Dropdown>
    );
};

export default NewConfig;