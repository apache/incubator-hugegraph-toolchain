import React, {
  useContext,
  useState,
  useEffect,
  useRef,
  useCallback
} from 'react';
import { observer } from 'mobx-react';
import { intersection, size, without, values } from 'lodash-es';
import { motion } from 'framer-motion';
import {
  Input,
  Button,
  Table,
  Modal,
  Select,
  Message,
  Loading
} from '@baidu/one-ui';
import Highlighter from 'react-highlight-words';

import { Tooltip, LoadingDataView } from '../../../common';
import DataAnalyzeStore from '../../../../stores/GraphManagementStore/dataAnalyzeStore/dataAnalyzeStore';
import MetadataConfigsRootStore from '../../../../stores/GraphManagementStore/metadataConfigsStore/metadataConfigsStore';

import type { MetadataProperty } from '../../../../stores/types/GraphManagementStore/metadataConfigsStore';

import AddIcon from '../../../../assets/imgs/ic_add.svg';
import CloseIcon from '../../../../assets/imgs/ic_close_16.svg';
import WhiteCloseIcon from '../../../../assets/imgs/ic_close_white.svg';
import './MetadataProperties.less';
import ReuseProperties from './ReuseProperties';

const styles = {
  button: {
    marginLeft: '12px',
    width: 78
  },
  extraMargin: {
    marginRight: 4
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

const MetadataProperties: React.FC = observer(() => {
  const dataAnalyzeStore = useContext(DataAnalyzeStore);
  const metadataConfigsRootStore = useContext(MetadataConfigsRootStore);
  const { metadataPropertyStore, graphViewStore } = metadataConfigsRootStore;
  const [preLoading, switchPreLoading] = useState(true);
  const [sortOrder, setSortOrder] = useState('');
  const [selectedRowKeys, mutateSelectedRowKeys] = useState<string[]>([]);
  const [isShowModal, switchShowModal] = useState(false);

  const isLoading =
    preLoading ||
    metadataPropertyStore.requestStatus.fetchMetadataPropertyList === 'pending';

  const currentSelectedRowKeys = intersection(
    selectedRowKeys,
    metadataPropertyStore.metadataProperties.map(({ name }) => name)
  );

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

  const handleSelectedTableRow = (
    newSelectedRowKeys: string[],
    selectedRows: MetadataProperty[]
  ) => {
    mutateSelectedRowKeys(newSelectedRowKeys);
    // mutateSelectedRowKeys(selectedRows.map(({ name }) => name));
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
    // mutateSelectedRowKeys([]);
    metadataPropertyStore.mutatePageNumber(Number(e.target.value));
    await metadataPropertyStore.fetchMetadataPropertyList();

    if (
      metadataPropertyStore.requestStatus.fetchMetadataPropertyList === 'failed'
    ) {
      printError();
    }
  };

  const batchDeleteProperties = async () => {
    if (
      values(currentSelectedRowKeys).every(
        (key) => metadataPropertyStore.metadataPropertyUsingStatus?.[key]
      )
    ) {
      Message.error({
        content: '无可删除属性',
        size: 'medium',
        showCloseIcon: false
      });

      return;
    }

    switchShowModal(false);
    // need to set a copy in store since local row key state would be cleared
    metadataPropertyStore.mutateSelectedMetadataProperyNames(
      currentSelectedRowKeys
    );
    // mutateSelectedRowKeys([]);
    await metadataPropertyStore.deleteMetadataProperty(currentSelectedRowKeys);
    // metadataPropertyStore.mutateSelectedMetadataProperyNames([]);

    if (
      metadataPropertyStore.requestStatus.deleteMetadataProperty === 'success'
    ) {
      Message.success({
        content: '删除成功',
        size: 'medium',
        showCloseIcon: false
      });

      mutateSelectedRowKeys(
        without(selectedRowKeys, ...currentSelectedRowKeys)
      );

      await metadataPropertyStore.fetchMetadataPropertyList();

      // fetch previous page data if it's empty
      if (
        metadataPropertyStore.requestStatus.fetchMetadataPropertyList ===
          'success' &&
        size(metadataPropertyStore.metadataProperties) === 0 &&
        metadataPropertyStore.metadataPropertyPageConfig.pageNumber > 1
      ) {
        metadataPropertyStore.mutatePageNumber(
          metadataPropertyStore.metadataPropertyPageConfig.pageNumber - 1
        );

        metadataPropertyStore.fetchMetadataPropertyList();
      }

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
              value={metadataPropertyStore.newMetadataProperty._name}
              onChange={(e: any) => {
                metadataPropertyStore.mutateNewProperty({
                  ...metadataPropertyStore.newMetadataProperty,
                  _name: e.value
                });

                metadataPropertyStore.validateNewProperty();
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
              {dataTypeOptions.map((option) => {
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
              {cardinalityOptions.map((option) => {
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
        return (
          <MetadataPropertiesManipulation
            propertyName={records.name}
            propertyIndex={index}
          />
        );
      }
    }
  ];

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
  // useEffect(() => {
  //   dataAnalyzeStore.fetchAllNodeStyle();
  //   dataAnalyzeStore.fetchAllEdgeStyle();
  // }, [dataAnalyzeStore]);

  if (metadataPropertyStore.currentTabStatus === 'reuse') {
    return <ReuseProperties />;
  }

  return (
    <motion.div
      initial="initial"
      animate="animate"
      exit="exit"
      variants={variants}
    >
      <div className="metadata-configs-content-wrapper metadata-properties">
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
            disabled={isLoading || size(currentSelectedRowKeys) !== 0}
          />
          <Button
            type="primary"
            size="medium"
            style={styles.button}
            disabled={
              isLoading ||
              metadataPropertyStore.isCreateNewProperty ||
              size(currentSelectedRowKeys) !== 0
            }
            onClick={() => {
              metadataPropertyStore.switchIsCreateNewProperty(true);
            }}
          >
            创建
          </Button>
          <Button
            size="medium"
            style={styles.button}
            disabled={isLoading || metadataPropertyStore.isCreateNewProperty}
            onClick={() => {
              mutateSelectedRowKeys([]);
              metadataPropertyStore.changeCurrentTabStatus('reuse');
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
                metadataPropertyStore.checkIfUsing(currentSelectedRowKeys);
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
          rowKey={(rowData: MetadataProperty) => rowData.name}
          locale={{
            emptyText: (
              <LoadingDataView
                isLoading={isLoading}
                emptyView={
                  metadataPropertyStore.isSearched.status ? (
                    <span>无结果</span>
                  ) : (
                    <EmptyPropertyHints />
                  )
                }
              />
            )
          }}
          rowSelection={{
            selectedRowKeys,
            onChange: handleSelectedTableRow
          }}
          onSortClick={handleSortClick}
          dataSource={
            isLoading
              ? []
              : metadataPropertyStore.isCreateNewProperty
              ? metadataPropertyStore.reunionMetadataProperty
              : metadataPropertyStore.metadataProperties
          }
          pagination={
            isLoading
              ? null
              : {
                  hideOnSinglePage: false,
                  pageNo:
                    metadataPropertyStore.metadataPropertyPageConfig.pageNumber,
                  pageSize: 10,
                  showSizeChange: false,
                  showPageJumper: false,
                  total:
                    metadataPropertyStore.metadataPropertyPageConfig.pageTotal,
                  onPageNoChange: handlePageChange
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
                    metadataPropertyStore.metadataPropertyUsingStatus !==
                      null &&
                    // data may have some delay which leads to no matching propety value
                    !!metadataPropertyStore.metadataPropertyUsingStatus[name]
                };
              })}
              pagination={false}
            />
          </div>
        </Modal>
      </div>
    </motion.div>
  );
});

export interface MetadataPropertiesManipulationProps {
  propertyName: string;
  propertyIndex: number;
  // allSelectedKeys: number[];
}

const MetadataPropertiesManipulation: React.FC<MetadataPropertiesManipulationProps> = observer(
  ({ propertyName, propertyIndex }) => {
    const { metadataPropertyStore } = useContext(MetadataConfigsRootStore);
    const [isPopDeleteModal, switchPopDeleteModal] = useState(false);
    const [isDeleting, switchDeleting] = useState(false);
    const deleteWrapperRef = useRef<HTMLDivElement>(null);
    const isDeleteOrBatchDeleting =
      isDeleting ||
      (metadataPropertyStore.requestStatus.deleteMetadataProperty ===
        'pending' &&
        metadataPropertyStore.selectedMetadataPropertyNames.includes(
          propertyName
        ));

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

    if (
      metadataPropertyStore.isCreateNewProperty === true &&
      propertyIndex === 0
    ) {
      return (
        <div>
          <span
            className="metadata-properties-manipulation"
            style={{
              marginRight: 16,
              color: metadataPropertyStore.isCreatedReady ? '#2b65ff' : '#999',
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
            style={styles.extraMargin}
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
      <div style={styles.deleteWrapper}>
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
              {metadataPropertyStore.metadataPropertyUsingStatus &&
              metadataPropertyStore.metadataPropertyUsingStatus[
                propertyName
              ] ? (
                <p style={{ width: 200 }}>当前属性数据正在使用中，不可删除。</p>
              ) : (
                <>
                  <p className="metadata-properties-tooltips-title">
                    确认删除此属性？
                  </p>
                  <p>确认删除此属性？删除后无法恢复，请谨慎操作</p>
                  <p>删除元数据耗时较久，详情可在任务管理中查看</p>
                  <div className="metadata-properties-tooltips-footer">
                    <Button
                      size="medium"
                      type="primary"
                      style={{ width: 60, marginRight: 12 }}
                      onClick={async () => {
                        switchPopDeleteModal(false);
                        switchDeleting(true);

                        await metadataPropertyStore.deleteMetadataProperty([
                          propertyName
                        ]);

                        switchDeleting(false);

                        if (
                          metadataPropertyStore.requestStatus
                            .deleteMetadataProperty === 'success'
                        ) {
                          Message.success({
                            content: '删除成功',
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
            className: 'metadata-properties-manipulation no-line-break',
            style: styles.extraMargin,
            title: isDeleteOrBatchDeleting ? '删除中' : '删除',
            async onClick() {
              if (isDeleteOrBatchDeleting) {
                return;
              }

              await metadataPropertyStore.checkIfUsing([propertyName]);

              if (
                metadataPropertyStore.requestStatus.checkIfUsing === 'success'
              ) {
                switchPopDeleteModal(true);
              }
            }
          }}
        >
          {isDeleteOrBatchDeleting ? '删除中' : '删除'}
        </Tooltip>
      </div>
    );
  }
);

const EmptyPropertyHints: React.FC = observer(() => {
  const { metadataPropertyStore } = useContext(MetadataConfigsRootStore);

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
  );
});

export default MetadataProperties;
