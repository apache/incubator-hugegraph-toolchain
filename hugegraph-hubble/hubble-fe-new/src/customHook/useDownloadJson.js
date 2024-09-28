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
 * @author
 */

const useDownloadJson = () => {

    const downloadJsonHandler = (fileName, data) => {
        const formatedFileName = fileName.split('.').join('');
        let element = document.createElement('a');
        const processedData = JSON.stringify(data);
        element.setAttribute('href',
            `data:application/json;charset=utf-8,\ufeff${encodeURIComponent(processedData)}`);
        element.setAttribute('download', formatedFileName);
        element.style.display = 'none';
        document.body.appendChild(element);
        element.click();
        document.body.removeChild(element);
    };

    return {downloadJsonHandler};
};

export default useDownloadJson;
