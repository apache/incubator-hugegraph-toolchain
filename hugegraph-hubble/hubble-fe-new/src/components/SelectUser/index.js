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
