import React, { useContext } from 'react';
import { observer } from 'mobx-react';
import { Progress } from 'hubble-ui';

import { DataImportRootStoreContext } from '../../../../../stores';

const UploadedFiles: React.FC = observer(() => {
  const { dataMapStore } = useContext(DataImportRootStoreContext);

  return (
    <div className="import-job-details-content-wrapper">
      {dataMapStore.fileMapInfos.map(({ name, total_size }) => (
        <div className="import-job-details-uploaded-file-infos">
          <div className="import-job-details-uploaded-file-infos-titles">
            <span>{name}</span>
            <span>{total_size}</span>
          </div>
          <div className="import-job-details-uploaded-file-infos-progress-status">
            <Progress percent={100} status="success" />
          </div>
        </div>
      ))}
    </div>
  );
});

export default UploadedFiles;
