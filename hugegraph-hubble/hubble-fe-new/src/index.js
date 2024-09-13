import React from 'react';
import ReactDOM from 'react-dom/client';
import {BrowserRouter} from 'react-router-dom';
import {ConfigProvider} from 'antd';
import 'moment/locale/zh-cn';
import './index.css';
import App from './App';
import reportWebVitals from './reportWebVitals';
import zhCN from 'antd/lib/locale/zh_CN';
import enUS from 'antd/lib/locale/en_US';
import './i18n';

const languageType = localStorage.getItem('languageType') === 'en-US' ? enUS : zhCN;
const root = ReactDOM.createRoot(document.getElementById('root'));

root.render(
    <BrowserRouter>
        <ConfigProvider locale={languageType}>
            <App />
        </ConfigProvider>
    </BrowserRouter>
);

// If you want to start measuring performance in your app, pass a function
// to log results (for example: reportWebVitals(console.log))
// or send to an analytics endpoint. Learn more: https://bit.ly/CRA-vitals
reportWebVitals();
