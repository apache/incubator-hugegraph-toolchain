import React, {
  useContext,
  useState,
  useEffect,
  useRef,
  useCallback
} from 'react';
import { observer } from 'mobx-react';
import classnames from 'classnames';
import { cloneDeep, merge, isUndefined, isEmpty } from 'lodash-es';
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
  generateGraphModeId,
  formatVertexIdText,
  edgeWidthMapping
} from '../../../../stores/utils';

import type { EdgeTypeValidatePropertyIndexes } from '../../../../stores/types/GraphManagementStore/metadataConfigsStore';

import BlueArrowIcon from '../../../../assets/imgs/ic_arrow_blue.svg';
import CloseIcon from '../../../../assets/imgs/ic_close_16.svg';
import SelectedSoilidArrowIcon from '../../../../assets/imgs/ic_arrow_selected.svg';
import NoSelectedSoilidArrowIcon from '../../../../assets/imgs/ic_arrow.svg';
import SelectedSoilidStraightIcon from '../../../../assets/imgs/ic_straight_selected.svg';
import NoSelectedSoilidStraightIcon from '../../../../assets/imgs/ic_straight.svg';
import i18next from '../../../../i18n';
import { useTranslation } from 'react-i18next';

const propertyIndexTypeMappings: Record<string, string> = {
  SECONDARY: i18next.t('addition.menu.secondary-index'),
  RANGE: i18next.t('addition.menu.range-index'),
  SEARCH: i18next.t('addition.menu.full-text-index')
};

const CheckAndEditEdge: React.FC = observer(() => {
  const { metadataPropertyStore, edgeTypeStore, graphViewStore } = useContext(
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

  const isEditEdge = graphViewStore.currentDrawer === 'edit-edge';

  const metadataDrawerOptionClass = classnames({
    'metadata-drawer-options': true,
    'metadata-drawer-options-disabled': isEditEdge
  });

  // need useCallback to stop infinite callings of useEffect
  const handleOutSideClick = useCallback(
    (e: MouseEvent) => {
      const drawerWrapper = document.querySelector(
        '.new-fc-one-drawer-content-wrapper'
      );

      if (
        graphViewStore.currentDrawer === 'check-edge' &&
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
        isEditEdge &&
        isAddProperty &&
        dropdownWrapperRef.current &&
        !dropdownWrapperRef.current.contains(e.target as Element)
      ) {
        switchIsAddProperty(false);
        return;
      }

      if (
        isDeletePop !== null &&
        deleteWrapperRef.current &&
        !deleteWrapperRef.current.contains(e.target as Element)
      ) {
        switchDeletePop(false);
      }
    },
    [graphViewStore, isEditEdge, isAddProperty, isDeletePop]
  );

  const handleCloseDrawer = () => {
    switchIsAddProperty(false);
    graphViewStore.setCurrentDrawer('');
    edgeTypeStore.selectEdgeType(null);
    // clear mutations in <Drawer />
    edgeTypeStore.resetEditedSelectedEdgeType();
  };

  const handleDeleteEdge = async () => {
    // cache vertex name here before it gets removed
    const edgeName = edgeTypeStore.selectedEdgeType!.name;
    const edgeId = generateGraphModeId(
      edgeName,
      edgeTypeStore.selectedEdgeType!.source_label,
      edgeTypeStore.selectedEdgeType!.target_label
    );
    const edgeInfo = graphViewStore.visDataSet!.edges.get(edgeName);

    switchDeletePop(false);
    handleCloseDrawer();

    graphViewStore.visDataSet!.edges.remove(edgeId);

    await edgeTypeStore.deleteEdgeType([edgeName]);

    if (edgeTypeStore.requestStatus.deleteEdgeType === 'success') {
      Message.success({
        content: t('addition.common.del-success'),
        size: 'medium',
        showCloseIcon: false
      });

      edgeTypeStore.fetchEdgeTypeList({ fetchAll: true });
    }

    if (edgeTypeStore.requestStatus.deleteEdgeType === 'failed') {
      Message.error({
        content: edgeTypeStore.errorMessage,
        size: 'medium',
        showCloseIcon: false
      });

      // if failed, re-add edge
      graphViewStore.visDataSet?.edges.add(edgeInfo);
    }
  };

  useEffect(() => {
    document.addEventListener('click', handleOutSideClick, false);

    return () => {
      document.removeEventListener('click', handleOutSideClick, false);
    };
  }, [handleOutSideClick]);

  if (edgeTypeStore.selectedEdgeType === null) {
    return null;
  }

  return (
    <Drawer
      title={
        !isEditEdge
          ? t('addition.common.edge-type-detail')
          : t('addition.common.modify-edge-type')
      }
      width={580}
      destroyOnClose
      visible={['check-edge', 'edit-edge'].includes(
        graphViewStore.currentDrawer
      )}
      mask={isEditEdge}
      onClose={handleCloseDrawer}
      footer={[
        <Button
          type="primary"
          size="medium"
          style={{ width: 60 }}
          disabled={
            isEditEdge &&
            (edgeTypeStore.editedSelectedEdgeType.style.display_fields
              .length === 0 ||
              !edgeTypeStore.isEditReady)
          }
          onClick={async () => {
            if (!isEditEdge) {
              graphViewStore.setCurrentDrawer('edit-edge');
              edgeTypeStore.validateEditEdgeType();
            } else {
              const id = generateGraphModeId(
                edgeTypeStore.selectedEdgeType!.name,
                edgeTypeStore.selectedEdgeType!.source_label,
                edgeTypeStore.selectedEdgeType!.target_label
              );
              const updateInfo: Record<string, any> = {};

              if (
                !isEmpty(edgeTypeStore.editedSelectedEdgeType.append_properties)
              ) {
                const mappedProperties = mapMetadataProperties(
                  edgeTypeStore.selectedEdgeType!.properties,
                  metadataPropertyStore.metadataProperties
                );

                const newMappedProperties = mapMetadataProperties(
                  edgeTypeStore.editedSelectedEdgeType.append_properties,
                  metadataPropertyStore.metadataProperties
                );

                const mergedProperties = merge(
                  mappedProperties,
                  newMappedProperties
                );

                updateInfo.title = `
                  <div class="metadata-graph-view-tooltip-fields">
                    <div>${t('addition.common.edge-type')}：</div>
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

              if (edgeTypeStore.editedSelectedEdgeType.style.color !== null) {
                updateInfo.color = {
                  color: edgeTypeStore.editedSelectedEdgeType.style.color,
                  highlight: edgeTypeStore.editedSelectedEdgeType.style.color,
                  hover: edgeTypeStore.editedSelectedEdgeType.style.color
                };
              }

              if (
                edgeTypeStore.editedSelectedEdgeType.style.with_arrow !== null
              ) {
                updateInfo.arrows =
                  edgeTypeStore.editedSelectedEdgeType.style.with_arrow === true
                    ? 'to'
                    : '';
              }

              if (
                edgeTypeStore.editedSelectedEdgeType.style.thickness !== null
              ) {
                updateInfo.value =
                  edgeWidthMapping[
                    edgeTypeStore.editedSelectedEdgeType.style.thickness
                  ];
              }

              if (!isEmpty(updateInfo)) {
                updateInfo.id = id;

                graphViewStore.visDataSet!.edges.update(updateInfo);
              }
              await edgeTypeStore.updateEdgeType();

              if (edgeTypeStore.requestStatus.updateEdgeType === 'failed') {
                Message.error({
                  content: edgeTypeStore.errorMessage,
                  size: 'medium',
                  showCloseIcon: false
                });

                return;
              }

              if (edgeTypeStore.requestStatus.updateEdgeType === 'success') {
                Message.success({
                  content: t('addition.operate.modify-success'),
                  size: 'medium',
                  showCloseIcon: false
                });
              }

              handleCloseDrawer();
              graphViewStore.visNetwork!.unselectAll();
              edgeTypeStore.fetchEdgeTypeList({ fetchAll: true });
            }
          }}
          key="drawer-manipulation"
        >
          {isEditEdge ? t('addition.common.save') : t('addition.common.edit')}
        </Button>,
        <Tooltip
          placement="top-start"
          tooltipShown={isDeletePop}
          modifiers={{
            offset: {
              offset: '0, 10'
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
              <p style={{ marginBottom: 8 }}>
                {t('addition.edge.confirm-del-edge-type')}
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
                  onClick={handleDeleteEdge}
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
            </div>
          }
          childrenProps={{
            onClick() {
              if (isEditEdge) {
                handleCloseDrawer();
                return;
              }

              switchDeletePop(true);
            }
          }}
        >
          <Button size="medium" style={{ width: 60 }}>
            {isEditEdge ? t('addition.common.close') : t('addition.common.del')}
          </Button>
        </Tooltip>
      ]}
    >
      <div>
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
              <span>{t('addition.common.edge-type-name')}：</span>
            </div>
            <div style={{ maxWidth: 420 }}>
              {edgeTypeStore.selectedEdgeType!.name}
            </div>
          </div>

          <div className="metadata-drawer-options">
            <div className="metadata-drawer-options-name">
              <span
                className={
                  isEditEdge ? 'metadata-drawer-options-name-edit' : ''
                }
              >
                {t('addition.common.edge-style')}：
              </span>
            </div>
            <div className="new-vertex-type-options-colors">
              <Select
                width={66}
                size="medium"
                prefixCls="new-fc-one-select-another"
                dropdownMatchSelectWidth={false}
                showSearch={false}
                disabled={!isEditEdge}
                value={
                  <div
                    className="new-vertex-type-select"
                    style={{
                      background:
                        edgeTypeStore.editedSelectedEdgeType.style.color !==
                        null
                          ? edgeTypeStore.editedSelectedEdgeType.style.color.toLowerCase()
                          : edgeTypeStore.selectedEdgeType!.style.color!.toLowerCase(),
                      marginTop: 5
                    }}
                  ></div>
                }
                onChange={(value: string) => {
                  edgeTypeStore.mutateEditedSelectedEdgeType({
                    ...edgeTypeStore.editedSelectedEdgeType,
                    style: {
                      color: value,
                      icon: null,
                      with_arrow:
                        edgeTypeStore.editedSelectedEdgeType.style
                          .with_arrow !== null
                          ? edgeTypeStore.editedSelectedEdgeType.style
                              .with_arrow
                          : edgeTypeStore.selectedEdgeType!.style.with_arrow,
                      thickness:
                        edgeTypeStore.editedSelectedEdgeType.style.thickness !==
                        null
                          ? edgeTypeStore.editedSelectedEdgeType.style.thickness
                          : edgeTypeStore.selectedEdgeType!.style.thickness,
                      display_fields:
                        edgeTypeStore.editedSelectedEdgeType.style
                          .display_fields.length !== 0
                          ? edgeTypeStore.editedSelectedEdgeType.style
                              .display_fields
                          : edgeTypeStore.editedSelectedEdgeType!.style
                              .display_fields
                    }
                  });
                }}
              >
                {edgeTypeStore.colorSchemas.map(
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
                          (edgeTypeStore.editedSelectedEdgeType.style.color !==
                          null
                            ? edgeTypeStore.editedSelectedEdgeType.style.color.toLowerCase()
                            : edgeTypeStore.selectedEdgeType!.style.color!.toLowerCase()) ===
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
                width={66}
                size="medium"
                showSearch={false}
                disabled={!isEditEdge}
                value={
                  (
                    edgeTypeStore.editedSelectedEdgeType.style.with_arrow !==
                    null
                      ? edgeTypeStore.editedSelectedEdgeType.style.with_arrow
                      : edgeTypeStore.selectedEdgeType!.style.with_arrow
                  ) ? (
                    <div>
                      <img src={NoSelectedSoilidArrowIcon} />
                    </div>
                  ) : (
                    <div>
                      <img src={NoSelectedSoilidStraightIcon} />
                    </div>
                  )
                }
                onChange={(e: any) => {
                  edgeTypeStore.mutateEditedSelectedEdgeType({
                    ...edgeTypeStore.editedSelectedEdgeType,
                    style: {
                      with_arrow: e[0] && e[1] === 'solid',
                      color:
                        edgeTypeStore.editedSelectedEdgeType.style.color !==
                        null
                          ? edgeTypeStore.editedSelectedEdgeType.style.color.toLowerCase()
                          : edgeTypeStore.selectedEdgeType!.style.color!.toLowerCase(),
                      icon: null,
                      thickness:
                        edgeTypeStore.editedSelectedEdgeType.style.thickness !==
                        null
                          ? edgeTypeStore.editedSelectedEdgeType.style.thickness
                          : edgeTypeStore.selectedEdgeType!.style.thickness,
                      display_fields:
                        edgeTypeStore.editedSelectedEdgeType.style
                          .display_fields.length !== 0
                          ? edgeTypeStore.editedSelectedEdgeType.style
                              .display_fields
                          : edgeTypeStore.selectedEdgeType!.style.display_fields
                    }
                  });
                }}
              >
                {edgeTypeStore.edgeShapeSchemas.map((item, index) => (
                  <Select.Option
                    value={[item.flag, item.shape]}
                    key={item.flag}
                    style={{ width: 66 }}
                  >
                    <div
                      className="new-vertex-type-options-color"
                      style={{ marginTop: 5, marginLeft: 5 }}
                    >
                      <img
                        src={
                          edgeTypeStore.editedSelectedEdgeType.style
                            .with_arrow === null
                            ? item.flag ===
                              edgeTypeStore.selectedEdgeType!.style.with_arrow
                              ? item.blueicon
                              : item.blackicon
                            : edgeTypeStore.editedSelectedEdgeType.style
                                .with_arrow === item.flag
                            ? item.blueicon
                            : item.blackicon
                        }
                        alt="toogleEdgeArrow"
                      />
                    </div>
                  </Select.Option>
                ))}
              </Select>
            </div>
            <div className="new-vertex-type-options-colors">
              <Select
                width={66}
                size="medium"
                showSearch={false}
                disabled={!isEditEdge}
                style={{ paddingLeft: 7 }}
                value={
                  edgeTypeStore.editedSelectedEdgeType.style.thickness !== null
                    ? edgeTypeStore.editedSelectedEdgeType.style.thickness
                    : edgeTypeStore.selectedEdgeType!.style.thickness
                }
                onChange={(value: string) => {
                  edgeTypeStore.mutateEditedSelectedEdgeType({
                    ...edgeTypeStore.editedSelectedEdgeType,
                    style: {
                      with_arrow:
                        edgeTypeStore.editedSelectedEdgeType.style
                          .with_arrow !== null
                          ? edgeTypeStore.editedSelectedEdgeType.style
                              .with_arrow
                          : edgeTypeStore.selectedEdgeType!.style.with_arrow,
                      color:
                        edgeTypeStore.editedSelectedEdgeType.style.color !==
                        null
                          ? edgeTypeStore.editedSelectedEdgeType.style.color.toLowerCase()
                          : edgeTypeStore.selectedEdgeType!.style.color!.toLowerCase(),
                      icon: null,
                      thickness: value,
                      display_fields:
                        edgeTypeStore.editedSelectedEdgeType.style
                          .display_fields.length !== 0
                          ? edgeTypeStore.editedSelectedEdgeType.style
                              .display_fields
                          : edgeTypeStore.selectedEdgeType!.style.display_fields
                    }
                  });
                }}
              >
                {edgeTypeStore.thicknessSchemas.map((value, index) => (
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
              <span>{t('addition.common.source-type')}：</span>
            </div>
            <div style={{ maxWidth: 420 }}>
              {edgeTypeStore.selectedEdgeType!.source_label}
            </div>
          </div>
          <div className={metadataDrawerOptionClass}>
            <div className="metadata-drawer-options-name">
              <span>{t('addition.common.target-type')}：</span>
            </div>
            <div style={{ maxWidth: 420 }}>
              {edgeTypeStore.selectedEdgeType!.target_label}
            </div>
          </div>
          <div className={metadataDrawerOptionClass}>
            <div className="metadata-drawer-options-name">
              <span>{t('addition.common.allow-multiple-connections')}：</span>
            </div>
            <Switch
              checkedChildren={t('addition.operate.open')}
              unCheckedChildren={t('addition.operate.close')}
              checked={edgeTypeStore.selectedEdgeType!.link_multi_times}
              size="large"
              disabled
            />
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
              {edgeTypeStore.selectedEdgeType!.properties.map(
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
              {isEditEdge &&
                edgeTypeStore.editedSelectedEdgeType.append_properties.map(
                  ({ name }) => (
                    <div
                      className="metadata-drawer-options-list-row"
                      key={name}
                    >
                      <div>{name}</div>
                      <div style={{ width: 70, textAlign: 'center' }}>
                        <Switch
                          checkedChildren={t('addition.operate.open')}
                          unCheckedChildren={t('addition.operate.open')}
                          checked={true}
                          size="large"
                          disabled
                        />
                      </div>
                    </div>
                  )
                )}
              {isEditEdge && (
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
              {isEditEdge && isAddProperty && (
                <div
                  className="metadata-configs-content-dropdown"
                  ref={dropdownWrapperRef}
                >
                  {metadataPropertyStore.metadataProperties
                    .filter(
                      (property) =>
                        edgeTypeStore.selectedEdgeType!.properties.find(
                          ({ name }) => name === property.name
                        ) === undefined
                    )
                    .map((property) => (
                      <div key={property.name}>
                        <span>
                          <Checkbox
                            checked={
                              [
                                ...edgeTypeStore.addedPropertiesInSelectedEdgeType
                              ].findIndex(
                                (propertyIndex) =>
                                  propertyIndex === property.name
                              ) !== -1
                            }
                            onChange={() => {
                              const addedPropertiesInSelectedVertextType =
                                edgeTypeStore.addedPropertiesInSelectedEdgeType;

                              addedPropertiesInSelectedVertextType.has(
                                property.name
                              )
                                ? addedPropertiesInSelectedVertextType.delete(
                                    property.name
                                  )
                                : addedPropertiesInSelectedVertextType.add(
                                    property.name
                                  );

                              edgeTypeStore.mutateEditedSelectedEdgeType({
                                ...edgeTypeStore.editedSelectedEdgeType,
                                append_properties: [
                                  ...addedPropertiesInSelectedVertextType
                                ].map((propertyName) => {
                                  const currentProperty = edgeTypeStore.newEdgeType.properties.find(
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
              <span>{t('addition.common.distinguishing-key-property')}：</span>
            </div>
            <div style={{ maxWidth: 420 }}>
              {edgeTypeStore.selectedEdgeType!.sort_keys.join(';')}
            </div>
          </div>

          <div className="metadata-drawer-options">
            <div className="metadata-drawer-options-name">
              <span
                className={
                  isEditEdge ? 'metadata-drawer-options-name-edit' : ''
                }
              >
                {t('addition.edge.display-content')}：
              </span>
            </div>
            {isEditEdge ? (
              <Select
                width={420}
                mode="multiple"
                size="medium"
                showSearch={false}
                disabled={!isEditEdge}
                placeholder={t('addition.edge.display-content-select-desc')}
                onChange={(value: string[]) => {
                  edgeTypeStore.mutateEditedSelectedEdgeType({
                    ...edgeTypeStore.editedSelectedEdgeType,
                    style: {
                      ...edgeTypeStore.editedSelectedEdgeType.style,
                      display_fields: value.map((field) =>
                        formatVertexIdText(
                          field,
                          t('addition.function-parameter.edge-type'),
                          true
                        )
                      )
                    }
                  });
                }}
                value={edgeTypeStore.editedSelectedEdgeType.style.display_fields.map(
                  (field) =>
                    formatVertexIdText(
                      field,
                      t('addition.function-parameter.edge-type')
                    )
                )}
              >
                {edgeTypeStore.selectedEdgeType?.properties
                  .concat({ name: '~id', nullable: false })
                  .concat(
                    edgeTypeStore.editedSelectedEdgeType.append_properties
                  )
                  .filter(({ nullable }) => !nullable)
                  .map((item) => {
                    const order = edgeTypeStore.editedSelectedEdgeType.style.display_fields.findIndex(
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
                          t('addition.function-parameter.edge-type')
                        )}
                        key={item.name}
                      >
                        <div className={multiSelectOptionClassName}>
                          <div
                            style={{
                              backgroundColor: edgeTypeStore.editedSelectedEdgeType.style.display_fields.includes(
                                item.name
                              )
                                ? '#2b65ff'
                                : '#fff',
                              borderColor: edgeTypeStore.editedSelectedEdgeType.style.display_fields.includes(
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
                              t('addition.function-parameter.edge-type')
                            )}
                          </div>
                        </div>
                      </Select.Option>
                    );
                  })}
              </Select>
            ) : (
              <div style={{ maxWidth: 420 }}>
                {edgeTypeStore.selectedEdgeType?.style.display_fields
                  .map((field) =>
                    formatVertexIdText(
                      field,
                      t('addition.function-parameter.edge-type')
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
              checked={edgeTypeStore.selectedEdgeType!.open_label_index}
              size="large"
              disabled
            />
          </div>
          <div className="metadata-drawer-options">
            <div className="metadata-drawer-options-name">
              <span>{t('addition.common.property-index')}：</span>
            </div>
            <div className="metadata-drawer-options-list">
              {(edgeTypeStore.selectedEdgeType!.property_indexes.length !== 0 ||
                edgeTypeStore.editedSelectedEdgeType.append_property_indexes
                  .length !== 0) && (
                <div className="metadata-drawer-options-list-row metadata-drawer-options-list-row-normal">
                  <span>{t('addition.edge.index-name')}</span>
                  <span>{t('addition.edge.index-type')}</span>
                  <span>{t('addition.common.property')}</span>
                </div>
              )}
              {edgeTypeStore
                .selectedEdgeType!.property_indexes.filter((propertyIndex) =>
                  isUndefined(
                    edgeTypeStore.editedSelectedEdgeType.remove_property_indexes.find(
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
                      <div
                        style={{
                          display: 'flex',
                          alignItems: 'center'
                        }}
                      >
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

                        {isEditEdge && (
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
                                      const removedPropertyIndex = cloneDeep(
                                        edgeTypeStore.editedSelectedEdgeType
                                          .remove_property_indexes
                                      );

                                      removedPropertyIndex.push(
                                        edgeTypeStore.selectedEdgeType!
                                          .property_indexes[index].name
                                      );

                                      edgeTypeStore.mutateEditedSelectedEdgeType(
                                        {
                                          ...edgeTypeStore.editedSelectedEdgeType,
                                          remove_property_indexes: removedPropertyIndex
                                        }
                                      );

                                      setDeleteExistPopIndexInDrawer(null);
                                      edgeTypeStore.validateEditEdgeType(true);
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
                            childrenWrapperElement="img"
                          />
                        )}
                      </div>
                    </div>
                  );
                })}
              {edgeTypeStore.editedSelectedEdgeType.append_property_indexes.map(
                ({ name, type, fields }, index) => {
                  return (
                    <div
                      className="metadata-drawer-options-list-row metadata-drawer-options-list-row-normal"
                      style={{
                        display: 'flex',
                        alignItems: 'start',
                        position: 'relative'
                      }}
                      // cannot set key prop with name here, weired
                    >
                      <div>
                        <Input
                          size="medium"
                          width={100}
                          placeholder={t('addition.edge.index-name')}
                          errorLocation="layer"
                          errorMessage={
                            edgeTypeStore.validateEditEdgeTypeErrorMessage
                              .propertyIndexes.length !== 0
                              ? (edgeTypeStore.validateEditEdgeTypeErrorMessage
                                  .propertyIndexes[
                                  index
                                ] as EdgeTypeValidatePropertyIndexes).name
                              : ''
                          }
                          value={name}
                          onChange={(e: any) => {
                            const propertyIndexEntities = cloneDeep(
                              edgeTypeStore.editedSelectedEdgeType
                                .append_property_indexes
                            );

                            propertyIndexEntities[index].name = e.value;

                            edgeTypeStore.mutateEditedSelectedEdgeType({
                              ...edgeTypeStore.editedSelectedEdgeType,
                              append_property_indexes: propertyIndexEntities
                            });
                          }}
                          originInputProps={{
                            onBlur() {
                              // check is ready to create
                              edgeTypeStore.validateEditEdgeType();
                            }
                          }}
                        />
                      </div>
                      <div>
                        <Select
                          width={110}
                          placeholder={t(
                            'addition.edge.index-type-select-desc'
                          )}
                          size="medium"
                          showSearch={false}
                          value={type === '' ? [] : type}
                          onChange={(value: string) => {
                            const propertyIndexEntities = cloneDeep(
                              edgeTypeStore.editedSelectedEdgeType
                                .append_property_indexes
                            );

                            propertyIndexEntities[index].type = value;

                            edgeTypeStore.mutateEditedSelectedEdgeType({
                              ...edgeTypeStore.editedSelectedEdgeType,
                              append_property_indexes: propertyIndexEntities
                            });

                            edgeTypeStore.validateEditEdgeType();
                          }}
                        >
                          <Select.Option value="SECONDARY" key="SECONDARY">
                            {t('addition.menu.secondary-index')}
                          </Select.Option>
                          <Select.Option value="RANGE" key="RANGE">
                            {t('addition.menu.range-index')}
                          </Select.Option>
                          <Select.Option value="SEARCH" key="SEARCH">
                            {t('addition.menu.full-text-index')}
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
                              edgeTypeStore.editedSelectedEdgeType
                                .append_property_indexes
                            );

                            if (Array.isArray(value)) {
                              propertyIndexEntities[index].fields = value;
                            } else {
                              propertyIndexEntities[index].fields = [value];
                            }

                            edgeTypeStore.mutateEditedSelectedEdgeType({
                              ...edgeTypeStore.editedSelectedEdgeType,
                              append_property_indexes: propertyIndexEntities
                            });

                            edgeTypeStore.validateEditEdgeType();
                          }}
                        >
                          {type === 'SECONDARY' &&
                            edgeTypeStore
                              .selectedEdgeType!.properties.concat(
                                edgeTypeStore.editedSelectedEdgeType
                                  .append_properties
                              )
                              .map((property) => {
                                const order = edgeTypeStore.editedSelectedEdgeType.append_property_indexes[
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
                            edgeTypeStore
                              .selectedEdgeType!.properties.concat(
                                edgeTypeStore.editedSelectedEdgeType
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
                            edgeTypeStore
                              .selectedEdgeType!.properties.concat(
                                edgeTypeStore.editedSelectedEdgeType
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
                                      edgeTypeStore.editedSelectedEdgeType!
                                        .append_property_indexes
                                    );

                                    appendPropertyIndexes.splice(index, 1);

                                    edgeTypeStore.mutateEditedSelectedEdgeType({
                                      ...edgeTypeStore.editedSelectedEdgeType,
                                      append_property_indexes: appendPropertyIndexes
                                    });

                                    setDeleteAddedPopIndexInDrawer(null);
                                    edgeTypeStore.validateEditEdgeType(true);
                                  }}
                                >
                                  {t('addition.common.confirm')}
                                </div>
                                <div
                                  onClick={() => {
                                    edgeTypeStore.resetEditedSelectedEdgeType();
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
              {isEditEdge && (
                <div
                  onClick={() => {
                    if (
                      edgeTypeStore.editedSelectedEdgeType
                        .append_property_indexes.length === 0 ||
                      edgeTypeStore.isEditReady
                    ) {
                      edgeTypeStore.mutateEditedSelectedEdgeType({
                        ...edgeTypeStore.editedSelectedEdgeType,
                        append_property_indexes: [
                          ...edgeTypeStore.editedSelectedEdgeType
                            .append_property_indexes,
                          {
                            name: '',
                            type: '',
                            fields: []
                          }
                        ]
                      });

                      edgeTypeStore.validateEditEdgeType(true);
                    }
                  }}
                  style={{
                    cursor: 'pointer',
                    color: edgeTypeStore.isEditReady ? '#2b65ff' : '#999'
                  }}
                >
                  {t('addition.edge.add-group')}
                </div>
              )}
            </div>
          </div>
        </div>
      </div>
    </Drawer>
  );
});

export default CheckAndEditEdge;
