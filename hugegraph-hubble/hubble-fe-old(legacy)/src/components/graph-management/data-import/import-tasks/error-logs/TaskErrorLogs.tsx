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

import React, { useContext, useEffect } from 'react';
import { useRoute } from 'wouter';
import { observer } from 'mobx-react';
import { DataImportRootStoreContext } from '../../../../../stores';

import './TaskErrorLogs.less';

const TaskErrorLogs: React.FC = observer(() => {
  const dataImportRootStore = useContext(DataImportRootStoreContext);
  const { serverDataImportStore } = dataImportRootStore;
  const [, params] = useRoute(
    '/graph-management/:id/data-import/:jobId/task-error-log/:taskId'
  );

  useEffect(() => {
    serverDataImportStore.checkErrorLogs(
      Number(params!.id),
      Number(params!.jobId),
      Number(params!.taskId)
    );
  }, [params!.id, params!.jobId, params!.taskId]);

  return (
    <section className="task-error-logs">
      {/* <div>
        {serverDataImportStore.requestStatus.checkErrorLogs === 'failed'
          ? serverDataImportStore.errorInfo.checkErrorLogs.message
          : serverDataImportStore.errorLogs}
      </div> */}
      <div>
        {serverDataImportStore.requestStatus.checkErrorLogs === 'failed'
          ? serverDataImportStore.errorInfo.checkErrorLogs.message
          : serverDataImportStore.errorLogs
              .split('\n')
              .map((text) => <p>{text}</p>)}
      </div>
    </section>
  );
});

export default TaskErrorLogs;
