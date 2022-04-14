import React, { useContext, useState, useEffect, useRef } from 'react';
import { observer } from 'mobx-react';
import { isEmpty, size } from 'lodash-es';
import { useLocation } from 'wouter';
import { useTranslation } from 'react-i18next';
import classnames from 'classnames';
import { Button } from 'hubble-ui';

import { DataImportRootStoreContext } from '../../../../../stores';

import { Tooltip } from '../../../../common';
import TypeInfo from './TypeInfo';
import VertexMap from './VertexMap';
import EdgeMap from './EdgeMap';

import ArrowIcon from '../../../../../assets/imgs/ic_arrow_16.svg';

const TypeConfigs: React.FC = observer(() => {
  const dataImportRootStore = useContext(DataImportRootStoreContext);
  const [isExpand, switchExpand] = useState(true);
  const { dataMapStore, serverDataImportStore } = dataImportRootStore;
  const [isCreateVertexMap, switchCreateVertexMap] = useState(false);
  const [isCreateEdgeMap, switchCreateEdgeMap] = useState(false);
  const [, setLocation] = useLocation();
  const { t } = useTranslation();

  const shouldRevealInitalButtons =
    !isCreateVertexMap &&
    !isCreateEdgeMap &&
    isEmpty(dataMapStore.selectedFileInfo?.vertex_mappings) &&
    isEmpty(dataMapStore.selectedFileInfo?.edge_mappings);

  const invalidFileMaps = !dataMapStore.isIrregularProcess
    ? dataMapStore.fileMapInfos.filter(
        ({ name, vertex_mappings, edge_mappings }) =>
          dataImportRootStore.successFileUploadTaskNames.includes(name) &&
          isEmpty(vertex_mappings) &&
          isEmpty(edge_mappings)
      )
    : dataMapStore.fileMapInfos.filter(
        ({ vertex_mappings, edge_mappings }) =>
          isEmpty(vertex_mappings) && isEmpty(edge_mappings)
      );

  const expandClassName = classnames({
    'import-tasks-step-content-header-expand': isExpand,
    'import-tasks-step-content-header-collpase': !isExpand
  });

  const nextButtonTooltipClassName = classnames({
    'import-tasks-data-map-tooltip': true,
    'no-display': size(invalidFileMaps) === 0
  });

  const handleExpand = () => {
    switchExpand(!isExpand);
  };

  const handleCreate = (type: 'vertex' | 'edge', flag: boolean) => () => {
    dataMapStore.switchExpand('file', false);
    // Adding a new type config counts editing as well
    dataMapStore.switchAddNewTypeConfig(flag);

    if (type === 'vertex') {
      switchCreateVertexMap(flag);
    } else {
      switchCreateEdgeMap(flag);
    }
  };

  useEffect(() => {
    // close dashboard when user selects another file
    // since the state is not in mobx store, we have to do this for now
    switchCreateVertexMap(false);
    switchCreateEdgeMap(false);
    switchExpand(true);
  }, [dataMapStore.selectedFileId]);

  return (
    <div className="import-tasks-data-map" style={{ marginBottom: 16 }}>
      <div className="import-tasks-step-content-header">
        <span style={{ lineHeight: '32px' }}>
          {t('data-configs.type.title')}
        </span>
        <img
          src={ArrowIcon}
          alt="collpaseOrExpand"
          className={expandClassName}
          onClick={handleExpand}
        />
        {!dataMapStore.readOnly &&
          !dataMapStore.lock &&
          !shouldRevealInitalButtons && (
            <TypeConfigMapCreations
              onCreateVertex={handleCreate('vertex', true)}
              onCreateEdge={handleCreate('edge', true)}
              disabled={
                isCreateVertexMap ||
                isCreateEdgeMap ||
                dataMapStore.isExpandTypeConfig ||
                serverDataImportStore.isServerStartImport
              }
            />
          )}
      </div>
      {!dataMapStore.readOnly &&
        !dataMapStore.lock &&
        shouldRevealInitalButtons && (
          <TypeConfigMapCreations
            onCreateVertex={handleCreate('vertex', true)}
            onCreateEdge={handleCreate('edge', true)}
          />
        )}
      {isExpand && (
        <>
          {isCreateVertexMap && (
            <VertexMap
              checkOrEdit={false}
              onCancelCreateVertex={handleCreate('vertex', false)}
            />
          )}
          {isCreateEdgeMap && (
            <EdgeMap
              checkOrEdit={false}
              onCancelCreateEdge={handleCreate('edge', false)}
            />
          )}
          {dataMapStore.selectedFileInfo?.vertex_mappings
            .map((mapping, index) => (
              <TypeInfo type="vertex" mapIndex={index} key={mapping.id} />
            ))
            .reverse()}
          {dataMapStore.selectedFileInfo?.edge_mappings
            .map((mapping, index) => (
              <TypeInfo type="edge" mapIndex={index} key={mapping.id} />
            ))
            .reverse()}
        </>
      )}
      {!dataMapStore.readOnly && (
        <div className="import-tasks-data-map-manipulations">
          {!serverDataImportStore.isServerStartImport && (
            <Button
              size="medium"
              style={{ marginRight: 16 }}
              onClick={() => {
                setLocation(
                  `/graph-management/${dataImportRootStore.currentId}/data-import/import-manager/${dataImportRootStore.currentJobId}/import-tasks/upload`
                );
                dataImportRootStore.setCurrentStep(1);
              }}
              disabled={
                // disable previous when it's irregular process
                // dataMapStore.isIrregularProcess ||
                // serverDataImportStore.isIrregularProcess
                dataMapStore.lock
              }
            >
              {t('data-configs.manipulations.previous')}
            </Button>
          )}
          <Tooltip
            trigger="hover"
            placement="top-start"
            modifiers={{
              offset: {
                offset: '0, 10'
              }
            }}
            tooltipWrapperProps={{
              className: nextButtonTooltipClassName
            }}
            tooltipWrapper={
              <>
                <div className="import-tasks-data-map-tooltip-text">
                  {t('data-configs.type.hint.no-vertex-or-edge-mapping')}
                </div>
                {invalidFileMaps.map(({ name }) => (
                  <div
                    className="import-tasks-data-map-tooltip-text"
                    key={name}
                  >
                    {name}
                  </div>
                ))}
              </>
            }
          >
            <Button
              type="primary"
              size="medium"
              disabled={size(invalidFileMaps) !== 0}
              onClick={() => {
                setLocation(
                  `/graph-management/${dataImportRootStore.currentId}/data-import/import-manager/${dataImportRootStore.currentJobId}/import-tasks/loading`
                );

                dataImportRootStore.setCurrentStep(3);
                // avoid rests when user moves back to previous step
                if (dataImportRootStore.currentStatus === 'MAPPING') {
                  // users may browse from <JobDetails />
                  // need to reset readonly relative states
                  serverDataImportStore.switchExpandImportConfig(true);
                  serverDataImportStore.switchReadOnly(false);
                  serverDataImportStore.switchIrregularProcess(false);
                  serverDataImportStore.syncImportConfigs(
                    dataMapStore.selectedFileInfo!.load_parameter
                  );
                  // same with click jumps in <ImportTaskList />
                  serverDataImportStore.switchFetchImportStatus('standby');
                  serverDataImportStore.switchImportFinished(false);
                  serverDataImportStore.resetImportTasks();

                  dataImportRootStore.setCurrentStatus('SETTING');
                  dataImportRootStore.sendMappingCompleteSignal();
                }
              }}
            >
              {t('data-configs.manipulations.next')}
            </Button>
          </Tooltip>
        </div>
      )}
    </div>
  );
});

export interface TypeConfigMapCreationsProps {
  onCreateVertex: () => void;
  onCreateEdge: () => void;
  disabled?: boolean;
}

const TypeConfigMapCreations: React.FC<TypeConfigMapCreationsProps> = observer(
  ({ onCreateVertex, onCreateEdge, disabled = false }) => {
    const { t } = useTranslation();

    return (
      <div className="import-tasks-data-type-manipulations">
        <Button
          size="medium"
          style={{ marginRight: 16 }}
          onClick={onCreateVertex}
          disabled={disabled}
        >
          {t('data-configs.type.manipulation.create-vertex')}
        </Button>
        <Button size="medium" onClick={onCreateEdge} disabled={disabled}>
          {t('data-configs.type.manipulation.create-edge')}
        </Button>
      </div>
    );
  }
);

export default TypeConfigs;
