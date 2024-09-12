import {useCallback} from 'react';
import {Card, Dropdown, Menu, Typography, Tooltip} from 'antd';
import {UnorderedListOutlined, EyeOutlined} from '@ant-design/icons';
import {useNavigate} from 'react-router-dom';
import GraphView from '../../components/GraphinView';
import moment from 'moment';
import _ from 'lodash';
import {formatToGraphInData} from '../../utils/formatGraphInData';
import style from './index.module.scss';
import {byteConvert} from '../../utils/format';

const TitleField = ({item, onClick}) => (
    <>
        <Typography.Text
            style={{maxWidth: 244}}
            ellipsis={{ellipsis: true}}
            title={`${item.graphspace_nickname}-${item.nickname}`}
            onClick={onClick}
        >
            {_.truncate(item.graphspace_nickname, {length: 12})}-{_.truncate(item.nickname, {length: 12})}
        </Typography.Text>
        {item.default && <span className={style.default}>默认</span>}
        <div className={style.subtitle}>
            存储空间：{item.storage >= 0 ? byteConvert(item.storage) : '--'}
        </div>
    </>
);

const GraphCard = ({item, menus}) => {
    const navigate = useNavigate();
    const graphinData = formatToGraphInData(item.schemaview, false);

    const handleGotoAnalysis = useCallback(() => {
        navigate(`/gremlin/${item.graphspace}/${item.name}`);
    }, [item, navigate]);

    const handleGotoDetail = useCallback(() => {
        navigate(`/graphspace/${item.graphspace}/graph/${item.name}/detail`);
    }, [item, navigate]);

    return (
        <Card
            className={style.card}
            title={<TitleField item={item} onClick={handleGotoAnalysis} />}
            headStyle={{
                paddingLeft: 20,
            }}
            extra={(
                <Dropdown
                    overlay={<Menu
                        items={menus}
                    />}
                    trigger={['click']}
                >
                    <UnorderedListOutlined />
                </Dropdown>
            )}
            actions={[
                <span key="setting" onClick={handleGotoAnalysis}>
                    创建时间：{moment(item.create_time).format('YYYY-MM-DD')}
                </span>,
                <span key='statistic' onClick={handleGotoDetail}>
                    <Tooltip title={'点击可以查看本图目前存储的点边的数量'}><EyeOutlined />详情</Tooltip>
                </span>,
            ]}
        >
            <div className={style.card_content} onClick={handleGotoAnalysis}>
                <GraphView
                    data={graphinData}
                    style={{minHeight: '153px'}}
                    layout={{
                        type: 'gForce',
                        // center: [200, 200],
                        linkDistance: 100,
                        coulombDisScale: 0.01,
                        // preventOverlap: true,
                        // begin: item.schemaview.vertices.length > 1
                        //     ? [0, 0] : [200, 100],
                    }}
                    height={147}
                    config={{
                        // minZoom: 0.6,
                        // maxZoom: 0.6,
                        fitView: false,
                        fitCenter: true,
                        // handleZoomIn: false,
                    }}
                    behaviors={{
                        zoomCanvas: {disabled: true},
                        dragNode: {disabled: true},
                        dragCanvas: {disabled: true},
                        clickSelect: {disabled: true},
                        hoverable: {disabled: true},
                    }}
                />
            </div>
        </Card>
    );
};

export default GraphCard;
