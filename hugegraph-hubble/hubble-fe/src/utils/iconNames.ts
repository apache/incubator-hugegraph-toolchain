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

export const iconNames: Record<string, string> = {
    UserOutlined: 'user',
    UsergroupAddOutlined: 'add user',
    IdcardOutlined: 'id card',
    VerifiedOutlined: 'verified',
    UserSwitchOutlined: 'switch user',
    PropertySafetyOutlined: 'property safety',
    TransactionOutlined: 'transaction',
    InsuranceOutlined: 'insurance ',
    ShoppingOutlined: 'shopping',
    CloudSyncOutlined: 'cloud-sync',
    HomeOutlined: 'home',
    BankOutlined: 'bank',
    ShopOutlined: 'shop',
    ReadOutlined: 'read',
    MedicineBoxOutlined: 'medicinebox',
    CommentOutlined: 'comment',
    ChromeOutlined: 'chrome',
    DribbbleOutlined: 'dribbble',
    SlackOutlined: 'slack',
    MailOutlined: 'mail',
    LaptopOutlined: 'laptop',
    PhoneOutlined: 'phone',
    WhatsAppOutlined: 'whatsapp',
    AlertOutlined: 'alert',
    DashboardOutlined: 'dashboard',
    SettingOutlined: 'setting',
    StarOutlined: 'star',
    TrademarkOutlined: 'trademark',
    SoundOutlined: 'sound',
    LockOutlined: 'lock',
};

export const DEFAULT_ICON = 'UserOutlined';

// Old saved styles may contain either an Ant Design export name or a Graphin glyph alias.
// Normalize only at presentation boundaries; callers retain the original saved value.
export const resolveIconName = (value: unknown, fallback: string = DEFAULT_ICON): string => {
    if (typeof value !== 'string') {
        return fallback;
    }
    const name = value.trim();
    if (Object.prototype.hasOwnProperty.call(iconNames, name)) {
        return name;
    }
    return Object.keys(iconNames).find(key => iconNames[key].trim() === name) || fallback;
};
