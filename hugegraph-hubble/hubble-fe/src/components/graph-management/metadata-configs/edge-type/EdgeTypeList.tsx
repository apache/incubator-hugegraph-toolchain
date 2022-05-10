import React, {
  useContext,
  useState,
  useEffect,
  useRef,
  useCallback
} from 'react';
import { observer } from 'mobx-react';
import {
  isEmpty,
  isUndefined,
  cloneDeep,
  intersection,
  size,
  without
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
import MetadataConfigsRootStore from '../../../../stores/GraphManagementStore/metadataConfigsStore/metadataConfigsStore';

import NewEdgeType from './NewEdgeType';
import ReuseEdgeTypes from './ReuseEdgeTypes';
import DataAnalyzeStore from '../../../../stores/GraphManagementStore/dataAnalyzeStore/dataAnalyzeStore';
import { formatVertexIdText } from '../../../../stores/utils';

import type {
  EdgeTypeValidatePropertyIndexes,
  EdgeType
} from '../../../../stores/types/GraphManagementStore/metadataConfigsStore';

import AddIcon from '../../../../assets/imgs/ic_add.svg';
import BlueArrowIcon from '../../../../assets/imgs/ic_arrow_blue.svg';
import WhiteCloseIcon from '../../../../assets/imgs/ic_close_white.svg';
import CloseIcon from '../../../../assets/imgs/ic_close_16.svg';
import LoadingBackIcon from '../../../../assets/imgs/ic_loading_back.svg';
import LoadingFrontIcon from '../../../../assets/imgs/ic_loading_front.svg';
import SelectedSoilidArrowIcon from '../../../../assets/imgs/ic_arrow_selected.svg';
import NoSelectedSoilidArrowIcon from '../../../../assets/imgs/ic_arrow.svg';
import SelectedSoilidStraightIcon from '../../../../assets/imgs/ic_straight_selected.svg';
import NoSelectedSoilidStraightIcon from '../../../../assets/imgs/ic_straight.svg';
import i18next from '../../../../i18n';
import { useTranslation } from 'react-i18next';

import './EdgeTypeList.less';

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

const propertyIndexTypeMappings: Record<string, string> = {
  SECONDARY: i18next.t('addition.menu.secondary-index'),
  RANGE: i18next.t('addition.menu.range-index'),
  SEARCH: i18next.t('addition.menu.full-text-index')
};

const EdgeTypeList: React.FC = observer(() => {
  const { t } = useTranslation();
  const metadataConfigsRootStore = useContext(MetadataConfigsRootStore);
  const { metadataPropertyStore, edgeTypeStore } = metadataConfigsRootStore;
  const [preLoading, switchPreLoading] = useState(true);
  const [sortOrder, setSortOrder] = useState('');
  const [selectedRowKeys, mutateSelectedRowKeys] = useState<string[]>([]);
  const [isShowModal, switchShowModal] = useState(false);
  const [isAddProperty, switchIsAddProperty] = useState(false);
  const [isEditEdge, switchIsEditEdge] = useState(false);
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
    preLoading || edgeTypeStore.requestStatus.fetchEdgeTypeList === 'pending';

  const currentSelectedRowKeys = intersection(
    selectedRowKeys,
    edgeTypeStore.edgeTypes.map(({ name }) => name)
  );

  const handleOutSideClick = useCallback(
    (e: MouseEvent) => {
      if (
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
    [deleteExistPopIndexInDrawer, deleteWrapperInDrawerRef, isAddProperty]
  );

  const handleSelectedTableRow = (newSelectedRowKeys: string[]) => {
    mutateSelectedRowKeys(newSelectedRowKeys);
  };

  const handleCloseDrawer = () => {
    switchIsAddProperty(false);
    switchIsEditEdge(false);
    edgeTypeStore.selectEdgeType(null);
    edgeTypeStore.resetEditedSelectedEdgeType();
  };

  const handleSortClick = () => {
    switchPreLoading(true);

    if (sortOrder === 'descend') {
      edgeTypeStore.mutatePageSort('asc');
      setSortOrder('ascend');
    } else {
      edgeTypeStore.mutatePageSort('desc');
      setSortOrder('descend');
    }

    edgeTypeStore.fetchEdgeTypeList();
  };

  const batchDeleteProperties = async () => {
    switchShowModal(false);
    // need to set a copy in store since local row key state would be cleared
    edgeTypeStore.mutateSelectedEdgeTypeNames(currentSelectedRowKeys);
    // mutateSelectedRowKeys([]);
    await edgeTypeStore.deleteEdgeType(currentSelectedRowKeys);
    // edgeTypeStore.mutateSelectedEdgeTypeNames([]);

    if (edgeTypeStore.requestStatus.deleteEdgeType === 'success') {
      Message.success({
        content: t('addition.common.del-success'),
        size: 'medium',
        showCloseIcon: false
      });

      mutateSelectedRowKeys(
        without(selectedRowKeys, ...currentSelectedRowKeys)
      );

      await edgeTypeStore.fetchEdgeTypeList();

      // fetch previous page data if it's empty
      if (
        edgeTypeStore.requestStatus.fetchEdgeTypeList === 'success' &&
        size(edgeTypeStore.edgeTypes) === 0 &&
        edgeTypeStore.edgeTypeListPageConfig.pageNumber > 1
      ) {
        edgeTypeStore.mutatePageNumber(
          edgeTypeStore.edgeTypeListPageConfig.pageNumber - 1
        );

        edgeTypeStore.fetchEdgeTypeList();
      }

      return;
    }

    if (edgeTypeStore.requestStatus.deleteEdgeType === 'failed') {
      Message.error({
        content: edgeTypeStore.errorMessage,
        size: 'medium',
        showCloseIcon: false
      });
    }
  };

  const columnConfigs = [
    {
      title: t('addition.common.edge-type-name'),
      dataIndex: 'name',
      sorter: true,
      sortOrder,
      width: '14%',
      render(text: string, records: any[], index: number) {
        return (
          <div
            className="metadata-properties-manipulation no-line-break"
            title={text}
            onClick={() => {
              edgeTypeStore.selectEdgeType(index);

              // check also need style infos
              edgeTypeStore.mutateEditedSelectedEdgeType({
                ...edgeTypeStore.editedSelectedEdgeType,
                style: {
                  color: edgeTypeStore.selectedEdgeType!.style.color,
                  icon: null,
                  with_arrow: edgeTypeStore.selectedEdgeType!.style.with_arrow,
                  thickness: edgeTypeStore.selectedEdgeType!.style.thickness,
                  display_fields: edgeTypeStore.selectedEdgeType!.style
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
      title: t('addition.common.source-type'),
      dataIndex: 'source_label',
      width: '14%',
      render(text: string, records: any[], index: number) {
        return (
          <div className="no-line-break" title={text}>
            {text}
          </div>
        );
      }
    },
    {
      title: t('addition.common.target-type'),
      dataIndex: 'target_label',
      width: '14%',
      render(text: string, records: any[], index: number) {
        return (
          <div className="no-line-break" title={text}>
            {text}
          </div>
        );
      }
    },
    {
      title: t('addition.common.association-property'),
      dataIndex: 'properties',
      width: '14%',
      render(properties: { name: string; nullable: boolean }[]) {
        const text =
          properties.length !== 0
            ? properties.map(({ name }) => name).join('; ')
            : '-';

        return (
          <div className="no-line-break" title={text}>
            {text}
          </div>
        );
      }
    },
    {
      title: t('addition.common.distinguishing-key'),
      dataIndex: 'sort_keys',
      width: '10%',
      render(values: string[]) {
        const text = values.length !== 0 ? values.join(';') : '-';

        return (
          <div className="no-line-break" title={text}>
            {text}
          </div>
        );
      }
    },
    {
      title: t('addition.menu.type-index'),
      dataIndex: 'open_label_index',
      width: '8%',
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
      width: '14%',
      render(indexes: { name: string; type: string; fields: string[] }[]) {
        const text =
          indexes.length !== 0
            ? indexes.map(({ name }) => name).join('; ')
            : '-';

        return (
          <div className="no-line-break" title={text}>
            {text}
          </div>
        );
      }
    },
    {
      title: t('addition.operate.operate'),
      dataIndex: 'manipulation',
      width: '12%',
      render(_: any, records: EdgeType, index: number) {
        return (
          <EdgeTypeListManipulation
            edgeName={records.name}
            edgeIndex={index}
            switchIsEditEdge={switchIsEditEdge}
          />
        );
      }
    }
  ];

  const metadataDrawerOptionClass = classnames({
    'metadata-drawer-options': true,
    'metadata-drawer-options-disabled': isEditEdge
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
      edgeTypeStore.fetchEdgeTypeList();
    }

    return () => {
      edgeTypeStore.dispose();
    };
  }, [
    metadataPropertyStore,
    metadataConfigsRootStore.currentId,
    edgeTypeStore
  ]);

  useEffect(() => {
    document.addEventListener('click', handleOutSideClick, false);

    return () => {
      document.removeEventListener('click', handleOutSideClick, false);
    };
  }, [handleOutSideClick]);

  if (edgeTypeStore.currentTabStatus === 'new') {
    return <NewEdgeType />;
  }

  if (edgeTypeStore.currentTabStatus === 'reuse') {
    return <ReuseEdgeTypes />;
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
            style={styles.button}
            disabled={isLoading || size(currentSelectedRowKeys) !== 0}
            onClick={() => {
              edgeTypeStore.changeCurrentTabStatus('new');
            }}
          >
            {t('addition.newGraphConfig.create')}
          </Button>
          <Button
            size="medium"
            style={styles.button}
            disabled={isLoading}
            onClick={() => {
              edgeTypeStore.changeCurrentTabStatus('reuse');
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
          rowKey={(rowData: EdgeType) => rowData.name}
          locale={{
            emptyText: (
              <LoadingDataView
                isLoading={isLoading}
                emptyView={<EmptyEdgeTypeHints />}
              />
            )
          }}
          rowSelection={{
            selectedRowKeys,
            onChange: handleSelectedTableRow
          }}
          onSortClick={handleSortClick}
          dataSource={isLoading ? [] : edgeTypeStore.edgeTypes}
          pagination={
            isLoading
              ? null
              : {
                  hideOnSinglePage: false,
                  pageNo: edgeTypeStore.edgeTypeListPageConfig.pageNumber,
                  pageSize: 10,
                  showSizeChange: false,
                  showPageJumper: false,
                  total: edgeTypeStore.edgeTypeListPageConfig.pageTotal,
                  onPageNoChange: (e: React.ChangeEvent<HTMLSelectElement>) => {
                    // mutateSelectedRowKeys([]);
                    edgeTypeStore.mutatePageNumber(Number(e.target.value));
                    edgeTypeStore.fetchEdgeTypeList();
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
              {t('addition.message.edge-del-confirm')}
            </div>
            <div className="metadata-properties-modal-description">
              {t('addition.message.long-time-notice')}
            </div>
            <Table
              columns={[
                {
                  title: t('addition.common.edge-name'),
                  dataIndex: 'name',
                  render(text: string, records: Record<string, any>) {
                    return (
                      <span style={{ color: records.status ? '#999' : '#333' }}>
                        {text}
                      </span>
                    );
                  }
                }
              ]}
              dataSource={currentSelectedRowKeys.map((name) => ({
                name
              }))}
              pagination={false}
            />
          </div>
        </Modal>
        <Drawer
          title={
            isEditEdge
              ? t('addition.common.modify-edge-type')
              : t('addition.common.edge-type-detail')
          }
          width={580}
          destroyOnClose
          maskClosable={!isEditEdge}
          visible={!isEmpty(edgeTypeStore.selectedEdgeType)}
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
                  switchIsEditEdge(true);
                  edgeTypeStore.validateEditEdgeType();
                  edgeTypeStore.mutateEditedSelectedEdgeType({
                    ...edgeTypeStore.editedSelectedEdgeType,
                    style: {
                      color: edgeTypeStore.selectedEdgeType!.style.color,
                      icon: null,
                      with_arrow: edgeTypeStore.selectedEdgeType!.style
                        .with_arrow,
                      thickness: edgeTypeStore.selectedEdgeType!.style
                        .thickness,
                      display_fields: edgeTypeStore.selectedEdgeType!.style
                        .display_fields
                    }
                  });
                } else {
                  await edgeTypeStore.updateEdgeType();

                  if (edgeTypeStore.requestStatus.updateEdgeType === 'failed') {
                    Message.error({
                      content: edgeTypeStore.errorMessage,
                      size: 'medium',
                      showCloseIcon: false
                    });

                    return;
                  }

                  if (
                    edgeTypeStore.requestStatus.updateEdgeType === 'success'
                  ) {
                    if (
                      isEmpty(
                        edgeTypeStore.editedSelectedEdgeType
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

                  switchIsEditEdge(false);
                  edgeTypeStore.selectEdgeType(null);
                  edgeTypeStore.fetchEdgeTypeList();
                  edgeTypeStore.resetEditedSelectedEdgeType();
                }
              }}
            >
              {isEditEdge
                ? t('addition.common.save')
                : t('addition.common.edit')}
            </Button>,
            <Button
              size="medium"
              style={{ width: 60 }}
              onClick={handleCloseDrawer}
            >
              {t('addition.common.close')}
            </Button>
          ]}
        >
          {!isEmpty(edgeTypeStore.selectedEdgeType) && (
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
                    <span style={{ lineHeight: 2.5 }}>
                      {t('addition.common.edge-style')}：
                    </span>
                  </div>
                  <div className="new-vertex-type-options-colors">
                    <Select
                      width={66}
                      size="medium"
                      showSearch={false}
                      disabled={!isEditEdge}
                      prefixCls="new-fc-one-select-another"
                      dropdownMatchSelectWidth={false}
                      value={edgeTypeStore.editedSelectedEdgeType.style.color}
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
                                : edgeTypeStore.selectedEdgeType!.style
                                    .with_arrow,
                            thickness:
                              edgeTypeStore.editedSelectedEdgeType.style
                                .thickness !== null
                                ? edgeTypeStore.editedSelectedEdgeType.style
                                    .thickness
                                : edgeTypeStore.selectedEdgeType!.style
                                    .thickness,
                            display_fields:
                              edgeTypeStore.editedSelectedEdgeType.style
                                .display_fields.length !== 0
                                ? edgeTypeStore.editedSelectedEdgeType.style
                                    .display_fields
                                : edgeTypeStore.selectedEdgeType!.style
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
                                (edgeTypeStore.editedSelectedEdgeType.style
                                  .color !== null
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
                        edgeTypeStore.editedSelectedEdgeType.style.with_arrow
                      }
                      onChange={(e: any) => {
                        edgeTypeStore.mutateEditedSelectedEdgeType({
                          ...edgeTypeStore.editedSelectedEdgeType,
                          style: {
                            with_arrow: e,
                            color:
                              edgeTypeStore.editedSelectedEdgeType.style
                                .color !== null
                                ? edgeTypeStore.editedSelectedEdgeType.style.color.toLowerCase()
                                : edgeTypeStore.selectedEdgeType!.style.color!.toLowerCase(),
                            icon: null,
                            thickness:
                              edgeTypeStore.editedSelectedEdgeType.style
                                .thickness !== null
                                ? edgeTypeStore.editedSelectedEdgeType.style
                                    .thickness
                                : edgeTypeStore.selectedEdgeType!.style
                                    .thickness,
                            display_fields:
                              edgeTypeStore.editedSelectedEdgeType.style
                                .display_fields.length !== 0
                                ? edgeTypeStore.editedSelectedEdgeType.style
                                    .display_fields
                                : edgeTypeStore.selectedEdgeType!.style
                                    .display_fields
                          }
                        });
                      }}
                    >
                      {edgeTypeStore.edgeShapeSchemas.map((item, index) => (
                        <Select.Option
                          value={item.flag}
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
                                    edgeTypeStore.selectedEdgeType!.style
                                      .with_arrow
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
                        edgeTypeStore.editedSelectedEdgeType.style.thickness !==
                        null
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
                                : edgeTypeStore.selectedEdgeType!.style
                                    .with_arrow,
                            color:
                              edgeTypeStore.editedSelectedEdgeType.style
                                .color !== null
                                ? edgeTypeStore.editedSelectedEdgeType.style.color.toLowerCase()
                                : edgeTypeStore.selectedEdgeType!.style.color!.toLowerCase(),
                            icon: null,
                            thickness: value,
                            display_fields:
                              edgeTypeStore.editedSelectedEdgeType.style
                                .display_fields.length !== 0
                                ? edgeTypeStore.editedSelectedEdgeType.style
                                    .display_fields
                                : edgeTypeStore.selectedEdgeType!.style
                                    .display_fields
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
                    <span>
                      {t('addition.common.allow-multiple-connections')}：
                    </span>
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
                    {isEditEdge &&
                      edgeTypeStore.editedSelectedEdgeType.append_properties.map(
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
                                          nullable: !isUndefined(
                                            currentProperty
                                          )
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
                    <span>
                      {t('addition.common.distinguishing-key-property')}：
                    </span>
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
                      placeholder={t(
                        'addition.edge.display-content-select-desc'
                      )}
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
                    {(edgeTypeStore.selectedEdgeType!.property_indexes
                      .length !== 0 ||
                      edgeTypeStore.editedSelectedEdgeType
                        .append_property_indexes.length !== 0) && (
                      <div className="metadata-drawer-options-list-row metadata-drawer-options-list-row-normal">
                        <span>{t('addition.edge.index-name')}</span>
                        <span>{t('addition.edge.index-type')}</span>
                        <span>{t('addition.common.property')}</span>
                      </div>
                    )}
                    {edgeTypeStore
                      .selectedEdgeType!.property_indexes.filter(
                        (propertyIndex) =>
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
                                  .map(
                                    (field, index) => index + 1 + '.' + field
                                  )
                                  .join(';')}
                              </span>

                              {isEditEdge && (
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
                                        {t(
                                          'addition.message.index-del-confirm'
                                        )}
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
                                              edgeTypeStore
                                                .editedSelectedEdgeType
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

                                            setDeleteExistPopIndexInDrawer(
                                              null
                                            );
                                            edgeTypeStore.validateEditEdgeType(
                                              true
                                            );
                                          }}
                                        >
                                          {t('addition.common.confirm')}
                                        </div>
                                        <div
                                          onClick={() => {
                                            setDeleteExistPopIndexInDrawer(
                                              null
                                            );
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
                                    ? (edgeTypeStore
                                        .validateEditEdgeTypeErrorMessage
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
                                  propertyIndexEntities[index].fields = [];

                                  edgeTypeStore.mutateEditedSelectedEdgeType({
                                    ...edgeTypeStore.editedSelectedEdgeType,
                                    append_property_indexes: propertyIndexEntities
                                  });

                                  edgeTypeStore.validateEditEdgeType();
                                }}
                              >
                                <Select.Option
                                  value="SECONDARY"
                                  key="SECONDARY"
                                >
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
                                value={fields}
                                onChange={(value: string | string[]) => {
                                  const propertyIndexEntities = cloneDeep(
                                    edgeTypeStore.editedSelectedEdgeType
                                      .append_property_indexes
                                  );

                                  if (Array.isArray(value)) {
                                    propertyIndexEntities[index].fields = value;
                                  } else {
                                    propertyIndexEntities[index].fields = [
                                      value
                                    ];
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
                                            className={
                                              multiSelectOptionClassName
                                            }
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
                                          const appendPropertyIndexes = cloneDeep(
                                            edgeTypeStore.editedSelectedEdgeType!
                                              .append_property_indexes
                                          );

                                          appendPropertyIndexes.splice(
                                            index,
                                            1
                                          );

                                          edgeTypeStore.mutateEditedSelectedEdgeType(
                                            {
                                              ...edgeTypeStore.editedSelectedEdgeType,
                                              append_property_indexes: appendPropertyIndexes
                                            }
                                          );

                                          setDeleteAddedPopIndexInDrawer(null);
                                          edgeTypeStore.validateEditEdgeType(
                                            true
                                          );
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
                      <span
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
                      </span>
                    )}
                  </div>
                </div>
              </div>
            </div>
          )}
        </Drawer>
      </div>
    </motion.div>
  );
});

export interface EdgeTypeListManipulation {
  edgeName: string;
  edgeIndex: number;
  switchIsEditEdge: (flag: boolean) => void;
}

const EdgeTypeListManipulation: React.FC<EdgeTypeListManipulation> = observer(
  ({ edgeName, edgeIndex, switchIsEditEdge }) => {
    const { edgeTypeStore } = useContext(MetadataConfigsRootStore);
    const { t } = useTranslation();
    const [isPopDeleteModal, switchPopDeleteModal] = useState(false);
    const [isDeleting, switchDeleting] = useState(false);
    const deleteWrapperRef = useRef<HTMLDivElement>(null);
    const isDeleteOrBatchDeleting =
      isDeleting ||
      (edgeTypeStore.requestStatus.deleteEdgeType === 'pending' &&
        edgeTypeStore.selectedEdgeTypeNames.includes(edgeName));

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
            edgeTypeStore.selectEdgeType(edgeIndex);
            edgeTypeStore.validateEditEdgeType(true);
            switchIsEditEdge(true);

            edgeTypeStore.mutateEditedSelectedEdgeType({
              ...edgeTypeStore.editedSelectedEdgeType,
              style: {
                color: edgeTypeStore.selectedEdgeType!.style.color,
                icon: null,
                with_arrow: edgeTypeStore.selectedEdgeType!.style.with_arrow,
                thickness: edgeTypeStore.selectedEdgeType!.style.thickness,
                display_fields: edgeTypeStore.selectedEdgeType!.style
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
                <p className="metadata-properties-tooltips-title">
                  {t('addition.edge.confirm-del-edge-type')}
                </p>
                <p>{t('addition.edge.confirm-del-edge-type-again')}</p>
                <p>{t('addition.message.long-time-notice')}</p>
                <div className="metadata-properties-tooltips-footer">
                  <Button
                    size="medium"
                    type="primary"
                    style={{ width: 60, marginRight: 12 }}
                    onClick={async () => {
                      switchPopDeleteModal(false);
                      switchDeleting(true);
                      await edgeTypeStore.deleteEdgeType([edgeName]);
                      switchDeleting(false);

                      if (
                        edgeTypeStore.requestStatus.deleteEdgeType === 'success'
                      ) {
                        Message.success({
                          content: t('addition.common.del-success'),
                          size: 'medium',
                          showCloseIcon: false
                        });

                        edgeTypeStore.fetchEdgeTypeList();
                      }

                      if (
                        edgeTypeStore.requestStatus.deleteEdgeType === 'failed'
                      ) {
                        Message.error({
                          content: edgeTypeStore.errorMessage,
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
              </div>
            }
            childrenProps={{
              className: 'metadata-properties-manipulation',
              title: isDeleteOrBatchDeleting
                ? t('addition.operate.del-ing')
                : t('addition.common.del'),
              onClick() {
                if (isDeleteOrBatchDeleting) {
                  return;
                }

                switchPopDeleteModal(true);
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

const EmptyEdgeTypeHints: React.FC = observer(() => {
  const { edgeTypeStore } = useContext(MetadataConfigsRootStore);
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
        {t('addition.edge.no-edge-desc')}
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
          style={{ width: 128, marginRight: 16 }}
          onClick={() => {
            edgeTypeStore.changeCurrentTabStatus('new');
          }}
        >
          {t('addition.edge.create-edge')}
        </Button>
        <Button
          size="large"
          style={{ width: 144 }}
          onClick={() => {
            edgeTypeStore.changeCurrentTabStatus('reuse');
          }}
        >
          {t('addition.edge.multiplexing-existing-type')}
        </Button>
      </div>
    </div>
  );
});

export default EdgeTypeList;
