import Route from './routes';
import 'antd/dist/antd.css';
import './App.scss';
import './App.css';
import Layout from './layout.ant';

function App() {

    return (
        <div>
            <Route element={<Layout />} />
        </div>
    );
};

export default App;
