import React, { useContext, useState, useEffect } from 'react';
import { observer } from 'mobx-react';
import Highlighter from 'react-highlight-words';
import { Input, Table } from '@baidu/one-ui';

import MetadataConfigsRootStore from '../../../../stores/GraphManagementStore/metadataConfigsStore/metadataConfigsStore';
import LoadingBackIcon from '../../../../assets/imgs/ic_loading_back.svg';
import LoadingFrontIcon from '../../../../assets/imgs/ic_loading_front.svg';
import './PropertyIndex.less';

const IndexTypeMappings: Record<string, string> = {
  SECONDARY: '二级索引',
  RANGE: '范围索引',
  SEARCH: '全文索引'
};

const PropertyIndex: React.FC = observer(() => {
  const { metadataPropertyIndexStore } = useContext(MetadataConfigsRootStore);
  const [preLoading, switchPreLoading] = useState(true);
  const [currentTab, switchCurrentTab] = useState<'vertex' | 'edge'>('vertex');

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
      title: currentTab === 'vertex' ? '顶点类型名称' : '边类型名称',
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

        const [
          collpaseStartIndexes,
          collpaseNumbers
        ] = metadataPropertyIndexStore.collpaseInfo;

        const startIndex = collpaseStartIndexes.findIndex(
          indexNumber => indexNumber === index
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
      title: '索引名称',
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
      title: '索引类型',
      dataIndex: 'type',
      render(text: string) {
        return IndexTypeMappings[text];
      }
    },
    {
      title: '属性',
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
    <div>
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
          顶点索引
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
          边索引
        </div>
      </div>
      <div className="vertex-index-wrapper">
        {preLoading ||
        metadataPropertyIndexStore.requestStatus.fetchMetadataPropertIndexes ===
          'pending' ? (
          <div
            className="metadata-configs-content-loading-wrapper"
            style={{ height: 'calc(100vh - 272px)' }}
          >
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
                value={metadataPropertyIndexStore.searchWords}
                onChange={handleSearchChange}
                onSearch={handleSearch}
                onClearClick={handleClearSearch}
                isShowDropDown={false}
              />
            </div>
            <Table
              bordered
              columns={columnConfigs}
              dataSource={metadataPropertyIndexStore.metadataPropertyIndexes}
              pagination={{
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
              }}
            />
          </>
        )}
      </div>
    </div>
  );
});

export default PropertyIndex;
