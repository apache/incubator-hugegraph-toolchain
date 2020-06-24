import React, { useContext, useCallback, useRef, useEffect } from 'react';
import { observer } from 'mobx-react';
import { Drawer, Select, Input, Button, Message } from '@baidu/one-ui';

import { DataAnalyzeStoreContext } from '../../../stores';
import { addGraphNodes, addGraphEdges } from '../../../stores/utils';
import { isUndefined, isEmpty } from 'lodash-es';

const IDStrategyMappings: Record<string, string> = {
  PRIMARY_KEY: '主键ID',
  AUTOMATIC: '自动生成',
  CUSTOMIZE_STRING: '自定义字符串',
  CUSTOMIZE_NUMBER: '自定义数字',
  CUSTOMIZE_UUID: '自定义UUID'
};

const DataAnalyzeAddEdge: React.FC = observer(() => {
  const dataAnalyzeStore = useContext(DataAnalyzeStoreContext);

  const title =
    dataAnalyzeStore.dynamicAddGraphDataStatus === 'inEdge' ? '入边' : '出边';

  const selectedEdgeLabel = dataAnalyzeStore.edgeTypes.find(
    ({ name }) => name === dataAnalyzeStore.newGraphEdgeConfigs.label
  )!;

  const nonNullableProperties = [
    ...dataAnalyzeStore.newGraphEdgeConfigs.properties.nonNullable.keys()
  ];

  const nullableProperties = [
    ...dataAnalyzeStore.newGraphEdgeConfigs.properties.nullable.keys()
  ];

  const premitAddGraphEdge =
    !isEmpty(dataAnalyzeStore.newGraphEdgeConfigs.label) &&
    !isEmpty(dataAnalyzeStore.newGraphEdgeConfigs.id) &&
    ![
      ...dataAnalyzeStore.newGraphEdgeConfigs.properties.nonNullable.values()
    ].includes('') &&
    [
      ...dataAnalyzeStore.validateAddGraphEdgeErrorMessage!.properties.nonNullable.values()
    ].every((value) => value === '') &&
    [
      ...dataAnalyzeStore.validateAddGraphEdgeErrorMessage!.properties.nullable.values()
    ].every((value) => value === '');

  const handleDrawerClose = useCallback(() => {
    dataAnalyzeStore.setDynamicAddGraphDataStatus('');
    dataAnalyzeStore.resetNewGraphData('edge');
  }, [dataAnalyzeStore]);

  useEffect(() => {
    const handleOutSideClick = (e: MouseEvent) => {
      const drawerWrapper = document.querySelector(
        '.new-fc-one-drawer-content-wrapper'
      );

      if (
        dataAnalyzeStore.isShowGraphInfo &&
        !dataAnalyzeStore.isClickOnNodeOrEdge &&
        drawerWrapper &&
        !drawerWrapper.contains(e.target as Element)
      ) {
        dataAnalyzeStore.setDynamicAddGraphDataStatus('');
      }
    };

    document.addEventListener('click', handleOutSideClick, false);

    return () => {
      document.removeEventListener('click', handleOutSideClick, false);
    };
  }, [dataAnalyzeStore]);

  return (
    <Drawer
      width={580}
      title={`添加${title}`}
      visible={dataAnalyzeStore.dynamicAddGraphDataStatus.includes('Edge')}
      onClose={handleDrawerClose}
      maskClosable={false}
      footer={[
        <Button
          type="primary"
          size="medium"
          style={{ width: 60 }}
          disabled={!premitAddGraphEdge}
          onClick={async () => {
            const graphViewData = await dataAnalyzeStore.addGraphEdge();

            if (!isUndefined(graphViewData)) {
              const { vertices, originalVertices, edges } = graphViewData;

              addGraphNodes(
                vertices,
                dataAnalyzeStore.visDataSet?.nodes,
                dataAnalyzeStore.vertexSizeMappings,
                dataAnalyzeStore.colorMappings,
                dataAnalyzeStore.vertexWritingMappings
              );

              addGraphEdges(
                edges,
                dataAnalyzeStore.visDataSet?.edges,
                dataAnalyzeStore.edgeColorMappings,
                dataAnalyzeStore.edgeThicknessMappings,
                dataAnalyzeStore.edgeWithArrowMappings,
                dataAnalyzeStore.edgeWritingMappings
              );

              dataAnalyzeStore.visNetwork?.selectNodes(
                originalVertices.map(({ id }) => id)
              );

              dataAnalyzeStore.visNetwork?.selectEdges(
                edges.map(({ id }) => id)
              );

              Message.success({
                content: `添加成功`,
                size: 'medium',
                showCloseIcon: false
              });
            } else {
              Message.error({
                content: `添加失败`,
                size: 'medium',
                showCloseIcon: false
              });
            }

            handleDrawerClose();
          }}
        >
          添加
        </Button>,
        <Button size="medium" style={{ width: 60 }} onClick={handleDrawerClose}>
          取消
        </Button>
      ]}
    >
      <div className="data-analyze-dynamic-add-options">
        <div
          className="data-analyze-dynamic-add-option"
          style={{ marginBottom: 24 }}
        >
          <div>{title === '出边' ? '起点' : '终点'}：</div>
          <span>{dataAnalyzeStore.rightClickedGraphData.id}</span>
        </div>
        <div className="data-analyze-dynamic-add-option">
          <div>边类型：</div>
          <Select
            size="medium"
            trigger="click"
            selectorName="请选择边类型"
            value={dataAnalyzeStore.newGraphEdgeConfigs.label}
            width={420}
            onChange={(value: string) => {
              dataAnalyzeStore.setNewGraphDataConfig('edge', 'label', value);
              dataAnalyzeStore.syncNewGraphDataProperties('edge');
              dataAnalyzeStore.initValidateAddGraphDataErrorMessage('edge');
            }}
            dropdownClassName="data-analyze-sidebar-select"
          >
            {dataAnalyzeStore.relatedGraphEdges.map((label) => (
              <Select.Option value={label} key={label}>
                {label}
              </Select.Option>
            ))}
          </Select>
        </div>

        {!isUndefined(selectedEdgeLabel) && (
          <>
            <div
              className="data-analyze-dynamic-add-option"
              // override style bugs in index.less
              style={{ alignItems: 'normal' }}
            >
              <div style={{ lineHeight: '32px' }}>
                {title === '出边' ? '终点' : '起点'}：
              </div>
              <div>
                <Input
                  size="medium"
                  width={420}
                  placeholder={`请输入${title === '出边' ? '终点' : '起点'}ID`}
                  errorLocation="layer"
                  errorMessage={
                    dataAnalyzeStore.validateAddGraphEdgeErrorMessage!.id
                  }
                  value={dataAnalyzeStore.newGraphEdgeConfigs.id}
                  onChange={(e: any) => {
                    dataAnalyzeStore.setNewGraphDataConfig(
                      'edge',
                      'id',
                      e.value
                    );

                    dataAnalyzeStore.validateAddGraphEdge('id', false);
                  }}
                  onBlur={() => {
                    dataAnalyzeStore.validateAddGraphEdge('id', false);
                  }}
                />
              </div>
            </div>

            {dataAnalyzeStore.newGraphEdgeConfigs.properties.nonNullable
              .size !== 0 && (
              <div className="data-analyze-dynamic-add-option data-analyze-dynamic-add-option-with-expand">
                <div>不可空属性：</div>
                <div className="data-analyze-dynamic-add-option-expands">
                  <div className="data-analyze-dynamic-add-option-expand">
                    <div>属性</div>
                    <div>属性值</div>
                  </div>
                  {nonNullableProperties.map((property) => (
                    <div
                      className="data-analyze-dynamic-add-option-expand"
                      key={property}
                    >
                      <div>{property}</div>
                      <div>
                        <Input
                          size="medium"
                          width={190}
                          placeholder="请输入属性值"
                          errorLocation="layer"
                          errorMessage={dataAnalyzeStore.validateAddGraphEdgeErrorMessage?.properties.nonNullable.get(
                            property
                          )}
                          value={dataAnalyzeStore.newGraphEdgeConfigs.properties.nonNullable.get(
                            property
                          )}
                          onChange={(e: any) => {
                            dataAnalyzeStore.setNewGraphDataConfigProperties(
                              'edge',
                              'nonNullable',
                              property,
                              e.value
                            );

                            dataAnalyzeStore.validateAddGraphEdge(
                              'nonNullable',
                              false,
                              property
                            );
                          }}
                          onBlur={() => {
                            dataAnalyzeStore.validateAddGraphEdge(
                              'nonNullable',
                              false,
                              property
                            );
                          }}
                        />
                      </div>
                    </div>
                  ))}
                </div>
              </div>
            )}

            {dataAnalyzeStore.newGraphEdgeConfigs.properties.nullable.size !==
              0 && (
              <div className="data-analyze-dynamic-add-option data-analyze-dynamic-add-option-with-expand">
                <div>可空属性：</div>
                <div className="data-analyze-dynamic-add-option-expands">
                  <div className="data-analyze-dynamic-add-option-expand">
                    <div>属性</div>
                    <div>属性值</div>
                  </div>
                  {nullableProperties.map((property) => (
                    <div
                      className="data-analyze-dynamic-add-option-expand"
                      key={property}
                    >
                      <div>{property}</div>
                      <div>
                        <Input
                          size="medium"
                          width={190}
                          placeholder="请输入属性值"
                          errorLocation="layer"
                          errorMessage={dataAnalyzeStore.validateAddGraphEdgeErrorMessage!.properties.nullable.get(
                            property
                          )}
                          value={dataAnalyzeStore.newGraphEdgeConfigs.properties.nullable.get(
                            property
                          )}
                          onChange={(e: any) => {
                            dataAnalyzeStore.setNewGraphDataConfigProperties(
                              'edge',
                              'nullable',
                              property,
                              e.value
                            );

                            dataAnalyzeStore.validateAddGraphEdge(
                              'nullable',
                              false,
                              property
                            );
                          }}
                          onBlur={() => {
                            dataAnalyzeStore.validateAddGraphEdge(
                              'nullable',
                              false,
                              property
                            );
                          }}
                        />
                      </div>
                    </div>
                  ))}
                </div>
              </div>
            )}
          </>
        )}
      </div>
    </Drawer>
  );
});

export default DataAnalyzeAddEdge;
