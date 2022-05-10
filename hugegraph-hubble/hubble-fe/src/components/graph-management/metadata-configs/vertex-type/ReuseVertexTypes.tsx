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
} from 'hubble-ui';

import { GraphManagementStoreContext } from '../../../../stores';
import MetadataConfigsRootStore from '../../../../stores/GraphManagementStore/metadataConfigsStore/metadataConfigsStore';

import PassIcon from '../../../../assets/imgs/ic_pass.svg';

import './ReuseVertexTypes.less';
import { cloneDeep } from 'lodash-es';
import { useTranslation } from 'react-i18next';

const ReuseVertexTypes: React.FC = observer(() => {
  const graphManagementStore = useContext(GraphManagementStoreContext);
  const metadataConfigsRootStore = useContext(MetadataConfigsRootStore);
  const { vertexTypeStore } = metadataConfigsRootStore;
  const { t } = useTranslation();
  const [currentStatus, setCurrentStatus] = useState(1);
  // acutally the name, not id in database
  const [selectedId, mutateSelectedId] = useState<[] | string>([]);
  const [selectedList, mutateSelectedList] = useState<string[]>([]);

  // step 2
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
  vertexTypeStore.validateReuseErrorMessage.vertexType.toUpperCase();
  vertexTypeStore.validateReuseErrorMessage.property.toUpperCase();
  vertexTypeStore.validateReuseErrorMessage.property_index.toUpperCase();

  const vertexTypeColumnConfigs = [
    {
      title: t('addition.common.vertex-name'),
      dataIndex: 'name',
      width: '50%',
      render(text: string, records: any, index: number) {
        if (index !== vertexTypeEditIndex) {
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
            placeholder={t('addition.message.edge-name-rule')}
            errorLocation="layer"
            errorMessage={vertexTypeStore.validateReuseErrorMessage.vertexType}
            value={
              vertexTypeStore.editedCheckedReusableData!.vertexlabel_conflicts[
                index
              ].entity.name
            }
            onChange={(e: any) => {
              // remove validate message after user input changes
              vertexTypeStore.resetValidateReuseErrorMessage('vertexType');

              const editedCheckedReusableData = cloneDeep(
                vertexTypeStore.editedCheckedReusableData!
              );

              editedCheckedReusableData.vertexlabel_conflicts[
                index
              ].entity.name = e.value;

              vertexTypeStore.mutateEditedReusableData(
                editedCheckedReusableData
              );
            }}
            originInputProps={{
              onBlur() {
                vertexTypeStore.validateReuseData(
                  'vertexType',
                  vertexTypeStore.checkedReusableData!.vertexlabel_conflicts[
                    index
                  ].entity.name,
                  vertexTypeStore.editedCheckedReusableData!
                    .vertexlabel_conflicts[index].entity.name
                );
              }
            }}
          />
        );
      }
    },
    {
      title: t('addition.edge.verification-result'),
      dataIndex: 'status',
      width: '30%',
      render(value: string, records: any, index: number) {
        let classname = '';
        let text = '';

        if (vertexTypeStore.reusableVertexTypeNameChangeIndexes.has(index)) {
          return (
            <div
              className="reuse-properties-validate-duplicate"
              style={{ marginLeft: 0 }}
            >
              {t('addition.edge.be-verified')}
            </div>
          );
        }

        switch (value) {
          case 'DUPNAME':
            classname = 'reuse-properties-validate-duplicate';
            text = t('addition.message.duplicate-name');
            break;

          case 'DEP_CONFLICT':
            classname = 'reuse-properties-validate-duplicate';
            text = t('addition.message.dependency-conflict');
            break;

          case 'EXISTED':
            classname = 'reuse-properties-validate-exist';
            text = t('addition.message.already-exist');
            break;

          case 'PASSED':
            classname = 'reuse-properties-validate-pass';
            text = t('addition.message.pass');
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
      title: t('addition.operate.operate'),
      dataIndex: 'manipulation',
      width: '20%',
      render(_: never, records: any, index: number) {
        if (index === vertexTypeEditIndex) {
          const originalName = vertexTypeStore.checkedReusableData!
            .vertexlabel_conflicts[index].entity.name;
          const changedName = vertexTypeStore.editedCheckedReusableData!
            .vertexlabel_conflicts[index].entity.name;
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
                    !vertexTypeStore.validateReuseData(
                      'vertexType',
                      originalName,
                      changedName
                    )
                  ) {
                    return;
                  }

                  vertexTypeStore.mutateReuseData(
                    'vertexType',
                    originalName,
                    changedName
                  );
                  setVertexTypeEditIndex(null);
                  vertexTypeStore.mutateReusableVertexTypeChangeIndexes(index);
                }}
              >
                {t('addition.common.save')}
              </span>
              <span
                className="metadata-properties-manipulation"
                onClick={() => {
                  vertexTypeStore.resetValidateReuseErrorMessage('vertexType');
                  setVertexTypeEditIndex(null);
                  vertexTypeStore.resetEditedReusableVertexTypeName(index);
                }}
              >
                {t('addition.common.cancel')}
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
              {t('addition.operate.rename')}
            </span>
            <span
              className="metadata-properties-manipulation"
              style={{
                color: vertexTypeEditIndex === null ? '#2b65ff' : '#999'
              }}
              onClick={() => {
                if (vertexTypeEditIndex !== null) {
                  return;
                }

                setPropertyEditIndex(null);

                // remove selected status of the property in <Transfer />
                const newSelectedList = [...selectedList].filter(
                  (property) =>
                    property !==
                    vertexTypeStore.editedCheckedReusableData!
                      .vertexlabel_conflicts[index].entity.name
                );

                mutateSelectedList(newSelectedList);

                // notice: useState hooks cannot sync updated state value, so the length is still 1
                if (selectedList.length === 1) {
                  setCurrentStatus(1);
                  // remove edit status after return previous
                  vertexTypeStore.clearReusableNameChangeIndexes();
                  return;
                }

                vertexTypeStore.deleteReuseData('vertexlabel_conflicts', index);
              }}
            >
              {t('addition.common.del')}
            </span>
          </div>
        );
      }
    }
  ];

  const metadataPropertyColumnConfigs = [
    {
      title: t('addition.common.property-name'),
      dataIndex: 'name',
      width: '50%',
      render(text: string, records: any, index: number) {
        if (index !== propertyEditIndex) {
          return text;
        }

        return (
          <Input
            size="medium"
            width={370}
            placeholder={t('addition.message.edge-name-rule')}
            errorLocation="layer"
            errorMessage={vertexTypeStore.validateReuseErrorMessage.property}
            value={
              vertexTypeStore.editedCheckedReusableData!.propertykey_conflicts[
                index
              ].entity.name
            }
            onChange={(e: any) => {
              // remove validate message after user input changes
              vertexTypeStore.resetValidateReuseErrorMessage('property');

              const editedCheckedReusableData = cloneDeep(
                vertexTypeStore.editedCheckedReusableData!
              );

              editedCheckedReusableData.propertykey_conflicts[
                index
              ].entity.name = e.value;

              vertexTypeStore.mutateEditedReusableData(
                editedCheckedReusableData
              );
            }}
            originInputProps={{
              onBlur() {
                vertexTypeStore.validateReuseData(
                  'property',
                  vertexTypeStore.checkedReusableData!.propertykey_conflicts[
                    index
                  ].entity.name,
                  vertexTypeStore.editedCheckedReusableData!
                    .propertykey_conflicts[index].entity.name
                );
              }
            }}
          />
        );
      }
    },
    {
      title: t('addition.common.data-type'),
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
      title: t('addition.edge.verification-result'),
      dataIndex: 'status',
      align: 'center',
      width: '15%',
      render(value: string, records: any, index: number) {
        let classname = '';
        let text = '';

        if (vertexTypeStore.reusablePropertyNameChangeIndexes.has(index)) {
          return (
            <div className="reuse-properties-validate-duplicate">
              {t('addition.edge.be-verified')}
            </div>
          );
        }

        switch (value) {
          case 'DUPNAME':
            classname = 'reuse-properties-validate-duplicate';
            text = t('addition.message.duplicate-name');
            break;

          case 'DEP_CONFLICT':
            classname = 'reuse-properties-validate-duplicate';
            text = t('addition.message.dependency-conflict');
            break;

          case 'EXISTED':
            classname = 'reuse-properties-validate-exist';
            text = t('addition.message.already-exist');
            break;

          case 'PASSED':
            classname = 'reuse-properties-validate-pass';
            text = t('addition.message.pass');
            break;
        }

        return <div className={classname}>{text}</div>;
      }
    },
    {
      title: t('addition.operate.operate'),
      dataIndex: 'manipulation',
      width: '20%',
      render(_: never, records: any, index: number) {
        if (index === propertyEditIndex) {
          const originalName = vertexTypeStore.checkedReusableData!
            .propertykey_conflicts[index].entity.name;
          const changedName = vertexTypeStore.editedCheckedReusableData!
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
                    !vertexTypeStore.validateReuseData(
                      'property',
                      originalName,
                      changedName
                    )
                  ) {
                    return;
                  }

                  vertexTypeStore.mutateReuseData(
                    'property',
                    originalName,
                    changedName
                  );

                  setPropertyEditIndex(null);
                  vertexTypeStore.mutateReusablePropertyNameChangeIndexes(
                    index
                  );
                }}
              >
                {t('addition.common.save')}
              </span>
              <span
                className="metadata-properties-manipulation"
                onClick={() => {
                  vertexTypeStore.resetValidateReuseErrorMessage('property');
                  setPropertyEditIndex(null);
                  vertexTypeStore.resetEditedReusablePropertyName(index);
                }}
              >
                {t('addition.common.cancel')}
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
              {t('addition.operate.rename')}
            </span>
            <span
              className="metadata-properties-manipulation"
              style={{
                color: propertyEditIndex === null ? '#2b65ff' : '#999'
              }}
              onClick={() => {
                if (propertyEditIndex !== null) {
                  return;
                }

                setPropertyEditIndex(null);

                vertexTypeStore.deleteReuseData('propertykey_conflicts', index);
              }}
            >
              {t('addition.common.del')}
            </span>
          </div>
        );
      }
    }
  ];

  const metadataPropertyIndexColumnConfigs = [
    {
      title: t('addition.common.property-index-name'),
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
            placeholder={t('addition.message.edge-name-rule')}
            errorLocation="layer"
            errorMessage={
              vertexTypeStore.validateReuseErrorMessage.property_index
            }
            value={
              vertexTypeStore.editedCheckedReusableData!
                .propertyindex_conflicts[index].entity.name
            }
            onChange={(e: any) => {
              vertexTypeStore.resetValidateReuseErrorMessage('property_index');

              const editedCheckedReusableData = cloneDeep(
                vertexTypeStore.editedCheckedReusableData!
              );

              editedCheckedReusableData.propertyindex_conflicts[
                index
              ].entity.name = e.value;

              vertexTypeStore.mutateEditedReusableData(
                editedCheckedReusableData
              );
            }}
            originInputProps={{
              onBlur() {
                vertexTypeStore.validateReuseData(
                  'property_index',
                  vertexTypeStore.checkedReusableData!.propertyindex_conflicts[
                    index
                  ].entity.name,
                  vertexTypeStore.editedCheckedReusableData!
                    .propertyindex_conflicts[index].entity.name
                );
              }
            }}
          />
        );
      }
    },
    {
      title: t('addition.vertex.corresponding-vertex-type'),
      dataIndex: 'owner',
      width: '15%'
    },
    {
      title: t('addition.edge.verification-result'),
      dataIndex: 'status',
      align: 'center',
      width: '15%',
      render(value: string, records: any, index: number) {
        let classname = '';
        let text = '';

        if (vertexTypeStore.reusablePropertyIndexNameChangeIndexes.has(index)) {
          return (
            <div className="reuse-properties-validate-duplicate">
              {t('addition.edge.be-verified')}
            </div>
          );
        }

        switch (value) {
          case 'DUPNAME':
            classname = 'reuse-properties-validate-duplicate';
            text = t('addition.message.duplicate-name');
            break;

          case 'DEP_CONFLICT':
            classname = 'reuse-properties-validate-duplicate';
            text = t('addition.message.dependency-conflict');
            break;

          case 'EXISTED':
            classname = 'reuse-properties-validate-exist';
            text = t('addition.message.already-exist');
            break;

          case 'PASSED':
            classname = 'reuse-properties-validate-pass';
            text = t('addition.message.pass');
            break;
        }

        return <div className={classname}>{text}</div>;
      }
    },
    {
      title: t('addition.operate.operate'),
      dataIndex: 'manipulation',
      width: '20%',
      render(_: never, records: any, index: number) {
        if (index === propertyIndexEditIndex) {
          const originalName = vertexTypeStore.checkedReusableData!
            .propertyindex_conflicts[index].entity.name;
          const changedName = vertexTypeStore.editedCheckedReusableData!
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
                    !vertexTypeStore.validateReuseData(
                      'property_index',
                      originalName,
                      changedName
                    )
                  ) {
                    return;
                  }

                  vertexTypeStore.mutateReuseData(
                    'property_index',
                    originalName,
                    changedName
                  );

                  setPropertyIndexEditIndex(null);
                  vertexTypeStore.mutateReusablePropertyIndexNameChangeIndexes(
                    index
                  );
                }}
              >
                {t('addition.common.save')}
              </span>
              <span
                className="metadata-properties-manipulation"
                onClick={() => {
                  vertexTypeStore.resetValidateReuseErrorMessage(
                    'property_index'
                  );
                  setPropertyIndexEditIndex(null);
                  vertexTypeStore.resetEditedReusablePropertyIndexName(index);
                }}
              >
                {t('addition.common.cancel')}
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
              {t('addition.operate.rename')}
            </span>
            <span
              className="metadata-properties-manipulation"
              style={{
                color: propertyIndexEditIndex === null ? '#2b65ff' : '#999'
              }}
              onClick={() => {
                if (propertyIndexEditIndex !== null) {
                  return;
                }

                setPropertyIndexEditIndex(null);

                vertexTypeStore.deleteReuseData(
                  'propertyindex_conflicts',
                  index
                );
              }}
            >
              {t('addition.common.del')}
            </span>
          </div>
        );
      }
    }
  ];

  useEffect(() => {
    // unlike metadata properties, all vertex types only needs here(in reuse)
    vertexTypeStore.fetchVertexTypeList({ fetchAll: true });
  }, [vertexTypeStore]);

  return (
    <div className="reuse-properties-wrapper">
      <div className="metadata-title">
        {t('addition.operate.reuse-vertex-type')}
      </div>
      <div
        style={{
          marginTop: 8,
          fontSize: 12,
          color: '#666',
          lineHeight: '18px'
        }}
      >
        {t('addition.message.reuse-vertex-type-notice')}
      </div>
      <div className="reuse-steps">
        <Steps current={currentStatus}>
          {[
            t('addition.menu.select-reuse-item'),
            t('addition.menu.confirm-reuse-item'),
            t('addition.menu.complete-reuse')
          ].map((title: string, index: number) => (
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
          ))}
        </Steps>

        {currentStatus === 1 && (
          <>
            <div className="reuse-properties-row">
              <div className="reuse-properties-row-name" style={{ width: 106 }}>
                <span className="metdata-essential-form-options">*</span>
                <span>{t('addition.newGraphConfig.id')}：</span>
              </div>
              <Select
                width={420}
                placeholder={t(
                  'addition.message.select-reuse-graph-placeholder'
                )}
                size="medium"
                showSearch={false}
                onChange={(selectedName: string) => {
                  mutateSelectedId(selectedName);

                  const id = graphManagementStore.idList.find(
                    ({ name }) => name === selectedName
                  )!.id;

                  mutateSelectedList([]);

                  vertexTypeStore.fetchVertexTypeList({
                    reuseId: Number(id)
                  });

                  const enable = graphManagementStore.graphData.find(
                    ({ name }) => name === selectedName
                  )?.enabled;

                  if (!enable) {
                    Message.error({
                      content: t('data-analyze.hint.graph-disabled'),
                      size: 'medium',
                      showCloseIcon: false
                    });
                  }
                }}
                value={selectedId}
              >
                {graphManagementStore.idList
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
              <div className="reuse-properties-row-name" style={{ width: 106 }}>
                <span className="metdata-essential-form-options">*</span>
                <span>{t('addition.operate.reuse-vertex-type')}：</span>
              </div>
              <Transfer
                treeName={t('addition.common.vertex-type')}
                allDataMap={vertexTypeStore.reusableVertexTypeDataMap}
                candidateList={vertexTypeStore.reusableVertexTypes.map(
                  ({ name }) => name
                )}
                selectedList={selectedList}
                showSearchBox={false}
                candidateTreeStyle={{
                  width: 355,
                  fontSize: 14
                }}
                selectedTreeStyle={{
                  width: 355,
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
                  vertexTypeStore.checkConflict(
                    selectedId as string,
                    selectedList
                  );

                  if (
                    vertexTypeStore.requestStatus.checkConflict === 'failed'
                  ) {
                    Message.error({
                      content: vertexTypeStore.errorMessage,
                      size: 'medium',
                      showCloseIcon: false
                    });
                  }
                }}
              >
                {t('addition.operate.next-step')}
              </Button>
              <Button
                size="medium"
                style={{ width: 88 }}
                onClick={() => {
                  mutateSelectedId('');
                  mutateSelectedList([]);
                  vertexTypeStore.resetReusableVertexTypes();

                  vertexTypeStore.vertexTypes.length === 0
                    ? vertexTypeStore.changeCurrentTabStatus('empty')
                    : vertexTypeStore.changeCurrentTabStatus('list');
                }}
              >
                {t('addition.operate.de-multiplexing')}
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
              {t('addition.common.selected-vertex-type')}
            </div>
            <Table
              columns={vertexTypeColumnConfigs}
              dataSource={
                vertexTypeStore.editedCheckedReusableData
                  ? vertexTypeStore.editedCheckedReusableData!.vertexlabel_conflicts.map(
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
              {t('addition.common.selected-property')}
            </div>
            <Table
              columns={metadataPropertyColumnConfigs}
              dataSource={
                vertexTypeStore.editedCheckedReusableData
                  ? vertexTypeStore.editedCheckedReusableData!.propertykey_conflicts.map(
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
              {t('addition.common.selected-property-index')}
            </div>
            <Table
              columns={metadataPropertyIndexColumnConfigs}
              dataSource={
                vertexTypeStore.editedCheckedReusableData
                  ? vertexTypeStore.editedCheckedReusableData!.propertyindex_conflicts.map(
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
                  width: vertexTypeStore.isReadyToReuse ? 78 : 88,
                  marginRight: 12
                }}
                onClick={async () => {
                  if (vertexTypeStore.isReadyToReuse) {
                    await vertexTypeStore.reuseVertexType();

                    if (
                      vertexTypeStore.requestStatus.reuseVertexType === 'failed'
                    ) {
                      Message.error({
                        content: vertexTypeStore.errorMessage,
                        size: 'medium',
                        showCloseIcon: false
                      });

                      return;
                    }

                    vertexTypeStore.mutatePageNumber(1);
                    vertexTypeStore.fetchVertexTypeList();
                    vertexTypeStore.clearReusableNameChangeIndexes();
                    setCurrentStatus(3);
                  } else {
                    vertexTypeStore.clearReusableNameChangeIndexes();
                    vertexTypeStore.recheckConflict();
                  }
                }}
                disabled={
                  vertexTypeEditIndex !== null ||
                  propertyEditIndex !== null ||
                  propertyIndexEditIndex !== null
                }
              >
                {vertexTypeStore.isReadyToReuse
                  ? t('addition.operate.complete')
                  : t('addition.edge.verified-again')}
              </Button>
              <Button
                size="medium"
                style={{ width: 88 }}
                onClick={() => {
                  setCurrentStatus(1);
                  // remove edit status after return previous
                  vertexTypeStore.clearReusableNameChangeIndexes();
                }}
              >
                {t('addition.operate.previous-step')}
              </Button>
            </div>
          </>
        )}

        {currentStatus === 3 && (
          <div className="reuse-properties-complete-hint">
            <div className="reuse-properties-complete-hint-description">
              <img src={PassIcon} alt={t('addition.message.reuse-complete')} />
              <div>
                <div>{t('addition.message.reuse-complete')}</div>
                <div>{t('addition.message.vertex-type-reuse-success')}</div>
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
                  vertexTypeStore.resetReusableVertexTypes();
                  vertexTypeStore.changeCurrentTabStatus('list');
                }}
              >
                {t('addition.operate.back-to-view')}
              </Button>
              <Button
                size="medium"
                style={{ width: 88 }}
                onClick={() => {
                  mutateSelectedId([]);
                  mutateSelectedList([]);
                  vertexTypeStore.resetReusableVertexTypes();
                  setCurrentStatus(1);
                }}
              >
                {t('addition.operate.continue-reuse')}
              </Button>
            </div>
          </div>
        )}
      </div>
    </div>
  );
});

export default ReuseVertexTypes;
