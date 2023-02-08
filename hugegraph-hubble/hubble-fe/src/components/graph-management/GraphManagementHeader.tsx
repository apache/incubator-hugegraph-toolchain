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
import { Input, Button } from 'hubble-ui';
import { GraphManagementStoreContext } from '../../stores';
import { useTranslation } from 'react-i18next';

const styles = {
  marginLeft: '20px',
  width: 88
};

const GraphManagementHeader: React.FC = observer(() => {
  const graphManagementStore = useContext(GraphManagementStoreContext);
  const { t } = useTranslation();
  const handleLayoutSwitch = useCallback(
    (flag: boolean) => () => {
      graphManagementStore.switchCreateNewGraph(flag);
    },
    [graphManagementStore]
  );

  const handleSearchChange = useCallback(
    (e: React.ChangeEvent<HTMLInputElement>) => {
      graphManagementStore.mutateSearchWords(e.target.value);
    },
    [graphManagementStore]
  );

  const handleSearch = useCallback(() => {
    graphManagementStore.mutatePageNumber(1);
    graphManagementStore.swtichIsSearchedStatus(true);
    graphManagementStore.fetchGraphDataList();
  }, [graphManagementStore]);

  const handleClearSearch = useCallback(() => {
    graphManagementStore.mutateSearchWords('');
    graphManagementStore.mutatePageNumber(1);
    graphManagementStore.swtichIsSearchedStatus(false);
    graphManagementStore.fetchGraphDataList();
  }, [graphManagementStore]);

  return (
    <div className="graph-management-header">
      {graphManagementStore.licenseInfo &&
      graphManagementStore.licenseInfo.edition === 'community' ? (
        <div className="graph-management-header-description-community">
          <div>{t('addition.graphManagementHeader.graph-manager')}</div>
          <div>
            {graphManagementStore.licenseInfo.edition === 'community'
              ? t('addition.graphManagementHeader.community')
              : t('addition.graphManagementHeader.business')}
            ：{t('addition.graphManagementHeader.limit-desc')}{' '}
            {graphManagementStore.licenseInfo.allowed_graphs + ' '}
            {t('addition.graphManagementHeader.individual')}，
            {t('addition.graphManagementHeader.limit-desc1')}{' '}
            {graphManagementStore.licenseInfo.allowed_datasize}
          </div>
        </div>
      ) : (
        <span className="graph-management-header-description">
          {t('addition.graphManagementHeader.graph-manager')}
        </span>
      )}
      <Input.Search
        size="medium"
        width={200}
        placeholder={t('addition.graphManagementHeader.input-placeholder')}
        value={graphManagementStore.searchWords}
        onChange={handleSearchChange}
        onSearch={handleSearch}
        onClearClick={handleClearSearch}
        isShowDropDown={false}
        disabled={
          graphManagementStore.showCreateNewGraph ||
          graphManagementStore.selectedEditIndex !== null
        }
      />
      <Button
        type="primary"
        size="medium"
        style={styles}
        onClick={handleLayoutSwitch(true)}
        disabled={
          (graphManagementStore.licenseInfo &&
            graphManagementStore.graphData.length >=
              graphManagementStore.licenseInfo.allowed_graphs) ||
          graphManagementStore.showCreateNewGraph ||
          graphManagementStore.selectedEditIndex !== null
        }
      >
        {t('addition.graphManagementHeader.graph-create')}
      </Button>
    </div>
  );
});

export default GraphManagementHeader;
