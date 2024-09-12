/**
 * @file  统计Panel Home
 * @author gouzixing
 */

import React, {useCallback, useState} from 'react';
import {Radio} from 'antd';
import LabelStatistics from '../LabelStatistics';
import GraphStatistics from '../GraphStatistics/Home';
import classnames from 'classnames';
import c from './index.module.scss';

const StaticsTab = {
    LABEL: 0,
    GRAPH: 1,
};

const {LABEL, GRAPH} = StaticsTab;

const StatisticPanel = props => {
    const {open, ...args} = props;

    const [activePanel, setActivePanel] = useState(LABEL);

    const settingClassName = classnames(
        c.statistics,
        {[c.statisticsHidden]: !open}
    );

    const handleRadioChange = useCallback(
        e => {
            setActivePanel(e.target.value);
        },
        []
    );

    return (
        <div className={settingClassName}>
            <Radio.Group defaultValue={LABEL} onChange={handleRadioChange} size="middle" buttonStyle="solid">
                <Radio.Button value={LABEL}>标签统计</Radio.Button>
                <Radio.Button value={GRAPH}>图统计</Radio.Button>
            </Radio.Group>
            {open && (activePanel === LABEL ? <LabelStatistics {...args} /> : <GraphStatistics {...args} />)}
        </div>
    );
};

export default StatisticPanel;