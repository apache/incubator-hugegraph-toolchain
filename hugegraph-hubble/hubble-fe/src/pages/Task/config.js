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

const sourceType = [
    {label: 'HDFS', value: 'HDFS'},
    {label: '本地上传', value: 'FILE'},
    {label: 'Kafka', value: 'KAFKA'},
    {label: 'JDBC', value: 'JDBC'},
];

const syncType = [
    {label: '执行一次', value: 'ONCE'},
    {label: '实时执行', value: 'REALTIME'},
    {label: '周期执行', value: 'CRON'},
];

export {sourceType, syncType};
