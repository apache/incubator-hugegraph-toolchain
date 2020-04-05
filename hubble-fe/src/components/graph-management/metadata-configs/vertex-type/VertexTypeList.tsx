import React, {
  useContext,
  useState,
  useEffect,
  useRef,
  useCallback
} from 'react';
import { observer } from 'mobx-react';
import classnames from 'classnames';
import {
  Button,
  Table,
  Switch,
  Modal,
  Drawer,
  Input,
  Select,
  Checkbox,
  Message
} from '@baidu/one-ui';
import { cloneDeep } from 'lodash-es';
import TooltipTrigger from 'react-popper-tooltip';
import { isEmpty, isUndefined } from 'lodash-es';

import NewVertexType from './NewVertexType';
import ReuseVertexTypes from './ReuseVertexTypes';
import MetadataConfigsRootStore from '../../../../stores/GraphManagementStore/metadataConfigsStore/metadataConfigsStore';
import AddIcon from '../../../../assets/imgs/ic_add.svg';
import BlueArrowIcon from '../../../../assets/imgs/ic_arrow_blue.svg';
import WhiteCloseIcon from '../../../../assets/imgs/ic_close_white.svg';
import CloseIcon from '../../../../assets/imgs/ic_close_16.svg';
import LoadingBackIcon from '../../../../assets/imgs/ic_loading_back.svg';
import LoadingFrontIcon from '../../../../assets/imgs/ic_loading_front.svg';
import './VertexTypeList.less';
import { VertexTypeValidatePropertyIndexes } from '../../../../stores/types/GraphManagementStore/metadataConfigsStore';

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
  const [selectedRowKeys, mutateSelectedRowKeys] = useState<any[]>([]);
  const [isShowModal, switchShowModal] = useState(false);
  const [isAddProperty, switchIsAddProperty] = useState(false);
  const [isEditVertex, switchIsEditVertex] = useState(false);
  const [deletePopIndex, setDeletePopIndex] = useState<number | null>(null);
  const [
    deleteExistPopIndexInDrawer,
    setDeleteExistPopIndexInDrawer
  ] = useState<number | null>(null);
  const [
    deleteAddedPopIndexInDrawer,
    setDeleteAddedPopIndexInDrawer
  ] = useState<number | null>(null);

  const dropdownWrapperRef = useRef<HTMLDivElement>(null);
  const deleteWrapperRef = useRef<HTMLDivElement>(null);
  const deleteWrapperInDrawerRef = useRef<HTMLDivElement>(null);

  const handleSelectedTableRow = (selectedRowKeys: any[]) => {
    mutateSelectedRowKeys(selectedRowKeys);
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
    switchShowModal(false);
    mutateSelectedRowKeys([]);
    await vertexTypeStore.deleteVertexType(selectedRowKeys);

    if (vertexTypeStore.requestStatus.deleteVertexType === 'success') {
      Message.success({
        content: '已删除未使用项',
        size: 'medium',
        showCloseIcon: false
      });

      vertexTypeStore.fetchVertexTypeList();
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

  const handleOutSideClick = useCallback(
    (e: MouseEvent) => {
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
        deletePopIndex !== null &&
        deleteWrapperRef.current &&
        !deleteWrapperRef.current.contains(e.target as Element)
      ) {
        setDeletePopIndex(null);
      }
    },
    [isAddProperty, isEditVertex, deletePopIndex]
  );

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
      width: '20%',
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
      width: '10%',
      render(_: any, records: any, index: number) {
        return (
          <div>
            <span
              className="metadata-properties-manipulation"
              style={styles.manipulation}
              onClick={() => {
                vertexTypeStore.selectVertexType(index);
                vertexTypeStore.validateEditVertexType(true);
                switchIsEditVertex(true);
              }}
            >
              编辑
            </span>
            <TooltipTrigger
              tooltipShown={index === deletePopIndex}
              placement="bottom"
              tooltip={({
                arrowRef,
                tooltipRef,
                getArrowProps,
                getTooltipProps,
                placement
              }) => (
                <div
                  {...getTooltipProps({
                    ref: tooltipRef,
                    className: 'metadata-properties-tooltips'
                  })}
                >
                  <div
                    {...getArrowProps({
                      ref: arrowRef,
                      className: 'tooltip-arrow',
                      'data-placement': placement
                    })}
                  />
                  <div ref={deleteWrapperRef}>
                    {vertexTypeStore.vertexTypeUsingStatus &&
                    vertexTypeStore.vertexTypeUsingStatus[records.name] ? (
                      <p style={{ width: 200 }}>
                        当前顶点类型正在使用中，不可删除。
                      </p>
                    ) : (
                      <>
                        <p>确认删除此顶点？</p>
                        <p>删除后无法恢复，请谨慎操作。</p>
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
                            onClick={async () => {
                              setDeletePopIndex(null);
                              await vertexTypeStore.deleteVertexType([index]);

                              if (
                                vertexTypeStore.requestStatus
                                  .deleteVertexType === 'success'
                              ) {
                                Message.success({
                                  content: '已删除未使用项',
                                  size: 'medium',
                                  showCloseIcon: false
                                });

                                vertexTypeStore.fetchVertexTypeList();
                              }

                              if (
                                vertexTypeStore.requestStatus
                                  .deleteVertexType === 'failed'
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
                          </div>
                          <div
                            onClick={() => {
                              setDeletePopIndex(null);
                            }}
                          >
                            取消
                          </div>
                        </div>
                      </>
                    )}
                  </div>
                </div>
              )}
            >
              {({ getTriggerProps, triggerRef }) => (
                <span
                  {...getTriggerProps({
                    ref: triggerRef,
                    className: 'metadata-properties-manipulation',
                    async onClick() {
                      await vertexTypeStore.checkIfUsing([index]);

                      if (
                        vertexTypeStore.requestStatus.checkIfUsing === 'success'
                      ) {
                        setDeletePopIndex(index);
                      }
                    }
                  })}
                >
                  删除
                </span>
              )}
            </TooltipTrigger>
          </div>
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

  if (vertexTypeStore.currentTabStatus === 'empty') {
    return <EmptyVertxTypeHints />;
  }

  if (vertexTypeStore.currentTabStatus === 'new') {
    return <NewVertexType />;
  }

  if (vertexTypeStore.currentTabStatus === 'reuse') {
    return <ReuseVertexTypes />;
  }

  return (
    <div className="metadata-configs-content-wrapper">
      {preLoading ||
      vertexTypeStore.requestStatus.fetchVertexTypeList === 'pending' ? (
        <div className="metadata-configs-content-loading-wrapper">
          <div className="metadata-configs-content-loading-bg">
            <img
              className="metadata-configs-content-loading-back"
              src={LoadingBackIcon}
              alt="加载背景"
            />
            <img
              className="metadata-configs-content-loading-front"
              src={LoadingFrontIcon}
              alt="加载 spinner"
            />
          </div>
          <span>数据加载中...</span>
        </div>
      ) : (
        <>
          <div
            className="metadata-configs-content-header"
            style={styles.header}
          >
            <Button
              type="primary"
              size="medium"
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
              onClick={() => {
                vertexTypeStore.changeCurrentTabStatus('reuse');
              }}
            >
              复用
            </Button>
          </div>
          {selectedRowKeys.length !== 0 && (
            <div className="metadata-properties-selected-reveals">
              <div>已选{selectedRowKeys.length}项</div>
              <Button
                onClick={() => {
                  switchShowModal(true);
                  vertexTypeStore.checkIfUsing(selectedRowKeys);
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
            rowSelection={{
              selectedRowKeys,
              onChange: handleSelectedTableRow
            }}
            onSortClick={handleSortClick}
            dataSource={vertexTypeStore.vertexTypes}
            pagination={{
              hideOnSinglePage: false,
              pageNo: vertexTypeStore.vertexListPageConfig.pageNumber,
              pageSize: 10,
              showSizeChange: false,
              showPageJumper: false,
              total: vertexTypeStore.vertexListPageConfig.pageTotal,
              onPageNoChange: (e: React.ChangeEvent<HTMLSelectElement>) => {
                vertexTypeStore.mutatePageNumber(Number(e.target.value));
                vertexTypeStore.fetchVertexTypeList();
              }
            }}
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
              <div className="metadata-properties-modal-description">
                使用中顶点不可删除，确认删除以下未使用顶点？
              </div>
              <Table
                columns={[
                  {
                    title: '顶点名称',
                    dataIndex: 'name',
                    render(text: string, records: Record<string, any>) {
                      return (
                        <span
                          style={{ color: records.status ? '#999' : '#333' }}
                        >
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
                dataSource={selectedRowKeys
                  .map((rowNumber: number) => {
                    // data in selectedRowKeys[index] could be non-exist in circustance that response data length don't match
                    // so pointer(index) could out of bounds
                    if (!isUndefined(vertexTypeStore.vertexTypes[rowNumber])) {
                      const name = vertexTypeStore.vertexTypes[rowNumber].name;

                      return {
                        name,
                        status:
                          vertexTypeStore.vertexTypeUsingStatus !== null
                            ? vertexTypeStore.vertexTypeUsingStatus[name]
                            : true
                      };
                    }

                    return {
                      name: '',
                      status: false
                    };
                  })
                  .filter(({ name }) => name !== '')}
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
                disabled={isEditVertex && !vertexTypeStore.isEditReady}
                onClick={async () => {
                  if (!isEditVertex) {
                    switchIsEditVertex(true);
                    vertexTypeStore.validateEditVertexType();
                  } else {
                    await vertexTypeStore.updateVertexType();

                    if (
                      vertexTypeStore.requestStatus.updateVertexType ===
                      'failed'
                    ) {
                      Message.error({
                        content: vertexTypeStore.errorMessage,
                        size: 'medium',
                        showCloseIcon: false
                      });

                      return;
                    }

                    if (
                      vertexTypeStore.requestStatus.updateVertexType ===
                      'success'
                    ) {
                      Message.success({
                        content: '修改成功',
                        size: 'medium',
                        showCloseIcon: false
                      });
                    }

                    switchIsEditVertex(false);
                    vertexTypeStore.selectVertexType(null);
                    vertexTypeStore.resetEditedSelectedVertexType();
                    vertexTypeStore.fetchVertexTypeList();
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
                  <div className="new-vertex-type-options-colors">
                    <Select
                      width={66}
                      size="medium"
                      showSearch={false}
                      disabled={!isEditVertex}
                      value={
                        vertexTypeStore.editedSelectedVertexType.style.color !==
                        null
                          ? vertexTypeStore.editedSelectedVertexType.style.color.toLowerCase()
                          : vertexTypeStore.selectedVertexType!.style.color!.toLowerCase()
                      }
                      onChange={(value: string) => {
                        vertexTypeStore.mutateEditedSelectedVertexType({
                          ...vertexTypeStore.editedSelectedVertexType,
                          style: {
                            color: value,
                            icon: null
                          }
                        });
                      }}
                    >
                      {vertexTypeStore.colorSchemas.map((color: string) => (
                        <Select.Option value={color} key={color}>
                          <div
                            className="new-vertex-type-options-color"
                            style={{
                              background: color,
                              marginTop: 6
                            }}
                          ></div>
                        </Select.Option>
                      ))}
                    </Select>
                  </div>
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
                            property =>
                              vertexTypeStore.selectedVertexType!.properties.find(
                                ({ name }) => name === property.name
                              ) === undefined
                          )
                          .map(property => (
                            <div key={property.name}>
                              <span>
                                <Checkbox
                                  checked={
                                    [
                                      ...vertexTypeStore.addedPropertiesInSelectedVertextType
                                    ].findIndex(
                                      propertyIndex =>
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
                                        ].map(propertyName => {
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
                    <span>ID策略：</span>
                  </div>
                  {
                    IDStrategyMappings[
                      vertexTypeStore.selectedVertexType!.id_strategy
                    ]
                  }
                </div>
                <div className={metadataDrawerOptionClass}>
                  <div className="metadata-drawer-options-name">
                    <span>主键属性：</span>
                  </div>
                  {vertexTypeStore.selectedVertexType!.primary_keys.join(';')}
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
                    checked={
                      vertexTypeStore.selectedVertexType!.open_label_index
                    }
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
                        propertyIndex =>
                          isUndefined(
                            vertexTypeStore.editedSelectedVertexType.remove_property_indexes.find(
                              removedPropertyName =>
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
                                  .map(
                                    (field, index) => index + 1 + '.' + field
                                  )
                                  .join(';')}
                              </span>

                              {isEditVertex && (
                                <TooltipTrigger
                                  tooltipShown={
                                    index === deleteExistPopIndexInDrawer
                                  }
                                  placement="bottom-end"
                                  tooltip={({
                                    arrowRef,
                                    tooltipRef,
                                    getArrowProps,
                                    getTooltipProps,
                                    placement
                                  }) => (
                                    <div
                                      {...getTooltipProps({
                                        ref: tooltipRef,
                                        className:
                                          'metadata-properties-tooltips',
                                        style: { zIndex: 1041 }
                                      })}
                                    >
                                      <div
                                        {...getArrowProps({
                                          ref: arrowRef,
                                          className: 'tooltip-arrow',
                                          'data-placement': placement
                                        })}
                                      />
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

                                              setDeleteExistPopIndexInDrawer(
                                                null
                                              );
                                              vertexTypeStore.validateEditVertexType(
                                                true
                                              );
                                            }}
                                          >
                                            确认
                                          </div>
                                          <div
                                            onClick={() => {
                                              setDeleteExistPopIndexInDrawer(
                                                null
                                              );
                                            }}
                                          >
                                            取消
                                          </div>
                                        </div>
                                      </div>
                                    </div>
                                  )}
                                >
                                  {({ getTriggerProps, triggerRef }) => (
                                    <img
                                      {...getTriggerProps({
                                        ref: triggerRef,
                                        src: CloseIcon,
                                        alt: 'close',
                                        style: { cursor: 'pointer' },
                                        onClick() {
                                          setDeleteExistPopIndexInDrawer(index);
                                        }
                                      })}
                                    />
                                  )}
                                </TooltipTrigger>
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
                                      ] as VertexTypeValidatePropertyIndexes)
                                        .name
                                    : ''
                                }
                                value={name}
                                onChange={(e: any) => {
                                  const propertyIndexEntities = cloneDeep(
                                    vertexTypeStore.editedSelectedVertexType
                                      .append_property_indexes
                                  );

                                  propertyIndexEntities[index].name = e.value;

                                  vertexTypeStore.mutateEditedSelectedVertexType(
                                    {
                                      ...vertexTypeStore.editedSelectedVertexType,
                                      append_property_indexes: propertyIndexEntities
                                    }
                                  );
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

                                  vertexTypeStore.mutateEditedSelectedVertexType(
                                    {
                                      ...vertexTypeStore.editedSelectedVertexType,
                                      append_property_indexes: propertyIndexEntities
                                    }
                                  );

                                  vertexTypeStore.validateEditVertexType();
                                }}
                              >
                                <Select.Option
                                  value="SECONDARY"
                                  key="SECONDARY"
                                >
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
                                value={fields}
                                onChange={(value: string | string[]) => {
                                  const propertyIndexEntities = cloneDeep(
                                    vertexTypeStore.editedSelectedVertexType
                                      .append_property_indexes
                                  );

                                  if (Array.isArray(value)) {
                                    propertyIndexEntities[index].fields = value;
                                  } else {
                                    propertyIndexEntities[index].fields = [
                                      value
                                    ];
                                  }

                                  vertexTypeStore.mutateEditedSelectedVertexType(
                                    {
                                      ...vertexTypeStore.editedSelectedVertexType,
                                      append_property_indexes: propertyIndexEntities
                                    }
                                  );

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
                                      property =>
                                        !vertexTypeStore.selectedVertexType!.primary_keys.includes(
                                          property.name
                                        )
                                    )
                                    .map(property => {
                                      const order = vertexTypeStore.editedSelectedVertexType.append_property_indexes[
                                        index
                                      ].fields.findIndex(
                                        name => name === property.name
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
                                  vertexTypeStore
                                    .selectedVertexType!.properties.concat(
                                      vertexTypeStore.editedSelectedVertexType
                                        .append_properties
                                    )
                                    .filter(property => {
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
                                    .filter(property => {
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

                              <TooltipTrigger
                                tooltipShown={
                                  index === deleteAddedPopIndexInDrawer
                                }
                                placement="bottom-end"
                                tooltip={({
                                  arrowRef,
                                  tooltipRef,
                                  getArrowProps,
                                  getTooltipProps,
                                  placement
                                }) => (
                                  <div
                                    {...getTooltipProps({
                                      ref: tooltipRef,
                                      className: 'metadata-properties-tooltips',
                                      style: { zIndex: 1041 }
                                    })}
                                  >
                                    <div
                                      {...getArrowProps({
                                        ref: arrowRef,
                                        className: 'tooltip-arrow',
                                        'data-placement': placement
                                      })}
                                    />
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

                                            appendPropertyIndexes.splice(
                                              index,
                                              1
                                            );

                                            vertexTypeStore.mutateEditedSelectedVertexType(
                                              {
                                                ...vertexTypeStore.editedSelectedVertexType,
                                                append_property_indexes: appendPropertyIndexes
                                              }
                                            );

                                            setDeleteAddedPopIndexInDrawer(
                                              null
                                            );
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
                                            setDeleteAddedPopIndexInDrawer(
                                              null
                                            );
                                          }}
                                        >
                                          取消
                                        </div>
                                      </div>
                                    </div>
                                  </div>
                                )}
                              >
                                {({ getTriggerProps, triggerRef }) => (
                                  <img
                                    {...getTriggerProps({
                                      ref: triggerRef,
                                      src: CloseIcon,
                                      alt: 'close',
                                      style: { cursor: 'pointer' },
                                      onClick() {
                                        setDeleteAddedPopIndexInDrawer(index);
                                      }
                                    })}
                                  />
                                )}
                              </TooltipTrigger>
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
                          color: vertexTypeStore.isEditReady
                            ? '#2b65ff'
                            : '#999'
                        }}
                      >
                        新增一组
                      </div>
                    )}
                  </div>
                </div>
              </div>
            )}
          </Drawer>
        </>
      )}
    </div>
  );
});

const EmptyVertxTypeHints: React.FC = observer(() => {
  const { vertexTypeStore } = useContext(MetadataConfigsRootStore);

  return (
    <div
      className="metadata-configs-content-wrapper"
      style={{
        height: 'calc(100vh - 201px)',
        display: 'flex',
        justifyContent: 'center',
        alignItems: 'center'
      }}
    >
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
    </div>
  );
});

export default VertexTypeList;
