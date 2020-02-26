import React, { useContext, useState, useEffect } from 'react';
import { observer } from 'mobx-react';
import { Select, Steps, Transfer, Button, Table, Input } from '@baidu/one-ui';

import MetadataConfigsRootStore from '../../../../stores/GraphManagementStore/metadataConfigsStore/metadataConfigsStore';
import PassIcon from '../../../../assets/imgs/ic_pass.svg';
import './ReuseProperties.less';
import { cloneDeep } from 'lodash-es';

const ReuseProperties: React.FC = observer(() => {
  const metadataConfigsRootStore = useContext(MetadataConfigsRootStore);
  const { metadataPropertyStore } = metadataConfigsRootStore;
  const [currentStatus, setCurrentStatus] = useState(1);
  // acutally the name, not id in database
  const [selectedId, mutateSelectedId] = useState<[] | string>([]);
  const [selectedList, mutateSelectedList] = useState<string[]>([]);

  // step 2
  const [editIndex, setEditIndex] = useState<number | null>(null);

  // hack: need to call @observable at here to dispatch re-render by mobx
  // since @action in onBlur() in <Input /> doesn't dispatch re-render
  metadataPropertyStore.validateRenameReusePropertyErrorMessage.name.toUpperCase();

  const columnConfigs = [
    {
      title: '属性名称',
      dataIndex: 'name',
      width: '50%',
      render(text: string, records: any, index: number) {
        if (editIndex !== index) {
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
            errorMessage={
              metadataPropertyStore.validateRenameReusePropertyErrorMessage.name
            }
            value={
              metadataPropertyStore.editedCheckedReusableProperties!
                .propertykey_conflicts[index].entity.name
            }
            onChange={(e: any) => {
              const editedCheckedReusableProperties = cloneDeep(
                metadataPropertyStore.editedCheckedReusableProperties!
              );

              editedCheckedReusableProperties.propertykey_conflicts[
                index
              ].entity.name = e.value;

              metadataPropertyStore.mutateEditedReusableProperties(
                editedCheckedReusableProperties
              );
            }}
            originInputProps={{
              onBlur() {
                metadataPropertyStore.validateRenameReuseProperty(index);
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
          return (
            <div className="no-line-break" title="string">
              string
            </div>
          );
        }

        return (
          <div className="no-line-break" title={text.toLowerCase()}>
            {text.toLowerCase()}
          </div>
        );
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

        if (
          metadataPropertyStore.reusablePropertyNameChangeIndexes.has(index)
        ) {
          return (
            <div className="reuse-properties-validate-duplicate">待校验</div>
          );
        }

        switch (value) {
          case 'DUPNAME':
            classname = 'reuse-properties-validate-duplicate';
            text = '有重名';
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
        if (index === editIndex) {
          return (
            <div>
              <span
                className="metadata-properties-manipulation"
                style={{ marginRight: 16 }}
                onClick={() => {
                  const isReady = metadataPropertyStore.validateRenameReuseProperty(
                    index
                  );

                  if (isReady) {
                    setEditIndex(null);

                    metadataPropertyStore.mutateReusablePropertyNameChangeIndexes(
                      index
                    );
                  }
                }}
              >
                保存
              </span>
              <span
                className="metadata-properties-manipulation"
                onClick={() => {
                  setEditIndex(null);
                  metadataPropertyStore.resetValidateRenameReuseProperty();
                  metadataPropertyStore.resetEditedReusablePropertyName(index);
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
                color: editIndex === null ? '#2b65ff' : '#999'
              }}
              onClick={() => {
                if (editIndex !== null) {
                  return;
                }

                setEditIndex(index);
              }}
            >
              重命名
            </span>
            <span
              className="metadata-properties-manipulation"
              style={{
                color: editIndex === null ? '#2b65ff' : '#999'
              }}
              onClick={async () => {
                if (editIndex !== null) {
                  return;
                }

                setEditIndex(null);

                const editedCheckedReusableProperties = cloneDeep(
                  metadataPropertyStore.editedCheckedReusableProperties!
                );

                editedCheckedReusableProperties.propertykey_conflicts.splice(
                  index,
                  1
                );

                // remove selected status of the property in <Transfer />
                mutateSelectedList(
                  [...selectedList].filter(
                    property =>
                      property !==
                      metadataPropertyStore.editedCheckedReusableProperties!
                        .propertykey_conflicts[index].entity.name
                  )
                );

                // remove property in Table
                metadataPropertyStore.mutateEditedReusableProperties(
                  editedCheckedReusableProperties
                );
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
    return () => {
      // reset validate error message before unmount to avoid conflict with other component validation
      metadataPropertyStore.resetValidateRenameReuseProperty();
    };
  }, [metadataPropertyStore]);

  return (
    <div className="reuse-properties-wrapper">
      <div className="metadata-title">复用属性</div>
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

                  metadataPropertyStore.fetchMetadataPropertyList({
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
                <span>复用属性：</span>
              </div>
              <Transfer
                treeName="属性"
                allDataMap={metadataPropertyStore.reuseablePropertyDataMap}
                candidateList={metadataPropertyStore.reuseableProperties.map(
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
                  metadataPropertyStore.checkConflict(selectedList);
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
                  metadataPropertyStore.resetReusablePropeties();
                  metadataPropertyStore.metadataProperties.length === 0
                    ? metadataPropertyStore.changeCurrentTabStatus('empty')
                    : metadataPropertyStore.changeCurrentTabStatus('list');
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
              style={{ marginTop: 18, marginBottom: 16 }}
            >
              已选属性
            </div>
            <Table
              columns={columnConfigs}
              dataSource={
                metadataPropertyStore.editedCheckedReusableProperties
                  ? metadataPropertyStore.editedCheckedReusableProperties!.propertykey_conflicts.map(
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
              className="reuse-properties-manipulations"
              style={{ margin: '24px 0' }}
            >
              <Button
                type="primary"
                size="medium"
                style={{
                  width: metadataPropertyStore.isReadyToReuse ? 78 : 88,
                  marginRight: 12
                }}
                onClick={async () => {
                  if (metadataPropertyStore.isReadyToReuse) {
                    await metadataPropertyStore.reuseMetadataProperties();
                    metadataPropertyStore.mutatePageNumber(1);
                    metadataPropertyStore.fetchMetadataPropertyList();
                    setCurrentStatus(3);
                  } else {
                    metadataPropertyStore.recheckConflict();
                  }

                  metadataPropertyStore.clearReusablePropertyNameChangeIndexes();
                }}
                disabled={editIndex !== null}
              >
                {metadataPropertyStore.isReadyToReuse ? '完成' : '重新校验'}
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
                <div>已成功复用属性</div>
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
                  metadataPropertyStore.resetReusablePropeties();
                  metadataPropertyStore.changeCurrentTabStatus('list');
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
                  metadataPropertyStore.resetReusablePropeties();
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

export default ReuseProperties;
