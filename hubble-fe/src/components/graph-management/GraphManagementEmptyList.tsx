import React, { useContext, useCallback } from 'react';
import { observer } from 'mobx-react';
import { Button } from '@baidu/one-ui';

import { GraphManagementStoreContext } from '../../stores';
import AddIcon from '../../assets/imgs/ic_add.svg';
import EmptyIcon from '../../assets/imgs/ic_sousuo_empty.svg';

const GraphManagementEmptyList: React.FC = observer(() => {
  const graphManagementStore = useContext(GraphManagementStoreContext);

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
            <img src={EmptyIcon} alt="无匹配结果" />
            <div>无匹配结果</div>
          </>
        ) : (
          <>
            <img src={AddIcon} alt="创建图" />
            <div>您暂时还没有任何图，立即创建</div>
            <Button
              type="primary"
              size="large"
              style={{
                width: 112
              }}
              onClick={handleLayoutSwitch(true)}
            >
              创建图
            </Button>
          </>
        )}
      </div>
    );
  }

  return null;
});

export default GraphManagementEmptyList;
