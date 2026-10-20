/*
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

import UserOutlined from '@ant-design/icons-svg/lib/asn/UserOutlined';
import UsergroupAddOutlined from '@ant-design/icons-svg/lib/asn/UsergroupAddOutlined';
import IdcardOutlined from '@ant-design/icons-svg/lib/asn/IdcardOutlined';
import VerifiedOutlined from '@ant-design/icons-svg/lib/asn/VerifiedOutlined';
import UserSwitchOutlined from '@ant-design/icons-svg/lib/asn/UserSwitchOutlined';
import PropertySafetyOutlined from '@ant-design/icons-svg/lib/asn/PropertySafetyOutlined';
import TransactionOutlined from '@ant-design/icons-svg/lib/asn/TransactionOutlined';
import InsuranceOutlined from '@ant-design/icons-svg/lib/asn/InsuranceOutlined';
import ShoppingOutlined from '@ant-design/icons-svg/lib/asn/ShoppingOutlined';
import CloudSyncOutlined from '@ant-design/icons-svg/lib/asn/CloudSyncOutlined';
import HomeOutlined from '@ant-design/icons-svg/lib/asn/HomeOutlined';
import BankOutlined from '@ant-design/icons-svg/lib/asn/BankOutlined';
import ShopOutlined from '@ant-design/icons-svg/lib/asn/ShopOutlined';
import ReadOutlined from '@ant-design/icons-svg/lib/asn/ReadOutlined';
import MedicineBoxOutlined from '@ant-design/icons-svg/lib/asn/MedicineBoxOutlined';
import CommentOutlined from '@ant-design/icons-svg/lib/asn/CommentOutlined';
import ChromeOutlined from '@ant-design/icons-svg/lib/asn/ChromeOutlined';
import DribbbleOutlined from '@ant-design/icons-svg/lib/asn/DribbbleOutlined';
import SlackOutlined from '@ant-design/icons-svg/lib/asn/SlackOutlined';
import MailOutlined from '@ant-design/icons-svg/lib/asn/MailOutlined';
import LaptopOutlined from '@ant-design/icons-svg/lib/asn/LaptopOutlined';
import PhoneOutlined from '@ant-design/icons-svg/lib/asn/PhoneOutlined';
import WhatsAppOutlined from '@ant-design/icons-svg/lib/asn/WhatsAppOutlined';
import AlertOutlined from '@ant-design/icons-svg/lib/asn/AlertOutlined';
import DashboardOutlined from '@ant-design/icons-svg/lib/asn/DashboardOutlined';
import SettingOutlined from '@ant-design/icons-svg/lib/asn/SettingOutlined';
import StarOutlined from '@ant-design/icons-svg/lib/asn/StarOutlined';
import TrademarkOutlined from '@ant-design/icons-svg/lib/asn/TrademarkOutlined';
import SoundOutlined from '@ant-design/icons-svg/lib/asn/SoundOutlined';
import LockOutlined from '@ant-design/icons-svg/lib/asn/LockOutlined';
import type {AbstractNode, IconDefinition} from '@ant-design/icons-svg/lib/types';
import {resolveIconName} from '../../utils/iconNames';

// Only these bundled definitions can become an image. Persisted text never becomes SVG markup.
const definitions: Record<string, IconDefinition> = {
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
const cache = new Map<string, string>();
const tags = new Set(['svg', 'g', 'path', 'circle', 'rect', 'ellipse', 'polygon', 'polyline', 'line']);
const attributes = new Set(['viewBox', 'focusable', 'd', 'fill', 'fill-rule', 'clip-rule', 'cx', 'cy', 'r',
    'rx', 'ry', 'x', 'y', 'x1', 'x2', 'y1', 'y2', 'width', 'height', 'points', 'stroke', 'stroke-width',
    'transform', 'opacity']);
const escapeAttribute = (value: string): string => value.replace(/&/g, '&amp;').replace(/"/g, '&quot;')
    .replace(/</g, '&lt;').replace(/>/g, '&gt;');
const serialize = (node: AbstractNode): string => {
    if (!tags.has(node.tag)) {
        throw new Error('Unsupported bundled icon element');
    }
    const attrs = Object.entries(node.attrs).filter(([key]) => attributes.has(key))
        .map(([key, value]) => ` ${key}="${escapeAttribute(String(value))}"`).join('');
    return `<${node.tag}${attrs}>${(node.children ?? []).map(serialize).join('')}</${node.tag}>`;
};
const safeColor = (value: unknown): string => typeof value === 'string'
    && /^(?:#[\da-f]{3,8}|[a-z]+|(?:rgb|hsl)a?\([\d.,%\s+-]+\))$/i.test(value) ? value : '#343434';

/** Local image URI with no runtime font, CDN, React renderer or user-provided SVG. */
export const hubbleIconSource = (value: unknown, color?: unknown): string | undefined => {
    const name = resolveIconName(value, '');
    if (!name) {
        return undefined;
    }
    const fill = safeColor(color);
    const key = `${name}:${fill}`;
    const cached = cache.get(key);
    if (cached) {
        return cached;
    }
    const definition = definitions[name];
    const tree = typeof definition.icon === 'function' ? definition.icon(fill, fill) : definition.icon;
    const svg = serialize({...tree, attrs: {...tree.attrs, fill}})
        .replace('<svg', '<svg xmlns="http://www.w3.org/2000/svg"');
    const source = `data:image/svg+xml,${encodeURIComponent(svg)}`;
    // Color-picker drafts must not create a permanent unbounded icon cache.
    if (cache.size >= 128) {
        cache.delete(cache.keys().next().value!);
    }
    cache.set(key, source);
    return source;
};
