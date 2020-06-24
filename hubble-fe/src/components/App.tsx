import React, { useContext, useEffect } from 'react';
import { Route, useRoute } from 'wouter';
import { observer } from 'mobx-react';
import { AppBar } from './common';
import {
  GraphManagement,
  DataAnalyze,
  MetadataConfigs,
  ImportTasks
} from './graph-management';
import GraphManagementSidebar from './graph-management/GraphManagementSidebar';
import { DataImportRootStoreContext } from '../stores';

const App: React.FC = () => {
  return (
    <div>
      <AppBar />
      <GraphManagementSidebar />
      <Route
        path="/graph-management/:id/data-import/import-tasks/:taskId/error-log"
        component={ErrorLogs}
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
        path="/graph-management/:id/data-import/import-tasks"
        component={ImportTasks}
      />
      <Route path="/graph-management" component={GraphManagement} />
      <Route path="/" component={GraphManagement} />
    </div>
  );
};

const ErrorLogs: React.FC = observer(() => {
  const dataImportRootStore = useContext(DataImportRootStoreContext);
  const { serverDataImportStore } = dataImportRootStore;
  const [, params] = useRoute(
    '/graph-management/:id/data-import/import-tasks/:taskId/error-log'
  );

  useEffect(() => {
    serverDataImportStore.checkErrorLogs(
      Number(params!.taskId),
      Number(params!.id)
    );
  }, [params!.id, params!.taskId]);

  return (
    <section
      style={{
        width: '80vw',
        display: 'flex',
        justifyContent: 'center',
        alignItems: 'center',
        marginBottom: 16,
        position: 'relative',
        top: 76,
        left: '10vw',
        lineHeight: 2,
        whiteSpace: 'pre',
        fontSize: 14
      }}
    >
      <div>{serverDataImportStore.errorLogs}</div>
    </section>
  );
});

export default App;
