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
} from '@baidu/one-ui';

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
  PRIMARY_KEY: '主键ID',
  AUTOMATIC: '自动生成',
  CUSTOMIZE_STRING: '自定义字符串',
  CUSTOMIZE_NUMBER: '自定义数字',
  CUSTOMIZE_UUID: '自定义UUID'
};

const propertyIndexTypeMappings: Record<string, string> = {
  SECONDARY: '二级索引',
  RANGE: '范围索引',
  SEARCH: '全文索引'
};

const VertexTypeList: React.FC = observer(() => {
  const metadataConfigsRootStore = useContext(MetadataConfigsRootStore);
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
        content: '无可删除顶点类型',
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
        content: '删除成功',
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
      title: '顶点类型名称',
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
      title: '关联属性',
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
      title: 'ID策略',
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
      title: '主键属性',
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
      title: '类型索引',
      dataIndex: 'open_label_index',
      width: '10%',
      render(value: boolean) {
        return (
          <Switch
            checkedChildren="开"
            unCheckedChildren="关"
            checked={value}
            size="large"
            disabled
          />
        );
      }
    },
    {
      title: '属性索引',
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
      title: '操作',
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
            创建
          </Button>
          <Button
            size="medium"
            style={styles.button}
            disabled={isLoading}
            onClick={() => {
              vertexTypeStore.changeCurrentTabStatus('reuse');
            }}
          >
            复用
          </Button>
        </div>
        {size(currentSelectedRowKeys) !== 0 && (
          <div className="metadata-properties-selected-reveals">
            <div>已选{size(currentSelectedRowKeys)}项</div>
            <Button
              onClick={() => {
                switchShowModal(true);
                vertexTypeStore.checkIfUsing(currentSelectedRowKeys);
              }}
            >
              批量删除
            </Button>
            <img
              src={WhiteCloseIcon}
              alt="关闭"
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
          okText="删除"
          cancelText="取消"
          onOk={batchDeleteProperties}
          buttonSize="medium"
          onCancel={() => {
            switchShowModal(false);
          }}
        >
          <div className="metadata-properties-modal">
            <div className="metadata-title metadata-properties-modal-title">
              <span>确认删除</span>
              <img
                src={CloseIcon}
                alt="关闭"
                onClick={() => {
                  switchShowModal(false);
                }}
              />
            </div>
            <div
              className="metadata-properties-modal-description"
              style={{ marginBottom: 0 }}
            >
              使用中顶点类型不可删除，确认删除以下未使用顶点类型？
            </div>
            <div className="metadata-properties-modal-description">
              删除元数据耗时较久，详情可在任务管理中查看。
            </div>
            <Table
              columns={[
                {
                  title: '顶点名称',
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
                  title: '状态',
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
                        {isUsing ? '使用中' : '未使用'}
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
          title={isEditVertex ? '编辑顶点类型' : '顶点类型详情'}
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
                        content: '修改成功',
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
                              保存成功
                            </div>
                            <div style={{ marginBottom: 2 }}>
                              创建索引可能耗时较久，详情可在任务管理中查看
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
                              去任务管理查看
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
              {isEditVertex ? '保存' : '编辑'}
            </Button>,
            <Button
              size="medium"
              style={{ width: 60 }}
              onClick={handleCloseDrawer}
              key="drawer-close"
            >
              关闭
            </Button>
          ]}
        >
          {!isEmpty(vertexTypeStore.selectedVertexType) && (
            <div className="metadata-configs-drawer">
              <div
                className="metadata-title"
                style={{ marginBottom: 16, width: 88, textAlign: 'right' }}
              >
                基础信息
              </div>
              <div
                className={metadataDrawerOptionClass}
                style={{ alignItems: 'center' }}
              >
                <div className="metadata-drawer-options-name">
                  <span>顶点类型名称：</span>
                </div>
                {vertexTypeStore.selectedVertexType!.name}
              </div>
              <div className="metadata-drawer-options">
                <div className="metadata-drawer-options-name">
                  <span style={{ lineHeight: 2.5 }}>顶点样式：</span>
                </div>
                <div className="new-vertex-type-options-colors">
                  <Select
                    width={66}
                    size="medium"
                    prefixCls="new-fc-one-select-another"
                    dropdownMatchSelectWidth={false}
                    showSearch={false}
                    disabled={!isEditVertex}
                    value={
                      <div
                        className="new-vertex-type-select"
                        style={{
                          background:
                            vertexTypeStore.editedSelectedVertexType.style
                              .color !== null
                              ? vertexTypeStore.editedSelectedVertexType.style.color.toLowerCase()
                              : vertexTypeStore.selectedVertexType!.style.color!.toLowerCase(),
                          marginTop: 5
                        }}
                      ></div>
                    }
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
                  <span>ID策略：</span>
                </div>
                {
                  IDStrategyMappings[
                    vertexTypeStore.selectedVertexType!.id_strategy
                  ]
                }
              </div>
              <div className="metadata-drawer-options">
                <div className="metadata-drawer-options-name">
                  <span>关联属性：</span>
                </div>
                <div className="metadata-drawer-options-list">
                  <div className="metadata-drawer-options-list-row">
                    <span>属性</span>
                    <span>允许为空</span>
                  </div>
                  {vertexTypeStore.selectedVertexType!.properties.map(
                    ({ name, nullable }) => (
                      <div
                        className="metadata-drawer-options-list-row"
                        key={name}
                      >
                        <div>{name}</div>
                        <div style={{ width: 70, textAlign: 'center' }}>
                          <Switch
                            checkedChildren="开"
                            unCheckedChildren="关"
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
                          <div>{name}</div>
                          <div style={{ width: 70, textAlign: 'center' }}>
                            <Switch
                              checkedChildren="开"
                              unCheckedChildren="关"
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
                      <span style={{ marginRight: 4 }}>添加属性</span>
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
                  <span>主键属性：</span>
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
                    顶点展示内容：
                  </span>
                </div>
                {isEditVertex ? (
                  <Select
                    width={420}
                    mode="multiple"
                    size="medium"
                    placeholder="请选择顶点展示内容"
                    showSearch={false}
                    value={vertexTypeStore.editedSelectedVertexType.style.display_fields.map(
                      (field) => formatVertexIdText(field, '顶点ID')
                    )}
                    onChange={(value: string[]) => {
                      vertexTypeStore.mutateEditedSelectedVertexType({
                        ...vertexTypeStore.editedSelectedVertexType,
                        style: {
                          ...vertexTypeStore.editedSelectedVertexType.style,
                          display_fields: value.map((field) =>
                            formatVertexIdText(field, '顶点ID', true)
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
                            value={formatVertexIdText(item.name, '顶点ID')}
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
                                {formatVertexIdText(item.name, '顶点ID')}
                              </div>
                            </div>
                          </Select.Option>
                        );
                      })}
                  </Select>
                ) : (
                  <div>
                    {vertexTypeStore.selectedVertexType?.style.display_fields
                      .map((field) => formatVertexIdText(field, '顶点ID'))
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
                索引信息
              </div>
              <div className={metadataDrawerOptionClass}>
                <div className="metadata-drawer-options-name">
                  <span>类型索引：</span>
                </div>
                <Switch
                  checkedChildren="开"
                  unCheckedChildren="关"
                  checked={vertexTypeStore.selectedVertexType!.open_label_index}
                  size="large"
                  disabled
                />
              </div>
              <div className="metadata-drawer-options">
                <div className="metadata-drawer-options-name">
                  <span>属性索引：</span>
                </div>
                <div className="metadata-drawer-options-list">
                  {(vertexTypeStore.selectedVertexType!.property_indexes
                    .length !== 0 ||
                    vertexTypeStore.editedSelectedVertexType
                      .append_property_indexes.length !== 0) && (
                    <div className="metadata-drawer-options-list-row metadata-drawer-options-list-row-normal">
                      <span>索引名称</span>
                      <span>索引类型</span>
                      <span>属性</span>
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
                                      确认删除此属性？
                                    </p>
                                    <p
                                      style={{
                                        width: 200,
                                        lineHeight: '28px'
                                      }}
                                    >
                                      删除索引后，无法根据此属性索引进行查询，请谨慎操作。
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
                                        确认
                                      </div>
                                      <div
                                        onClick={() => {
                                          setDeleteExistPopIndexInDrawer(null);
                                        }}
                                      >
                                        取消
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
                              placeholder="索引名称"
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
                              placeholder="请选择索引类型"
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
                                二级索引
                              </Select.Option>
                              <Select.Option value="RANGE" key="RANGE">
                                范围索引
                              </Select.Option>
                              <Select.Option value="SEARCH" key="SEARCH">
                                全文索引
                              </Select.Option>
                            </Select>
                          </div>
                          <div>
                            <Select
                              width={120}
                              mode={
                                type === 'SECONDARY' ? 'multiple' : 'default'
                              }
                              placeholder="请选择属性"
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
                                    确认删除此属性？
                                  </p>
                                  <p
                                    style={{
                                      width: 200,
                                      lineHeight: '28px'
                                    }}
                                  >
                                    删除索引后，无法根据此属性索引进行查询，请谨慎操作。
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
                                      确认
                                    </div>
                                    <div
                                      onClick={() => {
                                        vertexTypeStore.resetEditedSelectedVertexType();
                                        setDeleteAddedPopIndexInDrawer(null);
                                      }}
                                    >
                                      取消
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
                      新增一组
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
          编辑
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
                    当前顶点类型正在使用中，不可删除。
                  </p>
                ) : (
                  <>
                    <p className="metadata-properties-tooltips-title">
                      确认删除此顶点类型？
                    </p>
                    <p>确认删除此顶点类型？删除后无法恢复，请谨慎操作</p>
                    <p>删除元数据耗时较久，详情可在任务管理中查看</p>
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
                              content: '删除成功',
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
                        确认
                      </Button>
                      <Button
                        size="medium"
                        style={{ width: 60 }}
                        onClick={() => {
                          switchPopDeleteModal(false);
                        }}
                      >
                        取消
                      </Button>
                    </div>
                  </>
                )}
              </div>
            }
            childrenProps={{
              className: 'metadata-properties-manipulation',
              title: isDeleteOrBatchDeleting ? '删除中' : '删除',
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
            {isDeleteOrBatchDeleting ? '删除中' : '删除'}
          </Tooltip>
        </div>
      </div>
    );
  }
);

const EmptyVertxTypeHints: React.FC = observer(() => {
  const { vertexTypeStore } = useContext(MetadataConfigsRootStore);

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
        您暂时还没有任何顶点类型，立即创建
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
          创建顶点类型
        </Button>
        <Button
          size="large"
          style={{ width: 144 }}
          onClick={() => {
            vertexTypeStore.changeCurrentTabStatus('reuse');
          }}
        >
          复用已有类型
        </Button>
      </div>
    </div>
  );
});

export default VertexTypeList;
