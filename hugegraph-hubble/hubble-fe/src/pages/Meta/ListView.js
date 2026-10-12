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
import {useTranslation} from 'react-i18next';
import {useParams} from 'react-router-dom';
import {useGraphspaceAccess} from '../../auth/graphspaceAccess';

const ListView = () => {
    const {t} = useTranslation();
    const {graphspace} = useParams();
    const {canWrite} = useGraphspaceAccess(graphspace);
    return (
        <Tabs
            defaultActiveKey='1'
            destroyInactiveTabPane
            items={[
                {
                    label: t('schema.tab.property'),
                    key: '1',
                    children: <PropertyTable canWrite={canWrite} />,
                },
                {
                    label: t('schema.tab.vertex'),
                    key: '2',
                    children: <VertexTable canWrite={canWrite} />,
                },
                {
                    label: t('schema.tab.edge'),
                    key: '3',
                    children: <EdgeTable canWrite={canWrite} />,
                },
                {
                    label: t('schema.tab.vertex_index'),
                    key: '4',
                    children: <VertexIndexTable />,
                },
                {
                    label: t('schema.tab.edge_index'),
                    key: '5',
                    children: <EdgeIndexTable />,
                },
            ]}
        />
    );
};

export default ListView;
