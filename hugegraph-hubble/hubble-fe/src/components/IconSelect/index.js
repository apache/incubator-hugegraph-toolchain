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

import {useMemo, useState} from 'react';
import {
    UserOutlined,
    UsergroupAddOutlined,
    IdcardOutlined,
    VerifiedOutlined,
    UserSwitchOutlined,
    PropertySafetyOutlined,
    TransactionOutlined,
    InsuranceOutlined,
    ShoppingOutlined,
    CloudSyncOutlined,
    HomeOutlined,
    BankOutlined,
    ShopOutlined,
    ReadOutlined,
    MedicineBoxOutlined,
    CommentOutlined,
    ChromeOutlined,
    DribbbleOutlined,
    SlackOutlined,
    MailOutlined,
    LaptopOutlined,
    PhoneOutlined,
    WhatsAppOutlined,
    AlertOutlined,
    DashboardOutlined,
    SettingOutlined,
    StarOutlined,
    TrademarkOutlined,
    SoundOutlined,
    LockOutlined,
} from '@ant-design/icons';
import {Popover, Input} from 'antd';
import classnames from 'classnames';

import {iconNames, resolveIconName} from '../../utils/iconNames';

import c from './index.module.scss';
import {useTranslation} from 'react-i18next';

const iconComponents = {
    UserOutlined,
    UsergroupAddOutlined,
    IdcardOutlined,
    VerifiedOutlined,
    UserSwitchOutlined,
    PropertySafetyOutlined,
    TransactionOutlined,
    InsuranceOutlined,
    ShoppingOutlined,
    CloudSyncOutlined,
    HomeOutlined,
    BankOutlined,
    ShopOutlined,
    ReadOutlined,
    MedicineBoxOutlined,
    CommentOutlined,
    ChromeOutlined,
    DribbbleOutlined,
    SlackOutlined,
    MailOutlined,
    LaptopOutlined,
    PhoneOutlined,
    WhatsAppOutlined,
    AlertOutlined,
    DashboardOutlined,
    SettingOutlined,
    StarOutlined,
    TrademarkOutlined,
    SoundOutlined,
    LockOutlined,
};

const IconSelect = props => {
    const {
        value,
        onChange,
        disabled,
    } = props;
    const {t} = useTranslation();
    const [selectedIcon, setSelectedIcon] = useState();
    const icon = Object.prototype.hasOwnProperty.call(props, 'value') ? value : selectedIcon;

    const handleClickIconCallbacks = useMemo(
        () => {
            const icons = Object.keys(iconNames);
            const callbacks = {};
            for (const item of icons) {
                callbacks[item] = () => {
                    setSelectedIcon(item);
                    onChange?.(item);
                };
            }
            return callbacks;
        },
        [onChange]
    );

    const renderIcons = () => {
        const icons = Object.keys(iconNames);
        return icons.map(item => {
            const Icon = iconComponents[item];
            const iconClassName = classnames(
                c.icon,
                {[c.iconSelected]: item === resolveIconName(icon)}
            );
            return (
                <button
                    type='button'
                    aria-label={iconNames[item].trim()}
                    aria-pressed={item === resolveIconName(icon)}
                    key={item}
                    className={iconClassName}
                    onClick={handleClickIconCallbacks[item]}
                >
                    <Icon />
                </button>
            );
        });
    };

    if (disabled) {
        return (
            <Input
                placeholder={t('selector.placeholder')}
                disabled
            />
        );
    }

    const CurrentIcon = iconComponents[resolveIconName(icon)];
    return (
        <Popover
            trigger={['click']}
            placement="bottomLeft"
            content={<div className={c.iconsWrapper}>{renderIcons()}</div>}
        >
            <Input
                placeholder={t('selector.placeholder')}
                value={icon || ''}
                readOnly
                suffix={<CurrentIcon />}
            />
        </Popover>
    );
};

export default IconSelect;
