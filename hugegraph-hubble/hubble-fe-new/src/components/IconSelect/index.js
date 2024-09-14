import React, {useEffect, useMemo, useState} from 'react';
import * as AntIcon from '@ant-design/icons';
import {Popover, Input} from 'antd';
import classnames from 'classnames';

import {iconsMap} from '../../utils/constants';

import c from './index.module.scss';
import {useTranslation} from 'react-i18next';

const DEFAULT_ICON = 'UserOutlined';

const IconSelect = props => {
    const {
        value,
        onChange,
        disabled,
    } = props;
    const {t} = useTranslation();
    const [icon, setIcon] = useState();

    useEffect(
        () => {
            value && setIcon(value);
        },
        [value]
    );

    const handleClickIconCallbacks = useMemo(
        () => {
            const icons = Object.keys(iconsMap);
            const callbacks = {};
            for (const item of icons) {
                callbacks[item] = () => {
                    setIcon(item);
                    onChange(item);
                };
            }
            return callbacks;
        },
        [onChange]
    );

    const renderIcons = () => {
        const icons = Object.keys(iconsMap);
        return icons.map(item => {
            const Icon = AntIcon[item];
            const iconClassName = classnames(
                c.icon,
                {[c.iconSelected]: item === icon}
            );
            return (
                <div
                    key={item}
                    className={iconClassName}
                    onClick={handleClickIconCallbacks[item]}
                >
                    <Icon />
                </div>
            );
        });
    };

    if (disabled) {
        return (
            <Input
                placeholder="请选择"
                disabled
            />
        );
    }

    const CurrentIcon = AntIcon[icon || DEFAULT_ICON];
    return (
        <Popover
            trigger={['click']}
            placement="bottomLeft"
            content={<div className={c.iconsWrapper}>{renderIcons()}</div>}
        >
            <Input
                placeholder={t('selector.placeholder')}
                value={icon}
                suffix={<CurrentIcon />}
            />
        </Popover>
    );
};

export default IconSelect;
