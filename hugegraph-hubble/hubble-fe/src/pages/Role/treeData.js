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

import {Space, Switch, Tag, Modal, message} from 'antd';
import * as api from '../../api';
import {StatusA} from '../../components/Status';

const handleDelete = (data, refresh) => {
    Modal.confirm({
        title: `确定要删除资源 ${data.target_name} 吗？`,
        onOk: () => {
            api.auth.delRoleResource(data.graphspace, {
                role_id: data.role_id,
                target_id: data.target_id,
            }).then(res => {
                if (res.status === 200) {
                    message.success('删除成功');
                    refresh();
                    return;
                }

                message.error(res.message);
            });
        },
    });
};

const handleUpdate = (data, val, refresh) => {
    api.auth.updateRoleResource(data.graphspace, {
        role_id: data.role_id,
        target_id: data.target_id,
        permissions: val ? ['READ', 'WRITE', 'DELETE', 'EXECUTE'] : ['READ'],
    }).then(res => {
        if (res.status === 200) {
            message.success('操作成功');
            refresh();
            return;
        }
        message.error(res.message);
    });
};

const initTitle = item => {
    return (
        <>
            <Tag color='processing'>{item.label}</Tag>
            {item.properties
                && Object.keys(item.properties).map(k => (<Tag key={k}>{k}={item.properties[k]}</Tag>))}
        </>
    );
};

const treeData = (data, role, refresh) => {
    const checkREAD = permissions => {
        return permissions.length === 1 && permissions[0] === 'READ';
    };

    const isDefault = name => /.*DEFAULT_OBSERVER_GROUP$/.test(name);

    const meta = [];
    const list = [];
    let _vertex = [];
    let _edge = [];
    let _var = false;
    let _gremlin = false;
    let _task = false;

    for (let item of data.target_resources) {
        if (item.type === 'PROPERTY_KEY' || item.type === 'SCHEMA' || item.type === 'ALL') {
            meta.push({title: '属性类型', key: `${data.target_id}-PROPERTY_KEY`});
        }
        if (item.type === 'VERTEX_LABEL' || item.type === 'SCHEMA' || item.type === 'ALL') {
            meta.push({title: '顶点类型', key: `${data.target_id}-VERTEX_LABEL`});
        }
        if (item.type === 'EDGE_LABEL' || item.type === 'SCHEMA' || item.type === 'ALL') {
            meta.push({title: '边类型', key: `${data.target_id}-EDGE_LABEL`});
        }
        if (item.type === 'INDEX_LABEL' || item.type === 'SCHEMA' || item.type === 'ALL') {
            meta.push({title: '索引类型', key: `${data.target_id}-INDEX_LABEL`});
        }

        if (item.type === 'ALL' || item.type === 'VAR') {
            _var = {title: '变量', key: `${data.target_id}-VAR`};
        }
        if (item.type === 'ALL' || item.type === 'GREMLIN') {
            _gremlin = {title: 'gremlin查询', key: `${data.target_id}-GREMLIN`};
        }
        if (item.type === 'ALL' || item.type === 'TASK') {
            _task = {title: '任务', key: `${data.target_id}-TASK`};
        }

        if (item.type === 'ALL') {
            _vertex = '*';
            _edge = '*';
        }

        if (item.type === 'EDGE') {
            if (item.label === '*') {
                _edge = '*';
            }
            else {
                let title = initTitle(item);
                _edge.push({title, key: `${data.target_id}-${item.label}`});
            }
        }

        if (item.type === 'VERTEX') {
            if (item.label === '*') {
                _vertex = '*';
            }
            else {
                let title = initTitle(item);
                _vertex.push({title, key: `${data.target_id}-${item.label}`});
            }
        }
    }

    if (meta.length > 0) {
        list.push({title: '元数据', key: `${data.target_id}-meta`, children: meta});
    }

    if (_vertex === '*') {
        list.push({title: '全部顶点', key: `${data.target_id}-VERTEX`});
    }
    else if (_vertex.length > 0) {
        list.push({title: '顶点', key: `${data.target_id}-VERTEX`, children: _vertex});
    }

    if (_edge === '*') {
        list.push({title: '全部边', key: `${data.target_id}-EDGE`});
    }
    else if (_edge.length > 0) {
        list.push({title: '边', key: `${data.target_id}-EDGE`, children: _edge});
    }

    _var && list.push(_var);
    _gremlin && list.push(_gremlin);
    _task && list.push(_task);

    return {
        title: (
            <Space>
                {data.target_name}
                <Switch
                    checkedChildren="读写"
                    unCheckedChildren="只读"
                    checked={!checkREAD(data.permissions)}
                    onChange={val => handleUpdate(data, val, refresh)}
                    disabled={isDefault(role)}
                />
                <StatusA onClick={() => handleDelete(data, refresh)} disable={isDefault(role)}>删除</StatusA>
            </Space>
        ),
        key: data.target_id,
        children: list,
    };
};

export default treeData;
