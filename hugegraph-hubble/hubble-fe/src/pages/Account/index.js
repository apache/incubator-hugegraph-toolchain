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

import {
    PageHeader,
    Button,
    Space,
    Table,
    message,
    Tooltip,
    Modal,
} from 'antd';
import {useCallback, useEffect, useState} from 'react';
import {PlusOutlined} from '@ant-design/icons';
import TableHeader from '../../components/TableHeader';
import EditLayer from './EditLayer';
import * as api from '../../api';
import {getUser} from '../../utils/user';
import Upload from 'antd/lib/upload/Upload';

const IMPORT_TEXT = '导入账号文件，支持csv格式';
const IMPORT_ING = '导入中...';
const IMPORT_SUCCESS = '导入成功';
const IMPORT_ERROR = '导入失败';

const Account = () => {
    const [editLayerVisible, setEditLayerVisible] = useState(false);
    const [addLayerVisible, setAddLayerVisible] = useState(false);
    const [op, setOp] = useState('detail');
    const [detail, setDetail] = useState({});
    const [data, setData] = useState([]);
    const [refresh, setRefresh] = useState(false);
    const [search, setSearch] = useState('');
    const [pagination, setPagination] = useState({toatal: 0, current: 1, pageSize: 10});
    const [importTip, setImportTip] = useState(IMPORT_TEXT);

    const showDetail = row => {
        setDetail(row);
        setOp('detail');
        setEditLayerVisible(true);
    };

    const showEdit = row => {
        setDetail(row);
        setOp('edit');
        setEditLayerVisible(true);
    };

    const showAuth = row => {
        setDetail(row);
        setOp('auth');
        setEditLayerVisible(true);
    };

    const showAdd = useCallback(() => {
        setDetail({});
        setOp('create');
        setEditLayerVisible(true);
    }, []);

    const handleRefresh = useCallback(() => {
        setRefresh(!refresh);
    }, [refresh]);

    const handleHideLayer = useCallback(() => {
        setEditLayerVisible(false);
    }, []);

    const handleHideAddLayer = useCallback(() => {
        setAddLayerVisible(false);
    }, []);

    const handleSearch = useCallback(value => {
        setRefresh(!refresh);
        setSearch(value);
    }, [refresh]);

    const handleDelete = row => {
        Modal.confirm({
            title: `确定要删除账号 ${row.user_name}吗？`,
            onOk: () => {
                api.auth.delUser(row.id).then(res => {
                    if (res.status === 200) {
                        message.success('删除成功');
                        setRefresh(!refresh);
                        return;
                    }
                    message.error(res.message);
                });
            },
        });
    };

    const handleImport = useCallback(({file}) => {
        if (file?.status === 'uploading') {
            setImportTip(IMPORT_ING);
        }

        if (file?.status === 'done') {
            setImportTip(file?.response?.status === 200 ? IMPORT_SUCCESS : IMPORT_ERROR);
        }
    }, []);

    const handleTable = useCallback(page => {
        setPagination({...pagination, ...page});
    }, [pagination]);

    const columns = [
        {
            title: '账号ID',
            dataIndex: 'user_name',
        },
        {
            title: '账号名',
            dataIndex: 'user_nickname',
        },
        {
            title: '备注',
            dataIndex: 'user_description',
            ellipsis: {showTitle: false},
            render: val => <Tooltip title={val} placement='bottomLeft'>{val}</Tooltip>,
        },
        {
            title: '资源权限',
            dataIndex: 'spacenum',
            width: 120,
        },
        {
            title: '创建时间',
            dataIndex: 'user_create',
            align: 'center',
            width: 200,
        },
        {
            title: '操作',
            width: 300,
            align: 'center',
            render: row => (
                <Space>
                    <a onClick={() => showDetail(row)}>详情</a>
                    {<a onClick={() => showEdit(row)}>编辑</a>}
                    <a onClick={() => showAuth(row)}>分配权限</a>
                    {row.user_name !== 'admin'
                        && row.user_name !== getUser().id
                        && <a onClick={() => handleDelete(row)}>删除</a>}
                </Space>
            ),
        },
    ];

    const rowKey = useCallback(item => item.user_name, []);

    useEffect(() => {
        api.auth.getAllUserList({
            query: search,
            page_no: pagination.current,
            page_size: pagination.pageSize,
        }).then(res => {
            if (res.status === 200) {
                setData(res.data.records);
                setPagination({...pagination, total: res.data.total});
                return;
            }

            message.error(res.message);
        });
    }, [refresh, pagination.current, pagination.pageSize, search]);

    return (
        <>
            <PageHeader
                ghost={false}
                onBack={false}
                title={'账号管理'}
            />

            <div className='container'>
                // TODO remove here
                {/* <TableHeader> */}
                {/*     <Row justify="space-between"> */}
                {/*         <Col><Button onClick={showAdd} type='primary'><PlusOutlined
                 />添加账号</Button></Col> */}
                {/*         <Col> */}
                {/*             <Input.Search */}
                {/*                 placeholder='输入账号ID或账号名进行搜索' */}
                {/*                 onSearch={handleSearch} */}
                {/*                 style={{width: 240}} */}
                {/*             /> */}
                {/*         </Col> */}
                {/*     </Row> */}
                {/* </TableHeader> */}
                <TableHeader>
                    <Space>
                        <Button onClick={showAdd} type='primary'>创建账号</Button>
                        <Upload
                            key='2'
                            action={api.auth.importUserUrl}
                            showUploadList={false}
                            accept='*.csv'
                            onChange={handleImport}
                        >
                            <Tooltip placement='bottom' title={importTip}><Button>导入账号</Button></Tooltip>
                        </Upload>
                    </Space>
                </TableHeader>

                <Table
                    columns={columns}
                    dataSource={data}
                    rowKey={rowKey}
                    pagination={pagination}
                    onChange={handleTable}
                />
            </div>

            <EditLayer
                visible={editLayerVisible}
                op={op}
                data={detail}
                onCancel={handleHideLayer}
                refresh={handleRefresh}
            />
        </>
    );
};

export default Account;
