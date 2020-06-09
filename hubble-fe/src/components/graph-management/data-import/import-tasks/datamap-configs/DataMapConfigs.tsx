import React, { useContext } from 'react';
import { observer } from 'mobx-react';
import { Menu } from '@baidu/one-ui';

import { DataImportRootStoreContext } from '../../../../../stores';
import FileConfigs from './FileConfigs';
import TypeConfigs from './TypeConfigs';

import './DataMapConfigs.less';

const DataMapConfigs: React.FC = observer(() => {
  const dataImportRootStore = useContext(DataImportRootStoreContext);
  const { dataMapStore, serverDataImportStore } = dataImportRootStore;

  return (
    <div className="import-tasks-step-wrapper">
      <Menu
        mode="inline"
        needBorder={true}
        style={{ width: 200, height: 'calc(100vh - 194px)' }}
        selectedKeys={[String(dataMapStore.selectedFileId)]}
        onClick={(e: any) => {
          // reset state from the previous file
          dataMapStore.resetDataMaps();

          // if data import starts, do not expand collpase
          if (!serverDataImportStore.isServerStartImport) {
            dataMapStore.switchExpand('file', true);
          }

          dataMapStore.setSelectedFileId(Number(e.key));
          dataMapStore.setSelectedFileInfo();
          serverDataImportStore.switchImporting(false);
        }}
      >
        {dataMapStore.fileMapInfos
          .filter(({ name }) =>
            dataImportRootStore.successFileUploadTaskNames.includes(name)
          )
          .map(({ id, name }) => (
            <Menu.Item key={id}>
              <span>{name}</span>
            </Menu.Item>
          ))}
      </Menu>
      <div className="import-tasks-data-map-configs">
        <FileConfigs />
        <TypeConfigs />
      </div>
    </div>
  );
});

export default DataMapConfigs;
