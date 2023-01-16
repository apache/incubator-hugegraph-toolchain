import React, {
  useContext,
  useState,
  useEffect,
  useRef,
  useCallback
} from 'react';
import { observer } from 'mobx-react';
import classnames from 'classnames';
import { cloneDeep, isUndefined, merge, isEmpty } from 'lodash-es';
import {
  Drawer,
  Button,
  Input,
  Select,
  Switch,
  Checkbox,
  Message
} from 'hubble-ui';

import { Tooltip } from '../../../common';
import MetadataConfigsRootStore from '../../../../stores/GraphManagementStore/metadataConfigsStore/metadataConfigsStore';
import {
  mapMetadataProperties,
  formatVertexIdText,
  vertexRadiusMapping
} from '../../../../stores/utils';

import type {
  VertexTypeValidatePropertyIndexes,
  VertexTypeProperty
} from '../../../../stores/types/GraphManagementStore/metadataConfigsStore';

import BlueArrowIcon from '../../../../assets/imgs/ic_arrow_blue.svg';
import CloseIcon from '../../../../assets/imgs/ic_close_16.svg';
import i18next from '../../../../i18n';
import { useTranslation } from 'react-i18next';

const IDStrategyMappings: Record<string, string> = {
  PRIMARY_KEY: i18next.t('addition.constant.primary-key-id'),
  AUTOMATIC: i18next.t('addition.constant.automatic-generation'),
  CUSTOMIZE_STRING: i18next.t('addition.constant.custom-string'),
  CUSTOMIZE_NUMBER: i18next.t('addition.constant.custom-number'),
  CUSTOMIZE_UUID: i18next.t('addition.constant.custom-uuid')
};

const propertyIndexTypeMappings: Record<string, string> = {
  SECONDARY: i18next.t('addition.menu.secondary-index'),
  RANGE: i18next.t('addition.menu.range-index'),
  SEARCH: i18next.t('addition.menu.full-text-index')
};

const CheckAndEditVertex: React.FC = observer(() => {
  const { metadataPropertyStore, vertexTypeStore, graphViewStore } = useContext(
    MetadataConfigsRootStore
  );
  const { t } = useTranslation();
  const [isAddProperty, switchIsAddProperty] = useState(false);
  const [isDeletePop, switchDeletePop] = useState(false);
  const [
    deleteExistPopIndexInDrawer,
    setDeleteExistPopIndexInDrawer
  ] = useState<number | null>(null);
  const [
    deleteAddedPopIndexInDrawer,
    setDeleteAddedPopIndexInDrawer
  ] = useState<number | null>(null);

  const deleteWrapperRef = useRef<HTMLImageElement>(null);
  const dropdownWrapperRef = useRef<HTMLDivElement>(null);
  const deleteWrapperInDrawerRef = useRef<HTMLDivElement>(null);

  const isEditVertex = graphViewStore.currentDrawer === 'edit-vertex';

  const metadataDrawerOptionClass = classnames({
    'metadata-drawer-options': true,
    'metadata-drawer-options-disabled': isEditVertex
  });

  // need useCallback to stop infinite callings of useEffect
  const handleOutSideClick = useCallback(
    (e: MouseEvent) => {
      const drawerWrapper = document.querySelector(
        '.new-fc-one-drawer-content-wrapper'
      );

      if (
        graphViewStore.currentDrawer === 'check-vertex' &&
        drawerWrapper &&
        !drawerWrapper.contains(e.target as Element)
      ) {
        /*
          handleOutSideClick is being called after the value assignment of data and drawer-name,
          we need to judge whether a node or edge is being clicked
        */
        if (graphViewStore.isNodeOrEdgeClicked) {
          // if node/edge is clicked, reset state and prepare for next outside clicks
          graphViewStore.switchNodeOrEdgeClicked(false);
        } else {
          graphViewStore.setCurrentDrawer('');
        }
      }

      if (
        isAddProperty &&
        isEditVertex &&
        dropdownWrapperRef.current &&
        !dropdownWrapperRef.current.contains(e.target as Element)
      ) {
        switchIsAddProperty(false);
        return;
      }

      if (
        isDeletePop &&
        deleteWrapperRef.current &&
        !deleteWrapperRef.current.contains(e.target as Element)
      ) {
        switchDeletePop(false);
      }
    },
    [graphViewStore, isAddProperty, isEditVertex, isDeletePop]
  );

  const handleCloseDrawer = () => {
    switchIsAddProperty(false);
    graphViewStore.setCurrentDrawer('');
    vertexTypeStore.selectVertexType(null);
    // clear mutations in <Drawer />
    vertexTypeStore.resetEditedSelectedVertexType();
  };

  const handleDeleteVertex = async () => {
    // cache vertex name here before it gets removed
    const vertexName = vertexTypeStore.selectedVertexType!.name;
    const vertexInfo = graphViewStore.visDataSet?.nodes.get(vertexName);
    const connectedEdgeInfos = graphViewStore.visDataSet?.edges.get(
      graphViewStore.visNetwork?.getConnectedEdges(vertexName)
    );

    // close
    handleCloseDrawer();
    switchDeletePop(false);

    // if node > 1, delete node on local before send request
    if (graphViewStore.visDataSet!.nodes.length > 1) {
      graphViewStore.visDataSet!.nodes.remove(vertexName);
    }

    await vertexTypeStore.deleteVertexType([vertexName]);

    if (vertexTypeStore.requestStatus.deleteVertexType === 'success') {
      Message.success({
        content: t('addition.common.del-success'),
        size: 'medium',
        showCloseIcon: false
      });

      vertexTypeStore.fetchVertexTypeList({ fetchAll: true });

      // if delete the last node, fetch graph data to trigger re-render to reveal <EmptyDataView />
      if (graphViewStore.visDataSet?.nodes.length === 1) {
        graphViewStore.switchGraphDataEmpty(true);
        graphViewStore.fetchGraphViewData();
      }
    }

    if (vertexTypeStore.requestStatus.deleteVertexType === 'failed') {
      Message.error({
        content: vertexTypeStore.errorMessage,
        size: 'medium',
        showCloseIcon: false
      });

      // if failed, re-add vertex and edges
      graphViewStore.visDataSet!.nodes.add(vertexInfo);
      graphViewStore.visDataSet!.edges.add(connectedEdgeInfos);
    }
  };

  useEffect(() => {
    document.addEventListener('click', handleOutSideClick, false);

    return () => {
      document.removeEventListener('click', handleOutSideClick, false);
    };
  }, [handleOutSideClick]);

  if (vertexTypeStore.selectedVertexType === null) {
    return null;
  }

  return (
    <Drawer
      title={
        !isEditVertex
          ? t('addition.vertex.type-detail')
          : t('addition.vertex.edit-type')
      }
      width={580}
      destroyOnClose
      visible={['check-vertex', 'edit-vertex'].includes(
        graphViewStore.currentDrawer
      )}
      mask={isEditVertex}
      onClose={handleCloseDrawer}
      footer={[
        <Button
          type="primary"
          size="medium"
          style={{ width: 60 }}
          disabled={
            isEditVertex &&
            (vertexTypeStore.editedSelectedVertexType.style.display_fields
              .length === 0 ||
              !vertexTypeStore.isEditReady)
          }
          onClick={async () => {
            if (!isEditVertex) {
              graphViewStore.setCurrentDrawer('edit-vertex');
              vertexTypeStore.validateEditVertexType();
            } else {
              const id = vertexTypeStore.selectedVertexType!.name;
              const updateInfo: Record<string, any> = {};

              if (
                !isEmpty(
                  vertexTypeStore.editedSelectedVertexType.append_properties
                )
              ) {
                const mappedProperties = mapMetadataProperties(
                  vertexTypeStore.selectedVertexType!.properties,
                  metadataPropertyStore.metadataProperties
                );

                const newMappedProperties = mapMetadataProperties(
                  vertexTypeStore.editedSelectedVertexType.append_properties,
                  metadataPropertyStore.metadataProperties
                );

                const mergedProperties = merge(
                  mappedProperties,
                  newMappedProperties
                );

                updateInfo.title = `
                  <div class="metadata-graph-view-tooltip-fields">
                    <div>${t('addition.common.vertex-type')}：</div>
                    <div style="min-width: 60px; max-width: 145px; marigin-right: 0">${id}</div>
                  </div>
                  <div class="metadata-graph-view-tooltip-fields">
                    <div style="max-width: 120px">${t(
                      'addition.common.association-property-and-type'
                    )}：</div>
                  </div>
                  ${Object.entries(mergedProperties)
                    .map(([key, value]) => {
                      const convertedValue =
                        value.toLowerCase() === 'text'
                          ? 'string'
                          : value.toLowerCase();

                      return `<div class="metadata-graph-view-tooltip-fields">
                        <div>${key}: </div>
                        <div>${convertedValue}</div>
                        <div></div>
                      </div>`;
                    })
                    .join('')}
                `;
              }

              if (
                vertexTypeStore.editedSelectedVertexType.style.color !== null
              ) {
                updateInfo.color = {
                  background:
                    vertexTypeStore.editedSelectedVertexType.style.color,
                  border: vertexTypeStore.editedSelectedVertexType.style.color
                };
              }

              if (
                vertexTypeStore.editedSelectedVertexType.style.size !== null
              ) {
                updateInfo.value =
                  vertexRadiusMapping[
                    vertexTypeStore.editedSelectedVertexType.style.size
                  ];
              }

              if (!isEmpty(updateInfo)) {
                updateInfo.id = id;

                graphViewStore.visDataSet!.nodes.update(updateInfo);
              }
              await vertexTypeStore.updateVertexType();

              if (vertexTypeStore.requestStatus.updateVertexType === 'failed') {
                Message.error({
                  content: vertexTypeStore.errorMessage,
                  size: 'medium',
                  showCloseIcon: false
                });

                return;
              }

              if (
                vertexTypeStore.requestStatus.updateVertexType === 'success'
              ) {
                Message.success({
                  content: t('addition.operate.modify-success'),
                  size: 'medium',
                  showCloseIcon: false
                });
              }

              handleCloseDrawer();
              graphViewStore.visNetwork!.unselectAll();
              vertexTypeStore.fetchVertexTypeList({ fetchAll: true });
            }
          }}
          key="drawer-manipulation"
        >
          {isEditVertex ? t('addition.common.save') : t('addition.common.edit')}
        </Button>,
        <Tooltip
          placement="top-start"
          tooltipShown={isDeletePop}
          modifiers={{
            offset: {
              offset: '0, 15'
            }
          }}
          tooltipWrapperProps={{
            className: 'metadata-graph-tooltips',
            style: {
              zIndex: 1042
            }
          }}
          tooltipWrapper={
            <div ref={deleteWrapperRef}>
              {vertexTypeStore.vertexTypeUsingStatus &&
              vertexTypeStore.vertexTypeUsingStatus[
                vertexTypeStore.selectedVertexType!.name
              ] ? (
                <p style={{ width: 200 }}>
                  {t('addition.vertex.using-cannot-delete')}
                </p>
              ) : (
                <>
                  <p style={{ marginBottom: 8 }}>
                    {t('addition.vertex.del-vertex-confirm')}
                  </p>
                  <p>{t('addition.edge.confirm-del-edge-careful-notice')}</p>
                  <p>{t('addition.message.long-time-notice')}</p>
                  <div
                    style={{
                      display: 'flex',
                      marginTop: 12,
                      color: '#2b65ff',
                      cursor: 'pointer'
                    }}
                  >
                    <div
                      style={{ marginRight: 16, cursor: 'pointer' }}
                      onClick={handleDeleteVertex}
                    >
                      {t('addition.common.confirm')}
                    </div>
                    <div
                      onClick={() => {
                        switchDeletePop(false);
                      }}
                    >
                      {t('addition.common.cancel')}
                    </div>
                  </div>
                </>
              )}
            </div>
          }
          childrenProps={{
            onClick() {
              if (isEditVertex) {
                handleCloseDrawer();
                return;
              }

              switchDeletePop(true);
            }
          }}
        >
          <Button size="medium" style={{ width: 60 }}>
            {isEditVertex
              ? t('addition.common.close')
              : t('addition.common.del')}
          </Button>
        </Tooltip>
      ]}
    >
      <div className="metadata-configs-drawer">
        <div
          className="metadata-title"
          style={{ marginBottom: 16, width: 88, textAlign: 'right' }}
        >
          {t('addition.menu.base-info')}
        </div>
        <div
          className={metadataDrawerOptionClass}
          style={{ alignItems: 'center' }}
        >
          <div className="metadata-drawer-options-name">
            <span>{t('addition.vertex.vertex-type-name')}：</span>
          </div>
          <div style={{ maxWidth: 420 }}>
            {vertexTypeStore.selectedVertexType!.name}
          </div>
        </div>

        <div className="metadata-drawer-options">
          <div className="metadata-drawer-options-name">
            <span
              className={
                isEditVertex ? 'metadata-drawer-options-name-edit' : ''
              }
            >
              {t('addition.vertex.vertex-style')}：
            </span>
          </div>
          <div className="new-vertex-type-options-colors">
            <Select
              width={66}
              size="medium"
              prefixCls="new-fc-one-select-another"
              dropdownMatchSelectWidth={false}
              showSearch={false}
              disabled={!isEditVertex}
              value={vertexTypeStore.editedSelectedVertexType.style.color?.toLowerCase()}
              onChange={(value: string) => {
                vertexTypeStore.mutateEditedSelectedVertexType({
                  ...vertexTypeStore.editedSelectedVertexType,
                  style: {
                    color: value,
                    icon: null,
                    size:
                      vertexTypeStore.editedSelectedVertexType.style.size !==
                      null
                        ? vertexTypeStore.editedSelectedVertexType.style.size
                        : vertexTypeStore.selectedVertexType!.style.size,
                    display_fields:
                      vertexTypeStore.editedSelectedVertexType.style
                        .display_fields.length !== 0
                        ? vertexTypeStore.editedSelectedVertexType.style
                            .display_fields
                        : vertexTypeStore.selectedVertexType!.style
                            .display_fields
                  }
                });
              }}
            >
              {vertexTypeStore.colorSchemas.map(
                (color: string, index: number) => (
                  <Select.Option
                    value={color}
                    key={color}
                    style={{
                      display: 'inline-block',
                      marginLeft: index % 5 === 0 ? 8 : 0,
                      marginTop: index < 5 ? 6 : 2,
                      width: 31
                    }}
                  >
                    <div
                      className={
                        (vertexTypeStore.editedSelectedVertexType.style
                          .color !== null
                          ? vertexTypeStore.editedSelectedVertexType.style.color.toLowerCase()
                          : vertexTypeStore.selectedVertexType!.style.color!.toLowerCase()) ===
                        color
                          ? 'new-vertex-type-options-border new-vertex-type-options-color'
                          : 'new-vertex-type-options-no-border new-vertex-type-options-color'
                      }
                      style={{
                        background: color,
                        marginLeft: -4,
                        marginTop: 4.4
                      }}
                    ></div>
                  </Select.Option>
                )
              )}
            </Select>
          </div>
          <div className="new-vertex-type-options-colors">
            <Select
              width={67}
              size="medium"
              showSearch={false}
              disabled={!isEditVertex}
              style={{ paddingLeft: 7 }}
              value={
                vertexTypeStore.editedSelectedVertexType.style.size !== null
                  ? vertexTypeStore.editedSelectedVertexType.style.size
                  : vertexTypeStore.selectedVertexType!.style.size
              }
              onChange={(value: string) => {
                vertexTypeStore.mutateEditedSelectedVertexType({
                  ...vertexTypeStore.editedSelectedVertexType,
                  style: {
                    color:
                      vertexTypeStore.editedSelectedVertexType.style.color !==
                      null
                        ? vertexTypeStore.editedSelectedVertexType.style.color.toLowerCase()
                        : vertexTypeStore.selectedVertexType!.style.color!.toLowerCase(),
                    icon: null,
                    display_fields:
                      vertexTypeStore.editedSelectedVertexType.style
                        .display_fields.length !== 0
                        ? vertexTypeStore.editedSelectedVertexType.style
                            .display_fields
                        : vertexTypeStore.selectedVertexType!.style
                            .display_fields,
                    size: value
                  }
                });
              }}
            >
              {vertexTypeStore.vertexSizeSchemas.map((value, index) => (
                <Select.Option
                  value={value.en}
                  key={value.en}
                  style={{ width: 66 }}
                >
                  <div
                    className="new-vertex-type-options-color"
                    style={{
                      marginTop: 4,
                      marginLeft: 5
                    }}
                  >
                    {value.ch}
                  </div>
                </Select.Option>
              ))}
            </Select>
          </div>
        </div>
        <div className={metadataDrawerOptionClass}>
          <div className="metadata-drawer-options-name">
            <span>{t('addition.common.id-strategy')}：</span>
          </div>
          {IDStrategyMappings[vertexTypeStore.selectedVertexType!.id_strategy]}
        </div>
        <div className="metadata-drawer-options">
          <div className="metadata-drawer-options-name">
            <span>{t('addition.common.association-property')}：</span>
          </div>
          <div className="metadata-drawer-options-list">
            <div className="metadata-drawer-options-list-row">
              <span>{t('addition.common.property')}</span>
              <span>{t('addition.common.allow-null')}</span>
            </div>
            {vertexTypeStore.selectedVertexType!.properties.map(
              ({ name, nullable }) => (
                <div className="metadata-drawer-options-list-row" key={name}>
                  <div style={{ maxWidth: 260 }}>{name}</div>
                  <div style={{ width: 70, textAlign: 'center' }}>
                    <Switch
                      checkedChildren={t('addition.operate.open')}
                      unCheckedChildren={t('addition.operate.close')}
                      checked={nullable}
                      size="large"
                      disabled
                    />
                  </div>
                </div>
              )
            )}
            {isEditVertex &&
              vertexTypeStore.editedSelectedVertexType.append_properties.map(
                ({ name }) => (
                  <div className="metadata-drawer-options-list-row" key={name}>
                    <div>{name}</div>
                    <div style={{ width: 70, textAlign: 'center' }}>
                      <Switch
                        checkedChildren={t('addition.operate.open')}
                        unCheckedChildren={t('addition.operate.close')}
                        checked={true}
                        size="large"
                        disabled
                      />
                    </div>
                  </div>
                )
              )}
            {isEditVertex && (
              <div
                className="metadata-drawer-options-list-row"
                style={{
                  color: '#2b65ff',
                  cursor: 'pointer',
                  justifyContent: 'normal',
                  alignItems: 'center'
                }}
                onClick={() => {
                  switchIsAddProperty(!isAddProperty);
                }}
              >
                <span style={{ marginRight: 4 }}>
                  {t('addition.common.add-property')}
                </span>
                <img src={BlueArrowIcon} alt="toogleAddProperties" />
              </div>
            )}
            {isEditVertex && isAddProperty && (
              <div
                className="metadata-configs-content-dropdown"
                ref={dropdownWrapperRef}
              >
                {metadataPropertyStore.metadataProperties
                  .filter(
                    (property) =>
                      vertexTypeStore.selectedVertexType!.properties.find(
                        ({ name }) => name === property.name
                      ) === undefined
                  )
                  .map((property) => (
                    <div key={property.name}>
                      <span>
                        <Checkbox
                          checked={
                            [
                              ...vertexTypeStore.addedPropertiesInSelectedVertextType
                            ].findIndex(
                              (propertyIndex) => propertyIndex === property.name
                            ) !== -1
                          }
                          onChange={() => {
                            const addedPropertiesInSelectedVertextType =
                              vertexTypeStore.addedPropertiesInSelectedVertextType;

                            addedPropertiesInSelectedVertextType.has(
                              property.name
                            )
                              ? addedPropertiesInSelectedVertextType.delete(
                                  property.name
                                )
                              : addedPropertiesInSelectedVertextType.add(
                                  property.name
                                );

                            vertexTypeStore.mutateEditedSelectedVertexType({
                              ...vertexTypeStore.editedSelectedVertexType,
                              append_properties: [
                                ...addedPropertiesInSelectedVertextType
                              ].map((propertyName) => {
                                const currentProperty = vertexTypeStore.newVertexType.properties.find(
                                  ({ name }) => name === propertyName
                                );

                                return {
                                  name: propertyName,
                                  nullable: !isUndefined(currentProperty)
                                    ? currentProperty.nullable
                                    : true
                                };
                              })
                            });
                          }}
                        >
                          {property.name}
                        </Checkbox>
                      </span>
                    </div>
                  ))}
              </div>
            )}
          </div>
        </div>
        <div className={metadataDrawerOptionClass}>
          <div className="metadata-drawer-options-name">
            <span>{t('addition.common.primary-key-property')}：</span>
          </div>
          <div style={{ maxWidth: 420 }}>
            {vertexTypeStore.selectedVertexType!.primary_keys.join(';')}
          </div>
        </div>
        <div className="metadata-drawer-options">
          <div className="metadata-drawer-options-name">
            <span
              className={
                isEditVertex ? 'metadata-drawer-options-name-edit' : ''
              }
            >
              {t('addition.vertex.vertex-display-content')}：
            </span>
          </div>
          {isEditVertex ? (
            <Select
              width={420}
              mode="multiple"
              size="medium"
              showSearch={false}
              disabled={!isEditVertex}
              placeholder={t(
                'addition.vertex.select-vertex-display-content-placeholder'
              )}
              onChange={(value: string[]) => {
                vertexTypeStore.mutateEditedSelectedVertexType({
                  ...vertexTypeStore.editedSelectedVertexType,
                  style: {
                    ...vertexTypeStore.editedSelectedVertexType.style,
                    display_fields: value.map((field) =>
                      formatVertexIdText(
                        field,
                        t('addition.function-parameter.vertex-id'),
                        true
                      )
                    )
                  }
                });

                vertexTypeStore.validateEditVertexType();
              }}
              value={vertexTypeStore.editedSelectedVertexType.style.display_fields.map(
                (field) =>
                  formatVertexIdText(
                    field,
                    t('addition.function-parameter.vertex-id')
                  )
              )}
            >
              {vertexTypeStore.selectedVertexType?.properties
                .concat({ name: '~id', nullable: false })
                .concat(
                  vertexTypeStore.editedSelectedVertexType.append_properties
                )
                .filter(({ nullable }) => !nullable)
                .map((item) => {
                  const order = vertexTypeStore.editedSelectedVertexType.style.display_fields.findIndex(
                    (name) => name === item.name
                  );

                  const multiSelectOptionClassName = classnames({
                    'metadata-configs-sorted-multiSelect-option': true,
                    'metadata-configs-sorted-multiSelect-option-selected':
                      order !== -1
                  });

                  return (
                    <Select.Option
                      value={formatVertexIdText(
                        item.name,
                        t('addition.function-parameter.vertex-id')
                      )}
                      key={item.name}
                    >
                      <div className={multiSelectOptionClassName}>
                        <div
                          style={{
                            backgroundColor: vertexTypeStore.editedSelectedVertexType.style.display_fields.includes(
                              item.name
                            )
                              ? '#2b65ff'
                              : '#fff',
                            borderColor: vertexTypeStore.editedSelectedVertexType.style.display_fields.includes(
                              item.name
                            )
                              ? '#fff'
                              : '#e0e0e0'
                          }}
                        >
                          {order !== -1 ? order + 1 : ''}
                        </div>
                        <div style={{ color: '#333' }}>
                          {formatVertexIdText(
                            item.name,
                            t('addition.function-parameter.vertex-id')
                          )}
                        </div>
                      </div>
                    </Select.Option>
                  );
                })}
            </Select>
          ) : (
            <div style={{ maxWidth: 420 }}>
              {vertexTypeStore.selectedVertexType?.style.display_fields
                .map((field) =>
                  formatVertexIdText(
                    field,
                    t('addition.function-parameter.vertex-id')
                  )
                )
                .join('-')}
            </div>
          )}
        </div>
        <div
          className="metadata-title"
          style={{
            marginTop: 40,
            marginBottom: 16,
            width: 88,
            textAlign: 'right'
          }}
        >
          {t('addition.edge.index-info')}
        </div>
        <div className={metadataDrawerOptionClass}>
          <div className="metadata-drawer-options-name">
            <span>{t('addition.menu.type-index')}：</span>
          </div>
          <Switch
            checkedChildren={t('addition.operate.open')}
            unCheckedChildren={t('addition.operate.close')}
            checked={vertexTypeStore.selectedVertexType!.open_label_index}
            size="large"
            disabled
          />
        </div>
        <div className="metadata-drawer-options">
          <div className="metadata-drawer-options-name">
            <span>{t('addition.common.property-index')}：</span>
          </div>
          <div className="metadata-drawer-options-list">
            {(vertexTypeStore.selectedVertexType!.property_indexes.length !==
              0 ||
              vertexTypeStore.editedSelectedVertexType.append_property_indexes
                .length !== 0) && (
              <div className="metadata-drawer-options-list-row metadata-drawer-options-list-row-normal">
                <span>{t('addition.edge.index-name')}</span>
                <span>{t('addition.edge.index-type')}</span>
                <span>{t('addition.common.property')}</span>
              </div>
            )}
            {vertexTypeStore
              .selectedVertexType!.property_indexes.filter((propertyIndex) =>
                isUndefined(
                  vertexTypeStore.editedSelectedVertexType.remove_property_indexes.find(
                    (removedPropertyName) =>
                      removedPropertyName === propertyIndex.name
                  )
                )
              )
              .map(({ name, type, fields }, index) => {
                return (
                  <div
                    className="metadata-drawer-options-list-row metadata-drawer-options-list-row-normal"
                    key={name}
                  >
                    <div>{name}</div>
                    <div>{propertyIndexTypeMappings[type]}</div>
                    <div style={{ display: 'flex', alignItems: 'center' }}>
                      <span
                        style={{
                          marginRight: 3,
                          textOverflow: 'ellipsis',
                          whiteSpace: 'nowrap',
                          overflow: 'hidden'
                        }}
                      >
                        {fields
                          .map((field, index) => index + 1 + '.' + field)
                          .join(';')}
                      </span>
                      {isEditVertex && (
                        <Tooltip
                          placement="bottom-end"
                          tooltipShown={index === deleteExistPopIndexInDrawer}
                          modifiers={{
                            offset: {
                              offset: '0, 10'
                            }
                          }}
                          tooltipWrapperProps={{
                            className: 'metadata-properties-tooltips',
                            style: { zIndex: 1041 }
                          }}
                          tooltipWrapper={
                            <div ref={deleteWrapperInDrawerRef}>
                              <p
                                style={{
                                  width: 200,
                                  lineHeight: '28px'
                                }}
                              >
                                {t('addition.message.property-del-confirm')}
                              </p>
                              <p
                                style={{
                                  width: 200,
                                  lineHeight: '28px'
                                }}
                              >
                                {t('addition.message.index-del-confirm')}
                              </p>
                              <div
                                style={{
                                  display: 'flex',
                                  marginTop: 12,
                                  color: '#2b65ff',
                                  cursor: 'pointer'
                                }}
                              >
                                <div
                                  style={{
                                    marginRight: 16,
                                    cursor: 'pointer'
                                  }}
                                  onClick={() => {
                                    const removedPropertyIndexes = cloneDeep(
                                      vertexTypeStore.editedSelectedVertexType
                                        .remove_property_indexes
                                    );

                                    removedPropertyIndexes.push(
                                      vertexTypeStore.selectedVertexType!
                                        .property_indexes[index].name
                                    );

                                    vertexTypeStore.mutateEditedSelectedVertexType(
                                      {
                                        ...vertexTypeStore.editedSelectedVertexType,
                                        remove_property_indexes: removedPropertyIndexes
                                      }
                                    );

                                    setDeleteExistPopIndexInDrawer(null);
                                    vertexTypeStore.validateEditVertexType(
                                      true
                                    );
                                  }}
                                >
                                  {t('addition.common.confirm')}
                                </div>
                                <div
                                  onClick={() => {
                                    setDeleteExistPopIndexInDrawer(null);
                                  }}
                                >
                                  {t('addition.common.cancel')}
                                </div>
                              </div>
                            </div>
                          }
                          childrenProps={{
                            src: CloseIcon,
                            alt: 'close',
                            style: { cursor: 'pointer' },
                            onClick() {
                              setDeleteExistPopIndexInDrawer(index);
                            }
                          }}
                        />
                      )}
                    </div>
                  </div>
                );
              })}
            {vertexTypeStore.editedSelectedVertexType.append_property_indexes.map(
              ({ name, type, fields }, index) => {
                return (
                  <div
                    className="metadata-drawer-options-list-row metadata-drawer-options-list-row-normal"
                    style={{
                      display: 'flex',
                      alignItems: 'start',
                      position: 'relative'
                    }}
                  >
                    <div>
                      <Input
                        size="medium"
                        width={100}
                        placeholder={t('addition.edge.index-name')}
                        errorLocation="layer"
                        errorMessage={
                          vertexTypeStore.validateEditVertexTypeErrorMessage
                            .propertyIndexes.length !== 0
                            ? (vertexTypeStore
                                .validateEditVertexTypeErrorMessage
                                .propertyIndexes[
                                index
                              ] as VertexTypeValidatePropertyIndexes).name
                            : ''
                        }
                        value={name}
                        onChange={(e: any) => {
                          const propertyIndexEntities = cloneDeep(
                            vertexTypeStore.editedSelectedVertexType
                              .append_property_indexes
                          );

                          propertyIndexEntities[index].name = e.value;

                          vertexTypeStore.mutateEditedSelectedVertexType({
                            ...vertexTypeStore.editedSelectedVertexType,
                            append_property_indexes: propertyIndexEntities
                          });
                        }}
                        originInputProps={{
                          onBlur() {
                            // check is ready to create
                            vertexTypeStore.validateEditVertexType();
                          }
                        }}
                      />
                    </div>
                    <div>
                      <Select
                        width={110}
                        placeholder={t('addition.edge.index-type-select-desc')}
                        size="medium"
                        showSearch={false}
                        value={type === '' ? [] : type}
                        onChange={(value: string) => {
                          const propertyIndexEntities = cloneDeep(
                            vertexTypeStore.editedSelectedVertexType
                              .append_property_indexes
                          );

                          propertyIndexEntities[index].type = value;

                          vertexTypeStore.mutateEditedSelectedVertexType({
                            ...vertexTypeStore.editedSelectedVertexType,
                            append_property_indexes: propertyIndexEntities
                          });

                          vertexTypeStore.validateEditVertexType();
                        }}
                      >
                        <Select.Option value="SECONDARY" key="SECONDARY">
                          {t('addition.menu.secondary-index')}
                        </Select.Option>
                        <Select.Option value="RANGE" key="RANGE">
                          {t('addition.range.secondary-index')}
                        </Select.Option>
                        <Select.Option value="SEARCH" key="SEARCH">
                          {t('addition.range.full-text-index')}
                        </Select.Option>
                      </Select>
                    </div>
                    <div>
                      <Select
                        width={120}
                        mode={type === 'SECONDARY' ? 'multiple' : 'default'}
                        placeholder={t('addition.edge.property-select-desc')}
                        size="medium"
                        showSearch={false}
                        value={fields}
                        onChange={(value: string | string[]) => {
                          const propertyIndexEntities = cloneDeep(
                            vertexTypeStore.editedSelectedVertexType
                              .append_property_indexes
                          );

                          if (Array.isArray(value)) {
                            propertyIndexEntities[index].fields = value;
                          } else {
                            propertyIndexEntities[index].fields = [value];
                          }

                          vertexTypeStore.mutateEditedSelectedVertexType({
                            ...vertexTypeStore.editedSelectedVertexType,
                            append_property_indexes: propertyIndexEntities
                          });

                          vertexTypeStore.validateEditVertexType();
                        }}
                      >
                        {type === 'SECONDARY' &&
                          vertexTypeStore
                            .selectedVertexType!.properties.concat(
                              vertexTypeStore.editedSelectedVertexType
                                .append_properties
                            )
                            .filter(
                              (property) =>
                                !vertexTypeStore.selectedVertexType!.primary_keys.includes(
                                  property.name
                                )
                            )
                            .map((property) => {
                              const order = vertexTypeStore.editedSelectedVertexType.append_property_indexes[
                                index
                              ].fields.findIndex(
                                (name) => name === property.name
                              );

                              const multiSelectOptionClassName = classnames({
                                'metadata-configs-sorted-multiSelect-option': true,
                                'metadata-configs-sorted-multiSelect-option-selected':
                                  order !== -1
                              });

                              return (
                                <Select.Option
                                  value={property.name}
                                  key={property.name}
                                >
                                  <div className={multiSelectOptionClassName}>
                                    <div>{order !== -1 ? order + 1 : ''}</div>
                                    <div>{property.name}</div>
                                  </div>
                                </Select.Option>
                              );
                            })}

                        {type === 'RANGE' &&
                          vertexTypeStore
                            .selectedVertexType!.properties.concat(
                              vertexTypeStore.editedSelectedVertexType
                                .append_properties
                            )
                            .filter((property) => {
                              const matchedProperty = metadataPropertyStore.metadataProperties.find(
                                ({ name }) => name === property.name
                              );

                              if (!isUndefined(matchedProperty)) {
                                const { data_type } = matchedProperty;

                                return (
                                  data_type !== 'TEXT' &&
                                  data_type !== 'BOOLEAN' &&
                                  data_type !== 'UUID' &&
                                  data_type !== 'BLOB'
                                );
                              }
                            })
                            .map(({ name }) => (
                              <Select.Option value={name} key={name}>
                                {name}
                              </Select.Option>
                            ))}

                        {type === 'SEARCH' &&
                          vertexTypeStore
                            .selectedVertexType!.properties.concat(
                              vertexTypeStore.editedSelectedVertexType
                                .append_properties
                            )
                            .filter((property) => {
                              const matchedProperty = metadataPropertyStore.metadataProperties.find(
                                ({ name }) => name === property.name
                              );

                              if (!isUndefined(matchedProperty)) {
                                const { data_type } = matchedProperty;

                                return data_type === 'TEXT';
                              }
                            })
                            .map(({ name }) => (
                              <Select.Option value={name} key={name}>
                                {name}
                              </Select.Option>
                            ))}
                      </Select>

                      <Tooltip
                        placement="bottom-end"
                        tooltipShown={index === deleteAddedPopIndexInDrawer}
                        modifiers={{
                          offset: {
                            offset: '0, 10'
                          }
                        }}
                        tooltipWrapperProps={{
                          className: 'metadata-properties-tooltips',
                          style: { zIndex: 1041 }
                        }}
                        tooltipWrapper={
                          <div ref={deleteWrapperInDrawerRef}>
                            <p
                              style={{
                                width: 200,
                                lineHeight: '28px'
                              }}
                            >
                              {t('addition.message.property-del-confirm')}
                            </p>
                            <p
                              style={{
                                width: 200,
                                lineHeight: '28px'
                              }}
                            >
                              {t('addition.message.index-del-confirm')}
                            </p>
                            <div
                              style={{
                                display: 'flex',
                                marginTop: 12,
                                color: '#2b65ff',
                                cursor: 'pointer'
                              }}
                            >
                              <div
                                style={{
                                  marginRight: 16,
                                  cursor: 'pointer'
                                }}
                                onClick={() => {
                                  const appendPropertyIndexes = cloneDeep(
                                    vertexTypeStore.editedSelectedVertexType!
                                      .append_property_indexes
                                  );

                                  appendPropertyIndexes.splice(index, 1);

                                  vertexTypeStore.mutateEditedSelectedVertexType(
                                    {
                                      ...vertexTypeStore.editedSelectedVertexType,
                                      append_property_indexes: appendPropertyIndexes
                                    }
                                  );

                                  setDeleteAddedPopIndexInDrawer(null);
                                  vertexTypeStore.validateEditVertexType(true);
                                }}
                              >
                                {t('addition.common.confirm')}
                              </div>
                              <div
                                onClick={() => {
                                  vertexTypeStore.resetEditedSelectedVertexType();
                                  setDeleteAddedPopIndexInDrawer(null);
                                }}
                              >
                                {t('addition.common.cancel')}
                              </div>
                            </div>
                          </div>
                        }
                        childrenProps={{
                          src: CloseIcon,
                          alt: 'close',
                          style: { cursor: 'pointer' },
                          onClick() {
                            setDeleteAddedPopIndexInDrawer(index);
                          }
                        }}
                        childrenWrapperElement="img"
                      />
                    </div>
                  </div>
                );
              }
            )}
            {isEditVertex && (
              <div
                onClick={() => {
                  if (
                    vertexTypeStore.editedSelectedVertexType
                      .append_property_indexes.length === 0 ||
                    vertexTypeStore.isEditReady
                  ) {
                    vertexTypeStore.mutateEditedSelectedVertexType({
                      ...vertexTypeStore.editedSelectedVertexType,
                      append_property_indexes: [
                        ...vertexTypeStore.editedSelectedVertexType
                          .append_property_indexes,
                        {
                          name: '',
                          type: '',
                          fields: []
                        }
                      ]
                    });

                    vertexTypeStore.validateEditVertexType(true);
                  }
                }}
                style={{
                  cursor: 'pointer',
                  color: vertexTypeStore.isEditReady ? '#2b65ff' : '#999'
                }}
              >
                {t('addition.edge.add-group')}
              </div>
            )}
          </div>
        </div>
      </div>
    </Drawer>
  );
});

export default CheckAndEditVertex;
