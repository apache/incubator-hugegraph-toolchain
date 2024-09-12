import {PageHeader, Row, Col, Button, Spin, message, Space, Table} from 'antd';
import {useCallback, useEffect, useState} from 'react';
import {useNavigate, useParams} from 'react-router-dom';
import * as api from '../../api';
import style from './index.module.scss';
import vertexSvg from './assets/aaa.svg';
import edgeSvg from './assets/collaboration-full.svg';

const GraphDetail = () => {
    const [graphspaceInfo, setGraphspaceInfo] = useState({});
    const [graphIno, setGraphInfo] = useState({});
    const [loading, setLoading] = useState({graph: true, graphspace: true});
    const [statistic, setStatistic] = useState({});
    const {graphspace, graph} = useParams();
    const navigate = useNavigate();

    const handleBack = useCallback(() => {
        navigate(-1);
    }, [navigate]);

    const handleUpdate = useCallback(() => {
        api.manage.updateGraphStatistic(graphspace, graph).then(res => {
            if (res.status === 200) {
                message.success('请求成功，请稍后刷新页面查看结果');
                return;
            }

            message.error(res.message);
        });
    }, [graphspace, graph]);

    const formatList = data => {
        if (!data || Object.keys(data).length === 0) {
            return [];
        }

        return Object.keys(data).map(item => ({key: item, num: data[item]}));
    };

    useEffect(() => {
        if (!graphspace || !graph) {
            return;
        }

        api.manage.getGraphSpace(graphspace).then(res => {
            if (res.status === 200) {
                setGraphspaceInfo(res.data);
                setLoading(l => ({...l, graphspace: false}));
                return;
            }

            message.error(res.message);
        });

        api.manage.getGraph(graphspace, graph).then(res => {
            if (res.status === 200) {
                setGraphInfo(res.data);
                setLoading(l => ({...l, graph: false}));
                return;
            }

            setGraphInfo({});
            message.error(res.message);
        });

        api.manage.getGraphStatistic(graphspace, graph).then(res => {
            if (res.status === 200) {
                setStatistic(res.data);
                return;
            }

            message.error(res.message);
        });
    }, [graphspace, graph]);

    return (
        <Spin spinning={loading.graph || loading.graphspace}>
            {!loading.graph && !loading.graphspace && (
                <>
                    <PageHeader
                        ghost={false}
                        onBack={handleBack}
                        title={`${graphspaceInfo.nickname} - ${graphIno.nickname} - 详情`}
                    />

                    <div className={'container'}>
                        <>
                            <Row justify='end' className={style.top}>
                                <Col>
                                    <Space>
                                        <span>最近更新时间：{statistic.update_time ?? '--/--'}</span>
                                        <Button type='primary' onClick={handleUpdate}>数据更新</Button>
                                    </Space>
                                </Col>
                            </Row>

                            <Row gutter={[10, 10]}>
                                <Col span={12}>
                                    <div>
                                        <Row className={style.type}>
                                            <Col span={6} className={style.vertex}>
                                                <img width={20} src={vertexSvg} />
                                                <span>点总数</span>
                                            </Col>
                                            <Col span={18}>{statistic.vertex_count ?? 0}</Col>
                                        </Row>
                                        <Table
                                            columns={[
                                                {title: '点类型', dataIndex: 'key'},
                                                {title: '数量', dataIndex: 'num'},
                                            ]}
                                            dataSource={formatList(statistic.vertices)}
                                            className={style.card}
                                            pagination={false}
                                        />
                                    </div>
                                </Col>

                                <Col span={12}>
                                    <div>
                                        <Row className={style.type}>
                                            <Col span={6} className={style.edge}>
                                                <img width={20} src={edgeSvg} />边总数
                                            </Col>
                                            <Col span={18}>{statistic.edge_count ?? 0}</Col>
                                        </Row>
                                        <Table
                                            columns={[
                                                {title: '边类型', dataIndex: 'key'},
                                                {title: '数量', dataIndex: 'num'},
                                            ]}
                                            dataSource={formatList(statistic.edges)}
                                            pagination={false}
                                            className={style.card}
                                        />
                                    </div>
                                </Col>
                            </Row>
                        </>
                    </div>
                </>
            )}
        </Spin>
    );
};

export default GraphDetail;
