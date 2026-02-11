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
 * @file 分析子项块
 * @author
 */

import Item from '../Item';

const AnalyseItem = () => {
    return (
        <Item
            btnIndex={2}
            btnTitle={'业务分析'}
            listData={[
                {
                    title: '图语言分析',
                    url: '/gremlin',
                },
                {
                    title: '图算法',
                    url: '/algorithms',
                },
            ]}
        />
    );
};

export default AnalyseItem;
