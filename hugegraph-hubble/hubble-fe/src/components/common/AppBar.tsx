/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. The ASF
 * licenses this file to You under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

import React, { useCallback, useState } from 'react';
import { observer } from 'mobx-react';
import { useLocation } from 'wouter';
import { useTranslation } from 'react-i18next'
import { Select } from 'antd';
import './AppBar.less';
const { Option } = Select;

const AppBar: React.FC = observer(() => {
  const [_, setLocation] = useLocation();
  // init select language
  const [languageType, setLanguageType] = useState(localStorage.getItem('languageType') || 'zh-CN')
  const { t } = useTranslation()
  const setRoute = useCallback(
    (route: string) => () => {
      setLocation(route);
    },
    [setLocation]
  );
  /**
   * switch language and update localStorage
   */
  const i18Change = (e: string) => {
    localStorage.setItem('languageType', e)
    setLanguageType(e)
    // Refresh directly or through react.createcontext implements no refresh switching
    window.location.reload()
  }
  return (
    <nav className="navigator">
      <div className="navigator-logo" onClick={setRoute('/')}></div>
      <div className="navigator-items">
        <div
          className="navigator-item active"
          onClick={setRoute('/graph-management')}
        >
          <span>{t('addition.appbar.graph-manager')}</span>
        </div>
      </div>
      <div className="navigator-additions">
        <span></span>
      </div>
      {/* i18n */}
      <div className="i18n-box">
        <Select
          defaultValue={languageType}
          style={{ width: 120 }}
          size="small"
          onChange={i18Change}
        >
          <Option value="zh-CN">中文</Option>
          <Option value="en-US">English</Option>
        </Select>
      </div>
    </nav>
  );
});

export default AppBar;
