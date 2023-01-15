import React, {
  useContext,
  useState,
  useEffect,
  useRef,
  useCallback
} from 'react';
import { observer } from 'mobx-react';
import {
  cloneDeep,
  intersection,
  size,
  without,
  isEmpty,
  isUndefined,
  values
} from 'lodash-es';
import { useLocation } from 'wouter';
import classnames from 'classnames';
import { motion } from 'framer-motion';
import {
  Button,
  Table,
  Switch,
  Modal,
  Drawer,
  Input,
  Select,
  Checkbox,
  Message,
  Loading
} from 'hubble-ui';

import { Tooltip, LoadingDataView } from '../../../common';
import NewVertexType from './NewVertexType';
import ReuseVertexTypes from './ReuseVertexTypes';
import MetadataConfigsRootStore from '../../../../stores/GraphManagementStore/metadataConfigsStore/metadataConfigsStore';
import DataAnalyzeStore from '../../../../stores/GraphManagementStore/dataAnalyzeStore/dataAnalyzeStore';
import { formatVertexIdText } from '../../../../stores/utils';

import type {
  VertexTypeValidatePropertyIndexes,
  VertexType
} from '../../../../stores/types/GraphManagementStore/metadataConfigsStore';

import AddIcon from '../../../../assets/imgs/ic_add.svg';
import BlueArrowIcon from '../../../../assets/imgs/ic_arrow_blue.svg';
import WhiteCloseIcon from '../../../../assets/imgs/ic_close_white.svg';
import CloseIcon from '../../../../assets/imgs/ic_close_16.svg';
import i18next from '../../../../i18n';
import { useTranslation } from 'react-i18next';

import './VertexTypeList.less';

const styles = {
  button: {
    marginLeft: 12,
    width: 78
  },
  header: {
    marginBottom: 16
  },
  manipulation: {
    marginRight: 12
  },
  deleteWrapper: {
    display: 'flex',
    justifyContent: 'flex-end'
  },
  loading: {
    padding: 0,
    marginRight: 4
  }
};

const variants = {
  initial: {
    opacity: 0
  },
  animate: {
    opacity: 1,
    transition: {
      duration: 0.7
    }
  },
  exit: {
    opacity: 0,
    transition: {
      duration: 0.3
    }
  }
};

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

const VertexTypeList: React.FC = observer(() => {
  const metadataConfigsRootStore = useContext(MetadataConfigsRootStore);
  const { t } = useTranslation();
  const { metadataPropertyStore, vertexTypeStore } = metadataConfigsRootStore;
  const [preLoading, switchPreLoading] = useState(true);
  const [sortOrder, setSortOrder] = useState('');
  const [selectedRowKeys, mutateSelectedRowKeys] = useState<string[]>([]);
  const [isShowModal, switchShowModal] = useState(false);
  const [isAddProperty, switchIsAddProperty] = useState(false);
  const [isEditVertex, switchIsEditVertex] = useState(false);
  const [
    deleteExistPopIndexInDrawer,
    setDeleteExistPopIndexInDrawer
  ] = useState<number | null>(null);
  const [
    deleteAddedPopIndexInDrawer,
    setDeleteAddedPopIndexInDrawer
  ] = useState<number | null>(null);
  const [, setLocation] = useLocation();

  const dropdownWrapperRef = useRef<HTMLDivElement>(null);
  const deleteWrapperInDrawerRef = useRef<HTMLDivElement>(null);

  const isLoading =
    preLoading ||
    vertexTypeStore.requestStatus.fetchVertexTypeList === 'pending';

  const currentSelectedRowKeys = intersection(
    selectedRowKeys,
    vertexTypeStore.vertexTypes.map(({ name }) => name)
  );

  // need useCallback to stop infinite callings of useEffect
  const handleOutSideClick = useCallback(
    (e: MouseEvent) => {
      // if clicked element is not on dropdown, collpase it
      if (
        isEditVertex &&
        isAddProperty &&
        dropdownWrapperRef.current &&
        !dropdownWrapperRef.current.contains(e.target as Element)
      ) {
        switchIsAddProperty(false);
      }

      if (
        (deleteExistPopIndexInDrawer || deleteAddedPopIndexInDrawer) &&
        deleteWrapperInDrawerRef.current &&
        !deleteWrapperInDrawerRef.current.contains(e.target as Element)
      ) {
        setDeleteExistPopIndexInDrawer(null);
        setDeleteAddedPopIndexInDrawer(null);
      }
    },
    [deleteExistPopIndexInDrawer, deleteAddedPopIndexInDrawer, isAddProperty]
  );

  const handleSelectedTableRow = (newSelectedRowKeys: string[]) => {
    mutateSelectedRowKeys(newSelectedRowKeys);
  };

  const handleCloseDrawer = () => {
    switchIsAddProperty(false);
    switchIsEditVertex(false);
    vertexTypeStore.selectVertexType(null);
    // clear mutations in <Drawer />
    vertexTypeStore.resetEditedSelectedVertexType();
  };

  const handleSortClick = () => {
    switchPreLoading(true);

    if (sortOrder === 'descend') {
      vertexTypeStore.mutatePageSort('asc');
      setSortOrder('ascend');
    } else {
      vertexTypeStore.mutatePageSort('desc');
      setSortOrder('descend');
    }

    vertexTypeStore.fetchVertexTypeList();
  };

  const batchDeleteProperties = async () => {
    if (
      values(currentSelectedRowKeys).every(
        (key) => vertexTypeStore.vertexTypeUsingStatus?.[key]
      )
    ) {
      Message.error({
        content: t('addition.message.no-can-delete-vertex-type'),
        size: 'medium',
        showCloseIcon: false
      });

      return;
    }

    switchShowModal(false);
    // need to set a copy in store since local row key state would be cleared
    vertexTypeStore.mutateSelectedVertexTypeNames(currentSelectedRowKeys);
    // mutateSelectedRowKeys([]);
    await vertexTypeStore.deleteVertexType(currentSelectedRowKeys);

    if (vertexTypeStore.requestStatus.deleteVertexType === 'success') {
      Message.success({
        content: t('addition.common.del-success'),
        size: 'medium',
        showCloseIcon: false
      });

      mutateSelectedRowKeys(
        without(selectedRowKeys, ...currentSelectedRowKeys)
      );

      await vertexTypeStore.fetchVertexTypeList();

      // fetch previous page data if it's empty
      if (
        vertexTypeStore.requestStatus.fetchVertexTypeList === 'success' &&
        size(vertexTypeStore.vertexTypes) === 0 &&
        vertexTypeStore.vertexListPageConfig.pageNumber > 1
      ) {
        vertexTypeStore.mutatePageNumber(
          vertexTypeStore.vertexListPageConfig.pageNumber - 1
        );

        vertexTypeStore.fetchVertexTypeList();
      }

      return;
    }

    if (vertexTypeStore.requestStatus.deleteVertexType === 'failed') {
      Message.error({
        content: vertexTypeStore.errorMessage,
        size: 'medium',
        showCloseIcon: false
      });
    }
  };

  const columnConfigs = [
    {
      title: t('addition.vertex.vertex-type-name'),
      dataIndex: 'name',
      sorter: true,
      sortOrder,
      width: '20%',
      render(text: string, records: any[], index: number) {
        return (
          <div
            className="metadata-properties-manipulation no-line-break"
            title={text}
            onClick={() => {
              vertexTypeStore.selectVertexType(index);

              // check also need style infos
              vertexTypeStore.mutateEditedSelectedVertexType({
                ...vertexTypeStore.editedSelectedVertexType,
                style: {
                  color: vertexTypeStore.selectedVertexType!.style.color,
                  icon: null,
                  size: vertexTypeStore.selectedVertexType!.style.size,
                  display_fields: vertexTypeStore.selectedVertexType!.style
                    .display_fields
                }
              });
            }}
          >
            {text}
          </div>
        );
      }
    },
    {
      title: t('addition.common.association-property'),
      dataIndex: 'properties',
      width: '20%',
      render(properties: { name: string; nullable: boolean }[]) {
        const joinedProperties =
          properties.length !== 0
            ? properties.map(({ name }) => name).join('; ')
            : '-';

        return (
          <div className="no-line-break" title={joinedProperties}>
            {joinedProperties}
          </div>
        );
      }
    },
    {
      title: t('addition.common.id-strategy'),
      dataIndex: 'id_strategy',
      width: '10%',
      render(text: string) {
        return (
          <div className="no-line-break" title={IDStrategyMappings[text]}>
            {IDStrategyMappings[text]}
          </div>
        );
      }
    },
    {
      title: t('addition.common.primary-key-property'),
      dataIndex: 'primary_keys',
      width: '10%',
      render(properties: string[]) {
        const joinedProperties =
          properties.length !== 0 ? properties.join('; ') : '-';

        return (
          <div className="no-line-break" title={joinedProperties}>
            {joinedProperties}
          </div>
        );
      }
    },
    {
      title: t('addition.menu.type-index'),
      dataIndex: 'open_label_index',
      width: '10%',
      render(value: boolean) {
        return (
          <Switch
            checkedChildren={t('addition.operate.open')}
            unCheckedChildren={t('addition.operate.close')}
            checked={value}
            size="large"
            disabled
          />
        );
      }
    },
    {
      title: t('addition.common.property-index'),
      dataIndex: 'property_indexes',
      width: '17%',
      render(indexes: { name: string; type: string; fields: string[] }[]) {
        const joindedIndexes =
          indexes.length !== 0
            ? indexes.map(({ name }) => name).join('; ')
            : '-';

        return (
          <div className="no-line-break" title={joindedIndexes}>
            {joindedIndexes}
          </div>
        );
      }
    },
    {
      title: t('addition.operate.operate'),
      dataIndex: 'manipulation',
      width: '13%',
      render(_: any, records: VertexType, index: number) {
        return (
          <VertexTypeListManipulation
            vertexName={records.name}
            vertexIndex={index}
            switchIsEditVertex={switchIsEditVertex}
          />
        );
      }
    }
  ];

  const metadataDrawerOptionClass = classnames({
    'metadata-drawer-options': true,
    'metadata-drawer-options-disabled': isEditVertex
  });

  useEffect(() => {
    setTimeout(() => {
      switchPreLoading(false);
    }, 500);
  }, [sortOrder]);

  useEffect(() => {
    return () => {
      const messageComponents = document.querySelectorAll(
        '.new-fc-one-message'
      ) as NodeListOf<HTMLElement>;

      messageComponents.forEach((messageComponent) => {
        messageComponent.style.display = 'none';
      });
    };
  });

  useEffect(() => {
    if (metadataConfigsRootStore.currentId !== null) {
      metadataPropertyStore.fetchMetadataPropertyList({ fetchAll: true });
      vertexTypeStore.fetchVertexTypeList();
    }

    return () => {
      vertexTypeStore.dispose();
    };
  }, [
    metadataPropertyStore,
    metadataConfigsRootStore.currentId,
    vertexTypeStore
  ]);

  useEffect(() => {
    document.addEventListener('click', handleOutSideClick, false);

    return () => {
      document.removeEventListener('click', handleOutSideClick, false);
    };
  }, [handleOutSideClick]);

  if (vertexTypeStore.currentTabStatus === 'new') {
    return <NewVertexType />;
  }

  if (vertexTypeStore.currentTabStatus === 'reuse') {
    return <ReuseVertexTypes />;
  }

  return (
    <motion.div
      initial="initial"
      animate="animate"
      exit="exit"
      variants={variants}
    >
      <div className="metadata-configs-content-wrapper">
        <div className="metadata-configs-content-header" style={styles.header}>
          <Button
            type="primary"
            size="medium"
            disabled={isLoading || size(currentSelectedRowKeys) !== 0}
            style={styles.button}
            onClick={() => {
              vertexTypeStore.changeCurrentTabStatus('new');
            }}
          >
            {t('addition.newGraphConfig.create')}
          </Button>
          <Button
            size="medium"
            style={styles.button}
            disabled={isLoading}
            onClick={() => {
              vertexTypeStore.changeCurrentTabStatus('reuse');
            }}
          >
            {t('addition.operate.multiplexing')}
          </Button>
        </div>
        {size(currentSelectedRowKeys) !== 0 && (
          <div className="metadata-properties-selected-reveals">
            <div>
              {t('addition.message.selected')}
              {size(currentSelectedRowKeys)}
              {t('addition.common.term')}
            </div>
            <Button
              onClick={() => {
                switchShowModal(true);
                vertexTypeStore.checkIfUsing(currentSelectedRowKeys);
              }}
            >
              {t('addition.operate.batch-del')}
            </Button>
            <img
              src={WhiteCloseIcon}
              alt={t('addition.common.close')}
              onClick={() => {
                mutateSelectedRowKeys([]);
              }}
            />
          </div>
        )}
        <Table
          columns={columnConfigs}
          rowKey={(rowData: VertexType) => rowData.name}
          locale={{
            emptyText: (
              <LoadingDataView
                isLoading={isLoading}
                emptyView={<EmptyVertxTypeHints />}
              />
            )
          }}
          rowSelection={{
            selectedRowKeys,
            onChange: handleSelectedTableRow
          }}
          onSortClick={handleSortClick}
          dataSource={isLoading ? [] : vertexTypeStore.vertexTypes}
          pagination={
            isLoading
              ? null
              : {
                  hideOnSinglePage: false,
                  pageNo: vertexTypeStore.vertexListPageConfig.pageNumber,
                  pageSize: 10,
                  showSizeChange: false,
                  showPageJumper: false,
                  total: vertexTypeStore.vertexListPageConfig.pageTotal,
                  onPageNoChange: (e: React.ChangeEvent<HTMLSelectElement>) => {
                    // mutateSelectedRowKeys([]);
                    vertexTypeStore.mutatePageNumber(Number(e.target.value));
                    vertexTypeStore.fetchVertexTypeList();
                  }
                }
          }
        />

        <Modal
          visible={isShowModal}
          needCloseIcon={false}
          okText={t('addition.common.del')}
          cancelText={t('addition.common.cancel')}
          onOk={batchDeleteProperties}
          buttonSize="medium"
          onCancel={() => {
            switchShowModal(false);
          }}
        >
          <div className="metadata-properties-modal">
            <div className="metadata-title metadata-properties-modal-title">
              <span>{t('addition.common.del-comfirm')}</span>
              <img
                src={CloseIcon}
                alt={t('addition.common.close')}
                onClick={() => {
                  switchShowModal(false);
                }}
              />
            </div>
            <div
              className="metadata-properties-modal-description"
              style={{ marginBottom: 0 }}
            >
              {t('addition.vertex.using-cannot-delete-confirm')}
            </div>
            <div className="metadata-properties-modal-description">
              {t('addition.message.long-time-notice')}
            </div>
            <Table
              columns={[
                {
                  title: t('addition.common.vertex-name'),
                  dataIndex: 'name',
                  render(text: string, records: Record<string, any>) {
                    return (
                      <span style={{ color: records.status ? '#999' : '#333' }}>
                        {text}
                      </span>
                    );
                  }
                },
                {
                  title: t('addition.common.status'),
                  dataIndex: 'status',
                  render(isUsing: boolean) {
                    return (
                      <div
                        className={
                          isUsing
                            ? 'property-status-is-using'
                            : 'property-status-not-used'
                        }
                      >
                        {isUsing
                          ? t('addition.common.in-use')
                          : t('addition.common.not-used')}
                      </div>
                    );
                  }
                }
              ]}
              dataSource={currentSelectedRowKeys.map((name) => {
                return {
                  name,
                  status:
                    vertexTypeStore.vertexTypeUsingStatus !== null &&
                    // data may have some delay which leads to no matching propety value
                    !!vertexTypeStore.vertexTypeUsingStatus[name]
                };
              })}
              pagination={false}
            />
          </div>
        </Modal>

        <Drawer
          title={
            isEditVertex
              ? t('addition.vertex.edit-type')
              : t('addition.vertex.type-detail')
          }
          width={580}
          destroyOnClose
          maskClosable={!isEditVertex}
          visible={!isEmpty(vertexTypeStore.selectedVertexType)}
          onClose={handleCloseDrawer}
          footer={[
            <Button
              type="primary"
              size="medium"
              style={{ width: 60 }}
              disabled={
                isEditVertex &&
                (vertexTypeStore.editedSelectedVertexType.style?.display_fields
                  .length === 0 ||
                  !vertexTypeStore.isEditReady)
              }
              onClick={async () => {
                if (!isEditVertex) {
                  switchIsEditVertex(true);
                  vertexTypeStore.validateEditVertexType();
                  vertexTypeStore.mutateEditedSelectedVertexType({
                    ...vertexTypeStore.editedSelectedVertexType,
                    style: {
                      color: vertexTypeStore.selectedVertexType!.style.color,
                      icon: null,
                      size: vertexTypeStore.selectedVertexType!.style.size,
                      display_fields: vertexTypeStore.selectedVertexType!.style
                        .display_fields
                    }
                  });
                } else {
                  await vertexTypeStore.updateVertexType();

                  if (
                    vertexTypeStore.requestStatus.updateVertexType === 'failed'
                  ) {
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
                    if (
                      isEmpty(
                        vertexTypeStore.editedSelectedVertexType
                          .append_property_indexes
                      )
                    ) {
                      Message.success({
                        content: t('addition.operate.modify-success'),
                        size: 'medium',
                        showCloseIcon: false
                      });
                    } else {
                      Message.success({
                        content: (
                          <div
                            className="message-wrapper"
                            style={{ width: 168 }}
                          >
                            <div className="message-wrapper-title">
                              {t('addition.common.save-scuccess')}
                            </div>
                            <div style={{ marginBottom: 2 }}>
                              {t('addition.message.index-long-time-notice')}
                            </div>
                            <div
                              className="message-wrapper-manipulation"
                              style={{ marginBottom: 2 }}
                              onClick={() => {
                                setLocation(
                                  `/graph-management/${metadataConfigsRootStore.currentId}/async-tasks`
                                );
                              }}
                            >
                              {t('addition.operate.view-task-management')}
                            </div>
                          </div>
                        ),
                        duration: 60 * 60 * 24
                      });
                    }
                  }

                  switchIsEditVertex(false);
                  vertexTypeStore.selectVertexType(null);
                  vertexTypeStore.fetchVertexTypeList();
                  vertexTypeStore.resetEditedSelectedVertexType();
                }
              }}
              key="drawer-manipulation"
            >
              {isEditVertex
                ? t('addition.common.save')
                : t('addition.common.edit')}
            </Button>,
            <Button
              size="medium"
              style={{ width: 60 }}
              onClick={handleCloseDrawer}
              key="drawer-close"
            >
              {t('addition.common.close')}
            </Button>
          ]}
        >
          {!isEmpty(vertexTypeStore.selectedVertexType) && (
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
                  <span style={{ lineHeight: 2.5 }}>
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
                          display_fields:
                            vertexTypeStore.editedSelectedVertexType.style
                              .display_fields.length !== 0
                              ? vertexTypeStore.editedSelectedVertexType.style
                                  .display_fields
                              : vertexTypeStore.selectedVertexType!.style
                                  .display_fields,
                          size:
                            vertexTypeStore.editedSelectedVertexType.style
                              .size !== null
                              ? vertexTypeStore.editedSelectedVertexType.style
                                  .size
                              : vertexTypeStore.selectedVertexType!.style.size
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
                      vertexTypeStore.editedSelectedVertexType.style.size !==
                      null
                        ? vertexTypeStore.editedSelectedVertexType.style.size
                        : vertexTypeStore.selectedVertexType!.style.size
                    }
                    onChange={(value: string) => {
                      vertexTypeStore.mutateEditedSelectedVertexType({
                        ...vertexTypeStore.editedSelectedVertexType,
                        style: {
                          color:
                            vertexTypeStore.editedSelectedVertexType.style
                              .color !== null
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
                {
                  IDStrategyMappings[
                    vertexTypeStore.selectedVertexType!.id_strategy
                  ]
                }
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
                      <div
                        className="metadata-drawer-options-list-row"
                        key={name}
                      >
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
                        <div
                          className="metadata-drawer-options-list-row"
                          key={name}
                        >
                          <div style={{ maxWidth: 260 }}>{name}</div>
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
                                    (propertyIndex) =>
                                      propertyIndex === property.name
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

                                  vertexTypeStore.mutateEditedSelectedVertexType(
                                    {
                                      ...vertexTypeStore.editedSelectedVertexType,
                                      append_properties: [
                                        ...addedPropertiesInSelectedVertextType
                                      ].map((propertyName) => {
                                        const currentProperty = vertexTypeStore.newVertexType.properties.find(
                                          ({ name }) => name === propertyName
                                        );

                                        return {
                                          name: propertyName,
                                          nullable: !isUndefined(
                                            currentProperty
                                          )
                                            ? currentProperty.nullable
                                            : true
                                        };
                                      })
                                    }
                                  );
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
                {vertexTypeStore.selectedVertexType!.primary_keys.join(';')}
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
                    placeholder={t(
                      'addition.vertex.select-vertex-display-content-placeholder'
                    )}
                    showSearch={false}
                    value={vertexTypeStore.editedSelectedVertexType.style.display_fields.map(
                      (field) =>
                        formatVertexIdText(
                          field,
                          t('addition.function-parameter.vertex-id')
                        )
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
                  >
                    {vertexTypeStore.selectedVertexType?.properties
                      .concat({ name: '~id', nullable: false })
                      .concat(
                        vertexTypeStore.editedSelectedVertexType
                          .append_properties
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
                  {(vertexTypeStore.selectedVertexType!.property_indexes
                    .length !== 0 ||
                    vertexTypeStore.editedSelectedVertexType
                      .append_property_indexes.length !== 0) && (
                    <div className="metadata-drawer-options-list-row metadata-drawer-options-list-row-normal">
                      <span>{t('addition.edge.index-name')}</span>
                      <span>{t('addition.edge.index-type')}</span>
                      <span>{t('addition.common.property')}</span>
                    </div>
                  )}
                  {vertexTypeStore
                    .selectedVertexType!.property_indexes.filter(
                      (propertyIndex) =>
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
                          <div
                            style={{ display: 'flex', alignItems: 'center' }}
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

                            {isEditVertex && (
                              <Tooltip
                                placement="bottom-end"
                                tooltipShown={
                                  index === deleteExistPopIndexInDrawer
                                }
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
                                      {t(
                                        'addition.message.property-del-confirm'
                                      )}
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
                                            vertexTypeStore
                                              .editedSelectedVertexType
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
                                childrenWrapperElement="img"
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
                                vertexTypeStore
                                  .validateEditVertexTypeErrorMessage
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
                              placeholder={t(
                                'addition.edge.index-type-select-desc'
                              )}
                              size="medium"
                              showSearch={false}
                              value={type === '' ? [] : type}
                              onChange={(value: string) => {
                                const propertyIndexEntities = cloneDeep(
                                  vertexTypeStore.editedSelectedVertexType
                                    .append_property_indexes
                                );

                                propertyIndexEntities[index].type = value;
                                propertyIndexEntities[index].fields = [];

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
                              mode={
                                type === 'SECONDARY' ? 'multiple' : 'default'
                              }
                              placeholder={t(
                                'addition.edge.property-select-desc'
                              )}
                              size="medium"
                              showSearch={false}
                              onSearch={null}
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

                                    const multiSelectOptionClassName = classnames(
                                      {
                                        'metadata-configs-sorted-multiSelect-option': true,
                                        'metadata-configs-sorted-multiSelect-option-selected':
                                          order !== -1
                                      }
                                    );

                                    return (
                                      <Select.Option
                                        value={property.name}
                                        key={property.name}
                                      >
                                        <div
                                          className={multiSelectOptionClassName}
                                        >
                                          <div>
                                            {order !== -1 ? order + 1 : ''}
                                          </div>
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
                              tooltipShown={
                                index === deleteAddedPopIndexInDrawer
                              }
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
                                        vertexTypeStore.validateEditVertexType(
                                          true
                                        );
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
                    <span
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
                    </span>
                  )}
                </div>
              </div>
            </div>
          )}
        </Drawer>
      </div>
    </motion.div>
  );
});

export interface VertexTypeListManipulation {
  vertexName: string;
  vertexIndex: number;
  switchIsEditVertex: (flag: boolean) => void;
}

const VertexTypeListManipulation: React.FC<VertexTypeListManipulation> = observer(
  ({ vertexName, vertexIndex, switchIsEditVertex }) => {
    const { vertexTypeStore } = useContext(MetadataConfigsRootStore);
    const [isPopDeleteModal, switchPopDeleteModal] = useState(false);
    const [isDeleting, switchDeleting] = useState(false);
    const { t } = useTranslation();
    const deleteWrapperRef = useRef<HTMLDivElement>(null);
    const isDeleteOrBatchDeleting =
      isDeleting ||
      (vertexTypeStore.requestStatus.deleteVertexType === 'pending' &&
        vertexTypeStore.selectedVertexTypeNames.includes(vertexName));

    const handleOutSideClick = useCallback(
      (e: MouseEvent) => {
        if (
          isPopDeleteModal &&
          deleteWrapperRef &&
          deleteWrapperRef.current &&
          !deleteWrapperRef.current.contains(e.target as Element)
        ) {
          switchPopDeleteModal(false);
        }
      },
      [deleteWrapperRef, isPopDeleteModal]
    );

    useEffect(() => {
      document.addEventListener('click', handleOutSideClick, false);

      return () => {
        document.removeEventListener('click', handleOutSideClick, false);
      };
    }, [handleOutSideClick]);

    return (
      <div style={{ display: 'flex' }} className="no-line-break">
        <span
          className="metadata-properties-manipulation"
          style={styles.manipulation}
          onClick={() => {
            vertexTypeStore.selectVertexType(vertexIndex);
            vertexTypeStore.validateEditVertexType(true);
            switchIsEditVertex(true);

            vertexTypeStore.mutateEditedSelectedVertexType({
              ...vertexTypeStore.editedSelectedVertexType,
              style: {
                color: vertexTypeStore.selectedVertexType!.style.color,
                icon: null,
                size: vertexTypeStore.selectedVertexType!.style.size,
                display_fields: vertexTypeStore.selectedVertexType!.style
                  .display_fields
              }
            });
          }}
        >
          {t('addition.common.edit')}
        </span>
        <div className="no-line-break">
          {isDeleteOrBatchDeleting && (
            <Loading type="strong" style={styles.loading} />
          )}
          <Tooltip
            placement="bottom-end"
            tooltipShown={isPopDeleteModal}
            modifiers={{
              offset: {
                offset: '0, 10'
              }
            }}
            tooltipWrapperProps={{
              className: 'metadata-properties-tooltips'
            }}
            tooltipWrapper={
              <div ref={deleteWrapperRef}>
                {vertexTypeStore.vertexTypeUsingStatus &&
                vertexTypeStore.vertexTypeUsingStatus[vertexName] ? (
                  <p style={{ width: 200 }}>
                    {t('addition.vertex.using-cannot-delete')}
                  </p>
                ) : (
                  <>
                    <p className="metadata-properties-tooltips-title">
                      {t('addition.vertex.del-vertex-confirm')}
                    </p>
                    <p>{t('addition.vertex.del-vertex-confirm-again')}</p>
                    <p>{t('addition.message.long-time-notice')}</p>
                    <div className="metadata-properties-tooltips-footer">
                      <Button
                        size="medium"
                        type="primary"
                        style={{ width: 60, marginRight: 12 }}
                        onClick={async () => {
                          switchPopDeleteModal(false);
                          switchDeleting(true);
                          await vertexTypeStore.deleteVertexType([vertexName]);
                          switchDeleting(false);

                          if (
                            vertexTypeStore.requestStatus.deleteVertexType ===
                            'success'
                          ) {
                            Message.success({
                              content: t('addition.common.del-success'),
                              size: 'medium',
                              showCloseIcon: false
                            });

                            vertexTypeStore.fetchVertexTypeList();
                          }

                          if (
                            vertexTypeStore.requestStatus.deleteVertexType ===
                            'failed'
                          ) {
                            Message.error({
                              content: vertexTypeStore.errorMessage,
                              size: 'medium',
                              showCloseIcon: false
                            });
                          }
                        }}
                      >
                        {t('addition.common.confirm')}
                      </Button>
                      <Button
                        size="medium"
                        style={{ width: 60 }}
                        onClick={() => {
                          switchPopDeleteModal(false);
                        }}
                      >
                        {t('addition.common.cancel')}
                      </Button>
                    </div>
                  </>
                )}
              </div>
            }
            childrenProps={{
              className: 'metadata-properties-manipulation',
              title: isDeleteOrBatchDeleting
                ? t('addition.operate.del-ing')
                : t('addition.common.del'),
              async onClick() {
                if (isDeleteOrBatchDeleting) {
                  return;
                }

                await vertexTypeStore.checkIfUsing([vertexName]);

                if (vertexTypeStore.requestStatus.checkIfUsing === 'success') {
                  switchPopDeleteModal(true);
                }
              }
            }}
          >
            {isDeleteOrBatchDeleting
              ? t('addition.operate.del-ing')
              : t('addition.common.del')}
          </Tooltip>
        </div>
      </div>
    );
  }
);

const EmptyVertxTypeHints: React.FC = observer(() => {
  const { vertexTypeStore } = useContext(MetadataConfigsRootStore);
  const { t } = useTranslation();

  return (
    <div
      style={{
        display: 'flex',
        flexDirection: 'column',
        alignItems: 'center'
      }}
    >
      <img src={AddIcon} alt="Add new property" />
      <div style={{ marginTop: 8, fontSize: 14 }}>
        {t('addition.vertex.no-vertex-type-desc')}
      </div>
      <div
        style={{
          display: 'flex',
          justifyContent: 'space-between',
          marginTop: 24
        }}
      >
        <Button
          type="primary"
          size="large"
          style={{ width: 144, marginRight: 16 }}
          onClick={() => {
            vertexTypeStore.changeCurrentTabStatus('new');
          }}
        >
          {t('addition.vertex.create-vertex-type')}
        </Button>
        <Button
          size="large"
          style={{ width: 144 }}
          onClick={() => {
            vertexTypeStore.changeCurrentTabStatus('reuse');
          }}
        >
          {t('addition.edge.multiplexing-existing-type')}
        </Button>
      </div>
    </div>
  );
});

export default VertexTypeList;
