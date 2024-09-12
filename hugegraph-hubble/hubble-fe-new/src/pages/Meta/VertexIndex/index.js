import {Table} from 'antd';
import {indexTypeOptions} from '../common/config.js';
import {useEffect, useState, useCallback} from 'react';
import * as api from '../../../api';
import {useParams} from 'react-router-dom';

const VertexIndexTable = () => {
    const [data, setData] = useState([]);
    const [pagination, setPagination] = useState({current: 1, total: 10});
    const {graphspace, graph} = useParams();

    const handleTable = useCallback(newPagination => {
        setPagination(newPagination);
    }, []);

    const columns = [
        {
            title: '顶点类型名称',
            dataIndex: 'owner',
        },
        {
            title: '索引名称',
            dataIndex: 'name',
        },
        {
            title: '索引类型',
            dataIndex: 'type',
            render: val => indexTypeOptions.find(item => item.value === val)?.label || val,
        },
        {
            title: '属性',
            dataIndex: 'fields',
            render: val => val.join(','),
        },
    ];

    useEffect(() => {
        api.manage.getMetaVertexIndexList(graphspace, graph, {
            page_no: pagination.current,
        }).then(res => {
            if (res.status === 200) {
                setData(res.data.records);
                setPagination({...pagination, total: res.data.total});
            }
        });
    // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [pagination.current, graph, graphspace]);

    return (
        <>
            {/* <Row>
                <Col>
                    <Input.Search />
                </Col>
            </Row> */}

            <Table
                columns={columns}
                dataSource={data}
                pagination={pagination}
                onChange={handleTable}
            />
        </>
    );
};

export default VertexIndexTable;
