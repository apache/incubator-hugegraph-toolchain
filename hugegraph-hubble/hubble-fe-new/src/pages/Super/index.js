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

// import {PageHeader, Button, Space, Table, message, Popconfirm, Tooltip} from 'antd';
// import {useCallback, useEffect, useState} from 'react';
// import {PlusOutlined} from '@ant-design/icons';
// import TableHeader from '../../components/TableHeader';
// import EditLayer from './EditLayer';
// import * as api from '../../api';
// import {getUser} from '../../utils/user';
// TODO REMOVE SUPER
// const Super = () => {
//     const [editLayerVisible, setEditLayerVisible] = useState(false);
//     const [op, setOp] = useState('detail');
//     const [detail, setDetail] = useState({});
//     const [data, setData] = useState([]);
//     const [refresh, setRefresh] = useState(false);
//     const [pagination, setPagination] = useState({toatal: 0, current: 1, pageSize: 10});
//
//     const showAdd = useCallback(() => {
//         setDetail({});
//         setOp('create');
//         setEditLayerVisible(true);
//     }, []);
//
//     const handleRefresh = useCallback(() => {
//         setRefresh(!refresh);
//     }, [refresh]);
//
//     const handleHideLayer = useCallback(() => {
//         setEditLayerVisible(false);
//     }, []);
//
//     const handleTable = useCallback(page => {
//         setPagination({...pagination, ...page});
//     }, [pagination]);
//
//     const RemoveSuper = ({data}) => {
//         const handleDelete = useCallback(() => {
//             api.auth.removeSuperUser(data.id).then(res => {
//                 if (res.status === 200) {
//                     message.success('超级管理员移除成功');
//                     setRefresh(!refresh);
//                     return;
//                 }
//
//                 message.error(`超级管理员移除失败。失败原因：${res.message}`);
//             });
//         }, [data.id]);
//
//         return data.user_name === getUser().id
//             ? '-'
//             : (
//                 <Popconfirm
//                     title="你确定要移除该超管吗？"
//                     onConfirm={handleDelete}
//                 >
//                     <a>移除超管</a>
//                 </Popconfirm>
//             );
//     };
//
//     const columns = [
//         {
//             title: '账号ID',
//             dataIndex: 'user_name',
//         },
//         {
//             title: '账号名',
//             dataIndex: 'user_nickname',
//         },
//         {
//             title: '备注',
//             dataIndex: 'user_description',
//             ellipsis: {showTitle: false},
//             render: val => <Tooltip title={val} placement='bottomLeft'>{val}</Tooltip>,
//         },
//         {
//             title: '添加时间',
//             dataIndex: 'user_create',
//             align: 'center',
//             width: 200,
//         },
//         {
//             title: '操作',
//             width: 300,
//             align: 'center',
//             render: row => <RemoveSuper data={row} />,
//         },
//     ];
//
//     const rowKey = useCallback(item => item.user_name, []);
//
//     useEffect(() => {
//         api.auth.getSuperUser({
//             page_no: pagination.current,
//             page_size: pagination.pageSize,
//         }).then(res => {
//             if (res.status === 200) {
//                 setData(res.data.records);
//                 setPagination({...pagination, total: res.data.total});
//                 return;
//             }
//
//             message.error(res.message);
//         });
//     }, [refresh, pagination.current, pagination.pageSize]);
//
//     return (
//         <>
//             <PageHeader
//                 ghost={false}
//                 onBack={false}
//                 title={'超管管理'}
//             />
//
//             <div className='container'>
//                 <TableHeader>
//                     <Space>
//                         <Button onClick={showAdd} type='primary'><PlusOutlined />添加超管</Button>
//                     </Space>
//                 </TableHeader>
//
//                 <Table
//                     columns={columns}
//                     dataSource={data}
//                     rowKey={rowKey}
//                     pagination={pagination}
//                     onChange={handleTable}
//                 />
//             </div>
//
//             <EditLayer
//                 visible={editLayerVisible}
//                 op={op}
//                 data={detail}
//                 onCancel={handleHideLayer}
//                 refresh={handleRefresh}
//             />
//         </>
//     );
// };
//
// export default Super;
