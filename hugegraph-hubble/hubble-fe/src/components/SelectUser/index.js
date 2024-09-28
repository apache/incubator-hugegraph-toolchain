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

// import {useState, useCallback} from 'react';
// import {Select, List, Avatar} from 'antd';
// import * as api from '../../api';
// TODO REMOVE IT
// const SelectUser = props => {
//     const [userList, setUserList] = useState([]);
//
//     const handleAdmins = useCallback(val => {
//         if (val === '') {
//             setUserList([]);
//             return;
//         }
//
//         api.auth.getUUapList({username: val}).then(res => {
//             if (res.status === 200) {
//                 setUserList(res.data);
//             }
//             else {
//                 setUserList([]);
//             }
//         });
//     }, []);
//
//
//     return (
//         <Select
//             showSearch
//             showArrow={false}
//             mode="multiple"
//             notFoundContent={null}
//             // options={userList.map(item => ({label: item.name, value: item.name}))}
//             onSearch={handleAdmins}
//             optionLabelProp="label"
//             {...props}
//             // onChange={handleChange}
//         >
//             {userList.map(item => (
//                 <Select.Option
//                     value={`${item.username}##${item.name}`}
//                     label={item.username}
//                     key={`${item.username}##${item.name}`}
//                 >
//                     <List.Item.Meta
//                         avatar={<Avatar src={'TODO CHANGE IT'} />}
//                         title={`${item.name ?? item.username} ${item.departmentName ?? ''}`}
//                         description={item.email}
//                     />
//                 </Select.Option>
//             ))}
//         </Select>
//     );
// };
//
// export default SelectUser;
