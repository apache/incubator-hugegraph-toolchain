import {useState, useCallback, useEffect} from 'react';
import {Select, List, Avatar} from 'antd';
import * as api from '../../api';
import {debounce} from 'lodash';

const UserAvatar = ({name, type, ...props}) => {

    return (
        <Avatar
            src={'TODO CHANGE IT'}
            {...props}
        />
    );
};

const SelectUser = props => {
    const [userList, setUserList] = useState([]);

    const handleAdmins = useCallback(val => {
        if (val === '') {
            setUserList([]);
            return;
        }

        api.auth.getUUapList({username: val}).then(res => {
            if (res.status === 200) {
                setUserList(res.data);
            }
            else {
                setUserList([]);
            }
        });
    }, []);

    // const handleChange = useCallback(list => {
    //     console.log(list);
    //     props?.onChange(list.map(item => {
    //         console.log(item);
    //         const arr = item.split('##');

    //         return {name: arr[0], nickname: arr[1]};
    //     }));
    // }, [props]);

    return (
        <Select
            showSearch
            showArrow={false}
            mode="multiple"
            notFoundContent={null}
            // options={userList.map(item => ({label: item.name, value: item.name}))}
            onSearch={handleAdmins}
            optionLabelProp="label"
            {...props}
            // onChange={handleChange}
        >
            {userList.map(item => (
                <Select.Option
                    value={`${item.username}##${item.name}`}
                    label={item.username}
                    key={`${item.username}##${item.name}`}
                >
                    <List.Item.Meta
                        avatar={<Avatar src={'TODO CHANGE IT'} />}
                        title={`${item.name ?? item.username} ${item.departmentName ?? ''}`}
                        description={item.email}
                    />
                </Select.Option>
            ))}
        </Select>
    );
};

const SelectStaffUser = props => {
    const [userList, setUserList] = useState([]);

    const handleAdmins = useCallback(debounce(val => {
        // _.debounce(fetchUserList.bind(val, val), 30);
        if (val === '') {
            setUserList([]);
            return;
        }

        api.auth.getUUapList({username: val}).then(res => {
            if (res.status === 200) {
                setUserList(res.data);
            }
            else {
                setUserList([]);
            }
        });
    }, 200), []);

    return (
        <Select
            showSearch
            showArrow={false}
            notFoundContent={null}
            // options={userList.map(item => ({label: item.name, value: item.name}))}
            onSearch={handleAdmins}
            optionLabelProp="label"
            {...props}
            // onChange={handleChange}
        >
            {userList.map(item => (
                <Select.Option
                    value={`${item.username}`}
                    label={item.name}
                    key={`${item.username}`}
                >
                    <List.Item.Meta
                        avatar={<Avatar src={'TODO CHANGE IT'} />}
                        title={`${item.name ?? item.username} ${item.departmentName ?? ''}`}
                        description={item.email}
                    />
                </Select.Option>
            ))}
        </Select>
    );
};

export {UserAvatar, SelectStaffUser};
export default SelectUser;
