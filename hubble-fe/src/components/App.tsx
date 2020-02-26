import React from 'react';
import { Route } from 'wouter';
import { AppBar } from './common';
import {
  GraphManagement,
  DataAnalyze,
  MetadataConfigs
} from './graph-management';
import GraphManagementSidebar from './graph-management/GraphManagementSidebar';

const App: React.FC = () => {
  return (
    <div>
      <AppBar />
      <GraphManagementSidebar />
      <Route
        path="/graph-management/:id/data-analyze"
        component={DataAnalyze}
      />
      <Route
        path="/graph-management/:id/metadata-configs"
        component={MetadataConfigs}
      />
      <Route path="/graph-management" component={GraphManagement} />
      <Route path="/" component={GraphManagement} />
    </div>
  );
};

export default App;
