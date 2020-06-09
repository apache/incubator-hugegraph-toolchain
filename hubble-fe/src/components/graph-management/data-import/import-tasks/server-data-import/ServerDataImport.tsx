import React from 'react';
import { observer } from 'mobx-react';

import ImportConfigs from './ImportConfigs';

import './ServerDataImport.less';

const ServerDataImport: React.FC = observer(() => {
  return (
    <div className="import-tasks-step-wrapper" style={{ padding: '0 16px' }}>
      <ImportConfigs />
    </div>
  );
});

export default ServerDataImport;
