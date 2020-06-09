import i18n from 'i18next';
import { initReactI18next } from 'react-i18next';

import { zhCNResources } from './resources';

i18n.use(initReactI18next).init({
  lng: 'zh-CN',
  fallbackLng: 'zh-CN',

  resources: {
    'zh-CN': zhCNResources
  },

  interpolation: {
    escapeValue: false // not needed for react as it escapes by default
  }
});

export default i18n;
