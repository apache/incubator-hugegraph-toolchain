/**
 * @file 图分析组件 slide组件
 * @author
 */

import React from 'react';
import {Slider} from 'antd';
import c from './index.module.scss';

const SliderComponent = props => {
    const {min, max, step, value, onChange} = props;

    return (
        <div className={c.layoutSlider}>
            <Slider
                value={value}
                step={step}
                min={min}
                max={max}
                onChange={onChange}
            />
            <span>{value || 0}</span>
        </div>
    );
};

export default SliderComponent;
