import {PageHeader, Button, Space, Table, message, Row, Col, Modal, Select} from 'antd';
import {useCallback, useEffect, useState} from 'react';
import TableHeader from '../../components/TableHeader';
import {AddLayer} from './EditLayer';
import * as api from '../../api';

const statusOptions = [
    {label: '结算中', value: 1},
    {label: '暂停结算', value: 2},
    {label: '已完结', value: 3},
];

const Order = () => {
    const [addLayerVisible, setAddLayerVisible] = useState(false);
    const [loading, setLoading] = useState(false);
    const [data, setData] = useState([]);
    const [refresh, setRefresh] = useState(false);
    const [id, setId] = useState('');
    const [status, setStatus] = useState('');
    const [pagination, setPagination] = useState({total: 0, current: 1, pageSize: 10});

    const handleEdit = useCallback(row => {
        setId(row.id);
        setAddLayerVisible(true);
    }, []);

    const handleAdd = useCallback(row => {
        setId('');
        setAddLayerVisible(true);
    }, []);

    const handleRefresh = useCallback(() => {
        setRefresh(!refresh);
    }, [refresh]);

    const handleHideAddLayer = useCallback(() => {
        setAddLayerVisible(false);
    }, []);

    const handleTable = useCallback(page => {
        setPagination({...pagination, ...page});
    }, [pagination]);

    const handleStatus = useCallback((record, status) => {
        const text = {1: '重新开启', 2: '暂停', 3: '终止'};

        Modal.confirm({
            title: `确定${text[status]}吗?`,
            content: `确定${text[status]}订单${record.properties.order_id}吗?`,
            onOk: () => {
                api.cloud.updateOrderStatus(record.id, status).then(res => {
                    if (res.status === 200) {
                        message.success('操作成功');
                        setRefresh(!refresh);
                        return;
                    }

                    message.error(res.message);
                });
            },
        });
    }, [refresh]);

    const handleFilter = useCallback(value => {
        setStatus(value);
        setRefresh(!refresh);
    }, [refresh]);

    const columns = [
        {
            title: '订单id',
            width: 200,
            align: 'center',
            render: (_, record) => (
                <div style={{wordWrap: 'break-word', wordBreak: 'break-all'}}>
                    {record.properties.order_id}
                </div>
            ),
        },
        {
            title: '业务名称',
            width: 120,
            align: 'center',
            render: (_, record) => record.properties.name,
        },
        {
            title: '结算账户',
            align: 'center',
            width: 340,
            render: (_, record) => {
                return (
                    <>
                        <div>{record.properties.account_name}</div>
                        <div style={{wordWrap: 'break-word', wordBreak: 'break-all'}}>
                            ({record.properties.account_id})
                        </div>
                    </>
                );
            },
        },
        {
            title: '状态',
            align: 'center',
            width: 120,
            render: (_, record) => {
                return record.properties.status
                    ? statusOptions.find(item => item.value === Number(record.properties.status)).label : '';
            },
        },
        {
            title: '添加时间',
            dataIndex: 'd',
            align: 'center',
            width: 120,
            render: (_, record) => record.properties.create_time,
        },
        {
            title: '本期结算',
            align: 'center',
            // width: 300,
            render: (_, record) => {
                const goodsList = JSON.parse(record.properties.goods);
                return (
                    <Space direction="vertical">
                        {goodsList.map(goods => (
                            <Space key={goods.id}>
                                <span>{goods.name}</span>
                                <span>- {goods.custom > 0 ? goods.custom : goods.total}</span>
                                <span>- {goods.discount > 0 ? goods.discount : 0}</span>
                                <span>- {goods.reduce > 0 ? goods.reduce : 0}</span>
                            </Space>
                        ))}
                    </Space>
                );
            },
        },
        {
            title: '操作',
            width: 160,
            align: 'center',
            render: record => (
                <Space>
                    {record.properties.status !== '3' && <a onClick={() => handleEdit(record)}>编辑</a>}
                    {record.properties.status === '1' && <a onClick={() => handleStatus(record, 2)}>暂停</a>}
                    {record.properties.status === '2' && <a onClick={() => handleStatus(record, 1)}>重新开启</a>}
                    {record.properties.status !== '3' && <a onClick={() => handleStatus(record, 3)}>终止</a>}
                </Space>
            ),
        },
    ];

    const rowKey = useCallback(item => item.user_name, []);

    useEffect(() => {
        setLoading(true);
        api.cloud.getOrderList({
            status: status,
            // query: search,
            // page_no: pagination.current,
            // page_size: pagination.pageSize,
        }).then(res => {
            setLoading(false);
            if (res.status === 200) {
                setData(res.data);
                setPagination({...pagination, total: res.data.total});
                return;
            }

            message.error(res.message);
        });
        setData([]);
    }, [refresh, status]);

    return (
        <>
            <PageHeader
                ghost={false}
                onBack={false}
                title={'结算管理'}
            />

            <div className='container'>
                <TableHeader>
                    <Row justify="space-between">
                        <Col><Button onClick={handleAdd} type='primary'>新增结算</Button></Col>
                        <Col>
                            筛选：
                            <Select
                                options={[
                                    {label: '全部', value: ''},
                                    {label: '结算中', value: '1'},
                                    {label: '暂停', value: '2'},
                                    {label: '已终止', value: '3'},
                                ]}
                                onChange={handleFilter}
                                defaultValue={''}
                                style={{width: 120}}
                            />
                        </Col>
                    </Row>
                </TableHeader>

                <Table
                    columns={columns}
                    dataSource={data}
                    rowKey={rowKey}
                    pagination={pagination}
                    onChange={handleTable}
                    loading={loading}
                />
            </div>

            <AddLayer
                visible={addLayerVisible}
                onCancel={handleHideAddLayer}
                refresh={handleRefresh}
                id={id}
            />
        </>
    );
};

export default Order;
