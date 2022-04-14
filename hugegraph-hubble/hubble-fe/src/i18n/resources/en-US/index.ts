import { merge } from 'lodash-es';
import {
  CommonResources,
  DataAnalyzeResources,
  GraphManagementSideBarResources,
  DataImportResources,
  AsyncTasksResources,
  Addition
} from './graph-managment';

const translation = {
  translation: merge(
    CommonResources,
    DataAnalyzeResources,
    GraphManagementSideBarResources,
    DataImportResources,
    AsyncTasksResources,
    Addition
  )
};

export default translation;
