import { merge } from 'lodash-es';
import {
  CommonResources,
  GraphManagementSideBarResources,
  DataImportResources,
  AsyncTasksResources
} from './graph-managment';

const translation = {
  translation: merge(
    CommonResources,
    GraphManagementSideBarResources,
    DataImportResources,
    AsyncTasksResources
  )
};

export default translation;
