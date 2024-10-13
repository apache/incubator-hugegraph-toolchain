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
