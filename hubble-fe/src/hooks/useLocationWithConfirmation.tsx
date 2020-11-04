import { useContext } from 'react';
import useLocation from 'wouter/use-location';
import { last } from 'lodash-es';

import i18next from '../i18n';
import {
  ImportManagerStoreContext,
  DataImportRootStoreContext
} from '../stores';

export default function useLocationWithConfirmation() {
  const importManagerStore = useContext(ImportManagerStoreContext);
  const dataImportRootStore = useContext(DataImportRootStoreContext);
  const [location, setLocation] = useLocation();
  const status = last(location.split('/'));

  return [
    location,
    (newLocation: string) => {
      let perfomNavigation = true;
      const category = last(newLocation.split('/'));

      if (
        status === 'upload' &&
        category === 'import-manager' &&
        dataImportRootStore.fileUploadTasks.some(
          ({ status }) => status === 'uploading'
        )
      ) {
        perfomNavigation = window.confirm(
          i18next.t('server-data-import.hint.confirm-navigation')
        );
      }

      if (perfomNavigation) {
        if (
          ['upload', 'mapping', 'loading'].includes(String(status)) &&
          category === 'import-manager'
        ) {
          importManagerStore.setSelectedJob(null);
          importManagerStore.fetchImportJobList();
        }

        setLocation(newLocation);
      }
    }
  ];
}
