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

import React, { useContext, useCallback } from 'react';
import { observer } from 'mobx-react';
import { Button } from 'hubble-ui';

import { GraphManagementStoreContext } from '../../stores';
import AddIcon from '../../assets/imgs/ic_add.svg';
import EmptyIcon from '../../assets/imgs/ic_sousuo_empty.svg';
import { useTranslation } from 'react-i18next';

const GraphManagementEmptyList: React.FC = observer(() => {
  const graphManagementStore = useContext(GraphManagementStoreContext);
  const { t } = useTranslation();

  const handleLayoutSwitch = useCallback(
    (flag: boolean) => () => {
      if (flag) {
        graphManagementStore.fillInGraphDataDefaultConfig();
      }
      graphManagementStore.switchCreateNewGraph(flag);
    },
    [graphManagementStore]
  );

  if (
    graphManagementStore.graphData.length === 0 &&
    !graphManagementStore.showCreateNewGraph
  ) {
    return (
      <div className="graph-management-list-empty">
        {graphManagementStore.isSearched.status &&
        graphManagementStore.requestStatus.fetchGraphData === 'success' ? (
          <>
            <img
              src={EmptyIcon}
              alt={t('addition.graphManagementEmptyList.no-matching-results')}
            />
            <div>
              {t('addition.graphManagementEmptyList.no-matching-results')}
            </div>
          </>
        ) : (
          <>
            <img
              src={AddIcon}
              alt={t('addition.graphManagementEmptyList.graph-create')}
            />
            <div>
              {t('addition.graphManagementEmptyList.graph-create-desc')}
            </div>
            <Button
              type="primary"
              size="large"
              style={{
                width: 112
              }}
              onClick={handleLayoutSwitch(true)}
            >
              {t('addition.graphManagementEmptyList.graph-create')}
            </Button>
          </>
        )}
      </div>
    );
  }

  return null;
});

export default GraphManagementEmptyList;
