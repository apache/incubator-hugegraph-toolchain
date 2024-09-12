import React, {useState} from 'react';
import {Layout, Menu} from 'antd';
import {
    HomeOutlined,
    DatabaseOutlined,
    AlertOutlined,
    FundViewOutlined,
    MenuUnfoldOutlined,
    MenuFoldOutlined,
} from '@ant-design/icons';
import {Link, useLocation} from 'react-router-dom';
import * as user from '../../utils/user';

const items = () => {
    const userInfo = user.getUser();
    const MY = {label: <Link to='/my'>个人中心</Link>, key: 'my'};
    const ACCOUNT = {label: <Link to='/account'>账号管理</Link>, key: 'account'};
    const RESOURCE = {label: <Link to='/resource'>资源管理</Link>, key: 'resource'};
    const ROLE = {label: <Link to='/role'>角色管理</Link>, key: 'role'};

    let systemList = [MY];
    if (userInfo.is_superadmin) {
        systemList = [MY, ACCOUNT, RESOURCE, ROLE];
    }
    else if (userInfo.resSpaces && userInfo.resSpaces.length > 0) {
        systemList = [MY, RESOURCE, ROLE];
    }

    const menu = [
        {
            label: <Link to='/navigation'>导航</Link>,
            key: 'navigation',
            icon: <HomeOutlined />,
        },
        {
            label: '数据管理',
            key: 'manage',
            icon: <FundViewOutlined />,
            children: [
                {label: <Link to='/graphspace'>图管理</Link>, key: 'graphspace'},
                {label: <Link to='/source'>数据源管理</Link>, key: 'source'}, // TODO X fix import
                {label: <Link to='/task'>数据导入</Link>, key: 'task'},
            ],
        },
        {
            label: '业务分析',
            key: 'analysis',
            icon: <DatabaseOutlined />,
            children: [
                {label: <Link to='/gremlin'>图语言分析</Link>, key: 'gremlin'},
                {label: <Link to='/algorithms'>图算法</Link>, key: 'algorithms'},
                {label: <Link to='/asyncTasks'>任务管理</Link>, key: 'asyncTasks'},
            ],
        },
        {
            label: '系统管理',
            key: 'system',
            icon: <AlertOutlined />,
            children: [...systemList],
        },
    ];

    // if (userInfo.is_superadmin) {
    //     menu.push({
    //         label: '系统管理',
    //         key: 'system',
    //         icon: <AlertOutlined />,
    //         children: [
    //             {label: <Link to='/super'>超管管理</Link>, key: 'super'},
    //             {label: <Link to='/account'>账号管理</Link>, key: 'account'},
    //             {label: <Link to='/resource'>资源管理</Link>, key: 'resource'},
    //             {label: <Link to='/role'>角色管理</Link>, key: 'role'},
    //         ],
    //     });
    // }

    return menu;
};

const Sidebar = () => {
    const [collapsed, setCollapsed] = useState(false);
    const href = useLocation();
    const menuKey = href.pathname.split('/')[1] || 'navigation';

    return (
        <Layout.Sider
            collapsible
            collapsed={collapsed}
            onCollapse={value => setCollapsed(value)}
            theme='light'
            trigger={
                collapsed ? <MenuUnfoldOutlined /> : <MenuFoldOutlined />
            }
        >
            <Menu
                defaultSelectedKeys={['graphspace']}
                defaultOpenKeys={['manage', 'analysis', 'system']}
                mode="inline"
                items={items()}
                selectedKeys={[menuKey]}
            />
        </Layout.Sider>
    );
};

export default Sidebar;
