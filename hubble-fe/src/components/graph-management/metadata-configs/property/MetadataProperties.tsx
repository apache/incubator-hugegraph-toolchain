import React, {
  useContext,
  useState,
  useEffect,
  useRef,
  useCallback
} from 'react';
import { observer } from 'mobx-react';
import { isUndefined } from 'lodash-es';
import { Input, Button, Table, Modal, Select, Message } from '@baidu/one-ui';
import Highlighter from 'react-highlight-words';
import TooltipTrigger from 'react-popper-tooltip';

import DataAnalyzeStore from '../../../../stores/GraphManagementStore/dataAnalyzeStore';
import MetadataConfigsRootStore from '../../../../stores/GraphManagementStore/metadataConfigsStore/metadataConfigsStore';
import AddIcon from '../../../../assets/imgs/ic_add.svg';
import CloseIcon from '../../../../assets/imgs/ic_close_16.svg';
import WhiteCloseIcon from '../../../../assets/imgs/ic_close_white.svg';
import LoadingBackIcon from '../../../../assets/imgs/ic_loading_back.svg';
import LoadingFrontIcon from '../../../../assets/imgs/ic_loading_front.svg';
import './MetadataProperties.less';
import ReuseProperties from './ReuseProperties';

const styles = {
  marginLeft: '12px',
  width: 78
};

const dataTypeOptions = [
  'string',
  'boolean',
  'byte',
  'int',
  'long',
  'float',
  'double',
  'date',
  'uuid',
  'blob'
];

const cardinalityOptions = ['single', 'list', 'set'];

const MetadataProperties: React.FC = observer(() => {
  const dataAnalyzeStore = useContext(DataAnalyzeStore);
  const metadataConfigsRootStore = useContext(MetadataConfigsRootStore);
  const { metadataPropertyStore, graphViewStore } = metadataConfigsRootStore;
  const [preLoading, switchPreLoading] = useState(true);
  const [sortOrder, setSortOrder] = useState('');
  const [selectedRowKeys, mutateSelectedRowKeys] = useState<any[]>([]);
  const [isShowModal, switchShowModal] = useState(false);
  const [popIndex, setPopIndex] = useState<number | null>(null);
  const deleteWrapperRef = useRef<HTMLDivElement>(null);

  const printError = () => {
    Message.error({
      content: metadataPropertyStore.errorMessage,
      size: 'medium',
      showCloseIcon: false
    });
  };

  const handleSearchChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    metadataPropertyStore.mutateSearchWords(e.target.value);
  };

  const handleSearch = async () => {
    metadataPropertyStore.mutatePageNumber(1);
    metadataPropertyStore.switchIsSearchedStatus(true);
    await metadataPropertyStore.fetchMetadataPropertyList();

    if (
      metadataPropertyStore.requestStatus.fetchMetadataPropertyList === 'failed'
    ) {
      printError();
    }
  };

  const handleClearSearch = () => {
    metadataPropertyStore.mutateSearchWords('');
    metadataPropertyStore.mutatePageNumber(1);
    metadataPropertyStore.switchIsSearchedStatus(false);
    metadataPropertyStore.fetchMetadataPropertyList();
  };

  const handleSelectedTableRow = (selectedRowKeys: number[]) => {
    mutateSelectedRowKeys(selectedRowKeys);
  };

  const handleSortClick = () => {
    switchPreLoading(true);

    if (sortOrder === 'descend') {
      metadataPropertyStore.mutatePageSort('asc');
      setSortOrder('ascend');
    } else {
      metadataPropertyStore.mutatePageSort('desc');
      setSortOrder('descend');
    }

    metadataPropertyStore.fetchMetadataPropertyList();
  };

  const handlePageChange = async (e: React.ChangeEvent<HTMLSelectElement>) => {
    metadataPropertyStore.mutatePageNumber(Number(e.target.value));
    await metadataPropertyStore.fetchMetadataPropertyList();

    if (
      metadataPropertyStore.requestStatus.fetchMetadataPropertyList === 'failed'
    ) {
      printError();
    }
  };

  const batchDeleteProperties = async () => {
    switchShowModal(false);
    mutateSelectedRowKeys([]);
    await metadataPropertyStore.deleteMetadataProperty(selectedRowKeys);

    if (
      metadataPropertyStore.requestStatus.deleteMetadataProperty === 'success'
    ) {
      Message.success({
        content: '已删除未使用项',
        size: 'medium',
        showCloseIcon: false
      });

      metadataPropertyStore.fetchMetadataPropertyList();
      return;
    }

    if (
      metadataPropertyStore.requestStatus.deleteMetadataProperty === 'failed'
    ) {
      printError();
    }
  };

  // hack: need to call @observable at here to dispatch re-render by mobx
  // since @action in onBlur() in <Input /> doesn't dispatch re-render
  metadataPropertyStore.validateNewPropertyErrorMessage.name.toLowerCase();

  const columnConfigs = [
    {
      title: '属性名称',
      dataIndex: 'name',
      width: '45%',
      sorter: true,
      sortOrder,
      render(text: string, records: any, index: number) {
        if (metadataPropertyStore.isCreateNewProperty === true && index === 0) {
          return (
            <Input
              size="medium"
              width={320}
              maxLen={128}
              placeholder="允许出现中英文、数字、下划线"
              errorLocation="layer"
              errorMessage={
                metadataPropertyStore.validateNewPropertyErrorMessage.name
              }
              value={metadataPropertyStore.newMetadataProperty.name}
              onChange={(e: any) => {
                metadataPropertyStore.mutateNewProperty({
                  ...metadataPropertyStore.newMetadataProperty,
                  name: e.value
                });
              }}
              originInputProps={{
                autoFocus: 'autoFocus',
                onBlur() {
                  metadataPropertyStore.validateNewProperty();
                }
              }}
            />
          );
        }

        return (
          <div
            className="no-line-break"
            style={{
              width: 360
            }}
          >
            {metadataPropertyStore.isSearched.status ? (
              <Highlighter
                highlightClassName="metadata-properties-search-highlights"
                searchWords={[metadataPropertyStore.isSearched.value]}
                autoEscape={true}
                textToHighlight={text}
                // caution: no title property on type defination
                // @ts-ignore
                title={text}
              />
            ) : (
              <span title={text}>{text}</span>
            )}
          </div>
        );
      }
    },
    {
      title: '数据类型',
      dataIndex: 'data_type',
      width: '20%',
      render(text: string, records: any, index: number) {
        if (metadataPropertyStore.isCreateNewProperty === true && index === 0) {
          return (
            <Select
              options={dataTypeOptions}
              size="medium"
              trigger="click"
              value={metadataPropertyStore.newMetadataProperty.data_type}
              width={126}
              onChange={(value: string) => {
                metadataPropertyStore.mutateNewProperty({
                  ...metadataPropertyStore.newMetadataProperty,
                  data_type: value
                });
              }}
              dropdownClassName="data-analyze-sidebar-select"
            >
              {dataTypeOptions.map(option => {
                return (
                  <Select.Option value={option} key={option}>
                    {option}
                  </Select.Option>
                );
              })}
            </Select>
          );
        }

        const realText = text === 'TEXT' ? 'string' : text.toLowerCase();

        return (
          <div className="no-line-break">
            <span title={realText}>{realText}</span>
          </div>
        );
      }
    },
    {
      title: '基数',
      dataIndex: 'cardinality',
      width: '20%',
      render(text: string, records: any, index: number) {
        if (metadataPropertyStore.isCreateNewProperty === true && index === 0) {
          return (
            <Select
              options={cardinalityOptions}
              size="medium"
              trigger="click"
              value={metadataPropertyStore.newMetadataProperty.cardinality}
              width={126}
              onChange={(value: string) => {
                metadataPropertyStore.mutateNewProperty({
                  ...metadataPropertyStore.newMetadataProperty,
                  cardinality: value
                });
              }}
              dropdownClassName="data-analyze-sidebar-select"
            >
              {cardinalityOptions.map(option => {
                return (
                  <Select.Option value={option} key={option}>
                    {option}
                  </Select.Option>
                );
              })}
            </Select>
          );
        }

        return (
          <div className="no-line-break">
            <span title={text.toLowerCase()}>{text.toLowerCase()}</span>
          </div>
        );
      }
    },
    {
      title: '操作',
      dataIndex: 'manipulation',
      width: '15%',
      align: 'right',
      render(_: any, records: any, index: number) {
        if (metadataPropertyStore.isCreateNewProperty === true && index === 0) {
          return (
            <div>
              <span
                className="metadata-properties-manipulation"
                style={{
                  marginRight: 16,
                  color: metadataPropertyStore.isCreatedReady
                    ? '#2b65ff'
                    : '#999',
                  cursor: 'pointer'
                }}
                onClick={async () => {
                  metadataPropertyStore.validateNewProperty();

                  if (!metadataPropertyStore.isCreatedReady) {
                    return;
                  }

                  metadataPropertyStore.switchIsCreateNewProperty(false);
                  await metadataPropertyStore.addMetadataProperty();

                  if (
                    metadataPropertyStore.requestStatus.addMetadataProperty ===
                    'success'
                  ) {
                    Message.success({
                      content: '创建成功',
                      size: 'medium',
                      showCloseIcon: false
                    });
                  }

                  if (
                    metadataPropertyStore.requestStatus.addMetadataProperty ===
                    'failed'
                  ) {
                    Message.error({
                      content: metadataPropertyStore.errorMessage,
                      size: 'medium',
                      showCloseIcon: false
                    });
                  }

                  metadataPropertyStore.fetchMetadataPropertyList();
                  metadataPropertyStore.resetNewProperties();
                }}
              >
                创建
              </span>
              <span
                className="metadata-properties-manipulation"
                onClick={() => {
                  metadataPropertyStore.switchIsCreateNewProperty(false);
                  metadataPropertyStore.resetNewProperties();
                  metadataPropertyStore.resetValidateNewProperty();

                  if (metadataPropertyStore.metadataProperties.length === 0) {
                    metadataPropertyStore.changeCurrentTabStatus('empty');
                  }
                }}
              >
                取消
              </span>
            </div>
          );
        }

        return (
          <TooltipTrigger
            tooltipShown={index === popIndex}
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
                  {metadataPropertyStore.metadataPropertyUsingStatus &&
                  metadataPropertyStore.metadataPropertyUsingStatus[
                    records.name
                  ] ? (
                    <p style={{ width: 200 }}>
                      当前属性数据正在使用中，不可删除。
                    </p>
                  ) : (
                    <>
                      <p>确认删除此属性？</p>
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
                            setPopIndex(null);

                            await metadataPropertyStore.deleteMetadataProperty([
                              index
                            ]);

                            if (
                              metadataPropertyStore.requestStatus
                                .deleteMetadataProperty === 'success'
                            ) {
                              Message.success({
                                content: '已删除未使用项',
                                size: 'medium',
                                showCloseIcon: false
                              });

                              metadataPropertyStore.fetchMetadataPropertyList();
                            }

                            if (
                              metadataPropertyStore.requestStatus
                                .deleteMetadataProperty === 'failed'
                            ) {
                              Message.error({
                                content: metadataPropertyStore.errorMessage,
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
                            setPopIndex(null);
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
                    await metadataPropertyStore.checkIfUsing([index]);

                    if (
                      metadataPropertyStore.requestStatus.checkIfUsing ===
                      'success'
                    ) {
                      setPopIndex(index);
                    }
                  }
                })}
              >
                删除
              </span>
            )}
          </TooltipTrigger>
        );
      }
    }
  ];

  const handleOutSideClick = useCallback(
    (e: MouseEvent) => {
      if (
        popIndex !== null &&
        deleteWrapperRef &&
        deleteWrapperRef.current &&
        !deleteWrapperRef.current.contains(e.target as Element)
      ) {
        setPopIndex(null);
      }
    },
    [deleteWrapperRef, popIndex]
  );

  useEffect(() => {
    setTimeout(() => {
      switchPreLoading(false);
    }, 500);
  }, [sortOrder]);

  useEffect(() => {
    if (metadataConfigsRootStore.currentId !== null) {
      metadataPropertyStore.fetchMetadataPropertyList();
    }

    return () => {
      metadataPropertyStore.dispose();
    };
  }, [
    metadataPropertyStore,
    metadataConfigsRootStore.currentId,
    graphViewStore
  ]);

  // these would be called before <MetadataConfigs /> rendered, pre-load some data here
  useEffect(() => {
    dataAnalyzeStore.fetchAllNodeStyle();
    dataAnalyzeStore.fetchAllEdgeStyle();
  }, [dataAnalyzeStore]);

  useEffect(() => {
    document.addEventListener('click', handleOutSideClick, false);

    return () => {
      document.removeEventListener('click', handleOutSideClick, false);
    };
  }, [handleOutSideClick]);

  if (metadataPropertyStore.currentTabStatus === 'empty') {
    return <EmptyPropertyHints />;
  }

  if (metadataPropertyStore.currentTabStatus === 'reuse') {
    return <ReuseProperties />;
  }

  return (
    <div className="metadata-configs-content-wrapper metadata-properties">
      {preLoading ||
      metadataPropertyStore.requestStatus.fetchMetadataPropertyList ===
        'pending' ? (
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
          <div className="metadata-configs-content-header">
            <Input.Search
              size="medium"
              width={200}
              placeholder="请输入搜索关键字"
              value={metadataPropertyStore.searchWords}
              onChange={handleSearchChange}
              onSearch={handleSearch}
              onClearClick={handleClearSearch}
              isShowDropDown={false}
              disabled={selectedRowKeys.length !== 0}
            />
            <Button
              type="primary"
              size="medium"
              style={styles}
              disabled={
                metadataPropertyStore.isCreateNewProperty ||
                selectedRowKeys.length !== 0
              }
              onClick={() => {
                metadataPropertyStore.switchIsCreateNewProperty(true);
              }}
            >
              创建
            </Button>
            <Button
              size="medium"
              style={styles}
              disabled={metadataPropertyStore.isCreateNewProperty}
              onClick={() => {
                mutateSelectedRowKeys([]);
                metadataPropertyStore.changeCurrentTabStatus('reuse');
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
                  metadataPropertyStore.checkIfUsing(selectedRowKeys);
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
            dataSource={
              metadataPropertyStore.isCreateNewProperty
                ? metadataPropertyStore.reunionMetadataProperty
                : metadataPropertyStore.metadataProperties
            }
            pagination={{
              hideOnSinglePage: false,
              pageNo:
                metadataPropertyStore.metadataPropertyPageConfig.pageNumber,
              pageSize: 10,
              showSizeChange: false,
              showPageJumper: false,
              total: metadataPropertyStore.metadataPropertyPageConfig.pageTotal,
              onPageNoChange: handlePageChange
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
                使用中属性不可删除，确认删除以下未使用属性？
              </div>
              <Table
                columns={[
                  {
                    title: '属性名称',
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
                    if (
                      !isUndefined(
                        metadataPropertyStore.metadataProperties[rowNumber]
                      )
                    ) {
                      const name =
                        metadataPropertyStore.metadataProperties[rowNumber]
                          .name;

                      return {
                        name,
                        status:
                          metadataPropertyStore.metadataPropertyUsingStatus !==
                            null &&
                          metadataPropertyStore.metadataPropertyUsingStatus[
                            name
                          ]
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
        </>
      )}
    </div>
  );
});

const EmptyPropertyHints: React.FC = observer(() => {
  const { metadataPropertyStore } = useContext(MetadataConfigsRootStore);

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
          您暂时还没有任何属性，立即创建
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
            style={{ width: 112, marginRight: 16 }}
            onClick={() => {
              metadataPropertyStore.switchIsCreateNewProperty(true);
              metadataPropertyStore.changeCurrentTabStatus('list');
            }}
          >
            创建属性
          </Button>
          <Button
            size="large"
            style={{ width: 144 }}
            onClick={() => {
              metadataPropertyStore.changeCurrentTabStatus('reuse');
            }}
          >
            复用已有属性
          </Button>
        </div>
      </div>
    </div>
  );
});

export default MetadataProperties;
