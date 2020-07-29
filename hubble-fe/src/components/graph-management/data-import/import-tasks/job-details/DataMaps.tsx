import React from 'react';
import { observer } from 'mobx-react';

import DataMapConfigs from '../datamap-configs/DataMapConfigs';

import '../datamap-configs/DataMapConfigs.less';

const DataMaps: React.FC = observer(() => {
  return (
    <div
      className="import-job-details-content-wrapper"
      style={{ padding: '24px 0' }}
    >
      <DataMapConfigs height="calc(100vh - 225px)" />
    </div>
  );
});

export default DataMaps;
