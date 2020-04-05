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
import { isEmpty, isUndefined, cloneDeep } from 'lodash-es';
import TooltipTrigger from 'react-popper-tooltip';

import MetadataConfigsRootStore from '../../../../stores/GraphManagementStore/metadataConfigsStore/metadataConfigsStore';
import AddIcon from '../../../../assets/imgs/ic_add.svg';
import BlueArrowIcon from '../../../../assets/imgs/ic_arrow_blue.svg';
import WhiteCloseIcon from '../../../../assets/imgs/ic_close_white.svg';
import CloseIcon from '../../../../assets/imgs/ic_close_16.svg';
import LoadingBackIcon from '../../../../assets/imgs/ic_loading_back.svg';
import LoadingFrontIcon from '../../../../assets/imgs/ic_loading_front.svg';
import './EdgeTypeList.less';
import NewEdgeType from './NewEdgeType';
import ReuseEdgeTypes from './ReuseEdgeTypes';
import { EdgeTypeValidatePropertyIndexes } from '../../../../stores/types/GraphManagementStore/metadataConfigsStore';

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

const propertyIndexTypeMappings: Record<string, string> = {
  SECONDARY: '二级索引',
  RANGE: '范围索引',
  SEARCH: '全文索引'
};

const EdgeTypeList: React.FC = observer(() => {
  const metadataConfigsRootStore = useContext(MetadataConfigsRootStore);
  const { metadataPropertyStore, edgeTypeStore } = metadataConfigsRootStore;
  const [preLoading, switchPreLoading] = useState(true);
  const [sortOrder, setSortOrder] = useState('');
  const [selectedRowKeys, mutateSelectedRowKeys] = useState<any[]>([]);
  const [isShowModal, switchShowModal] = useState(false);
  const [isAddProperty, switchIsAddProperty] = useState(false);
  const [isEditEdge, switchIsEditEdge] = useState(false);
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
    mutateSelectedRowKeys([]);
    await edgeTypeStore.deleteEdgeType(selectedRowKeys);

    if (edgeTypeStore.requestStatus.deleteEdgeType === 'success') {
      Message.success({
        content: '已删除未使用项',
        size: 'medium',
        showCloseIcon: false
      });

      edgeTypeStore.fetchEdgeTypeList();
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

  const handleOutSideClick = useCallback(
    (e: MouseEvent) => {
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
        deletePopIndex !== null &&
        deleteWrapperRef.current &&
        !deleteWrapperRef.current.contains(e.target as Element)
      ) {
        setDeletePopIndex(null);
      }
    },
    [isAddProperty, isEditEdge, deletePopIndex]
  );

  const columnConfigs = [
    {
      title: '边类型名称',
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
            }}
          >
            {text}
          </div>
        );
      }
    },
    {
      title: '起点类型',
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
      title: '终点类型',
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
      title: '关联属性',
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
      title: '区分键',
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
                edgeTypeStore.selectEdgeType(index);
                edgeTypeStore.validateEditEdgeType(true);
                switchIsEditEdge(true);
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
                    <p>确认删除此边类型？</p>
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
                          await edgeTypeStore.deleteEdgeType([index]);

                          if (
                            edgeTypeStore.requestStatus.deleteEdgeType ===
                            'success'
                          ) {
                            Message.success({
                              content: '已删除未使用项',
                              size: 'medium',
                              showCloseIcon: false
                            });

                            edgeTypeStore.fetchEdgeTypeList();
                          }

                          if (
                            edgeTypeStore.requestStatus.deleteEdgeType ===
                            'failed'
                          ) {
                            Message.error({
                              content: edgeTypeStore.errorMessage,
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
                  </div>
                </div>
              )}
            >
              {({ getTriggerProps, triggerRef }) => (
                <span
                  {...getTriggerProps({
                    ref: triggerRef,
                    className: 'metadata-properties-manipulation',
                    onClick() {
                      setDeletePopIndex(index);
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
    'metadata-drawer-options-disabled': isEditEdge
  });

  useEffect(() => {
    setTimeout(() => {
      switchPreLoading(false);
    }, 500);
  }, [sortOrder]);

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

  if (edgeTypeStore.currentTabStatus === 'empty') {
    return <EmptyEdgeTypeHints />;
  }

  if (edgeTypeStore.currentTabStatus === 'new') {
    return <NewEdgeType />;
  }

  if (edgeTypeStore.currentTabStatus === 'reuse') {
    return <ReuseEdgeTypes />;
  }

  return (
    <div className="metadata-configs-content-wrapper">
      {preLoading ||
      edgeTypeStore.requestStatus.fetchEdgeTypeList === 'pending' ? (
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
                edgeTypeStore.changeCurrentTabStatus('new');
              }}
            >
              创建
            </Button>
            <Button
              size="medium"
              style={styles.button}
              onClick={() => {
                edgeTypeStore.changeCurrentTabStatus('reuse');
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
            dataSource={edgeTypeStore.edgeTypes}
            pagination={{
              hideOnSinglePage: false,
              pageNo: edgeTypeStore.edgeTypeListPageConfig.pageNumber,
              pageSize: 10,
              showSizeChange: false,
              showPageJumper: false,
              total: edgeTypeStore.edgeTypeListPageConfig.pageTotal,
              onPageNoChange: (e: React.ChangeEvent<HTMLSelectElement>) => {
                edgeTypeStore.mutatePageNumber(Number(e.target.value));
                edgeTypeStore.fetchEdgeTypeList();
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
                使用中边不可删除，确认删除以下未使用边？
              </div>
              <Table
                columns={[
                  {
                    title: '边名称',
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
                  }
                ]}
                dataSource={selectedRowKeys
                  .map((rowNumber: number) => {
                    // data in selectedRowKeys[index] could be non-exist in circustance that response data length don't match
                    // so pointer(index) could out of bounds
                    if (!isUndefined(edgeTypeStore.edgeTypes[rowNumber])) {
                      const name = edgeTypeStore.edgeTypes[rowNumber].name;

                      return {
                        name
                      };
                    }

                    return {
                      name: ''
                    };
                  })
                  .filter(({ name }) => name !== '')}
                pagination={false}
              />
            </div>
          </Modal>
          <Drawer
            title={isEditEdge ? '编辑边类型' : '边类型详情'}
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
                disabled={isEditEdge && !edgeTypeStore.isEditReady}
                onClick={async () => {
                  if (!isEditEdge) {
                    switchIsEditEdge(true);
                    edgeTypeStore.validateEditEdgeType();
                  } else {
                    await edgeTypeStore.updateEdgeType();

                    if (
                      edgeTypeStore.requestStatus.updateEdgeType === 'failed'
                    ) {
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
                      Message.success({
                        content: '修改成功',
                        size: 'medium',
                        showCloseIcon: false
                      });
                    }

                    switchIsEditEdge(false);
                    edgeTypeStore.selectEdgeType(null);
                    edgeTypeStore.resetEditedSelectedEdgeType();
                    edgeTypeStore.fetchEdgeTypeList();
                  }
                }}
              >
                {isEditEdge ? '保存' : '编辑'}
              </Button>,
              <Button
                size="medium"
                style={{ width: 60 }}
                onClick={handleCloseDrawer}
              >
                关闭
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
                    基础信息
                  </div>
                  <div
                    className={metadataDrawerOptionClass}
                    style={{ alignItems: 'center' }}
                  >
                    <div className="metadata-drawer-options-name">
                      <span>边类型名称：</span>
                    </div>
                    {edgeTypeStore.selectedEdgeType!.name}
                    <div className="new-vertex-type-options-colors">
                      <Select
                        width={66}
                        size="medium"
                        showSearch={false}
                        disabled={!isEditEdge}
                        value={
                          edgeTypeStore.editedSelectedEdgeType.style.color !==
                          null
                            ? edgeTypeStore.editedSelectedEdgeType.style.color.toLowerCase()
                            : edgeTypeStore.selectedEdgeType!.style.color!.toLowerCase()
                        }
                        onChange={(value: string) => {
                          edgeTypeStore.mutateEditedSelectedEdgeType({
                            ...edgeTypeStore.editedSelectedEdgeType,
                            style: {
                              color: value,
                              icon: null
                            }
                          });
                        }}
                      >
                        {edgeTypeStore.colorSchemas.map((color: string) => (
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
                  <div className={metadataDrawerOptionClass}>
                    <div className="metadata-drawer-options-name">
                      <span>起点类型：</span>
                    </div>
                    {edgeTypeStore.selectedEdgeType!.source_label}
                  </div>
                  <div className={metadataDrawerOptionClass}>
                    <div className="metadata-drawer-options-name">
                      <span>终点类型：</span>
                    </div>
                    {edgeTypeStore.selectedEdgeType!.target_label}
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
                      {edgeTypeStore.selectedEdgeType!.properties.map(
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
                          <span style={{ marginRight: 4 }}>添加属性</span>
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
                              property =>
                                edgeTypeStore.selectedEdgeType!.properties.find(
                                  ({ name }) => name === property.name
                                ) === undefined
                            )
                            .map(property => (
                              <div key={property.name}>
                                <span>
                                  <Checkbox
                                    checked={
                                      [
                                        ...edgeTypeStore.addedPropertiesInSelectedEdgeType
                                      ].findIndex(
                                        propertyIndex =>
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

                                      edgeTypeStore.mutateEditedSelectedEdgeType(
                                        {
                                          ...edgeTypeStore.editedSelectedEdgeType,
                                          append_properties: [
                                            ...addedPropertiesInSelectedVertextType
                                          ].map(propertyName => {
                                            const currentProperty = edgeTypeStore.newEdgeType.properties.find(
                                              ({ name }) =>
                                                name === propertyName
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
                      <span>允许多次连接：</span>
                    </div>
                    <Switch
                      checkedChildren="开"
                      unCheckedChildren="关"
                      checked={edgeTypeStore.selectedEdgeType!.link_multi_times}
                      size="large"
                      disabled
                    />
                  </div>
                  <div className={metadataDrawerOptionClass}>
                    <div className="metadata-drawer-options-name">
                      <span>区分键属性：</span>
                    </div>
                    {edgeTypeStore.selectedEdgeType!.sort_keys.join(';')}
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
                      checked={edgeTypeStore.selectedEdgeType!.open_label_index}
                      size="large"
                      disabled
                    />
                  </div>
                  <div className="metadata-drawer-options">
                    <div className="metadata-drawer-options-name">
                      <span>属性索引：</span>
                    </div>
                    <div className="metadata-drawer-options-list">
                      {(edgeTypeStore.selectedEdgeType!.property_indexes
                        .length !== 0 ||
                        edgeTypeStore.editedSelectedEdgeType
                          .append_property_indexes.length !== 0) && (
                        <div className="metadata-drawer-options-list-row metadata-drawer-options-list-row-normal">
                          <span>索引名称</span>
                          <span>索引类型</span>
                          <span>属性</span>
                        </div>
                      )}
                      {edgeTypeStore
                        .selectedEdgeType!.property_indexes.filter(
                          propertyIndex =>
                            isUndefined(
                              edgeTypeStore.editedSelectedEdgeType.remove_property_indexes.find(
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
                                                const removedPropertyIndex = cloneDeep(
                                                  edgeTypeStore
                                                    .editedSelectedEdgeType
                                                    .remove_property_indexes
                                                );

                                                removedPropertyIndex.push(
                                                  edgeTypeStore.selectedEdgeType!
                                                    .property_indexes[index]
                                                    .name
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
                                            setDeleteExistPopIndexInDrawer(
                                              index
                                            );
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
                                  placeholder="索引名称"
                                  errorLocation="layer"
                                  errorMessage={
                                    edgeTypeStore
                                      .validateEditEdgeTypeErrorMessage
                                      .propertyIndexes.length !== 0
                                      ? (edgeTypeStore
                                          .validateEditEdgeTypeErrorMessage
                                          .propertyIndexes[
                                          index
                                        ] as EdgeTypeValidatePropertyIndexes)
                                          .name
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
                                  placeholder="请选择索引类型"
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
                                    type === 'SECONDARY'
                                      ? 'multiple'
                                      : 'default'
                                  }
                                  placeholder="请选择属性"
                                  size="medium"
                                  showSearch={false}
                                  value={fields}
                                  onChange={(value: string | string[]) => {
                                    const propertyIndexEntities = cloneDeep(
                                      edgeTypeStore.editedSelectedEdgeType
                                        .append_property_indexes
                                    );

                                    if (Array.isArray(value)) {
                                      propertyIndexEntities[
                                        index
                                      ].fields = value;
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
                                      .map(property => {
                                        const order = edgeTypeStore.editedSelectedEdgeType.append_property_indexes[
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
                                    edgeTypeStore
                                      .selectedEdgeType!.properties.concat(
                                        edgeTypeStore.editedSelectedEdgeType
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
                                    edgeTypeStore
                                      .selectedEdgeType!.properties.concat(
                                        edgeTypeStore.editedSelectedEdgeType
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

                                              setDeleteAddedPopIndexInDrawer(
                                                null
                                              );
                                              edgeTypeStore.validateEditEdgeType(
                                                true
                                              );
                                            }}
                                          >
                                            确认
                                          </div>
                                          <div
                                            onClick={() => {
                                              edgeTypeStore.resetEditedSelectedEdgeType();
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
                            color: edgeTypeStore.isEditReady
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
              </div>
            )}
          </Drawer>
        </>
      )}
    </div>
  );
});

const EmptyEdgeTypeHints: React.FC = observer(() => {
  const { edgeTypeStore } = useContext(MetadataConfigsRootStore);

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
          您暂时还没有任何边类型，立即创建
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
            创建边类型
          </Button>
          <Button
            size="large"
            style={{ width: 144 }}
            onClick={() => {
              edgeTypeStore.changeCurrentTabStatus('reuse');
            }}
          >
            复用已有类型
          </Button>
        </div>
      </div>
    </div>
  );
});

export default EdgeTypeList;
