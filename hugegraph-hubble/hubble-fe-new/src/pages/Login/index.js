import {LockOutlined, UserOutlined} from '@ant-design/icons';
import {Button, Form, Input, Row, Col} from 'antd';
import Logo from '../../assets/logo.png';
import style from './index.module.scss';
import * as api from '../../api';
import {useNavigate} from 'react-router-dom';
import * as user from '../../utils/user';
import {useCallback} from 'react';

const Login = () => {
    const [form] = Form.useForm();
    const navigate = useNavigate();

    const onFinish = useCallback(() => {
        // console.log(form.getFieldsValue());
        form.validateFields().then(value => {
            api.auth.login(value).then(res => {
                if (res.status === 200) {
                    localStorage.setItem('user', value.user_name);
                    user.setUser(res.data);
                    navigate(sessionStorage.getItem('redirect') ?? '/');
                    sessionStorage.removeItem('redirect');
                }
            });
        });
    }, [form, navigate]);
    return (
        <div className={style.loginContainer}>
            <Form
                name="normal_login"
                className={style.loginForm}
                onFinish={onFinish}
                form={form}
            >
                <Row>
                    <Col span={24} className={style.title}><img src={Logo} alt='' /> | Admin Portal</Col>
                </Row>
                <Form.Item
                    name="user_name"
                    rules={[{required: true, message: 'Please input your Username!'}]}
                >
                    <Input prefix={<UserOutlined className="site-form-item-icon" />} placeholder="Username" />
                </Form.Item>
                <Form.Item
                    name="user_password"
                    rules={[{required: true, message: 'Please input your Password!'}]}
                >
                    <Input
                        prefix={<LockOutlined className="site-form-item-icon" />}
                        type="password"
                        placeholder="Password"
                    />
                </Form.Item>

                <Form.Item>
                    <Button type="primary" htmlType="submit" style={{width: '100%'}}>
                        登录
                    </Button>
                    <Row justify='space-between'>
                        {/* <Col className={style.left}>更改密码</Col> */}
                        {/* <Col className={style.right}>上次登录时间：2022-06-24 10:56:17</Col> */}
                    </Row>
                </Form.Item>
            </Form>
        </div>
    );
};

export default Login;
