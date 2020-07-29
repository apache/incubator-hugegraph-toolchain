import { merge } from 'lodash-es';
import {
  CommonResources,
  GraphManagementSideBarResources,
  DataImportResources
} from './graph-managment';

const translation = {
  translation: merge(
    CommonResources,
    GraphManagementSideBarResources,
    DataImportResources
  )
};

export default translation;
