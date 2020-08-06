import React, { useContext, useEffect } from 'react';
import { useRoute } from 'wouter';
import { observer } from 'mobx-react';
import { isNull } from 'lodash-es';
import ReactJsonView from 'react-json-view';
import { AsyncTasksStoreContext } from '../../../stores';

import './AsyncTaskResult.less';

const TaskErrorLogs: React.FC = observer(() => {
  const asyncTasksStore = useContext(AsyncTasksStoreContext);
  const [, params] = useRoute(
    '/graph-management/:id/async-tasks/:taskId/result'
  );

  useEffect(() => {
    asyncTasksStore.setCurrentId(Number(params!.id));
    asyncTasksStore.fetchAsyncTask(Number(params!.taskId));

    return () => {
      asyncTasksStore.dispose();
    };
  }, [params!.id, params!.taskId]);

  return (
    <section className="async-task-result">
      {!isNull(asyncTasksStore.singleAsyncTask) &&
        (asyncTasksStore.singleAsyncTask.task_status === 'success' ? (
          <ReactJsonView
            src={
              !isNull(asyncTasksStore.singleAsyncTask)
                ? JSON.parse(asyncTasksStore.singleAsyncTask!.task_result)
                : []
            }
            name={false}
            displayObjectSize={false}
            displayDataTypes={false}
            groupArraysAfterLength={50}
          />
        ) : (
          <div className="async-task-result-error">
            {asyncTasksStore.singleAsyncTask!.task_result}
          </div>
        ))}
    </section>
  );
});

export default TaskErrorLogs;
