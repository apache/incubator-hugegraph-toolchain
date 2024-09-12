// import {PageHeader, Button, Space, Table, message, Tooltip, Modal, Input, Row, Col} from 'antd';
// import {useCallback, useEffect, useState} from 'react';
// import TableHeader from '../../components/TableHeader';
// import * as api from '../../api';
// TODO  REMOVED
// const Bill = () => {
//     const [data, setData] = useState([]);
//     const [refresh, setRefresh] = useState(false);
//     const [search, setSearch] = useState('');
//     const [pagination, setPagination] = useState({toatal: 0, current: 1, pageSize: 10});
//
//     const handleSearch = useCallback(value => {
//         setRefresh(!refresh);
//         setSearch(value);
//     }, [refresh]);
//
//
//     const handleTable = useCallback(page => {
//         setPagination({...pagination, ...page});
//     }, [pagination]);
//
//     const columns = [
//         {
//             title: '出账时间',
//             width: 180,
//             render: (_, record) => {
//                 return (
//                     <>
//                         <div>{record.properties.create_time}</div>
//                     </>
//                 );
//             },
//         },
//         {
//             title: '账单时间',
//             width: 100,
//             render: (_, record) => {
//                 return (
//                     <>
//                         <div>{record.properties.bill_date}</div>
//                     </>
//                 );
//             },
//         },
//         {
//             title: '订单ID',
//             width: 200,
//             render: (_, record) => record.properties.order_id,
//         },
//         {
//             title: '业务名称',
//             width: 100,
//             render: (_, record) => record.properties.name,
//         },
//         {
//             title: '结算账号',
//             align: 'center',
//             width: 320,
//             render: (_, record) => {
//                 return (
//                     <>
//                         <div>{record.properties.account_name}</div>
//                         <div>({record.properties.account_id})</div>
//                     </>
//                 );
//             },
//         },
//         {
//             title: '订单内容',
//             ellipsis: {showTitle: false},
//             render: (_, record) => {
//                 const goods = JSON.parse(record.properties.goods);
//                 const val = goods.map(item =>
//                     `${item.name} - ${item.custom > 0 ? item.custom : item.total}
//                     - ${item.discount > 0 ? item.discount : 0}
//                     - ${item.reduce > 0 ? item.reduce : 0}`).join(';');
//                 return (
//                     <Tooltip title={val} placement='bottomLeft'>
//                         {val}
//                     </Tooltip>
//                 );
//             },
//         },
//     ];
//
//     const rowKey = useCallback(item => item.user_name, []);
//
//     useEffect(() => {
//         api.cloud.getBillList({s: search}).then(res => {
//             if (res.status === 200) {
//                 setData(res.data);
//                 // setPagination({...pagination, total: res.data.total});
//                 return;
//             }
//
//             message.error(res.message);
//         });
//         setData([]);
//     }, [refresh, search]);
//
//     return (
//         <>
//             <PageHeader
//                 ghost={false}
//                 onBack={false}
//                 title={'账单管理'}
//             />
//
//             <div className='container'>
//                 <TableHeader>
//                     <Row justify="space-between">
//                         <Col>
//                             <Input.Search
//                                 placeholder='输入订单ID/业务名称/结算账号搜索'
//                                 onSearch={handleSearch}
//                                 style={{width: 300}}
//                             />
//                         </Col>
//                     </Row>
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
//         </>
//     );
// };
//
// export default Bill;
