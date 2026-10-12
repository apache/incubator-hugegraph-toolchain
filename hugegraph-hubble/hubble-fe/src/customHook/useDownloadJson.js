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

/**
 * @file  下载Json数据
 */

const serializeDownloadJson = data => JSON.stringify(
    data,
    (key, value) => (typeof value === 'bigint' ? value.toString() : value)
);

const useDownloadJson = () => {

    const downloadJsonHandler = (fileName, data) => {
        const sanitizedFileName = String(fileName || '')
            .trim()
            .replace(/\.json$/i, '')
            .split('.')
            .join('');
        const formatedFileName = `${sanitizedFileName || 'graph-data'}.json`;
        const element = document.createElement('a');
        const processedData = serializeDownloadJson(data);
        const blob = new Blob([processedData], {
            type: 'application/json;charset=utf-8',
        });
        const objectUrl = URL.createObjectURL(blob);
        element.setAttribute('href', objectUrl);
        element.setAttribute('download', formatedFileName);
        element.style.display = 'none';
        document.body.appendChild(element);
        element.click();
        document.body.removeChild(element);
        URL.revokeObjectURL(objectUrl);
    };

    return {downloadJsonHandler};
};

export {serializeDownloadJson};
export default useDownloadJson;
