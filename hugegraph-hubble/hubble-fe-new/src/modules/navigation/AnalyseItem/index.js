/**
 * @file 分析子项块
 * @author
 */

import Item from '../Item';

const AnalyseItem = () => {
    return (
        <Item
            btnIndex={2}
            btnTitle={'业务分析'}
            listData={[
                {
                    title: '图语言分析',
                    url: '/gremlin',
                },
                {
                    title: '图算法',
                    url: '/algorithms',
                },
            ]}
        />
    );
};

export default AnalyseItem;
