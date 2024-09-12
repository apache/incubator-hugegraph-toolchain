import {Layout} from 'antd';
import Sidebar from './components/Sidebar/index.ant';
import Topbar from './components/Topbar/index.ant';
import {Outlet} from 'react-router-dom';
import 'antd/dist/antd.css';

const LayoutAnt = () => {
    return (
        <Layout>
            <Topbar />
            <Layout className="main">
                <Sidebar />
                <Layout>
                    <Layout.Content className='content'>
                        <Outlet />
                    </Layout.Content>
                </Layout>
            </Layout>
        </Layout>
    );
};

export default LayoutAnt;
