import React, { useContext, useState, useEffect } from 'react';
import { observer } from 'mobx-react';
import {
  Select,
  Steps,
  Transfer,
  Button,
  Table,
  Input,
  Message
} from '@baidu/one-ui';

import MetadataConfigsRootStore from '../../../../stores/GraphManagementStore/metadataConfigsStore/metadataConfigsStore';
import PassIcon from '../../../../assets/imgs/ic_pass.svg';
import './ReuseEdgeTypes.less';
import { cloneDeep } from 'lodash-es';

const ReuseEdgeTypes: React.FC = observer(() => {
  const metadataConfigsRootStore = useContext(MetadataConfigsRootStore);
  const { edgeTypeStore } = metadataConfigsRootStore;
  const [currentStatus, setCurrentStatus] = useState(1);
  // acutally the name, not id in database
  const [selectedId, mutateSelectedId] = useState<[] | string>([]);
  const [selectedList, mutateSelectedList] = useState<string[]>([]);

  // step 2
  const [edgeTypeEditIndex, setEdgeTypeEditIndex] = useState<number | null>(
    null
  );
  const [vertexTypeEditIndex, setVertexTypeEditIndex] = useState<number | null>(
    null
  );
  const [propertyEditIndex, setPropertyEditIndex] = useState<number | null>(
    null
  );
  const [propertyIndexEditIndex, setPropertyIndexEditIndex] = useState<
    number | null
  >(null);

  // hack: need to call @observable at here to dispatch re-render by mobx
  // since @action in onBlur() in <Input /> doesn't dispatch re-render
  edgeTypeStore.validateReuseErrorMessage.edgeType.toUpperCase();
  edgeTypeStore.validateReuseErrorMessage.vertexType.toUpperCase();
  edgeTypeStore.validateReuseErrorMessage.property.toUpperCase();
  edgeTypeStore.validateReuseErrorMessage.property_index.toUpperCase();

  const edgeTypeColumnConfigs = [
    {
      title: '边类型名称',
      dataIndex: 'name',
      width: '50%',
      render(text: string, records: any, index: number) {
        if (index !== edgeTypeEditIndex) {
          return (
            <div className="no-line-break" title={text}>
              {text}
            </div>
          );
        }

        return (
          <Input
            size="medium"
            width={370}
            placeholder="允许出现中英文、数字、下划线"
            errorLocation="layer"
            errorMessage={edgeTypeStore.validateReuseErrorMessage.edgeType}
            value={
              edgeTypeStore.editedCheckedReusableData!.edgelabel_conflicts[
                index
              ].entity.name
            }
            onChange={(e: any) => {
              edgeTypeStore.resetValidateReuseErrorMessage('edgeType');

              const editedCheckedReusableData = cloneDeep(
                edgeTypeStore.editedCheckedReusableData!
              );

              editedCheckedReusableData.edgelabel_conflicts[index].entity.name =
                e.value;

              edgeTypeStore.mutateEditedReusableData(editedCheckedReusableData);
            }}
            originInputProps={{
              onBlur() {
                edgeTypeStore.validateReuseData(
                  'edgeType',
                  edgeTypeStore.checkedReusableData!.edgelabel_conflicts[index]
                    .entity.name,
                  edgeTypeStore.editedCheckedReusableData!.edgelabel_conflicts[
                    index
                  ].entity.name
                );
              }
            }}
          />
        );
      }
    },
    {
      title: '校验结果',
      dataIndex: 'status',
      width: '30%',
      render(value: string, records: any, index: number) {
        let classname = '';
        let text = '';

        if (edgeTypeStore.reusableEdgeTypeNameChangeIndexes.has(index)) {
          return (
            <div
              className="reuse-properties-validate-duplicate"
              style={{ marginLeft: 0 }}
            >
              待校验
            </div>
          );
        }

        switch (value) {
          case 'DUPNAME':
            classname = classname = 'reuse-properties-validate-duplicate';
            text = '有重名';
            break;

          case 'DEP_CONFLICT':
            classname = 'reuse-properties-validate-duplicate';
            text = '依赖冲突';
            break;

          case 'EXISTED':
            classname = 'reuse-properties-validate-exist';
            text = '已存在';
            break;

          case 'PASSED':
            classname = 'reuse-properties-validate-pass';
            text = '通过';
            break;
        }

        return (
          <div className={classname} style={{ marginLeft: 0 }}>
            {text}
          </div>
        );
      }
    },
    {
      title: '操作',
      dataIndex: 'manipulation',
      width: '20%',
      render(_: never, records: any, index: number) {
        const originalName = edgeTypeStore.checkedReusableData!
          .edgelabel_conflicts[index].entity.name;
        const changedName = edgeTypeStore.editedCheckedReusableData!
          .edgelabel_conflicts[index].entity.name;
        const isChanged = changedName !== originalName;

        if (edgeTypeEditIndex === index) {
          return (
            <div>
              <span
                className="metadata-properties-manipulation"
                style={{
                  marginRight: 16,
                  color: isChanged ? '#2b65ff' : '#999'
                }}
                onClick={() => {
                  if (
                    !isChanged ||
                    !edgeTypeStore.validateReuseData(
                      'edgeType',
                      originalName,
                      changedName
                    )
                  ) {
                    return;
                  }

                  edgeTypeStore.mutateReuseData(
                    'edgeType',
                    originalName,
                    changedName
                  );
                  setEdgeTypeEditIndex(null);
                  edgeTypeStore.mutateReusableEdgeTypeChangeIndexes(index);
                }}
              >
                保存
              </span>
              <span
                className="metadata-properties-manipulation"
                onClick={() => {
                  edgeTypeStore.resetValidateReuseErrorMessage('edgeType');
                  setEdgeTypeEditIndex(null);
                  edgeTypeStore.resetEditedReusableEdgeTypeName(index);
                }}
              >
                取消
              </span>
            </div>
          );
        }

        return (
          <div>
            <span
              className="metadata-properties-manipulation"
              style={{
                marginRight: 16,
                color: edgeTypeEditIndex === null ? '#2b65ff' : '#999'
              }}
              onClick={() => {
                if (edgeTypeEditIndex !== null) {
                  return;
                }

                setEdgeTypeEditIndex(index);
              }}
            >
              重命名
            </span>
            <span
              className="metadata-properties-manipulation"
              style={{
                color: edgeTypeEditIndex === null ? '#2b65ff' : '#999'
              }}
              onClick={async () => {
                if (edgeTypeEditIndex !== null) {
                  return;
                }

                setEdgeTypeEditIndex(null);

                // remove selected status of the property in <Transfer />
                const newSelectedList = [...selectedList].filter(
                  property =>
                    property !==
                    edgeTypeStore.editedCheckedReusableData!
                      .edgelabel_conflicts[index].entity.name
                );

                mutateSelectedList(newSelectedList);

                // notice: useState hooks cannot sync updated state value, so the length is still 1
                if (selectedList.length === 1) {
                  setCurrentStatus(1);
                  // remove edit status after return previous
                  edgeTypeStore.clearReusableNameChangeIndexes();
                  return;
                }

                edgeTypeStore.deleteReuseData('edgelabel_conflicts', index);
              }}
            >
              删除
            </span>
          </div>
        );
      }
    }
  ];

  const vertexTypeColumnConfigs = [
    {
      title: '顶点名称',
      dataIndex: 'name',
      width: '50%',
      render(text: string, records: any, index: number) {
        if (index !== vertexTypeEditIndex) {
          return text;
        }

        return (
          <Input
            size="medium"
            width={370}
            placeholder="允许出现中英文、数字、下划线"
            errorLocation="layer"
            errorMessage={edgeTypeStore.validateReuseErrorMessage.vertexType}
            value={
              edgeTypeStore.editedCheckedReusableData!.vertexlabel_conflicts[
                index
              ].entity.name
            }
            onChange={(e: any) => {
              edgeTypeStore.resetValidateReuseErrorMessage('vertexType');

              const editedCheckedReusableData = cloneDeep(
                edgeTypeStore.editedCheckedReusableData!
              );

              editedCheckedReusableData.vertexlabel_conflicts[
                index
              ].entity.name = e.value;

              edgeTypeStore.mutateEditedReusableData(editedCheckedReusableData);
            }}
            originInputProps={{
              onBlur() {
                edgeTypeStore.validateReuseData(
                  'vertexType',
                  edgeTypeStore.checkedReusableData!.vertexlabel_conflicts[
                    index
                  ].entity.name,
                  edgeTypeStore.editedCheckedReusableData!
                    .vertexlabel_conflicts[index].entity.name
                );
              }
            }}
          />
        );
      }
    },
    {
      title: '校验结果',
      dataIndex: 'status',
      width: '30%',
      render(value: string, records: any, index: number) {
        let classname = '';
        let text = '';

        if (edgeTypeStore.reusableVertexTypeNameChangeIndexes.has(index)) {
          return (
            <div
              className="reuse-properties-validate-duplicate"
              style={{ marginLeft: 0 }}
            >
              待校验
            </div>
          );
        }

        switch (value) {
          case 'DUPNAME':
            classname = 'reuse-properties-validate-duplicate';
            text = '有重名';
            break;

          case 'DEP_CONFLICT':
            classname = 'reuse-properties-validate-duplicate';
            text = '依赖冲突';
            break;

          case 'EXISTED':
            classname = 'reuse-properties-validate-exist';
            text = '已存在';
            break;

          case 'PASSED':
            classname = 'reuse-properties-validate-pass';
            text = '通过';
            break;
        }

        return (
          <div className={classname} style={{ marginLeft: 0 }}>
            {text}
          </div>
        );
      }
    },
    {
      title: '操作',
      dataIndex: 'manipulation',
      width: '20%',
      render(_: never, records: any, index: number) {
        const originalName = edgeTypeStore.checkedReusableData!
          .vertexlabel_conflicts[index].entity.name;
        const changedName = edgeTypeStore.editedCheckedReusableData!
          .vertexlabel_conflicts[index].entity.name;
        const isChanged = changedName !== originalName;

        if (index === vertexTypeEditIndex) {
          return (
            <div>
              <span
                className="metadata-properties-manipulation"
                style={{
                  marginRight: 16,
                  color: isChanged ? '#2b65ff' : '#999'
                }}
                onClick={() => {
                  if (
                    !isChanged ||
                    !edgeTypeStore.validateReuseData(
                      'vertexType',
                      originalName,
                      changedName
                    )
                  ) {
                    return;
                  }

                  edgeTypeStore.mutateReuseData(
                    'vertexType',
                    originalName,
                    changedName
                  );
                  setVertexTypeEditIndex(null);
                  edgeTypeStore.mutateReusableVertexTypeChangeIndexes(index);
                }}
              >
                保存
              </span>
              <span
                className="metadata-properties-manipulation"
                onClick={() => {
                  edgeTypeStore.resetValidateReuseErrorMessage('vertexType');
                  setVertexTypeEditIndex(null);
                  edgeTypeStore.resetEditedReusableVertexTypeName(index);
                }}
              >
                取消
              </span>
            </div>
          );
        }

        return (
          <div>
            <span
              className="metadata-properties-manipulation"
              style={{
                marginRight: 16,
                color: vertexTypeEditIndex === null ? '#2b65ff' : '#999'
              }}
              onClick={() => {
                if (vertexTypeEditIndex !== null) {
                  return;
                }

                setVertexTypeEditIndex(index);
              }}
            >
              重命名
            </span>
          </div>
        );
      }
    }
  ];

  const metadataPropertyColumnConfigs = [
    {
      title: '属性名称',
      dataIndex: 'name',
      width: '50%',
      render(text: string, records: any, index: number) {
        if (propertyEditIndex !== index) {
          return text;
        }

        return (
          <Input
            size="medium"
            width={370}
            placeholder="允许出现中英文、数字、下划线"
            errorLocation="layer"
            errorMessage={edgeTypeStore.validateReuseErrorMessage.property}
            value={
              edgeTypeStore.editedCheckedReusableData!.propertykey_conflicts[
                index
              ].entity.name
            }
            onChange={(e: any) => {
              edgeTypeStore.resetValidateReuseErrorMessage('property');

              const editedCheckedReusableData = cloneDeep(
                edgeTypeStore.editedCheckedReusableData!
              );

              editedCheckedReusableData.propertykey_conflicts[
                index
              ].entity.name = e.value;

              edgeTypeStore.mutateEditedReusableData(editedCheckedReusableData);
            }}
            originInputProps={{
              onBlur() {
                edgeTypeStore.validateReuseData(
                  'property',
                  edgeTypeStore.checkedReusableData!.propertykey_conflicts[
                    index
                  ].entity.name,
                  edgeTypeStore.editedCheckedReusableData!
                    .propertykey_conflicts[index].entity.name
                );
              }
            }}
          />
        );
      }
    },
    {
      title: '数据类型',
      dataIndex: 'data_type',
      width: '15%',
      render(text: string) {
        if (text === 'TEXT') {
          return 'string';
        }

        return text.toLowerCase();
      }
    },
    {
      title: '校验结果',
      dataIndex: 'status',
      align: 'center',
      width: '15%',
      render(value: string, records: any, index: number) {
        let classname = '';
        let text = '';

        if (edgeTypeStore.reusablePropertyNameChangeIndexes.has(index)) {
          return (
            <div className="reuse-properties-validate-duplicate">待校验</div>
          );
        }

        switch (value) {
          case 'DUPNAME':
            classname = 'reuse-properties-validate-duplicate';
            text = '有重名';
            break;

          case 'DEP_CONFLICT':
            classname = 'reuse-properties-validate-duplicate';
            text = '依赖冲突';
            break;

          case 'EXISTED':
            classname = 'reuse-properties-validate-exist';
            text = '已存在';
            break;

          case 'PASSED':
            classname = 'reuse-properties-validate-pass';
            text = '通过';
            break;
        }

        return <div className={classname}>{text}</div>;
      }
    },
    {
      title: '操作',
      dataIndex: 'manipulation',
      width: '20%',
      render(_: never, records: any, index: number) {
        if (index === propertyEditIndex) {
          const originalName = edgeTypeStore.checkedReusableData!
            .propertykey_conflicts[index].entity.name;
          const changedName = edgeTypeStore.editedCheckedReusableData!
            .propertykey_conflicts[index].entity.name;
          const isChanged = changedName !== originalName;

          return (
            <div>
              <span
                className="metadata-properties-manipulation"
                style={{
                  marginRight: 16,
                  color: isChanged ? '#2b65ff' : '#999'
                }}
                onClick={() => {
                  if (
                    !isChanged ||
                    !edgeTypeStore.validateReuseData(
                      'property',
                      originalName,
                      changedName
                    )
                  ) {
                    return;
                  }

                  edgeTypeStore.mutateReuseData(
                    'property',
                    originalName,
                    changedName
                  );
                  setPropertyEditIndex(null);
                  edgeTypeStore.mutateReusablePropertyNameChangeIndexes(index);
                }}
              >
                保存
              </span>
              <span
                className="metadata-properties-manipulation"
                onClick={() => {
                  edgeTypeStore.resetValidateReuseErrorMessage('property');
                  setPropertyEditIndex(null);
                  edgeTypeStore.resetEditedReusablePropertyName(index);
                }}
              >
                取消
              </span>
            </div>
          );
        }

        return (
          <div>
            <span
              className="metadata-properties-manipulation"
              style={{
                marginRight: 16,
                color: propertyEditIndex === null ? '#2b65ff' : '#999'
              }}
              onClick={() => {
                if (propertyEditIndex !== null) {
                  return;
                }

                setPropertyEditIndex(index);
              }}
            >
              重命名
            </span>
            <span
              className="metadata-properties-manipulation"
              style={{
                color: propertyEditIndex === null ? '#2b65ff' : '#999'
              }}
              onClick={async () => {
                if (propertyEditIndex !== null) {
                  return;
                }

                setPropertyEditIndex(null);

                edgeTypeStore.deleteReuseData('propertykey_conflicts', index);
              }}
            >
              删除
            </span>
          </div>
        );
      }
    }
  ];

  const metadataPropertyIndexColumnConfigs = [
    {
      title: '属性索引名称',
      dataIndex: 'name',
      width: '50%',
      render(text: string, records: any, index: number) {
        if (index !== propertyIndexEditIndex) {
          return text;
        }

        return (
          <Input
            size="medium"
            width={370}
            placeholder="允许出现中英文、数字、下划线"
            errorLocation="layer"
            errorMessage={
              edgeTypeStore.validateReuseErrorMessage.property_index
            }
            value={
              edgeTypeStore.editedCheckedReusableData!.propertyindex_conflicts[
                index
              ].entity.name
            }
            onChange={(e: any) => {
              edgeTypeStore.resetValidateReuseErrorMessage('property_index');

              const editedCheckedReusableData = cloneDeep(
                edgeTypeStore.editedCheckedReusableData!
              );

              editedCheckedReusableData.propertyindex_conflicts[
                index
              ].entity.name = e.value;

              edgeTypeStore.mutateEditedReusableData(editedCheckedReusableData);
            }}
            originInputProps={{
              onBlur() {
                edgeTypeStore.validateReuseData(
                  'property_index',
                  edgeTypeStore.checkedReusableData!.propertyindex_conflicts[
                    index
                  ].entity.name,
                  edgeTypeStore.editedCheckedReusableData!
                    .propertyindex_conflicts[index].entity.name
                );
              }
            }}
          />
        );
      }
    },
    {
      title: '对应类型',
      dataIndex: 'owner',
      width: '15%'
    },
    {
      title: '校验结果',
      dataIndex: 'status',
      align: 'center',
      width: '15%',
      render(value: string, records: any, index: number) {
        let classname = '';
        let text = '';

        if (edgeTypeStore.reusablePropertyIndexNameChangeIndexes.has(index)) {
          return (
            <div className="reuse-properties-validate-duplicate">待校验</div>
          );
        }

        switch (value) {
          case 'DUPNAME':
            classname = 'reuse-properties-validate-duplicate';
            text = '有重名';
            break;

          case 'DEP_CONFLICT':
            classname = 'reuse-properties-validate-duplicate';
            text = '依赖冲突';
            break;

          case 'EXISTED':
            classname = 'reuse-properties-validate-exist';
            text = '已存在';
            break;

          case 'PASSED':
            classname = 'reuse-properties-validate-pass';
            text = '通过';
            break;
        }

        return <div className={classname}>{text}</div>;
      }
    },
    {
      title: '操作',
      dataIndex: 'manipulation',
      width: '20%',
      render(_: never, records: any, index: number) {
        if (index === propertyIndexEditIndex) {
          const originalName = edgeTypeStore.checkedReusableData!
            .propertyindex_conflicts[index].entity.name;
          const changedName = edgeTypeStore.editedCheckedReusableData!
            .propertyindex_conflicts[index].entity.name;
          const isChanged = changedName !== originalName;

          return (
            <div>
              <span
                className="metadata-properties-manipulation"
                style={{
                  marginRight: 16,
                  color: isChanged ? '#2b65ff' : '#999'
                }}
                onClick={() => {
                  if (
                    !isChanged ||
                    !edgeTypeStore.validateReuseData(
                      'property_index',
                      originalName,
                      changedName
                    )
                  ) {
                    return;
                  }

                  edgeTypeStore.mutateReuseData(
                    'property_index',
                    originalName,
                    changedName
                  );
                  setPropertyIndexEditIndex(null);
                  edgeTypeStore.mutateReusableVertexTypeChangeIndexes(index);
                }}
              >
                保存
              </span>
              <span
                className="metadata-properties-manipulation"
                onClick={() => {
                  edgeTypeStore.resetValidateReuseErrorMessage(
                    'property_index'
                  );
                  setPropertyIndexEditIndex(null);
                  edgeTypeStore.resetEditedReusablePropertyIndexName(index);
                }}
              >
                取消
              </span>
            </div>
          );
        }

        return (
          <div>
            <span
              className="metadata-properties-manipulation"
              style={{
                marginRight: 16,
                color: propertyIndexEditIndex === null ? '#2b65ff' : '#999'
              }}
              onClick={() => {
                if (propertyIndexEditIndex !== null) {
                  return;
                }

                setPropertyIndexEditIndex(index);
              }}
            >
              重命名
            </span>
            <span
              className="metadata-properties-manipulation"
              style={{
                color: propertyIndexEditIndex === null ? '#2b65ff' : '#999'
              }}
              onClick={async () => {
                if (propertyIndexEditIndex !== null) {
                  return;
                }

                setPropertyIndexEditIndex(null);

                edgeTypeStore.deleteReuseData('propertyindex_conflicts', index);
              }}
            >
              删除
            </span>
          </div>
        );
      }
    }
  ];

  useEffect(() => {
    // unlike metadata properties, all vertex types only needs here(in reuse)
    edgeTypeStore.fetchEdgeTypeList({ fetchAll: true });
  }, [edgeTypeStore]);

  return (
    <div className="reuse-properties-wrapper">
      <div className="metadata-title">复用边类型</div>
      <div
        style={{
          marginTop: 8,
          fontSize: 12,
          color: '#666',
          lineHeight: '18px'
        }}
      >
        边类型关联的属性和属性索引、起点类型和终点类型及其关联的属性和属性索引将一同复用
      </div>
      <div className="reuse-steps">
        <Steps current={currentStatus}>
          {['选择复用项', '确认复用项', '完成复用'].map(
            (title: string, index: number) => (
              <Steps.Step
                title={title}
                status={
                  currentStatus === index + 1
                    ? 'process'
                    : currentStatus > index + 1
                    ? 'finish'
                    : 'wait'
                }
                key={title}
              />
            )
          )}
        </Steps>

        {currentStatus === 1 && (
          <>
            <div className="reuse-properties-row">
              <div className="reuse-properties-row-name">
                <span className="metdata-essential-form-options">*</span>
                <span>图ID：</span>
              </div>
              <Select
                width={420}
                placeholder="请选择要复用的图"
                size="medium"
                showSearch={false}
                onChange={(selectedName: string) => {
                  mutateSelectedId(selectedName);

                  const id = metadataConfigsRootStore.idList.find(
                    ({ name }) => name === selectedName
                  )!.id;

                  mutateSelectedList([]);

                  edgeTypeStore.fetchEdgeTypeList({
                    reuseId: Number(id)
                  });
                }}
                value={selectedId}
              >
                {metadataConfigsRootStore.idList
                  .filter(
                    ({ id }) =>
                      Number(id) !== metadataConfigsRootStore.currentId
                  )
                  .map(({ name }) => (
                    <Select.Option value={name} key={name}>
                      {name}
                    </Select.Option>
                  ))}
              </Select>
            </div>
            <div
              className="reuse-properties-row"
              style={{ alignItems: 'normal' }}
            >
              <div className="reuse-properties-row-name">
                <span className="metdata-essential-form-options">*</span>
                <span>复用边类型：</span>
              </div>
              <Transfer
                treeName="边类型"
                allDataMap={edgeTypeStore.reusableEdgeTypeDataMap}
                candidateList={edgeTypeStore.reusableEdgeTypes.map(
                  ({ name }) => name
                )}
                selectedList={selectedList}
                showSearchBox={false}
                candidateTreeStyle={{
                  width: 359,
                  fontSize: 14
                }}
                selectedTreeStyle={{
                  width: 359,
                  fontSize: 14
                }}
                handleSelect={(selectedList: string[]) => {
                  mutateSelectedList(selectedList);
                }}
                handleSelectAll={(selectedList: string[]) => {
                  mutateSelectedList(selectedList);
                }}
                handleDelete={(selectedList: string[]) => {
                  mutateSelectedList(selectedList);
                }}
                handleDeleteAll={(selectedList: string[]) => {
                  mutateSelectedList(selectedList);
                }}
              />
            </div>
            <div className="reuse-properties-manipulations">
              <Button
                type="primary"
                size="medium"
                style={{ width: 78, marginRight: 12 }}
                disabled={selectedList.length === 0}
                onClick={() => {
                  setCurrentStatus(2);
                  edgeTypeStore.checkConflict(
                    selectedId as string,
                    selectedList
                  );
                }}
              >
                下一步
              </Button>
              <Button
                size="medium"
                style={{ width: 88 }}
                onClick={() => {
                  mutateSelectedId('');
                  mutateSelectedList([]);
                  edgeTypeStore.resetReusableEdgeTypes();

                  edgeTypeStore.edgeTypes.length === 0
                    ? edgeTypeStore.changeCurrentTabStatus('empty')
                    : edgeTypeStore.changeCurrentTabStatus('list');
                }}
              >
                取消复用
              </Button>
            </div>
          </>
        )}

        {currentStatus === 2 && (
          <>
            <div
              className="metadata-title"
              style={{ marginTop: 34, marginBottom: 16 }}
            >
              已选边类型
            </div>
            <Table
              columns={edgeTypeColumnConfigs}
              dataSource={
                edgeTypeStore.editedCheckedReusableData
                  ? edgeTypeStore.editedCheckedReusableData!.edgelabel_conflicts.map(
                      ({ entity, status }) => ({
                        name: entity.name,
                        status
                      })
                    )
                  : []
              }
              pagination={false}
            />

            <div
              className="metadata-title"
              style={{ marginTop: 30, marginBottom: 16 }}
            >
              已选顶点类型
            </div>
            <Table
              columns={vertexTypeColumnConfigs}
              dataSource={
                edgeTypeStore.editedCheckedReusableData
                  ? edgeTypeStore.editedCheckedReusableData!.vertexlabel_conflicts.map(
                      ({ entity, status }) => ({
                        name: entity.name,
                        status
                      })
                    )
                  : []
              }
              pagination={false}
            />

            <div
              className="metadata-title"
              style={{ marginTop: 30, marginBottom: 16 }}
            >
              已选属性
            </div>
            <Table
              columns={metadataPropertyColumnConfigs}
              dataSource={
                edgeTypeStore.editedCheckedReusableData
                  ? edgeTypeStore.editedCheckedReusableData!.propertykey_conflicts.map(
                      ({ entity, status }) => ({
                        name: entity.name,
                        data_type: entity.data_type,
                        status
                      })
                    )
                  : []
              }
              pagination={false}
            />

            <div
              className="metadata-title"
              style={{ marginTop: 30, marginBottom: 16 }}
            >
              已选属性索引
            </div>
            <Table
              columns={metadataPropertyIndexColumnConfigs}
              dataSource={
                edgeTypeStore.editedCheckedReusableData
                  ? edgeTypeStore.editedCheckedReusableData!.propertyindex_conflicts.map(
                      ({ entity, status }) => ({
                        name: entity.name,
                        owner: entity.owner,
                        status
                      })
                    )
                  : []
              }
              pagination={false}
            />

            <div
              className="reuse-properties-manipulations"
              style={{ margin: '24px 0' }}
            >
              <Button
                type="primary"
                size="medium"
                style={{
                  width: edgeTypeStore.isReadyToReuse ? 78 : 88,
                  marginRight: 12
                }}
                onClick={async () => {
                  if (edgeTypeStore.isReadyToReuse) {
                    await edgeTypeStore.reuseEdgeType();

                    if (
                      edgeTypeStore.requestStatus.reuseEdgeType === 'failed'
                    ) {
                      Message.error({
                        content: edgeTypeStore.errorMessage,
                        size: 'medium',
                        showCloseIcon: false
                      });

                      return;
                    }

                    edgeTypeStore.mutatePageNumber(1);
                    edgeTypeStore.fetchEdgeTypeList();
                    edgeTypeStore.clearReusableNameChangeIndexes();
                    setCurrentStatus(3);
                  } else {
                    edgeTypeStore.clearReusableNameChangeIndexes();
                    edgeTypeStore.recheckConflict();
                  }
                }}
                disabled={
                  edgeTypeEditIndex !== null ||
                  vertexTypeEditIndex !== null ||
                  propertyEditIndex !== null ||
                  propertyIndexEditIndex !== null
                }
              >
                {edgeTypeStore.isReadyToReuse ? '完成' : '重新校验'}
              </Button>
              <Button
                size="medium"
                style={{ width: 88 }}
                onClick={() => {
                  setCurrentStatus(1);
                }}
              >
                上一步
              </Button>
            </div>
          </>
        )}

        {currentStatus === 3 && (
          <div className="reuse-properties-complete-hint">
            <div className="reuse-properties-complete-hint-description">
              <img src={PassIcon} alt="复用完成" />
              <div>
                <div>复用完成</div>
                <div>已成功复用顶点类型</div>
              </div>
            </div>
            <div className="reuse-properties-complete-hint-manipulations">
              <Button
                type="primary"
                size="medium"
                style={{ width: 88, marginRight: 12 }}
                onClick={() => {
                  mutateSelectedId([]);
                  mutateSelectedList([]);
                  edgeTypeStore.resetReusableEdgeTypes();
                  edgeTypeStore.changeCurrentTabStatus('list');
                }}
              >
                返回查看
              </Button>
              <Button
                size="medium"
                style={{ width: 88 }}
                onClick={() => {
                  mutateSelectedId([]);
                  mutateSelectedList([]);
                  edgeTypeStore.resetReusableEdgeTypes();
                  setCurrentStatus(1);
                }}
              >
                继续复用
              </Button>
            </div>
          </div>
        )}
      </div>
    </div>
  );
});

export default ReuseEdgeTypes;
