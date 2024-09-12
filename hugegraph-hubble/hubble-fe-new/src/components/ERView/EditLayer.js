import {Form, Modal, Select} from 'antd';
import vertexData from './data/vertex.json';
import edgeData from './data/edge.json';
import {useCallback} from 'react';

const EditVertexLayer = ({open, onCancle, onChange}) => {
    const [form] = Form.useForm();

    const setVertex = useCallback((_, item) => {
        form.setFieldValue('vertex', item.info);
    }, [form]);

    const onFinish = useCallback(() => {
        console.log(form.getFieldsValue());
        onChange(form.getFieldValue('vertex'));
        onCancle();
    }, [form, onChange, onCancle]);

    return (
        <Modal
            title='新增顶点'
            onCancel={onCancle}
            open={open}
            onOk={onFinish}
        >
            <Form form={form}>
                <Form.Item label='顶点类型'>
                    <Select
                        options={vertexData.map(item => ({label: item.name, value: item.name, info: item}))}
                        onChange={setVertex}
                    />
                    <Form.Item name='vertex' />
                </Form.Item>
            </Form>
        </Modal>
    );
};

const EditEdgeLayer = ({open, onCancle, onChange}) => {
    const [form] = Form.useForm();

    const setEdge = useCallback((_, item) => {
        form.setFieldValue('edge', item.info);
    }, [form]);

    const onFinish = useCallback(() => {
        onChange(form.getFieldValue('edge'));
        onCancle();
    }, [form, onChange, onCancle]);

    return (
        <Modal
            title='新增边'
            onCancel={onCancle}
            open={open}
            onOk={onFinish}
        >
            <Form form={form}>
                <Form.Item label='边类型'>
                    <Select
                        options={edgeData.map(item => ({label: item.name, value: item.name, info: item}))}
                        onChange={setEdge}
                    />
                    <Form.Item name='edge' />
                </Form.Item>
            </Form>
        </Modal>
    );
};

export {EditVertexLayer, EditEdgeLayer};