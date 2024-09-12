import {Result} from 'antd';
import {Link} from 'react-router-dom';

const Error404 = () => (
    <Result
        status={404}
        title={404}
        subTitle='页面不存在'
        extra={<Link to='/'>返回首页</Link>}
    />
);

export default Error404;
