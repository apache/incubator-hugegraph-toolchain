import React, { useContext, useEffect } from 'react';
import { observer } from 'mobx-react';

import { GraphManagementStoreContext } from '../../stores';
import GraphManagementLimitHint from './GraphManagementLimitHint';
import GraphManagementHeader from './GraphManagementHeader';
import NewGraphConfig from './NewGraphConfig';
import GraphManagementList from './GraphManagementList';
import GraphManagementEmptyList from './GraphManagementEmptyList';

import './GraphManagement.less';

const GraphManagement: React.FC = observer(() => {
  const graphManagementStore = useContext(GraphManagementStoreContext);

  useEffect(() => {
    graphManagementStore.fetchLicenseInfo();
    graphManagementStore.fetchGraphDataList();

    return () => {
      graphManagementStore.dispose();
    };
  }, [graphManagementStore]);

  return (
    <section className="graph-management">
      <GraphManagementLimitHint />
      <GraphManagementHeader />
      <NewGraphConfig />
      <GraphManagementEmptyList />
      <GraphManagementList />
    </section>
  );
});

export default GraphManagement;
