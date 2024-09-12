/**
 * @file 图分析组件 算法标题
 * @author
 */

import React from 'react';
import Highlighter from 'react-highlight-words';
import {Typography, Tooltip, Button} from 'antd';
import c from './index.module.scss';
import classnames from 'classnames';

const {Text} = Typography;

import {
    QuestionCircleOutlined,
    CaretRightOutlined,
} from '@ant-design/icons';

const AlgorithmNameHeader = props => {
    const {
        icon,
        name,
        searchValue,
        description,
        isRunning,
        isDisabled,
        handleRunning,
        highlightName,
    } = props;

    const iconClassName = classnames(
        c.panelHeaderIcon,
        {[c.panelHeaderIconHighlight]: highlightName}

    );

    const renderAlgorithmName = name => {
        let res;
        if (name.includes(searchValue)) {
            res = (
                <Text
                    ellipsis={{
                        tooltip: name,
                    }}
                >
                    <Highlighter
                        highlightClassName={c.highlight}
                        searchWords={[searchValue]}
                        autoEscape
                        textToHighlight={name}
                    />
                </Text>
            );
        }
        else {
            res = (
                <Text
                    ellipsis={{
                        tooltip: name,
                    }}
                >
                    {name}
                </Text>
            );
        }
        if (highlightName) {
            res = (
                <Text
                    ellipsis={{
                        tooltip: name,
                    }}
                >
                    <Highlighter
                        highlightClassName={c.highlight}
                        searchWords={[name]}
                        autoEscape
                        textToHighlight={name}
                    />
                </Text>
            );
        }
        return res;
    };

    const renderRunningButton = () => {
        if (!isDisabled) {
            return (
                <Tooltip
                    placement="rightTop"
                    title={<span style={{color: '#000'}}>运行</span>}
                    color={'#fff'}
                >
                    <Button
                        className={c.panelHeaderRunningButton}
                        type="primary"
                        size='small'
                        loading={isRunning}
                        disabled={isDisabled}
                        icon={<CaretRightOutlined />}
                        onClick={handleRunning}
                    />
                </Tooltip>
            );
        }
        return (
            <Button
                className={c.panelHeaderRunningButton}
                type="primary"
                size='small'
                loading={isRunning}
                disabled={isDisabled}
                icon={<CaretRightOutlined />}
                onClick={handleRunning}
            />
        );
    };

    return (
        <div className={c.panelHeader}>
            <div className={iconClassName}>
                {icon}
            </div>
            <div className={c.panelHeaderName}>
                {renderAlgorithmName(name)}
            </div>
            <div className={c.panelHeaderRight}>
                <Tooltip placement="rightTop" title={description}>
                    <QuestionCircleOutlined />
                </Tooltip>
                {renderRunningButton()}
            </div>
        </div>
    );
};

export default AlgorithmNameHeader;