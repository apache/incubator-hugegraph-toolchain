import {Row, Col} from 'antd';
import style from './index.module.scss';

const CardBox = ({title, listItems}) => {

    return (
        <div className={style.cardbox}>
            <div className={style.title}>{title}</div>
            <Row justify='space-around'>
                {listItems.map(item => {
                    return (
                        <Col key={item.label}>
                            <div className={style.value}>{item.value ?? 0}</div>
                            <div className={style.label}>{item.label}</div>
                        </Col>
                    );
                })}
            </Row>
        </div>
    );
};

const TopStatistic = ({data}) => {

    return (
        <Row className={style.row}>
            <Col span={5}>
                <CardBox
                    title='实时任务'
                    listItems={[
                        {label: '最大并发', value: data.total_realtime_size},
                    ]}
                />
            </Col>
            <Col span={5}>
                <CardBox
                    title='非实时任务'
                    listItems={[
                        {label: '最大并发', value: data.total_other_size},
                    ]}
                />
            </Col>
            <Col span={7}>
                <CardBox
                    title='待执行'
                    listItems={[
                        {label: '一次性任务', value: data?.todo?.ONCE},
                        {label: '周期任务', value: data?.todo?.CRON},
                        {label: '实时任务', value: data?.todo?.REALTIME},
                    ]}
                />
            </Col>
            <Col span={7} className={style.last}>
                <CardBox
                    title='正在执行'
                    listItems={[
                        {label: '一次性任务', value: data?.running?.ONCE},
                        {label: '周期任务', value: data?.running?.CRON},
                        {label: '实时任务', value: data?.running?.REALTIME},
                    ]}
                />
            </Col>
        </Row>
    );
};

export default TopStatistic;
