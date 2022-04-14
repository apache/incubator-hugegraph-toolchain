import React from 'react';
import { observer } from 'mobx-react';

import { ServerDataImport } from '../server-data-import';

const DataImportDetails: React.FC = observer(() => {
  return (
    <div
      className="import-job-details-content-wrapper"
      style={{ padding: '16px 0' }}
    >
      <ServerDataImport />
    </div>
  );
});

export default DataImportDetails;
