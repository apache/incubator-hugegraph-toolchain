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

const sourceType = t => [
    {label: t('task.source.hdfs'), value: 'HDFS'},
    {label: t('task.source.file'), value: 'FILE'},
    {label: t('task.source.kafka'), value: 'KAFKA'},
    {label: t('task.source.jdbc'), value: 'JDBC'},
];

const syncType = t => [
    {label: t('task.sync.once'), value: 'ONCE'},
    {label: t('task.sync.realtime'), value: 'REALTIME'},
    {label: t('task.sync.cron'), value: 'CRON'},
];

export {sourceType, syncType};
