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
} from 'hubble-ui';
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
import { useTranslation } from 'react-i18next';

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
  const { t } = useTranslation();
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
        content: t('addition.message.no-property-can-delete'),
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
        content: t('addition.common.del-success'),
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
      title: t('addition.common.property-name'),
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
              placeholder={t('addition.message.edge-name-rule')}
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
      title: t('addition.common.data-type'),
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
      title: t('addition.common.cardinal-number'),
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
      title: t('addition.operate.operate'),
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
            placeholder={t('addition.message.please-enter-keywords')}
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
            {t('addition.newGraphConfig.create')}
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
                metadataPropertyStore.checkIfUsing(currentSelectedRowKeys);
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
          rowKey={(rowData: MetadataProperty) => rowData.name}
          locale={{
            emptyText: (
              <LoadingDataView
                isLoading={isLoading}
                emptyView={
                  metadataPropertyStore.isSearched.status ? (
                    <span>{t('addition.common.no-result')}</span>
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
            <div className="metadata-properties-modal-description">
              {t('addition.message.del-unused-property-notice')}
            </div>
            <Table
              columns={[
                {
                  title: t('addition.common.property-name'),
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

const MetadataPropertiesManipulation: React.FC<MetadataPropertiesManipulationProps> =
  observer(({ propertyName, propertyIndex }) => {
    const { metadataPropertyStore } = useContext(MetadataConfigsRootStore);
    const { t } = useTranslation();
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
                  content: t('addition.newGraphConfig.create-scuccess'),
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
            {t('addition.newGraphConfig.create')}
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
            {t('addition.common.cancel')}
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
                <p style={{ width: 200 }}>
                  {t('addition.message.property-using-cannot-delete')}
                </p>
              ) : (
                <>
                  <p className="metadata-properties-tooltips-title">
                    {t('addition.message.property-del-confirm')}
                  </p>
                  <p>{t('addition.message.property-del-confirm-again')}</p>
                  <p>{t('addition.message.long-time-notice')}</p>
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
                            content: t('addition.common.del-success'),
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
            className: 'metadata-properties-manipulation no-line-break',
            style: styles.extraMargin,
            title: isDeleteOrBatchDeleting
              ? t('addition.operate.del-ing')
              : t('addition.common.del'),
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
          {isDeleteOrBatchDeleting
            ? t('addition.operate.del-ing')
            : t('addition.common.del')}
        </Tooltip>
      </div>
    );
  });

const EmptyPropertyHints: React.FC = observer(() => {
  const { metadataPropertyStore } = useContext(MetadataConfigsRootStore);
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
        {t('addition.message.property-create-desc')}
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
          {t('addition.operate.create-property')}
        </Button>
        <Button
          size="large"
          style={{ width: 144 }}
          onClick={() => {
            metadataPropertyStore.changeCurrentTabStatus('reuse');
          }}
        >
          {t('addition.operate.reuse-existing-property')}
        </Button>
      </div>
    </div>
  );
});

export default MetadataProperties;
