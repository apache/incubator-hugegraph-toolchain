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

import {intervalToDuration, formatDuration} from 'date-fns';
import _ from 'lodash';

/**
 * 转换时间数据格式
 * @param {*} duration
 * @returns
 */

export default function formatTimeDuration(start, end) {
    const duration = intervalToDuration({start, end});
    const {days, hours, minutes, seconds} = duration || {};
    const preFormatedDuration = formatDuration(duration, {format: ['days', 'hours', 'minutes', 'seconds']});

    const replaceMap = {
        ' days': 'd',
        ' day': 'd',
        ' hours': 'h',
        ' hour': 'h',
        ' minutes': 'm',
        ' minute': 'm',
        ' seconds': 's',
        ' second': 's',
    };

    let formatedDuration = preFormatedDuration;
    Object.keys(replaceMap).forEach(
        key => {
            formatedDuration = _.replace(formatedDuration, key, replaceMap[key]);
        }
    );
    const restMilliSecond = end - start - seconds * 1000 - minutes * 1000 * 60
    - hours * 1000 * 60 * 60 - days * 1000 * 60 * 60 * 24;

    formatedDuration = formatedDuration + ` ${restMilliSecond}ms`;
    return formatedDuration;
}
