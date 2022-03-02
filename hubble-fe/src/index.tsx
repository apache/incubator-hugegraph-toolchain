import React from 'react';
import ReactDOM from 'react-dom';
import './index.less';
import App from './components/App';
import './i18n';
import { ConfigProvider } from 'antd';
import zhCN from 'antd/lib/locale/zh_CN';
import enUS from 'antd/lib/locale/en_US';

// UI component has built-in text internationalization, 
// such as confirmation, cancellation, etc
const languageType =
  localStorage.getItem('languageType') === 'en-US' ? enUS : zhCN;
ReactDOM.render(
  <ConfigProvider locale={languageType}>
    <App />
  </ConfigProvider>,
  document.getElementById('root')
);
