import React, {useContext} from 'react';
import {Form, Input, InputNumber} from 'antd';
import GraphAnalysisContext from '../../../../Context';

const desp = {
    computer_cpu: 'master最大CPU',
    worker_cpu: 'worker最大CPU',
    master_request_memory: 'master最小内存，不满足最小内存则分配不成功',
    worker_request_memory: 'worker最小内存，不满足最小内存则分配不成功',
    master_memory: 'master最大内存，超过最大内存则会被k8s中止',
    worker_memory: 'worker最大内存，超过最大内存则会被k8s中止',
};

const OlapComputerItem = () => {

    const {isVermeer} =  useContext(GraphAnalysisContext);

    const formItem = (
        <>
            <Form.Item
                label='k8s.computer_cpu'
                name='k8s.computer_cpu'
                tooltip={desp.computer_cpu}
            >
                <InputNumber />
            </Form.Item>
            <Form.Item
                label='k8s.worker_cpu'
                name='k8s.worker_cpu'
                tooltip={desp.worker_cpu}
            >
                <InputNumber />
            </Form.Item>
            <Form.Item
                label='k8s.master_request_memory'
                name='k8s.master_request_memory'
                tooltip={desp.master_request_memory}
            >
                <Input />
            </Form.Item>
            <Form.Item
                label='k8s.worker_request_memory'
                name='k8s.worker_request_memory'
                tooltip={desp.worker_request_memory}
            >
                <Input />
            </Form.Item>
            <Form.Item
                label='k8s.master_memory'
                name='k8s.master_memory'
                tooltip={desp.master_memory}
            >
                <Input />
            </Form.Item>
            <Form.Item
                label='k8s.worker_memory'
                name='k8s.worker_memory'
                tooltip={desp.worker_memory}
            >
                <Input />
            </Form.Item>
        </>
    );

    return (
        isVermeer ? null : formItem
    );
};

export default OlapComputerItem;