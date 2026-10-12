/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership. The ASF
 * licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with the
 * License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

import {
    getAccountPreset,
    getAccountPresetLabelKey,
    getPresetSpaces,
    PERMISSION_PRESETS,
    toPermissionPayload,
} from './permissionPresets';

test.each([
    [{is_superadmin: true}, PERMISSION_PRESETS.SUPER_ADMIN],
    [{permission_preset: 'GS_READ_WRITE'}, PERMISSION_PRESETS.GS_READ_WRITE],
    [{graphspace_permissions: [{
        graphspace: 'team',
        permission_preset: 'GS_READ_WRITE',
    }]}, PERMISSION_PRESETS.GS_READ_WRITE],
    [{adminSpaces: ['team']}, PERMISSION_PRESETS.GS_ADMIN],
    [{adminSpaces: []}, PERMISSION_PRESETS.GS_READ_ONLY],
])('normalizes account %j to %s', (account, expected) => {
    expect(getAccountPreset(account)).toBe(expected);
});

test('does not claim GraphSpace access without a supported preset or assignment', () => {
    expect(getAccountPresetLabelKey({adminSpaces: []}, false))
        .toBe('unassigned');
    expect(getAccountPresetLabelKey({
        permission_preset: PERMISSION_PRESETS.GS_READ_ONLY,
        graphspace_permissions: [],
    }, true)).toBe('unassigned');
    expect(getAccountPresetLabelKey({is_superadmin: true}, false))
        .toBe(PERMISSION_PRESETS.SUPER_ADMIN);
    expect(getAccountPresetLabelKey({adminSpaces: ['SPACE']}, false))
        .toBe(PERMISSION_PRESETS.GS_ADMIN);
});

test('normalizes GraphSpace objects and keeps legacy payload in one adapter', () => {
    expect(getPresetSpaces({adminSpaces: [{name: 'team'}]})).toEqual(['team']);
    expect(getPresetSpaces({
        graphspace_permissions: [{graphspace: 'team'}],
    })).toEqual(['team']);
    expect(toPermissionPayload({
        permission_preset: PERMISSION_PRESETS.GS_ADMIN,
        graphspaces: ['team'],
    })).toMatchObject({
        adminSpaces: ['team'],
        is_superadmin: false,
    });
    expect(toPermissionPayload({
        permission_preset: PERMISSION_PRESETS.GS_READ_WRITE,
        graphspaces: ['team'],
    })).toMatchObject({
        permission_preset: PERMISSION_PRESETS.GS_READ_WRITE,
        graphspace_permissions: [{
            graphspace: 'team',
            permission_preset: PERMISSION_PRESETS.GS_READ_WRITE,
        }],
    });
});

test('clears admin spaces for the super administrator preset', () => {
    expect(toPermissionPayload({
        permission_preset: PERMISSION_PRESETS.SUPER_ADMIN,
        graphspaces: ['team'],
    })).toMatchObject({
        adminSpaces: [],
        is_superadmin: true,
    });
});

test('requires an explicit choice before replacing mixed GraphSpace presets', () => {
    expect(getAccountPreset({
        graphspace_permissions: [
            {graphspace: 'read', permission_preset: 'GS_READ_ONLY'},
            {graphspace: 'write', permission_preset: 'GS_READ_WRITE'},
        ],
    })).toBeNull();
});

test('requires an explicit choice before migrating legacy custom access', () => {
    expect(getAccountPreset({
        permission_preset: 'LEGACY_CUSTOM',
        graphspace_permissions: [{
            graphspace: 'legacy',
            permission_preset: 'GS_READ_WRITE',
        }],
    })).toBeNull();
});
