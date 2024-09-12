/**
 * @file 运维管理子项块
 * @author
 */
import {useEffect, useState} from 'react';
import {message} from 'antd';

import * as api from '../../../api';
import Item from '../Item';

const ConsoleItem = () => {

    const [data, setData] = useState({});

    useEffect(() => {
        api.auth.getDashboard().then(res => {
            const {
                status,
                data,
                message: errMsg,
            } = res || {};
            if (status === 200) {
                setData(res.data);
            } else {
                !errMsg && message.error('获取dashboard失败');
            }
        });
    }, []);
    const address = data?.address;
    const clusterUrl = address ? `http://${address}` : '';
    const machineUrl = address ? clusterUrl + '/monitor/machine' : '';
    const nodeUrl = address ? clusterUrl + '/operate/node' : '';
    const ruleUrl = address ? clusterUrl + '/alert/rule' : '';

    return (
        <Item
            btnIndex={4}
            btnTitle={'运维管理'}
            listData={[
                {
                    title: '集群管理',
                    url: clusterUrl,
                },
                {
                    title: '监控管理',
                    url: machineUrl,
                },
                {
                    title: '运维管理',
                    url: nodeUrl,
                },
                {
                    title: '报警管理',
                    url: ruleUrl,
                },
            ]}
        />
    );
};

export default ConsoleItem;
