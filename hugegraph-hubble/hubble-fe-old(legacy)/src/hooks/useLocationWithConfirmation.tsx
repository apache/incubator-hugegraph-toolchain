/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. The ASF
 * licenses this file to You under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

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
