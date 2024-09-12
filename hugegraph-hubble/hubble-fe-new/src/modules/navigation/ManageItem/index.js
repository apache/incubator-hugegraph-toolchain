/**
 * @file 管理子项块
 * @author
 */

import Item from '../Item';

const ManageItem = () => {
    return (
        <Item
            btnIndex={1}
            btnTitle={'数据管理'}
            listData={[
                {
                    title: '图管理',
                    url: '/graphspace',
                },
                {
                    title: '数据管理',
                    url: '/source',
                },
                {
                    title: '数据导入',
                    url: '/task',
                },
            ]}
        />
    );
};

export default ManageItem;
