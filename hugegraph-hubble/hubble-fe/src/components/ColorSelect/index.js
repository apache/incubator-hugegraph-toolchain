
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

import {useCallback, useEffect, useState} from 'react';
import {ChromePicker} from 'react-color';
import styles from './index.module.scss';

const InputColorSelect = ({value, onChange, disable}) => {
    const [visible, setVisible] = useState(false);
    const [color, setColor] = useState('#5c73e6');

    const showPicker = useCallback(() => {
        if (disable) {
            return;
        }

        setVisible(true);
    }, [disable]);

    const hidePicker = useCallback(() => {
        if (disable) {
            return;
        }

        setVisible(false);
    }, [disable]);

    const handleClick = useCallback(color => {
        if (disable) {
            return;
        }

        setColor(color.hex);

        onChange?.(color.hex);
    }, [onChange, disable]);

    useEffect(() => {
        if (value) {
            setColor(value);
        }
    }, [value]);

    return (
        <div className={styles.wrap}>
            {disable ? <div className={styles.disable} /> : null}
            <div className={styles.swatch}>
                <div
                    className={styles.color}
                    style={{background: `${color}`}}
                    // style={{background: `rgba(${color.hex}, ${color.g}, ${color.b}, ${color.a})`}}
                    onClick={showPicker}
                />
            </div>
            {visible ? (
                <div className={styles.popover}>
                    <div className={styles.cover} onClick={hidePicker} />
                    <ChromePicker color={color} onChange={handleClick} disableAlpha />
                </div>
            ) : null}
        </div>
    );
};

export {InputColorSelect};
