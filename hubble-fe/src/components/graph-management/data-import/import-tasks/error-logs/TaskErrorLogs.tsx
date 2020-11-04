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
