import {PageHeader, Row, Col, Radio, Spin, message} from 'antd';
import {useCallback, useEffect, useState} from 'react';
import ImageView from './ImageView';
import ListView from './ListView';
import {useParams, useNavigate} from 'react-router-dom';
import * as api from '../../api';

const Meta = () => {
    const [viewType, setViewType] = useState('list');
    const [graphIno, setGraphInfo] = useState(false);
    const [graphspaceInfo, setGraphspaceInfo] = useState(false);
    const {graphspace, graph} = useParams();
    const navigate = useNavigate();

    const handlePageBack = useCallback(() => {
        // navigate(`/graphspace/${graphspace}`);
        navigate(-1);
    }, [navigate]);

    const handleChangeViewType = useCallback(e => {
        setViewType(e.target.value);
    }, []);

    useEffect(() => {
        if (!graphspace || !graph) {
            return;
        }

        api.manage.getGraph(graphspace, graph).then(res => {
            if (res.status === 200) {
                setGraphInfo(res.data);
                return;
            }

            setGraphInfo({});
            message.error(res.message);
        });

        api.manage.getGraphSpace(graphspace).then(res => {
            if (res.status === 200) {
                setGraphspaceInfo(res.data);
                return;
            }

            setGraphspaceInfo({});
            message.error(res.message);
        });
    }, [graphspace, graph]);

    return (
        <>
            <Spin spinning={graphIno === false || graphspaceInfo === false}>
                <PageHeader
                    ghost={false}
                    onBack={handlePageBack}
                    title={`${graphspaceInfo.nickname} - ${graphIno.nickname} - 元数据管理`}
                >
                    <Row justify='space-between'>
                        <Col>
                            <Radio.Group
                                options={[{label: '列表模式', value: 'list'}, {label: '图模式', value: 'image'}]}
                                optionType='button'
                                buttonStyle='solid'
                                defaultValue={'list'}
                                onChange={handleChangeViewType}
                            />
                        </Col>
                    </Row>
                </PageHeader>

                <div className='container'>
                    {viewType === 'list'
                        ? (
                            <ListView />
                        )
                        : (
                            <ImageView />
                        )
                    }
                </div>
            </Spin>
        </>
    );
};

export default Meta;
