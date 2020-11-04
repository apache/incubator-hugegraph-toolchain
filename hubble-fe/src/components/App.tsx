import React, { useEffect } from 'react';
import { Route, Router } from 'wouter';

import { AppBar } from './common';
import {
  GraphManagement,
  DataAnalyze,
  MetadataConfigs,
  ImportTasks,
  ImportManager,
  JobDetails
} from './graph-management';
import {
  TaskErrorLogs,
  JobErrorLogs
} from './graph-management/data-import/import-tasks/error-logs';
import { AsyncTaskList } from './graph-management';
import AsyncTaskResult from './graph-management/async-tasks/AsyncTaskResult';
import GraphManagementSidebar from './graph-management/GraphManagementSidebar';
import { useLocationWithConfirmation } from '../hooks';

const App: React.FC = () => {
  return (
    <div>
      <AppBar />
      <GraphManagementSidebar />
      {/* @ts-ignore */}
      <Router hook={useLocationWithConfirmation}>
        <Route
          path="/graph-management/:id/data-import/:jobId/task-error-log/:taskId"
          component={TaskErrorLogs}
        />
        <Route
          path="/graph-management/:id/data-import/job-error-log/:jobId"
          component={JobErrorLogs}
        />
        <Route
          path="/graph-management/:id/async-tasks/:taskId/result"
          component={AsyncTaskResult}
        />
        <Route
          path="/graph-management/:id/data-analyze"
          component={DataAnalyze}
        />
        <Route
          path="/graph-management/:id/metadata-configs"
          component={MetadataConfigs}
        />
        <Route
          path="/graph-management/:id/data-import/:jobId/import-tasks"
          component={ImportTasks}
        />
        {/* <Route
        path="/graph-management/:id/data-import/import-manager/:jobId/details"
        component={JobDetails}
      /> */}
        <Route
          path="/graph-management/:id/data-import/import-manager/:rest*"
          component={ImportManager}
        />
        <Route
          path="/graph-management/:id/async-tasks"
          component={AsyncTaskList}
        />
        <Route path="/graph-management" component={GraphManagement} />
        <Route path="/" component={GraphManagement} />
      </Router>
    </div>
  );
};

export default App;
