/**
 * @file 3D/2D 切换按钮
 * @author gouzixing
 */

import React from 'react';
import {Tooltip, Segmented} from 'antd';
import {GRAPH_RENDER_MODE} from '../../../utils/constants';
import c from './index.module.scss';

const RenderModeSwitcher = props => {

    const {buttonEnable, onClick, value, tooltip} = props;

    return (
        <Tooltip placement="bottom" title={!buttonEnable ? tooltip : ''}>
            <div className={c.threeModeSegment}>
                <Segmented
                    disabled={!buttonEnable}
                    options={Object.values(GRAPH_RENDER_MODE)}
                    onChange={onClick}
                    value={value}
                />
            </div>
        </Tooltip>
    );
};

export default RenderModeSwitcher;