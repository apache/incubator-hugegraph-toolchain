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
