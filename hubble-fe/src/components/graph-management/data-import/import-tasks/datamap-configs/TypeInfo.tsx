import React, {
  useState,
  useRef,
  useContext,
  useCallback,
  useEffect
} from 'react';
import { observer } from 'mobx-react';
import { useTranslation } from 'react-i18next';
import classnames from 'classnames';
import { Button } from '@baidu/one-ui';

import { Tooltip } from '../../../../common';
import { DataImportRootStoreContext } from '../../../../../stores';
import VertexMap from './VertexMap';
import EdgeMap from './EdgeMap';

import ArrowIcon from '../../../../../assets/imgs/ic_arrow_16.svg';
import { VertexType } from '../../../../../stores/types/GraphManagementStore/metadataConfigsStore';

export interface TypeInfoProps {
  type: 'vertex' | 'edge';
  mapIndex: number;
}

const TypeInfo: React.FC<TypeInfoProps> = observer(({ type, mapIndex }) => {
  const dataImportRootStore = useContext(DataImportRootStoreContext);
  const { dataMapStore, serverDataImportStore } = dataImportRootStore;
  const [isExpand, switchExpand] = useState(false);
  const [isDeletePop, switchDeletePop] = useState(false);
  const [checkOrEdit, setCheckOrEdit] = useState<'check' | 'edit' | boolean>(
    false
  );
  const typeInfoWrapperRef = useRef<HTMLDivElement>(null);
  const manipulationAreaRef = useRef<HTMLDivElement>(null);
  const editButtonRef = useRef<HTMLButtonElement>(null);
  const deleteButtonRef = useRef<HTMLButtonElement>(null);
  const deleteWrapperRef = useRef<HTMLImageElement>(null);
  const { t } = useTranslation();

  let vertexInfo: VertexType | undefined;
  let sourceVertexInfo: VertexType | undefined;
  let targetVertexInfo: VertexType | undefined;

  if (type === 'vertex') {
    vertexInfo = dataImportRootStore.vertexTypes.find(
      ({ name }) =>
        name === dataMapStore.selectedFileInfo?.vertex_mappings[mapIndex].label
    );
  } else {
    const edgeInfo = dataImportRootStore.edgeTypes.find(
      ({ name }) =>
        name === dataMapStore.selectedFileInfo!.edge_mappings[mapIndex].label
    );

    sourceVertexInfo = dataImportRootStore.vertexTypes.find(
      ({ name }) => name === edgeInfo?.source_label
    );

    targetVertexInfo = dataImportRootStore.vertexTypes.find(
      ({ name }) => name === edgeInfo?.target_label
    );
  }

  const handleImgClickExpand = () => {
    switchExpand(!isExpand);

    if (Boolean(checkOrEdit)) {
      if (checkOrEdit === 'edit') {
        dataMapStore.switchEditTypeConfig(false);
      }

      setCheckOrEdit(false);
    } else {
      if (type === 'vertex') {
        dataMapStore.syncEditMap('vertex', mapIndex);
      } else {
        dataMapStore.syncEditMap('edge', mapIndex);
      }

      setCheckOrEdit('check');
    }
  };

  const handleOutSideClick = useCallback(
    (e: MouseEvent) => {
      if (
        isDeletePop &&
        deleteWrapperRef.current &&
        !deleteWrapperRef.current.contains(e.target as Element)
      ) {
        switchDeletePop(false);
      }
    },
    [isDeletePop]
  );

  const handleTypeInfoExpandClick = useCallback(
    (e: MouseEvent) => {
      if (
        manipulationAreaRef.current &&
        !manipulationAreaRef.current?.contains(e.target as Element)
      ) {
        handleImgClickExpand();
      }
    },
    [handleImgClickExpand]
  );

  const expandClassName = classnames({
    expand: isExpand,
    collpase: !isExpand
  });

  useEffect(() => {
    document.addEventListener('click', handleOutSideClick, false);
    typeInfoWrapperRef.current!.addEventListener(
      'click',
      handleTypeInfoExpandClick,
      false
    );

    return () => {
      document.removeEventListener('click', handleOutSideClick, false);
      typeInfoWrapperRef.current!.removeEventListener(
        'click',
        handleTypeInfoExpandClick,
        false
      );
    };
  }, [handleOutSideClick, handleTypeInfoExpandClick]);

  return (
    <div style={{ marginBottom: 16 }}>
      <div
        className="import-tasks-data-type-info-wrapper"
        ref={typeInfoWrapperRef}
      >
        <div
          className="import-tasks-data-type-info"
          style={{ flexDirection: 'row' }}
        >
          <div style={{ marginRight: 16 }}>
            <img
              src={ArrowIcon}
              alt="collpaseOrExpand"
              className={expandClassName}
            />
          </div>
          <div style={{ flexDirection: 'column' }}>
            <span className="import-tasks-data-type-info-title">
              {t('data-configs.type.info.type')}
            </span>
            <span className="import-tasks-data-type-info-content">
              {type === 'vertex'
                ? t('data-configs.type.vertex.type')
                : t('data-configs.type.edge.type')}
            </span>
          </div>
        </div>
        <div className="import-tasks-data-type-info">
          <span className="import-tasks-data-type-info-title">
            {t('data-configs.type.info.name')}
          </span>
          <span className="import-tasks-data-type-info-content">
            {type === 'vertex'
              ? dataMapStore.selectedFileInfo?.vertex_mappings[mapIndex].label
              : dataMapStore.selectedFileInfo?.edge_mappings[mapIndex].label}
          </span>
        </div>
        <div className="import-tasks-data-type-info">
          <span className="import-tasks-data-type-info-title">
            {t('data-configs.type.info.ID-strategy')}
          </span>
          <span className="import-tasks-data-type-info-content">
            {type === 'vertex'
              ? t(`data-configs.type.ID-strategy.${vertexInfo?.id_strategy}`) +
                (vertexInfo?.id_strategy === 'PRIMARY_KEY'
                  ? '-' + vertexInfo?.primary_keys.join('，')
                  : '')
              : `起点：${
                  t(
                    `data-configs.type.ID-strategy.${sourceVertexInfo?.id_strategy}`
                  ) +
                  (sourceVertexInfo?.id_strategy === 'PRIMARY_KEY'
                    ? '-' + sourceVertexInfo?.primary_keys.join('，')
                    : '')
                } 终点：${
                  t(
                    `data-configs.type.ID-strategy.${targetVertexInfo?.id_strategy}`
                  ) +
                  (targetVertexInfo?.id_strategy === 'PRIMARY_KEY'
                    ? '-' + targetVertexInfo?.primary_keys.join('，')
                    : '')
                }`}
          </span>
        </div>
        <div className="import-tasks-data-type-info" ref={manipulationAreaRef}>
          {!dataMapStore.readOnly && !dataMapStore.lock && (
            <>
              <Button
                ref={editButtonRef}
                size="medium"
                style={{ width: 78, marginRight: 12 }}
                disabled={
                  dataMapStore.isExpandTypeConfig ||
                  dataMapStore.isAddNewTypeConfig ||
                  serverDataImportStore.isServerStartImport
                }
                onClick={() => {
                  switchExpand(true);
                  setCheckOrEdit('edit');
                  dataMapStore.switchEditTypeConfig(true);
                  dataMapStore.syncEditMap(type, mapIndex);
                  dataMapStore.syncValidateNullAndValueMappings(type);
                }}
              >
                {t('data-configs.manipulations.edit')}
              </Button>
              <Tooltip
                placement="bottom-end"
                tooltipShown={isDeletePop}
                modifiers={{
                  offset: {
                    offset: '0, 10'
                  }
                }}
                tooltipWrapperProps={{
                  className: 'import-tasks-tooltips',
                  style: {
                    zIndex: 1042
                  }
                }}
                tooltipWrapper={
                  <div ref={deleteWrapperRef}>
                    <p>
                      {t('data-configs.manipulations.hints.delete-confirm')}
                    </p>
                    <p>{t('data-configs.manipulations.hints.warning')}</p>
                    <div
                      style={{
                        display: 'flex',
                        marginTop: 12,
                        color: '#2b65ff',
                        cursor: 'pointer'
                      }}
                    >
                      <Button
                        ref={deleteButtonRef}
                        type="primary"
                        size="medium"
                        style={{ width: 60, marginRight: 12 }}
                        disabled={serverDataImportStore.isServerStartImport}
                        onClick={async () => {
                          // really weird! if import task comes from import-manager
                          // datamapStore.fileMapInfos cannot be updated in <TypeConfigs />
                          // though it's already re-rendered
                          if (dataMapStore.isIrregularProcess) {
                            type === 'vertex'
                              ? await dataMapStore.deleteVertexMap(mapIndex)
                              : await dataMapStore.deleteEdgeMap(mapIndex);

                            dataMapStore.fetchDataMaps();
                          } else {
                            type === 'vertex'
                              ? dataMapStore.deleteVertexMap(mapIndex)
                              : dataMapStore.deleteEdgeMap(mapIndex);
                          }

                          switchDeletePop(false);
                        }}
                      >
                        {t('data-configs.type.info.delete')}
                      </Button>
                      <Button
                        size="medium"
                        style={{ width: 60 }}
                        onClick={() => {
                          switchDeletePop(false);
                        }}
                      >
                        {t('data-configs.manipulations.cancel')}
                      </Button>
                    </div>
                  </div>
                }
                childrenProps={{
                  onClick() {
                    switchDeletePop(true);
                  }
                }}
              >
                <Button
                  size="medium"
                  style={{ width: 78 }}
                  disabled={
                    dataMapStore.isExpandTypeConfig ||
                    dataMapStore.isAddNewTypeConfig ||
                    serverDataImportStore.isServerStartImport
                  }
                >
                  {t('data-configs.manipulations.delete')}
                </Button>
              </Tooltip>
            </>
          )}
        </div>
      </div>
      {isExpand && Boolean(checkOrEdit) && type === 'vertex' && (
        <VertexMap
          checkOrEdit={checkOrEdit}
          onCancelCreateVertex={() => {
            setCheckOrEdit(false);
            switchExpand(false);
          }}
          vertexMapIndex={mapIndex}
        />
      )}
      {isExpand && Boolean(checkOrEdit) && type === 'edge' && (
        <EdgeMap
          checkOrEdit={checkOrEdit}
          onCancelCreateEdge={() => {
            setCheckOrEdit(false);
            switchExpand(false);
          }}
          edgeMapIndex={mapIndex}
        />
      )}
    </div>
  );
});

export default TypeInfo;
