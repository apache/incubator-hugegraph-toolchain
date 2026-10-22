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

export const formatStorageUsed = item => {
    const kb = item.storage_used_kb;
    if (typeof kb !== 'number' || !Number.isFinite(kb) || kb < 0) {
        return `${item.storage_used ?? 0} GB`;
    }
    if (kb === 0) {
        return '0 GB';
    }
    const [value, unit] = kb >= 1024 * 1024
        ? [kb / (1024 * 1024), 'GB']
        : kb >= 1024 ? [kb / 1024, 'MB'] : [kb, 'KB'];
    return `${Number(value.toFixed(2))} ${unit}`;
};

export const storageUsagePercent = item => {
    const kb = item.storage_used_kb;
    const limit = item.storage_limit;
    if (typeof kb === 'number' && Number.isFinite(kb) && kb >= 0
        && typeof limit === 'number' && Number.isFinite(limit) && limit > 0) {
        return kb / (limit * 1024 * 1024) * 100;
    }
    return (item.storage_percent ?? 0) * 100;
};

// Integer.MAX_VALUE is the legacy Server sentinel; large finite quotas remain numeric.
export const formatResourceLimit = (value, suffix, unlimited, empty = unlimited) => (
    value === 2147483647 ? empty : `${value}${suffix}`
);

export const formatComputeLimit = (value, inheritedValue, suffix, t) => {
    const effective = value === 0 ? inheritedValue : value;
    const formatted = effective == null ? '--'
        : formatResourceLimit(effective, suffix, t('graphspace.unit.unlimited'));
    return value === 0 ? t('graphspace.unit.inherited', {value: formatted}) : formatted;
};
