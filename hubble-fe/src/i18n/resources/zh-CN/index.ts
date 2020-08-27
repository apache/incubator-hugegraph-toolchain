import { merge } from 'lodash-es';
import {
  CommonResources,
  DataAnalyzeResources,
  GraphManagementSideBarResources,
  DataImportResources,
  AsyncTasksResources
} from './graph-managment';

const translation = {
  translation: merge(
    CommonResources,
    DataAnalyzeResources,
    GraphManagementSideBarResources,
    DataImportResources,
    AsyncTasksResources
  )
};

export default translation;
