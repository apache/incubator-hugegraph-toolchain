// import {Modal, Input, Form, Select, message, Spin, Switch} from 'antd';
// import {useCallback, useEffect, useState} from 'react';
// import * as api from '../../api';
// import * as rules from '../../utils/rules';
// import SelectUser from '../../components/SelectUser';
// import style from './index.module.scss';
//
// const EditLayer = ({visible, onCancel, data, op, refresh}) => {
//     const [form] = Form.useForm();
//     const [graphspaceList, setGraphspaceList] = useState([]);
//     const [detail, setDetail] = useState({});
//     const [loading, setLoading] = useState(false);
//
//     const title = {
//         'detail': '查看账号',
//         'edit': '编辑账号',
//         'auth': '分配权限',
//         'create': '创建账号',
//     };
//
//     const createUser = useCallback(values => {
//         api.auth.addUser(values).then(res => {
//             if (res.status === 200) {
//                 message.success('创建成功');
//                 onCancel();
//                 refresh();
//
//                 return;
//             }
//
//             message.error(res.message);
//         });
//     }, [onCancel, refresh]);
//
//     const updateUser = useCallback(values => {
//         api.auth.updateUser(data.id, values).then(res => {
//             if (res.status === 200) {
//                 message.success('编辑成功');
//                 onCancel();
//                 refresh();
//
//                 return;
//             }
//
//             message.error(res.message);
//         });
//     }, [onCancel, refresh, data.id]);
//
//     const updateUserAuth = useCallback(values => {
//         api.auth.updateAdminspace(data.id, values.adminSpaces).then(res => {
//             if (res.status === 200) {
//                 message.success('编辑成功');
//                 onCancel();
//                 refresh();
//
//                 return;
//             }
//
//             message.error(res.message);
//         });
//     }, [data.id, onCancel, refresh]);
//
//     const onFinish = useCallback(() => {
//         form.validateFields().then(values => {
//             if (op === 'create') {
//                 values.user_password = values.user_password ?? '123456';
//                 createUser(values);
//             }
//
//             if (op === 'edit') {
//                 updateUser(values);
//             }
//
//             if (op === 'auth') {
//                 updateUserAuth(values);
//             }
//         });
//     }, [createUser, form, op, updateUser, updateUserAuth]);
//
//     useEffect(() => {
//         if (!visible) {
//             return;
//         }
//
//         form.resetFields();
//
//         api.manage.getGraphSpaceList().then(res => {
//             if (res.status === 200) {
//                 setGraphspaceList(res.data.records.map(item => ({label: item.nickname, value: item.name})));
//
//                 return;
//             }
//
//             message.error(res.message);
//         });
//
//         if (data.id) {
//             setLoading(true);
//             api.auth.getUserInfo(data.id).then(res => {
//                 setLoading(false);
//                 if (res.status === 200) {
//                     form.setFieldsValue(res.data);
//                     setDetail(res.data);
//                     return;
//                 }
//
//                 message.error(res.message);
//             });
//         }
//     }, [visible, data.id, form, op]);
//
//     return (
//         op === 'detail'
//             ? (
//                 <Modal
//                     title={'查看账号'}
//                     onCancel={onCancel}
//                     open={visible}
//                     footer={null}
//                     width={600}
//                 >
//                     <Spin spinning={loading}>
//                         <Form
//                             labelCol={{span: 6}}
//                             preserve={false}
//                         >
//                             <Form.Item label='账号ID' className={style.item}>{detail.user_name}</Form.Item>
//                             <Form.Item label='账号名' className={style.item}>{detail.user_nickname}</Form.Item>
//                             <Form.Item label='是否为超级管理员' className={style.item}>
//                                 {detail.is_superadmin ? '是' : '否'}
//                             </Form.Item>
//                             <Form.Item label='备注' className={style.item}>{detail.user_description}</Form.Item>
//                             <Form.Item label='管理权限' className={style.item}>
//                                 {detail.adminSpaces
//                                     ? detail.adminSpaces.map(i => graphspaceList.find(_i => _i.value === i).label
//                                     ).join(',')
//                                     : ''}
//                             </Form.Item>
//                             <Form.Item label='创建时间' className={style.item}>{detail.user_create}</Form.Item>
//                         </Form>
//                     </Spin>
//                 </Modal>
//             )
//             : (
//                 <Modal
//                     title={title[op] ?? '创建账号'}
//                     onCancel={onCancel}
//                     open={visible}
//                     onOk={onFinish}
//                     width={600}
//                     maskClosable={false}
//                 >
//                     <Spin spinning={loading}>
//                         <Form
//                             labelCol={{span: 6}}
//                             // initialValues={data}
//                             form={form}
//                             preserve={false}
//                         >
//                             {(op === 'create' || op === 'edit') && (
//                                 <>
//                                     <Form.Item
//                                         label='账号ID'
//                                         name='user_name'
//                                         validateFirst
//                                         rules={[rules.required()]}
//                                     >
//                                         <Input placeholder='用户登录' disabled={op === 'edit'} />
//                                     </Form.Item>
//                                     <Form.Item
//                                         label='账号名'
//                                         name='user_nickname'
//                                         rules={[rules.required(), rules.isAccountName]}
//                                         validateFirst
//                                     >
//                                         <Input placeholder='账号名设置后可更改' disabled={op === 'edit'} />
//                                     </Form.Item>
//                                     <Form.Item label='备注' name='user_description'>
//                                         <Input.TextArea placeholder='输入账号备注' maxLength={32} rows={4} showCount />
//                                     </Form.Item>
//                                     <Form.Item label='管理权限' name='adminSpaces'>
//                                         <Select options={graphspaceList} mode='multiple' />
//                                     </Form.Item>
//                                     <Form.Item hidden name='is_superadmin' />
//                                 </>
//                             )}
//                             {op === 'auth' && (
//                                 <Form.Item label='管理权限' name='adminSpaces'>
//                                     <Select options={graphspaceList} mode='multiple' />
//                                 </Form.Item>
//                             )}
//                         </Form>
//                     </Spin>
//                 </Modal>
//             )
//     );
// };
//
// const AddLayer = ({visible, onCancel, refresh}) => {
//     const [form] = Form.useForm();
//     const [graphspaceList, setGraphspaceList] = useState([]);
//
//     const onFinish = useCallback(() => {
//         form.validateFields().then(values => {
//             // createUser(values);
//             const usernames = values.user_name.map(item => item.split('##')[0]).join(',');
//             const nicknames = values.user_name.map(item => item.split('##')[1]).join(',');
//
//             api.auth.addUuapUser({
//                 usernames: usernames,
//                 nicknames: nicknames,
//                 description: values.description,
//                 adminSpaces: values.adminSpaces?.join(','),
//             }).then(res => {
//                 if (res.status === 200) {
//                     message.success('添加成功');
//                     onCancel();
//                     refresh();
//                     return;
//                 }
//
//                 message.error(res.message);
//             });
//         });
//     }, [form, onCancel, refresh]);
//
//     useEffect(() => {
//         if (!visible) {
//             return;
//         }
//
//         form.resetFields();
//
//         api.manage.getGraphSpaceList().then(res => {
//             if (res.status === 200) {
//                 setGraphspaceList(res.data.records.map(item => ({label: item.nickname, value: item.name})));
//
//                 return;
//             }
//
//             message.error(res.message);
//         });
//     }, [form, visible]);
//
//     return (
//         <Modal
//             title={'添加账号'}
//             onCancel={onCancel}
//             open={visible}
//             onOk={onFinish}
//             width={600}
//             maskClosable={false}
//         >
//             <Form
//                 form={form}
//                 preserve={false}
//             >
//                 <Form.Item
//                     name='user_name'
//                     rules={[rules.required()]}
//                 >
//                     <SelectUser placeholder='搜索添加用户' />
//                 </Form.Item>
//                 <Form.Item
//                     name='description'
//                 >
//                     <Input.TextArea placeholder='请输入备注信息' rows={6} maxLength={32} showCount />
//                 </Form.Item>
//                 <Form.Item name='adminSpaces'>
//                     <Select options={graphspaceList} mode='multiple' placeholder='管理权限' />
//                 </Form.Item>
//             </Form>
//         </Modal>
//     );
// };
//
// export {EditLayer, AddLayer};
import {Modal, Input, Form, Select, message, Spin, Switch} from 'antd';
import {useCallback, useEffect, useState} from 'react';
import * as api from '../../api';
import * as rules from '../../utils/rules';
import style from './index.module.scss';

const EditLayer = ({visible, onCancel, data, op, refresh}) => {
    const [form] = Form.useForm();
    const [graphspaceList, setGraphspaceList] = useState([]);
    const [detail, setDetail] = useState({});
    const [loading, setLoading] = useState(false);

    const title = {
        'detail': '查看账号',
        'edit': '编辑账号',
        'auth': '分配权限',
        'create': '创建账号',
    };

    const createUser = useCallback(values => {
        api.auth.addUser(values).then(res => {
            if (res.status === 200) {
                message.success('创建成功');
                onCancel();
                refresh();
                return;
            }
            message.error(res.message);
        });
    }, [onCancel, refresh]);
    const updateUser = useCallback(values => {
        api.auth.updateUser(data.id, values).then(res => {
            if (res.status === 200) {
                message.success('创建成功');
                onCancel();
                refresh();

                return;
            }

            message.error(res.message);
        });
    }, [onCancel, refresh, data.id]);

    const updateUserAuth = useCallback(values => {
        api.auth.updateAdminspace(data.id, values.adminSpaces).then(res => {
            if (res.status === 200) {
                message.success('创建成功');
                onCancel();
                refresh();

                return;
            }

            message.error(res.message);
        });
    }, [data.id, onCancel, refresh]);

    const onFinish = useCallback(() => {
        form.validateFields().then(values => {
            if (op === 'create') {
                values.user_password = values.user_password ?? '123456';
                createUser(values);
            }

            if (op === 'edit') {
                updateUser(values);
            }

            if (op === 'auth') {
                updateUserAuth(values);
            }
        });
    }, [createUser, form, op, updateUser, updateUserAuth]);

    useEffect(() => {
        if (!visible) {
            return;
        }

        api.manage.getGraphSpaceList().then(res => {
            if (res.status === 200) {
                setGraphspaceList(res.data.records.map(item => ({label: item.name, value: item.name})));

                return;
            }

            message.error(res.message);
        });

        if (data.id) {
            setLoading(true);
            api.auth.getUserInfo(data.id).then(res => {
                setLoading(false);
                if (res.status === 200) {
                    form.setFieldsValue(res.data);
                    setDetail(res.data);
                    return;
                }

                message.error(res.message);
            });
        }
        else {
            form.resetFields();
        }
    }, [visible, data.id, form]);

    return (
        op === 'detail'
            ? (
                <Modal
                    title={'查看账号'}
                    onCancel={onCancel}
                    open={visible}
                    footer={null}
                    width={600}
                    maskClosable={false}
                >
                    <Spin spinning={loading}>
                        <Form
                            labelCol={{span: 6}}
                            preserve={false}
                        >
                            <Form.Item label="账号ID" className={style.item}>{detail.user_name}</Form.Item>
                            <Form.Item label="账号名" className={style.item}>{detail.user_nickname}</Form.Item>
                            <Form.Item label="是否为超级管理员" className={style.item}>
                                {detail.is_superadmin ? '是' : '否'}
                            </Form.Item>
                            <Form.Item label="备注" className={style.item}>{detail.user_description}</Form.Item>
                            <Form.Item label="管理权限" className={style.item}>
                                {detail.adminSpaces ? detail.adminSpaces.join(',') : ''}
                            </Form.Item>
                            <Form.Item label="创建时间" className={style.item}>{detail.user_create}</Form.Item>
                        </Form>
                    </Spin>
                </Modal>
            )
            : (
                <Modal
                    title={title[op] ?? '创建账号'}
                    onCancel={onCancel}
                    open={visible}
                    onOk={onFinish}
                    width={600}
                >
                    <Spin spinning={loading}>
                        <Form
                            labelCol={{span: 6}}
                            // initialValues={data}
                            form={form}
                            preserve={false}
                        >
                            {(op === 'create' || op === 'edit') && (
                                <>
                                    <Form.Item
                                        label="账号ID"
                                        name="user_name"
                                        validateFirst
                                        rules={[{type: 'string', min: 5, max: 16}, rules.isName, rules.required()]}
                                    >
                                        <Input placeholder="用户登录" disabled={op === 'edit'} />
                                    </Form.Item>
                                    <Form.Item
                                        label="账号名"
                                        name="user_nickname"
                                        rules={[rules.required(), rules.isAccountName]}
                                        validateFirst
                                    >
                                        <Input placeholder="账号名设置后可更改" />
                                    </Form.Item>
                                    <Form.Item label="是否为超级管理员" name="is_superadmin" valuePropName="checked">
                                        <Switch />
                                    </Form.Item>
                                    <Form.Item label="备注" name="user_description">
                                        <Input placeholder="输入账号备注" />
                                    </Form.Item>
                                    <Form.Item
                                        label="默认密码"
                                        name="user_password"
                                        rules={[{type: 'string', min: 5, max: 16}]}
                                    >
                                        <Input.Password
                                            placeholder="123456（创建后可前往个人中心更改）"
                                            autoComplete="new-password"
                                        />
                                    </Form.Item>
                                    <Form.Item label="管理权限" name="adminSpaces">
                                        <Select options={graphspaceList} mode="multiple" />
                                    </Form.Item>
                                </>
                            )}
                            {op === 'auth' && (
                                <Form.Item label="管理权限" name="adminSpaces">
                                    <Select options={graphspaceList} mode="multiple" />
                                </Form.Item>
                            )}
                        </Form>
                    </Spin>
                </Modal>
            )
    );
};

export default EditLayer;
