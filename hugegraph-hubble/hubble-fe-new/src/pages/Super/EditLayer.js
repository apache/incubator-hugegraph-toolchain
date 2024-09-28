/*
 *
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. The ASF
 * licenses this file to You under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

// import {Modal, Input, Form, message} from 'antd';
// import {useCallback, useEffect} from 'react';
// import * as api from '../../api';
// import * as rules from '../../utils/rules';
// import SelectUser from '../../components/SelectUser';
// TODO REMOVE SUPER
// const EditLayer = ({visible, onCancel, refresh}) => {
//     const [form] = Form.useForm();
//
//     const onFinish = useCallback(() => {
//         form.validateFields().then(values => {
//             // createUser(values);
//             const usernames = values.user_name.map(item => item.split('##')[0]).join(',');
//             const nicknames = values.user_name.map(item => item.split('##')[1]).join(',');
//
//             api.auth.addSuperUser({
//                 usernames: usernames,
//                 nicknames: nicknames,
//                 description: values.user_description,
//             }).then(res => {
//                 if (res.status === 200) {
//                     message.success('超级管理员添加成功');
//                     onCancel();
//                     refresh();
//                     return;
//                 }
//
//                 message.error(`超级管理员添加失败。失败原因：${res.data.message}`);
//             });
//         });
//     }, [form, onCancel, refresh]);
//
//     useEffect(() => {
//         form.resetFields();
//     }, [form, visible]);
//
//     return (
//         <Modal
//             title={'添加超管'}
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
//                     <SelectUser placeholder='请输入要添加为超管的账号' />
//                 </Form.Item>
//                 <Form.Item
//                     name='user_description'
//                 >
//                     <Input.TextArea placeholder='请输入备注信息' maxLength={32} showCount rows={6} />
//                 </Form.Item>
//             </Form>
//         </Modal>
//     );
// };
//
// export default EditLayer;
