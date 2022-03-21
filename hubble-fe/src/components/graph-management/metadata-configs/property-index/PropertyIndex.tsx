import React, { useContext, useState, useEffect } from 'react';
import { observer } from 'mobx-react';
import Highlighter from 'react-highlight-words';
import { motion } from 'framer-motion';
import { Input, Table } from 'hubble-ui';

import { LoadingDataView } from '../../../common';
import MetadataConfigsRootStore from '../../../../stores/GraphManagementStore/metadataConfigsStore/metadataConfigsStore';
import './PropertyIndex.less';
import i18next from '../../../../i18n';
import { useTranslation } from 'react-i18next';

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

const IndexTypeMappings: Record<string, string> = {
  SECONDARY: i18next.t('addition.menu.secondary-index'),
  RANGE: i18next.t('addition.menu.range-index'),
  SEARCH: i18next.t('addition.menu.full-text-index')
};

const PropertyIndex: React.FC = observer(() => {
  const { metadataPropertyIndexStore } = useContext(MetadataConfigsRootStore);
  const [preLoading, switchPreLoading] = useState(true);
  const [currentTab, switchCurrentTab] = useState<'vertex' | 'edge'>('vertex');
  const { t } = useTranslation();
  const isLoading =
    preLoading ||
    metadataPropertyIndexStore.requestStatus.fetchMetadataPropertIndexes ===
      'pending';

  const handleSearchChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    metadataPropertyIndexStore.mutateSearchWords(e.target.value);
  };

  const handleSearch = () => {
    metadataPropertyIndexStore.mutatePageNumber(1);
    metadataPropertyIndexStore.switchIsSearchedStatus(true);
    metadataPropertyIndexStore.fetchMetadataPropertIndexes(currentTab);
  };

  const handleClearSearch = () => {
    metadataPropertyIndexStore.mutateSearchWords('');
    metadataPropertyIndexStore.mutatePageNumber(1);
    metadataPropertyIndexStore.switchIsSearchedStatus(false);
    metadataPropertyIndexStore.fetchMetadataPropertIndexes(currentTab);
  };

  const handlePageNumberChange = (e: React.ChangeEvent<HTMLSelectElement>) => {
    metadataPropertyIndexStore.mutatePageNumber(Number(e.target.value));
    metadataPropertyIndexStore.fetchMetadataPropertIndexes(currentTab);
  };

  useEffect(() => {
    setTimeout(() => {
      switchPreLoading(false);
    }, 500);
  }, [currentTab]);

  useEffect(() => {
    metadataPropertyIndexStore.fetchMetadataPropertIndexes(currentTab);

    return () => {
      metadataPropertyIndexStore.dispose();
    };
  }, [currentTab, metadataPropertyIndexStore]);

  const columnConfigs = [
    {
      title:
        currentTab === 'vertex'
          ? t('addition.vertex.vertex-type-name')
          : t('addition.common.edge-type-name'),
      dataIndex: 'owner',
      render(text: string, records: any[], index: number) {
        if (metadataPropertyIndexStore.collpaseInfo === null) {
          // need highlighter here since searched result could be one row
          return (
            <Highlighter
              highlightClassName="vertex-index-search-highlights"
              searchWords={[metadataPropertyIndexStore.isSearched.value]}
              autoEscape={true}
              textToHighlight={text}
            />
          );
        }

        const [collpaseStartIndexes, collpaseNumbers] =
          metadataPropertyIndexStore.collpaseInfo;

        const startIndex = collpaseStartIndexes.findIndex(
          (indexNumber) => indexNumber === index
        );

        return startIndex !== -1
          ? {
              children: (
                <div>
                  <Highlighter
                    highlightClassName="vertex-index-search-highlights"
                    searchWords={[metadataPropertyIndexStore.isSearched.value]}
                    autoEscape={true}
                    textToHighlight={text}
                  />
                </div>
              ),
              props: {
                rowSpan: collpaseNumbers[startIndex]
              }
            }
          : {
              children: (
                <div>
                  <Highlighter
                    highlightClassName="vertex-index-search-highlights"
                    searchWords={[metadataPropertyIndexStore.isSearched.value]}
                    autoEscape={true}
                    textToHighlight={text}
                  />
                </div>
              ),
              props: {
                rowSpan: 0
              }
            };
      }
    },
    {
      title: t('addition.edge.index-name'),
      dataIndex: 'name',
      render(text: string) {
        return (
          <div>
            <Highlighter
              highlightClassName="vertex-index-search-highlights"
              searchWords={[metadataPropertyIndexStore.isSearched.value]}
              autoEscape={true}
              textToHighlight={text}
            />
          </div>
        );
      }
    },
    {
      title: t('addition.edge.index-type'),
      dataIndex: 'type',
      render(text: string) {
        return IndexTypeMappings[text];
      }
    },
    {
      title: t('addition.common.property'),
      dataIndex: 'fields',
      render(properties: string[]) {
        return (
          <div>
            <Highlighter
              highlightClassName="vertex-index-search-highlights"
              searchWords={[metadataPropertyIndexStore.isSearched.value]}
              autoEscape={true}
              textToHighlight={properties.join(';')}
            />
          </div>
        );
      }
    }
  ];

  return (
    <motion.div
      initial="initial"
      animate="animate"
      exit="exit"
      variants={variants}
    >
      <div className="vertex-index-tab-wrapper">
        <div
          onClick={() => {
            if (currentTab !== 'vertex') {
              metadataPropertyIndexStore.fetchMetadataPropertIndexes('vertex');
            }

            metadataPropertyIndexStore.mutateSearchWords('');
            metadataPropertyIndexStore.mutatePageNumber(1);
            metadataPropertyIndexStore.switchIsSearchedStatus(false);
            switchCurrentTab('vertex');
            switchPreLoading(true);
          }}
          className={
            currentTab === 'vertex'
              ? 'vertex-index-tab-index active'
              : 'vertex-index-tab-index'
          }
        >
          {t('addition.vertex.vertex-index')}
        </div>
        <div
          onClick={() => {
            if (currentTab !== 'edge') {
              metadataPropertyIndexStore.fetchMetadataPropertIndexes('edge');
            }

            metadataPropertyIndexStore.mutateSearchWords('');
            metadataPropertyIndexStore.mutatePageNumber(1);
            metadataPropertyIndexStore.switchIsSearchedStatus(false);
            switchCurrentTab('edge');
            switchPreLoading(true);
          }}
          className={
            currentTab === 'edge'
              ? 'vertex-index-tab-index active'
              : 'vertex-index-tab-index'
          }
        >
          {t('addition.edge.edge-index')}
        </div>
      </div>
      <div className="vertex-index-wrapper">
        <div className="metadata-configs-content-header">
          <Input.Search
            size="medium"
            width={200}
            placeholder={t('addition.message.please-enter-keywords')}
            value={metadataPropertyIndexStore.searchWords}
            onChange={handleSearchChange}
            onSearch={handleSearch}
            onClearClick={handleClearSearch}
            isShowDropDown={false}
            disabled={isLoading}
          />
        </div>
        <Table
          bordered
          columns={columnConfigs}
          locale={{
            emptyText: (
              <LoadingDataView
                isLoading={isLoading}
                emptyView={
                  metadataPropertyIndexStore.isSearched.status ? (
                    <span>{t('addition.common.no-result')}</span>
                  ) : (
                    <span>{t('addition.message.no-index-notice')}</span>
                  )
                }
              />
            )
          }}
          dataSource={
            isLoading ? [] : metadataPropertyIndexStore.metadataPropertyIndexes
          }
          pagination={
            isLoading
              ? null
              : {
                  hideOnSinglePage: false,
                  pageNo:
                    metadataPropertyIndexStore.metadataPropertyIndexPageConfig
                      .pageNumber,
                  pageSize: 10,
                  showSizeChange: false,
                  showPageJumper: false,
                  total:
                    metadataPropertyIndexStore.metadataPropertyIndexPageConfig
                      .pageTotal,
                  onPageNoChange: handlePageNumberChange
                }
          }
        />
      </div>
    </motion.div>
  );
});

export default PropertyIndex;
