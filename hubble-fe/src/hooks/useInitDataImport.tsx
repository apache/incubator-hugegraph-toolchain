import { useState, useEffect, useContext } from 'react';
import { useRoute, useLocation } from 'wouter';
import { isNull, isEmpty, isUndefined } from 'lodash-es';

import {
  GraphManagementStoreContext,
  ImportManagerStoreContext,
  DataImportRootStoreContext
} from '../stores';

export default function useInitDataImport() {
  const graphManagementStore = useContext(GraphManagementStoreContext);
  const importManagerStore = useContext(ImportManagerStoreContext);
  const dataImportRootStore = useContext(DataImportRootStoreContext);
  const { dataMapStore, serverDataImportStore } = dataImportRootStore;
  const [initReady, setInitReady] = useState(
    !isEmpty(importManagerStore.importJobList)
  );
  const [, setLocation] = useLocation();

  const [, params] = useRoute(
    '/graph-management/:id/data-import/import-manager/:jobId/import-tasks/:status'
  );

  useEffect(() => {
    const init = async (params: Record<string, string>) => {
      const { id, jobId, status } = params!;
      // sidebar data
      graphManagementStore.fetchIdList();

      // init importManagerStore
      importManagerStore.setCurrentId(Number(id));
      // import job list
      await importManagerStore.fetchImportJobList();
      importManagerStore.setSelectedJob(Number(jobId));

      // init dataImportRootStore
      dataImportRootStore.setCurrentId(Number(id));
      dataImportRootStore.setCurrentJobId(Number(jobId));

      await dataMapStore.fetchDataMaps();

      const job = importManagerStore.importJobList.find(
        ({ id: _jobId }) => _jobId === Number(jobId)
      );

      if (isUndefined(job)) {
        return;
      }

      const defautlSelectedFileId = dataMapStore.fileMapInfos.filter(
        ({ file_status }) => file_status === 'COMPLETED'
      )[0]?.id;

      dataImportRootStore.setCurrentStatus(job.job_status);

      if (status === 'upload') {
        if (job.job_status === 'DEFAULT' || job.job_status === 'UPLOADING') {
        }

        if (job.job_status === 'MAPPING') {
          dataMapStore.setSelectedFileId(defautlSelectedFileId);
          dataMapStore.setSelectedFileInfo();
          dataMapStore.switchIrregularProcess(true);
          serverDataImportStore.syncImportConfigs(
            dataMapStore.selectedFileInfo!.load_parameter
          );

          // setLocation(
          //   `/graph-management/${id}/data-import/import-manager/${jobId}/import-tasks/mapping`
          // );
        }

        if (job.job_status === 'SETTING' || job.job_status === 'LOADING') {
          dataMapStore.setSelectedFileId(defautlSelectedFileId);
          dataMapStore.setSelectedFileInfo();
          dataMapStore.switchIrregularProcess(true);

          serverDataImportStore.syncImportConfigs(
            dataMapStore.selectedFileInfo!.load_parameter
          );
          serverDataImportStore.switchIrregularProcess(true);

          // setLocation(
          //   `/graph-management/${id}/data-import/import-manager/${jobId}/import-tasks/loading`
          // );
        }

        if (job.job_status === 'LOADING') {
          dataMapStore.switchLock(true);
          serverDataImportStore.switchImportConfigReadOnly(true);

          setLocation(
            `/graph-management/${id}/data-import/import-manager/${jobId}/import-tasks/loading`
          );
        }

        if (job.job_status === 'SUCCESS' || job.job_status === 'FAILED') {
          setLocation(`/graph-management/${id}/data-import/import-manager`);
          importManagerStore.setSelectedJob(null);
        }
      }

      if (status === 'mapping') {
        if (job.job_status === 'DEFAULT' || job.job_status === 'UPLOADING') {
          dataMapStore.setSelectedFileId(defautlSelectedFileId);
          dataMapStore.setSelectedFileInfo();
          dataMapStore.switchIrregularProcess(true);
          serverDataImportStore.syncImportConfigs(
            dataMapStore.selectedFileInfo!.load_parameter
          );

          // setLocation(
          //   `/graph-management/${id}/data-import/import-manager/${jobId}/import-tasks/mapping`
          // );
        }

        if (job.job_status === 'MAPPING') {
          dataMapStore.setSelectedFileId(defautlSelectedFileId);
          dataMapStore.setSelectedFileInfo();
          dataMapStore.switchIrregularProcess(true);
          serverDataImportStore.syncImportConfigs(
            dataMapStore.selectedFileInfo!.load_parameter
          );
        }

        if (job.job_status === 'SETTING' || job.job_status === 'LOADING') {
          dataMapStore.setSelectedFileId(defautlSelectedFileId);
          dataMapStore.setSelectedFileInfo();
          dataMapStore.switchIrregularProcess(true);
          serverDataImportStore.syncImportConfigs(
            dataMapStore.selectedFileInfo!.load_parameter
          );
          serverDataImportStore.switchIrregularProcess(true);

          // setLocation(
          //   `/graph-management/${id}/data-import/import-manager/${jobId}/import-tasks/loading`
          // );
        }

        if (job.job_status === 'LOADING') {
          dataMapStore.switchLock(true);
          serverDataImportStore.switchImportConfigReadOnly(true);
          setLocation(
            `/graph-management/${id}/data-import/import-manager/${jobId}/import-tasks/loading`
          );
        }

        if (job.job_status === 'SUCCESS' || job.job_status === 'FAILED') {
          setLocation(`/graph-management/${id}/data-import/import-manager`);
          importManagerStore.setSelectedJob(null);
        }
      }

      if (status === 'loading') {
        if (job.job_status === 'DEFAULT' || job.job_status === 'UPLOADING') {
          setLocation(
            `/graph-management/${id}/data-import/import-manager/${jobId}/import-tasks/upload`
          );
        }

        if (job.job_status === 'MAPPING') {
          dataMapStore.setSelectedFileId(defautlSelectedFileId);
          dataMapStore.setSelectedFileInfo();
          dataMapStore.switchIrregularProcess(true);
          serverDataImportStore.syncImportConfigs(
            dataMapStore.selectedFileInfo!.load_parameter
          );

          setLocation(
            `/graph-management/${id}/data-import/import-manager/${jobId}/import-tasks/mapping`
          );
        }

        if (job.job_status === 'SETTING' || job.job_status === 'LOADING') {
          dataMapStore.setSelectedFileId(defautlSelectedFileId);
          dataMapStore.setSelectedFileInfo();
          dataMapStore.switchIrregularProcess(true);

          serverDataImportStore.syncImportConfigs(
            dataMapStore.selectedFileInfo!.load_parameter
          );
          serverDataImportStore.switchIrregularProcess(true);
        }

        if (job.job_status === 'SETTING') {
          dataMapStore.switchReadOnly(false);
        }

        if (job.job_status === 'LOADING') {
          dataMapStore.switchLock(true);
          serverDataImportStore.switchImportConfigReadOnly(true);
        }

        if (job.job_status === 'SUCCESS' || job.job_status === 'FAILED') {
          setLocation(`/graph-management/${id}/data-import/import-manager`);
          importManagerStore.setSelectedJob(null);
        }
      }

      setInitReady(true);
    };

    // if importJobList is empty, users may refresh their page
    // if fileMapInfos is empty, users may click back/forward button on browser
    if (
      !isNull(params) &&
      (isEmpty(importManagerStore.importJobList) ||
        isNull(importManagerStore.selectedJob))
    ) {
      init(params);
    }
  }, [
    params?.status,
    importManagerStore.importJobList,
    importManagerStore.selectedJob?.id
  ]);

  return initReady;
}
