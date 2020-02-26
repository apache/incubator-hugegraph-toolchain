import React, { useState, useContext } from 'react';
import { observer } from 'mobx-react';
import classnames from 'classnames';

import QueryAndAlgorithmLibrary from './QueryAndAlgorithmLibrary';
import { QueryResult } from './query-result';
import ExecLogAndQueryCollections from './ExecLogAndQueryCollections';
import { GraphManagementStoreContext } from '../../../stores';

const DataAnalyzeContent: React.FC = observer(() => {
  const graphManagementStore = useContext(GraphManagementStoreContext);
  const [queryResultSidebarIndex, setQueryResultSidebarIndex] = useState(0);

  const wrapperClassName = classnames({
    'data-analyze-content': true,
    'sidebar-expand': graphManagementStore.isExpanded
  });

  return (
    <div className={wrapperClassName}>
      <QueryAndAlgorithmLibrary />
      <QueryResult
        sidebarIndex={queryResultSidebarIndex}
        handleSetSidebarIndex={setQueryResultSidebarIndex}
      />
      <ExecLogAndQueryCollections />
    </div>
  );
});

export default DataAnalyzeContent;
