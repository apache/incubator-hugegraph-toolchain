import React, { useContext, useEffect } from 'react';
import { useRoute } from 'wouter';
import { observer } from 'mobx-react';
import { DataImportRootStoreContext } from '../../../../../stores';

import './TaskErrorLogs.less';

const TaskErrorLogs: React.FC = observer(() => {
  const dataImportRootStore = useContext(DataImportRootStoreContext);
  const { serverDataImportStore } = dataImportRootStore;
  const [, params] = useRoute(
    '/graph-management/:id/data-import/import-tasks/:jobId/error-log'
  );

  useEffect(() => {
    serverDataImportStore.checkErrorLogs(
      Number(params!.taskId),
      Number(params!.id)
    );
  }, [params!.id, params!.taskId]);

  return (
    <section className="task-error-logs">
      <div>{serverDataImportStore.errorLogs}</div>
    </section>
  );
});

export default TaskErrorLogs;
