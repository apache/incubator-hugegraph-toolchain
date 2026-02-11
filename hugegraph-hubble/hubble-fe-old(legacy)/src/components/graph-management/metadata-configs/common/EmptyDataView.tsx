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

import React from 'react';
import { observer } from 'mobx-react';

import LoadingBackIcon from '../../../../assets/imgs/ic_loading_back.svg';
import LoadingFrontIcon from '../../../../assets/imgs/ic_loading_front.svg';

import i18next from '../../../../i18n';
export interface LoadingDataViewProps {
  isLoading: boolean;
  emptyText?: string;
}

const LoadingDataView: React.FC<LoadingDataViewProps> = observer(
  ({ isLoading, emptyText }) =>
    isLoading ? (
      <div className="metadata-configs-content-loading-wrapper">
        <div className="metadata-configs-content-loading-bg">
          <img
            className="metadata-configs-content-loading-back"
            src={LoadingBackIcon}
            alt="load background"
          />
          <img
            className="metadata-configs-content-loading-front"
            src={LoadingFrontIcon}
            alt="load spinner"
          />
        </div>
        <span>{i18next.t('addition.message.data-loading')}...</span>
      </div>
    ) : emptyText ? (
      <span>{emptyText}</span>
    ) : null
);

export default LoadingDataView;
