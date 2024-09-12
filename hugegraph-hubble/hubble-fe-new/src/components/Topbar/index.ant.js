import {Layout, Space, Avatar, Dropdown, Menu, message, Modal} from 'antd';
import {UserOutlined} from '@ant-design/icons';
import style from './index.module.scss';
import Logo from '../../assets/logo.png';
import {useNavigate, useLocation} from 'react-router-dom';
import * as api from '../../api/index';
import * as user from '../../utils/user';

const Topbar = () => {
    const userInfo = user.getUser();
    const navigate = useNavigate();
    const location = useLocation();

    if (!userInfo || !userInfo.id) {
        sessionStorage.setItem('redirect', `${location.pathname}${location.search}`);
        window.location.href = '/login';
    }

    const logout = () => {

        api.auth.logout().then(res => {
            if (res.status === 200) {
                sessionStorage.removeItem('redirect');
                user.clearUser();
                message.success('退出成功');
                navigate('/login');
            }
        });
    };

    const confirm = () => {
        Modal.confirm({
            title: '确定退出吗？',
            okText: '确定',
            cancelText: '取消',
            onOk: logout,
        });
    };

    return (
        <Layout.Header>
            <div className={style.logo}><img src={Logo} alt='' /></div>
            <Dropdown overlay={<Menu items={[{key: 'logout', label: <a onClick={confirm}>退出登录</a>}]} />}>
                <Space className={style.right}>
                    <Avatar size={'small'} icon={<UserOutlined />} />
                    <span>{userInfo?.user_nickname ?? ''}</span>
                </Space>
            </Dropdown>
        </Layout.Header>
    );
};

export default Topbar;
