import i18n from 'i18next';
import { initReactI18next } from 'react-i18next';

import { zhCNResources, enUSResources } from './resources';

i18n.use(initReactI18next).init({
  lng: localStorage.getItem('languageType') || 'zh-CN',
  fallbackLng: 'zh-CN',

  resources: {
    'zh-CN': zhCNResources,
    'en-US': enUSResources
  },

  interpolation: {
    escapeValue: false // not needed for react as it escapes by default
  }
});

export default i18n;
