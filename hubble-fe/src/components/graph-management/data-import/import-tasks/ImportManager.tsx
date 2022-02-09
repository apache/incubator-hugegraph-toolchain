import React, { useContext, useEffect } from 'react';
import { observer } from 'mobx-react';
import { useRoute, useLocation, Switch, Route } from 'wouter';
import { useTranslation } from 'react-i18next';
import { isNull } from 'lodash-es';
import classnames from 'classnames';
import { Breadcrumb } from 'hubble-ui';

import { JobDetails } from './job-details';
import ImportTaskList from './ImportTaskList';
import {
  GraphManagementStoreContext,
  DataImportRootStoreContext,
  ImportManagerStoreContext
} from '../../../../stores';

import './ImportManager.less';
import ImportTasks from './ImportTasks';

const ImportManager: React.FC = observer(() => {
  const graphManagementStore = useContext(GraphManagementStoreContext);
  const dataImportRootStore = useContext(DataImportRootStoreContext);
  const importManagerStore = useContext(ImportManagerStoreContext);
  const { dataMapStore, serverDataImportStore } = dataImportRootStore;
  const [_, params] = useRoute(
    '/graph-management/:id/data-import/import-manager/:rest*'
  );
  const [, setLocation] = useLocation();
  const { t } = useTranslation();

  const wrapperClassName = classnames({
    'import-manager': true,
    'import-manager-with-expand-sidebar': graphManagementStore.isExpanded
  });

  useEffect(() => {
    window.scrollTo(0, 0);

    graphManagementStore.fetchIdList();
    importManagerStore.setCurrentId(Number(params!.id));

    return () => {
      importManagerStore.dispose();
    };
  }, []);

  return (
    <section className={wrapperClassName}>
      <div className="import-manager-breadcrumb-wrapper">
        <Breadcrumb>
          <Breadcrumb.Item
            onClick={() => {
              if (!isNull(importManagerStore.selectedJob)) {
                setLocation(
                  `/graph-management/${importManagerStore.currentId}/data-import/import-manager`
                );
                importManagerStore.setSelectedJob(null);
                importManagerStore.fetchImportJobList();
              }

              // reset stores
              // dataMapStore.dispose();
              // serverDataImportStore.dispose();
            }}
          >
            {t('breadcrumb.first')}
          </Breadcrumb.Item>
          {importManagerStore.selectedJob && (
            <Breadcrumb.Item>
              {importManagerStore.selectedJob.job_name}
            </Breadcrumb.Item>
          )}
        </Breadcrumb>
      </div>
      <Switch>
        <Route
          path="/graph-management/:id/data-import/import-manager/:jobId/import-tasks/:status*"
          component={ImportTasks}
        />
        <Route
          path="/graph-management/:id/data-import/import-manager/:jobId/details"
          component={JobDetails}
        />
        <Route
          path="/graph-management/:id/data-import/import-manager"
          component={ImportTaskList}
        />
      </Switch>
    </section>
  );
});

export default ImportManager;
