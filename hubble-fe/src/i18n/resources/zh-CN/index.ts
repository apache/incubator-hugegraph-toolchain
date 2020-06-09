import { merge } from 'lodash-es';
import {
  GraphManagementSideBarResources,
  DataImportResources
} from './graph-managment';

const translation = {
  translation: merge(GraphManagementSideBarResources, DataImportResources)
};

export default translation;
