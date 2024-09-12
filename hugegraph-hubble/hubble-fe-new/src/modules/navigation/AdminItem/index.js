/**
 * @file 系统管理子项块
 * @author
 */

import Item from '../Item';

const AdminItem = () => {
    return (
        <Item
            btnIndex={3}
            btnTitle={'系统管理'}
            listData={[
                {
                    title: '超管管理',
                    url: '/super',
                },
                {
                    title: '账号管理',
                    url: '/account',
                },
                {
                    title: '资源管理',
                    url: '/resource',
                },
                {
                    title: '角色管理',
                    url: '/role',
                },
            ]}
        />
    );
};

export default AdminItem;
