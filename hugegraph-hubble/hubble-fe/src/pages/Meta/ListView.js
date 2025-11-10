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

import {Tabs} from 'antd';
import PropertyTable from './Property';
import VertexTable from './Vertex';
import EdgeTable from './Edge';
import VertexIndexTable from './VertexIndex';
import EdgeIndexTable from './EdgeIndex';

const ListView = () => {
    return (
        <Tabs
            defaultActiveKey='1'
            destroyInactiveTabPane
            items={[
                {
                    label: '属性',
                    key: '1',
                    children: <PropertyTable />,
                },
                {
                    label: '顶点类型',
                    key: '2',
                    children: <VertexTable />,
                },
                {
                    label: '边类型',
                    key: '3',
                    children: <EdgeTable />,
                },
                {
                    label: '顶点索引',
                    key: '4',
                    children: <VertexIndexTable />,
                },
                {
                    label: '边索引',
                    key: '5',
                    children: <EdgeIndexTable />,
                },
            ]}
        />
    );
};

export default ListView;
