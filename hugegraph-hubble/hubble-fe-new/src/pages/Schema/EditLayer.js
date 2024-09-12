import {Modal, Form, Input, message} from 'antd';
import {useState, useEffect, useCallback} from 'react';
import * as api from '../../api/index';
import * as rules from '../../utils/rules';

const EditLayer = ({visible, onCancel, graphspace, refresh, mode, detail}) => {
    const [form] = Form.useForm();
    const [loading, setLoading] = useState(false);

    const updateSchema = useCallback((name, data) => {
        api.manage.updateSchema(graphspace, name, data).then(res => {
            setLoading(false);
            if (res.status === 200) {
                message.success('编辑成功');
                onCancel();
                refresh();

                return;
            }

            message.error(res.message);
        });
    }, [graphspace, onCancel, refresh]);

    const addSchema = useCallback(data => {
        api.manage.addSchema(graphspace, data).then(res => {
            setLoading(false);
            if (res.status === 200) {
                message.success('新增成功');
                onCancel();
                refresh();

                return;
            }

            message.error(res.message);
        });
    }, [graphspace, onCancel, refresh]);

    const onFinish = useCallback(() => {
        // form.resetFields();
        if (mode === 'view') {
            onCancel();
            return;
        }

        form.validateFields().then(values => {
            setLoading(true);
            if (mode === 'create') {
                addSchema(values);
                return;
            }

            updateSchema(detail.name, values);
        });
    }, [addSchema, detail.name, form, mode, onCancel, updateSchema]);

    useEffect(() => {
        if (!visible) {
            return;
        }

        if (mode === 'create') {
            // form.setFieldsValue({name: '', schema: ''});
            form.resetFields();
        }
        else {
            form.setFieldsValue(detail);
        }
    }, [visible, detail.name, mode, form, detail]);

    return (
        mode === 'view'
            ? (
                <Modal
                    open={visible}
                    onCancel={onCancel}
                    title={'查看'}
                    width={600}
                    footer={null}
                >
                    <Form
                        form={form}
                        labelCol={{span: 6}}
                        preserve={false}
                    >
                        <Form.Item
                            label='schema模版名称'
                        >
                            {detail.name}
                        </Form.Item>
                        <Form.Item
                            label='schema'
                        >
                            {detail.schema}
                        </Form.Item>
                    </Form>
                </Modal>
            ) : (
                <Modal
                    open={visible}
                    onCancel={onCancel}
                    title={mode === 'edit' ? '编辑' : '创建'}
                    width={600}
                    onOk={onFinish}
                    confirmLoading={loading}
                    destroyOnClose
                >
                    <Form
                        form={form}
                        labelCol={{span: 6}}
                        validateTrigger='onBlur'
                        preserve={false}
                    >
                        <Form.Item
                            label='schema模版名称'
                            rules={[rules.required(), rules.isName, {type: 'string', max: 48}]}
                            name='name'
                        >
                            <Input placeholder='请输入schema名称' disabled={mode === 'edit'} />
                        </Form.Item>
                        <Form.Item
                            label='schema'
                            rules={[rules.required()]}
                            name='schema'
                        >
                            <Input.TextArea placeholder='请输入schema' />
                        </Form.Item>
                    </Form>
                </Modal>
            )
    );
};

export default EditLayer;
