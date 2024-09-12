import {Modal, Input, Form, Select, message, TreeSelect, Space, Checkbox, Spin, InputNumber, Alert} from 'antd';
import {useCallback, useEffect, useState} from 'react';
import * as api from '../../api';
import * as rules from '../../utils/rules';
import {SelectStaffUser} from '../../components/SelectUser';

const goodsList = [
    {name: '标准图数据库SaaS服务', id: 'HugeGraphNormal', unit: '亿'},
    {name: '高级图计算SaaS服务', id: 'HugeGraphAdvanced', unit: '亿'},
    {name: '定制OLTP算法', id: 'HugeGraphOLTPCustomized', unit: '次'},
    {name: '定制OLAP算法', id: 'HugeGraphOLAPCustomized', unit: '次'},
    {name: '定制图学习算法', id: 'HugeGraphGLCustomized', unit: '次'},
    {name: '定制接口', id: 'HugeGraphAPICustomized', unit: '次'},
    {name: '定制业务服务', id: 'HugeGraphCustomized', unit: '次'},
].map(item => ({...item, total: 0, custom: 0, discount: '', reduce: '', checked: false}));

const loopsTreeData = (list, parent) => {
    return (list || []).map(({children, name, accountId}) => {
        const node = {
            parent,
            name,
            accountId,
            path: [...parent, name].join(' > '),
            selectable: children.length === 0,
        };

        node.children = loopsTreeData(children, [...parent, name]);
        return node;
    });
};

const AddLayer = ({visible, onCancel, refresh, id}) => {
    const [form] = Form.useForm();
    const [accountList, setAccountList] = useState([]);
    const [accountName, setAccountName] = useState('');
    const [graphspaceList, setGraphspaceList] = useState([]);
    const [loading, setLoading] = useState(false);
    const [spinning, setSpinning] = useState(false);
    const [errorMsg, setErrorMsg] = useState('');

    const onFinish = useCallback(() => {
        form.validateFields().then(values => {
            const subGoodsList = values.goods.filter(item => item.checked);
            if (subGoodsList.length === 0) {
                setErrorMsg('请至少选择一个商品');
                return;
            }
            setErrorMsg('');

            values.goods = JSON.stringify(subGoodsList);
            values.graphspaces = values.graphspaces.join(',');
            values.account_name = accountName;
            setLoading(true);

            if (id) {
                api.cloud.updateOrder(id, values).then(res => {
                    setLoading(false);
                    if (res.status === 200) {
                        refresh();
                        onCancel();
                        return;
                    }
                    message.error(res.message);
                });
                return;
            }

            api.cloud.createOrder(values).then(res => {
                setLoading(false);
                if (res.status === 200) {
                    refresh();
                    onCancel();
                    return;
                }
                message.error(res.message);
            });
        });
    }, [form, accountName, refresh, onCancel, id]);

    const handleAccount = useCallback((_, account) => {
        setAccountName(account.name);
    }, []);

    const handleChange = useCallback(val => {
        api.auth.getAccountsList(val).then(res => {
            if (res.status === 200) {
                setAccountList(loopsTreeData(res.data, []));
            }
        });
    }, []);

    useEffect(() => {
        if (!visible) {
            return;
        }

        form.resetFields();
        form.setFieldValue('goods', goodsList);

        api.manage.getGraphSpaceList().then(res => {
            if (res.status === 200) {
                setGraphspaceList(res.data.records.map(item => ({label: item.nickname, value: item.name})));

                return;
            }

            message.error(res.message);
        });

        if (id) {
            setSpinning(true);
            api.cloud.getOrder(id).then(res => {
                if (res.status === 200) {
                    const {properties} = res.data;
                    api.auth.getAccountsList(properties.user_name).then(res => {
                        setSpinning(false);
                        if (res.status === 200) {
                            setAccountList(loopsTreeData(res.data, []));
                        }
                        form.setFieldsValue({
                            ...properties,
                            // graphspaces: JSON.parse(properties.graphspaces),
                            graphspaces: properties.graphspaces.split(','),
                            goods: JSON.parse(properties.goods),
                        });
                    });
                }
            });
        }
    }, [form, visible, id]);

    return (
        <Modal
            title={'新增结算'}
            onCancel={onCancel}
            open={visible}
            onOk={onFinish}
            width={600}
            confirmLoading={loading}
        >
            <Spin spinning={spinning}>
                {errorMsg && <Alert message={errorMsg} type="error" showIcon style={{marginBottom: 10}} />}
                <Form form={form} labelCol={{span: 5}}>
                    <Form.Item
                        name='name'
                        rules={[rules.required()]}
                        label='业务名称'
                        required
                    >
                        <Input placeholder='请输入结算业务的名称，不可重复' disabled={!!id} />
                    </Form.Item>
                    <Form.Item
                        label='结算账户'
                        required
                    >
                        <Space direction="vertical">
                            <Form.Item noStyle name="user_name">
                                <SelectStaffUser
                                    placeholder='输入业务对接人员'
                                    style={{width: 320}}
                                    onChange={handleChange}
                                    disabled={!!id}
                                />
                            </Form.Item>
                            <Form.Item noStyle name={'account_id'}>
                                <TreeSelect
                                    treeData={accountList}
                                    placeholder='请选择关联的资源账户'
                                    fieldNames={{label: 'name', value: 'accountId'}}
                                    showSearch
                                    // treeDefaultExpandedKeys={id ? [orgData.pdb_path] : false}
                                    treeNodeFilterProp="name"
                                    treeNodeLabelProp='path'
                                    treeExpandAction={'click'}
                                    onSelect={handleAccount}
                                    disabled={!!id}
                                />
                            </Form.Item>
                        </Space>
                    </Form.Item>
                    <Form.Item
                        label='关联图空间'
                        required
                        name='graphspaces'
                    >
                        <Select options={graphspaceList} mode='multiple' placeholder='管理权限' />
                    </Form.Item>
                    <Form.Item
                        label='选择商品'
                        required
                    >
                        <Form.List
                            name='goods'
                        >
                            {(fields, {add, remove}, {errors}) => (
                                <>
                                    {fields.map(({key, name, ...restField}) => (
                                        <Space key={key} align="start">
                                            <Form.Item
                                                {...restField}
                                                name={[name, 'checked']}
                                                valuePropName={'checked'}
                                            >
                                                <Checkbox disabled={id} />
                                            </Form.Item>
                                            <Space
                                                key={key}
                                                align="baseline"
                                                size={[12, 0]}
                                                wrap
                                            >
                                                <Form.Item {...restField} name={[name, 'name']}>
                                                    <Input disabled />
                                                </Form.Item>
                                                <Form.Item
                                                    {...restField}
                                                    name={[name, 'discount']}
                                                >
                                                    <InputNumber
                                                        placeholder="折扣系数"
                                                        step={0.1}
                                                        max={1}
                                                        min={0}
                                                    />
                                                </Form.Item>
                                                <Form.Item
                                                    {...restField}
                                                    name={[name, 'reduce']}
                                                >
                                                    <InputNumber placeholder={'减免金额'} min={0} />
                                                </Form.Item>
                                                <Form.Item
                                                    {...restField}
                                                    name={[name, 'total']}
                                                    hidden={!id}
                                                >
                                                    <InputNumber
                                                        placeholder="实际用量"
                                                        disabled
                                                    />
                                                </Form.Item>
                                                <Form.Item
                                                    {...restField}
                                                    name={[name, 'custom']}
                                                    hidden={!id}
                                                >
                                                    <InputNumber placeholder={'本期用量'} />
                                                </Form.Item>
                                            </Space>
                                            <Form.Item {...restField} name={[name, 'id']} hidden noStyle />
                                            <Form.Item {...restField} name={[name, 'unit']} hidden noStyle />
                                        </Space>
                                    ))}
                                    <Form.ErrorList errors={errors} />
                                </>
                            )}
                        </Form.List>
                    </Form.Item>
                </Form>
            </Spin>
        </Modal>
    );
};

export {AddLayer};
