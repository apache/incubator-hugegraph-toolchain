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

const PERMISSION_PRESETS = Object.freeze({
    SUPER_ADMIN: 'SUPER_ADMIN',
    GS_READ_ONLY: 'GS_READ_ONLY',
    GS_READ_WRITE: 'GS_READ_WRITE',
    GS_ADMIN: 'GS_ADMIN',
});

const presetKeys = Object.values(PERMISSION_PRESETS);

const getAccountPreset = account => {
    const explicit = account?.permission_preset
        ?? account?.permissionPreset
        ?? account?.access_level;
    if (presetKeys.includes(explicit)) {
        return explicit;
    }
    if (account?.is_superadmin) {
        return PERMISSION_PRESETS.SUPER_ADMIN;
    }
    if (explicit === 'LEGACY_CUSTOM') {
        return null;
    }
    const scopedPermissions = Array.isArray(account?.graphspace_permissions) ? account.graphspace_permissions : [];
    const scopedPresets = [...scopedPermissions,
        ...(Array.isArray(account?.adminSpaces)
            ? account.adminSpaces.map(graphspace => ({
                graphspace,
                permission_preset: PERMISSION_PRESETS.GS_ADMIN,
            })) : [])]
        .map(permission => permission?.permission_preset)
        .filter(preset => presetKeys.includes(preset));
    if (new Set(scopedPresets).size > 1) {
        return null;
    }
    if (scopedPresets.includes(PERMISSION_PRESETS.GS_ADMIN)) {
        return PERMISSION_PRESETS.GS_ADMIN;
    }
    if (scopedPresets.includes(PERMISSION_PRESETS.GS_READ_WRITE)) {
        return PERMISSION_PRESETS.GS_READ_WRITE;
    }
    if (Array.isArray(account?.adminSpaces) && account.adminSpaces.length > 0) {
        return PERMISSION_PRESETS.GS_ADMIN;
    }
    return PERMISSION_PRESETS.GS_READ_ONLY;
};

const getPresetSpaces = account => {
    const scoped = Array.isArray(account?.graphspace_permissions)
        ? account.graphspace_permissions : [];
    const admins = Array.isArray(account?.adminSpaces)
        ? account.adminSpaces.map(graphspace => ({
            graphspace,
            permission_preset: PERMISSION_PRESETS.GS_ADMIN,
        })) : [];
    const spaces = [...scoped, ...admins];
    return Array.isArray(spaces)
        ? Array.from(new Set(spaces.map(space => {
            if (typeof space === 'string') {
                return space;
            }
            const value = space?.name ?? space?.graphspace;
            return typeof value === 'string' ? value : value?.name;
        }).filter(Boolean)))
        : [];
};

const getAccountPresetLabelKey = account => {
    if (!account?.is_superadmin && getPresetSpaces(account).length === 0) {
        return 'unassigned';
    }
    return getAccountPreset(account) ?? 'mixed';
};

// This is the only compatibility mapping used by account forms.
const toPermissionPayload = values => {
    const preset = values.permission_preset ?? PERMISSION_PRESETS.GS_READ_ONLY;
    const spaces = values.graphspaces ?? [];
    const payload = {...values};
    delete payload.permission_preset;
    delete payload.graphspaces;
    return {
        ...payload,
        permission_preset: preset,
        graphspace_permissions: spaces.map(graphspace => ({
            graphspace,
            permission_preset: preset,
        })),
        is_superadmin: preset === PERMISSION_PRESETS.SUPER_ADMIN,
        adminSpaces: preset === PERMISSION_PRESETS.GS_ADMIN ? spaces : [],
    };
};

export {
    PERMISSION_PRESETS,
    getAccountPreset,
    getAccountPresetLabelKey,
    getPresetSpaces,
    toPermissionPayload,
};
