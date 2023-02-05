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
import { useTranslation } from 'react-i18next';
import { Button } from 'hubble-ui';

interface TypeConfigManipulationsProps {
  type: 'vertex' | 'edge';
  status: 'add' | 'edit';
  disableSave?: boolean;
  onCreate: () => void;
  onCancel: () => void;
}

const TypeConfigManipulations: React.FC<TypeConfigManipulationsProps> = observer(
  ({ type, status, onCreate, onCancel, disableSave = false }) => {
    const { t } = useTranslation();

    return (
      <div
        className="import-tasks-data-options"
        style={{ marginTop: 40, marginBottom: 0 }}
      >
        <span className="import-tasks-data-options-title in-card"></span>
        <div
          className="import-tasks-data-type-manipulations"
          style={{ margin: 0 }}
        >
          <Button
            type="primary"
            size="medium"
            style={{ marginRight: 16 }}
            onClick={onCreate}
            disabled={disableSave}
          >
            {status === 'add'
              ? t('data-configs.type.manipulation.create')
              : t('data-configs.type.manipulation.save')}
          </Button>
          <Button size="medium" onClick={onCancel}>
            {t('data-configs.type.manipulation.cancel')}
          </Button>
        </div>
      </div>
    );
  }
);

export default TypeConfigManipulations;
