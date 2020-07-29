import React from 'react';
import { Route } from 'wouter';

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
import GraphManagementSidebar from './graph-management/GraphManagementSidebar';

const App: React.FC = () => {
  return (
    <div>
      <AppBar />
      <GraphManagementSidebar />
      <Route
        path="/graph-management/:id/data-import/import-tasks/:taskId/error-log"
        component={TaskErrorLogs}
      />
      <Route
        path="/graph-management/:id/data-import/import-manager/:jobId/error-log"
        component={JobErrorLogs}
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
      <Route
        path="/graph-management/:id/data-import/import-manager/:jobId/details"
        component={JobDetails}
      />
      <Route
        path="/graph-management/:id/data-import/import-manager"
        component={ImportManager}
      />
      <Route path="/graph-management" component={GraphManagement} />
      <Route path="/" component={GraphManagement} />
    </div>
  );
};

export default App;
