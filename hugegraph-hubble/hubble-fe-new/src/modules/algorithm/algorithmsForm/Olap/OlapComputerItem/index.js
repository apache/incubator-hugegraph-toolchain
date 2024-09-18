import React, {useContext} from 'react';
import {Form, Input, InputNumber} from 'antd';
import GraphAnalysisContext from '../../../../Context';
import {TEXT_PATH} from '../../../../../utils/constants';
import {useTranslation} from 'react-i18next';


const OWNED_TEXT_PATH = TEXT_PATH.OLAP + '.computer_item';
const OlapComputerItem = () => {
    const {t} = useTranslation();
    const {isVermeer} =  useContext(GraphAnalysisContext);
    const desp = {
        computer_cpu: t(`${OWNED_TEXT_PATH}.computer_cpu`),
        worker_cpu: t(`${OWNED_TEXT_PATH}.worker_cpu`),
        master_request_memory: t(`${OWNED_TEXT_PATH}.master_request_memory`),
        worker_request_memory: t(`${OWNED_TEXT_PATH}.worker_request_memory`),
        master_memory: t(`${OWNED_TEXT_PATH}.master_memory`),
        worker_memory: t(`${OWNED_TEXT_PATH}.worker_memory`),
    };
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
