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

import {expect, test} from '@jest/globals';
import {hubbleIconSource} from './icons';
import {iconNames} from '../../utils/iconNames';

test('every saved icon has a bundled local SVG and aliases resolve to the same image', () => {
    for (const [key, alias] of Object.entries(iconNames)) {
        const image = hubbleIconSource(key);
        expect(image?.startsWith('data:image/svg+xml,')).toBe(true);
        expect(hubbleIconSource(alias)).toBe(image);
        const svg = decodeURIComponent(image!.split(',')[1]);
        expect(svg).toContain('xmlns="http://www.w3.org/2000/svg"');
        expect(svg).toContain('<path');
        expect(svg).not.toMatch(/<script|\son[a-z]+=|href=|<foreignObject|https?:\/\/(?!www\.w3\.org\/2000\/svg")/i);
    }
});

test('does not embed user markup, URLs or arbitrary icon paths', () => {
    expect(hubbleIconSource('<svg onload="alert(1)">')).toBeUndefined();
    expect(hubbleIconSource('__proto__')).toBeUndefined();
    const colorPayload = '" onload="alert(1)';
    const source = hubbleIconSource('UserOutlined', colorPayload)!;
    const svg = decodeURIComponent(source.split(',')[1]);
    expect(svg).not.toContain('onload');
    expect(svg).toContain('fill="#343434"');
    expect(hubbleIconSource('UserOutlined', 'url(https://example.com/image)')).toBe(hubbleIconSource('UserOutlined'));
    expect(decodeURIComponent(hubbleIconSource('UserOutlined', '#008800')!.split(',')[1])).toContain('fill="#008800"');
});
