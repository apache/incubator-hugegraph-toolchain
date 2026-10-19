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

import {useCallback, useRef, useState} from 'react';
import {ColorPicker} from 'antd';
import styles from './index.module.scss';
import {useTranslation} from 'react-i18next';

const InputColorSelect = ({value, onChange, disable}) => {
    const {t} = useTranslation();
    const [visible, setVisible] = useState(false);
    const [color, setColor] = useState('#5c73e6');
    const triggerRef = useRef(null);
    const currentColor = value || color;
    const handleOpenChange = useCallback(open => {
        setVisible(open);
        if (!open) {
            triggerRef.current?.focus();
        }
    }, []);
    const handleChange = useCallback(next => {
        const hex = next.toHexString();
        setColor(hex);
        onChange?.(hex);
    }, [onChange]);
    const handleKeyDown = useCallback(event => {
        if (event.key === 'Escape') {
            event.preventDefault();
            handleOpenChange(false);
        }
    }, [handleOpenChange]);

    return (
        <ColorPicker value={currentColor} onChange={handleChange} disabled={disable}
            disabledAlpha format='hex' open={visible && !disable} onOpenChange={handleOpenChange}
            panelRender={panel => <div onKeyDown={handleKeyDown}>{panel}</div>}>
            <span className={styles.swatch} onKeyDown={handleKeyDown}>
                <button ref={triggerRef} type='button' className={styles.color}
                    style={{background: currentColor}} disabled={disable}
                    aria-label={t('style_config.color_picker')} aria-expanded={visible && !disable} />
            </span>
        </ColorPicker>
    );
};

export {InputColorSelect};
