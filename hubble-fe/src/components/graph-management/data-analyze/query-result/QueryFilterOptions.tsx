import React, { useContext, useCallback } from 'react';
import { observer } from 'mobx-react';
import { Select, Input, NumberBox, Calendar } from '@baidu/one-ui';
import vis from 'vis-network';
import { Message } from '@baidu/one-ui';

import { DataAnalyzeStoreContext } from '../../../../stores';

const getRuleOptions = (ruleType: string = '') => {
  switch (ruleType.toLowerCase()) {
    case 'float':
    case 'double':
    case 'byte':
    case 'int':
    case 'long':
    case 'date':
      return ['大于', '大于等于', '小于', '小于等于', '等于'];
    case 'object':
    case 'text':
    case 'blob':
    case 'uuid':
      return ['等于'];
    case 'boolean':
      return ['True', 'False'];
    default:
      return [];
  }
};

const QueryFilterOptions: React.FC<{
  visNetwork: vis.Network | null;
  visGraphNodes: vis.DataSetNodes;
  visGraphEdges: vis.DataSetEdges;
}> = observer(({ visNetwork, visGraphNodes, visGraphEdges }) => {
  const dataAnalyzeStore = useContext(DataAnalyzeStoreContext);
  const line = dataAnalyzeStore.filteredGraphQueryOptions.line;
  const properties = dataAnalyzeStore.filteredGraphQueryOptions.properties;
  const lastProperty = properties[properties.length - 1];

  // value of the corresponding revealed form should not be empty
  const allowSendFilterRequest =
    (properties.length === 0 && line.type !== '') ||
    (lastProperty &&
      lastProperty.property !== '' &&
      lastProperty.rule !== '' &&
      lastProperty.value !== '') ||
    (lastProperty &&
      (lastProperty.rule === 'True' || lastProperty.rule === 'False'));

  const allowAddProperties =
    allowSendFilterRequest &&
    dataAnalyzeStore.filteredPropertyOptions.length !== 0 &&
    dataAnalyzeStore.filteredGraphQueryOptions.properties.length !==
      dataAnalyzeStore.filteredPropertyOptions.length;

  const handleEdgeSelectChange = useCallback(
    (key: 'type' | 'direction') => (value: string) => {
      dataAnalyzeStore.editEdgeFilterOption(key, value);
      dataAnalyzeStore.fetchFilteredPropertyOptions(value);
    },
    [dataAnalyzeStore]
  );

  const handlePropertyChange = useCallback(
    (
      key: 'property' | 'rule' | 'value',
      value: string | number,
      index: number
    ) => {
      dataAnalyzeStore.editPropertyFilterOption(key, value, index);
    },
    [dataAnalyzeStore]
  );

  const renderPropertyValue = (
    type: string = '',
    value: string,
    index: number
  ) => {
    const shouldDisabled =
      dataAnalyzeStore.filteredGraphQueryOptions.properties[index].property ===
      '';

    switch (type.toLowerCase()) {
      case 'float':
      case 'double':
        return (
          <Input
            size="large"
            width={180}
            placeholder="请输入数字"
            value={value}
            onChange={(e: any) => {
              handlePropertyChange('value', e.value, index);
            }}
            disabled={shouldDisabled}
          />
        );
      case 'byte':
      case 'int':
      case 'long':
        return (
          <NumberBox
            mode="strong"
            size="medium"
            type="int"
            step={1}
            value={value}
            onChange={(e: any) => {
              handlePropertyChange('value', Number(e.target.value), index);
            }}
            disabled={shouldDisabled}
          />
        );
      case 'date':
        return (
          <Calendar
            size="medium"
            onSelectDay={(timeParams: { beginTime: string }) => {
              handlePropertyChange('value', timeParams.beginTime, index);
            }}
            disabled={shouldDisabled}
          />
        );
      case 'object':
      case 'text':
      case 'blob':
      case 'uuid':
        return (
          <Input
            size="large"
            width={180}
            placeholder="请输入字符串"
            value={value}
            onChange={(e: any) => {
              handlePropertyChange('value', e.value, index);
            }}
            disabled={shouldDisabled}
          />
        );
      case 'boolean':
        return <div style={{ lineHeight: '32px' }}>/</div>;
      default:
        return (
          <Input
            size="large"
            width={180}
            placeholder="请输入"
            disabled={true}
          />
        );
    }
  };

  return (
    <div className="query-result-filter-options">
      <div className="query-result-filter-options-edge-filter">
        <div>
          <span style={{ display: 'block', width: 56, marginRight: 8 }}>
            边类型：
          </span>
          <Select
            size="medium"
            trigger="click"
            value={dataAnalyzeStore.filteredGraphQueryOptions.line.type}
            width={180}
            onChange={handleEdgeSelectChange('type')}
            dropdownClassName="data-analyze-sidebar-select"
          >
            {dataAnalyzeStore.graphDataEdgeTypes.map(type => (
              <Select.Option value={type} key={type}>
                {type}
              </Select.Option>
            ))}
          </Select>
        </div>
        <div>
          <span style={{ display: 'block', width: 56, marginRight: 8 }}>
            边方向：
          </span>
          <Select
            size="medium"
            trigger="click"
            value={dataAnalyzeStore.filteredGraphQueryOptions.line.direction}
            width={180}
            onChange={handleEdgeSelectChange('direction')}
            dropdownClassName="data-analyze-sidebar-select"
          >
            {['IN', 'OUT', 'BOTH'].map(value => (
              <Select.Option value={value} key={value}>
                {value}
              </Select.Option>
            ))}
          </Select>
        </div>
        <div>
          <span
            style={{
              color: allowSendFilterRequest ? '#2b65ff' : '#ccc'
            }}
            onClick={async () => {
              if (!allowSendFilterRequest) {
                return;
              }

              await dataAnalyzeStore.filterGraphData();

              if (
                dataAnalyzeStore.requestStatus.filteredGraphData === 'success'
              ) {
                dataAnalyzeStore.expandedGraphData.data.graph_view.vertices.forEach(
                  ({ id, label, properties }) => {
                    visGraphNodes.add({
                      id,
                      label: id.length <= 15 ? id : id.slice(0, 15) + '...',
                      vLabel: label,
                      properties,
                      title: `
                          <div class="tooltip-fields">
                            <div>顶点类型：</div>
                            <div>${label}</div>
                          </div>
                          <div class="tooltip-fields">
                            <div>顶点ID：</div>
                            <div>${id}</div>
                          </div>
                          ${Object.entries(properties)
                            .map(([key, value]) => {
                              return `<div class="tooltip-fields">
                                        <div>${key}: </div>
                                        <div>${value}</div>
                                      </div>
                                      `;
                            })
                            .join('')}
                        `,
                      color: {
                        background:
                          dataAnalyzeStore.colorMappings[label] || '#5c73e6',
                        border:
                          dataAnalyzeStore.colorMappings[label] || '#5c73e6',
                        highlight: {
                          background: '#fb6a02',
                          border: '#fb6a02'
                        },
                        hover: { background: '#ec3112', border: '#ec3112' }
                      },
                      chosen: {
                        node(
                          values: any,
                          id: string,
                          selected: boolean,
                          hovering: boolean
                        ) {
                          if (hovering || selected) {
                            values.shadow = true;
                            values.shadowColor = 'rgba(0, 0, 0, 0.6)';
                            values.shadowX = 0;
                            values.shadowY = 0;
                            values.shadowSize = 25;
                          }

                          if (selected) {
                            values.size = 30;
                          }
                        }
                      }
                    });
                  }
                );

                dataAnalyzeStore.expandedGraphData.data.graph_view.edges.forEach(
                  edge => {
                    visGraphEdges.add({
                      ...edge,
                      from: edge.source,
                      to: edge.target,
                      font: {
                        color: '#666'
                      },
                      title: `
                        <div class="tooltip-fields">
                          <div>边类型：</div>
                          <div>${edge.label}</div>
                        </div>
                        <div class="tooltip-fields">
                          <div>边ID：</div>
                          <div>${edge.id}</div>
                        </div>
                        ${Object.entries(edge.properties)
                          .map(([key, value]) => {
                            return `<div class="tooltip-fields">
                                      <div>${key}: </div>
                                      <div>${value}</div>
                                    </div>`;
                          })
                          .join('')}
                      `,
                      color: {
                        color: dataAnalyzeStore.edgeColorMappings[edge.label],
                        highlight:
                          dataAnalyzeStore.edgeColorMappings[edge.label],
                        hover: dataAnalyzeStore.edgeColorMappings[edge.label]
                      }
                    });
                  }
                );

                // highlight new vertices
                if (visNetwork !== null) {
                  visNetwork.selectNodes(
                    dataAnalyzeStore.expandedGraphData.data.graph_view.vertices
                      .map(({ id }) => id)
                      .concat([dataAnalyzeStore.rightClickedGraphData.id]),
                    true
                  );
                }

                dataAnalyzeStore.switchShowFilterBoard(false);
                dataAnalyzeStore.clearFilteredGraphQueryOptions();
              } else {
                Message.error({
                  content: dataAnalyzeStore.errorInfo.filteredGraphData.message,
                  size: 'medium',
                  showCloseIcon: false
                });
              }
            }}
          >
            筛选
          </span>
          <span
            onClick={() => {
              dataAnalyzeStore.switchShowFilterBoard(false);
              dataAnalyzeStore.clearFilteredGraphQueryOptions();

              if (visNetwork) {
                visNetwork.unselectAll();
              }
            }}
          >
            取消
          </span>
        </div>
      </div>
      {dataAnalyzeStore.filteredGraphQueryOptions.properties.length !== 0 && (
        <div className="query-result-filter-options-hr" />
      )}
      {dataAnalyzeStore.filteredGraphQueryOptions.properties.map(
        ({ property, rule, value }, index) => {
          return (
            <div
              className="query-result-filter-options-property-filter"
              key={property}
            >
              <div>
                <span>属性：</span>
                <Select
                  size="medium"
                  trigger="click"
                  value={property}
                  showSearch
                  width={180}
                  onChange={(value: string) => {
                    handlePropertyChange('property', value, index);
                    handlePropertyChange('rule', '', index);

                    if (
                      ['byte', 'int', 'long'].includes(
                        dataAnalyzeStore.valueTypes[value].toLowerCase()
                      )
                    ) {
                      // set default value to 0
                      handlePropertyChange('value', 0, index);
                    } else {
                      handlePropertyChange('value', '', index);
                    }
                  }}
                  dropdownClassName="data-analyze-sidebar-select"
                >
                  {dataAnalyzeStore.filteredPropertyOptions
                    .filter(
                      option =>
                        !dataAnalyzeStore.filteredGraphQueryOptions.properties
                          .map(property => property.property)
                          .includes(option)
                    )
                    .map(prop => (
                      <Select.Option value={prop} key={prop}>
                        {prop}
                      </Select.Option>
                    ))}
                </Select>
              </div>
              <div>
                <span>规则：</span>
                <Select
                  size="medium"
                  trigger="click"
                  value={rule}
                  width={180}
                  placeholder="请输入"
                  onChange={(value: string) => {
                    handlePropertyChange('rule', value, index);
                  }}
                  dropdownClassName="data-analyze-sidebar-select"
                  disabled={
                    dataAnalyzeStore.filteredGraphQueryOptions.properties[index]
                      .property === ''
                  }
                >
                  {getRuleOptions(dataAnalyzeStore.valueTypes[property]).map(
                    value => (
                      <Select.Option value={value} key={value}>
                        {value}
                      </Select.Option>
                    )
                  )}
                </Select>
              </div>
              <div>
                <span>值：</span>
                {renderPropertyValue(
                  // the real type of value
                  dataAnalyzeStore.valueTypes[property],
                  value,
                  index
                )}
              </div>
              <div>
                <span
                  style={{ color: '#2b65ff', cursor: 'pointer' }}
                  onClick={() => {
                    dataAnalyzeStore.deletePropertyFilterOption(index);
                  }}
                >
                  删除
                </span>
              </div>
            </div>
          );
        }
      )}
      <div className="query-result-filter-options-manipulation">
        <span
          onClick={
            allowAddProperties
              ? () => {
                  dataAnalyzeStore.addPropertyFilterOption();
                }
              : undefined
          }
          style={{
            color: allowAddProperties ? '#2b65ff' : '#ccc'
          }}
        >
          添加属性筛选
        </span>
      </div>
    </div>
  );
});

export default QueryFilterOptions;
