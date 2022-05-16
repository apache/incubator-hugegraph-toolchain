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
import { cloneDeep } from 'lodash-es';
import { useTranslation } from 'react-i18next';

import { GraphManagementStoreContext } from '../../../../stores';
import MetadataConfigsRootStore from '../../../../stores/GraphManagementStore/metadataConfigsStore/metadataConfigsStore';

import PassIcon from '../../../../assets/imgs/ic_pass.svg';

import './ReuseProperties.less';

const ReuseProperties: React.FC = observer(() => {
  const graphManagementStore = useContext(GraphManagementStoreContext);
  const metadataConfigsRootStore = useContext(MetadataConfigsRootStore);
  const { metadataPropertyStore } = metadataConfigsRootStore;
  const { t } = useTranslation();
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
      title: t('addition.common.property-name'),
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
            placeholder={t('addition.message.edge-name-rule')}
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
      title: t('addition.common.data-type'),
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
      title: t('addition.edge.verification-result'),
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
                {t('addition.common.save')}
              </span>
              <span
                className="metadata-properties-manipulation"
                onClick={() => {
                  setEditIndex(null);
                  metadataPropertyStore.resetValidateRenameReuseProperty();
                  metadataPropertyStore.resetEditedReusablePropertyName(index);
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
                color: editIndex === null ? '#2b65ff' : '#999'
              }}
              onClick={() => {
                if (editIndex !== null) {
                  return;
                }

                setEditIndex(index);
              }}
            >
              {t('addition.operate.rename')}
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
                    (property) =>
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
              {t('addition.common.del')}
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
      <div className="metadata-title">
        {t('addition.operate.reuse-property')}
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
              <div className="reuse-properties-row-name">
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

                  metadataPropertyStore.fetchMetadataPropertyList({
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
              <div className="reuse-properties-row-name">
                <span className="metdata-essential-form-options">*</span>
                <span>{t('addition.operate.reuse-property')}：</span>
              </div>
              <Transfer
                treeName={t('addition.common.property')}
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

                  if (
                    metadataPropertyStore.requestStatus.checkConflict ===
                    'failed'
                  ) {
                    Message.error({
                      content: metadataPropertyStore.errorMessage,
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
                  metadataPropertyStore.resetReusablePropeties();
                  metadataPropertyStore.metadataProperties.length === 0
                    ? metadataPropertyStore.changeCurrentTabStatus('empty')
                    : metadataPropertyStore.changeCurrentTabStatus('list');
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
              style={{ marginTop: 18, marginBottom: 16 }}
            >
              {t('addition.common.selected-property')}
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
                {metadataPropertyStore.isReadyToReuse
                  ? t('addition.operate.complete')
                  : t('addition.edge.verified-again')}
              </Button>
              <Button
                size="medium"
                style={{ width: 88 }}
                onClick={() => {
                  setCurrentStatus(1);
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
                <div>{t('addition.message.reuse-property-success')}</div>
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
                {t('addition.operate.back-to-view')}
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
                {t('addition.operate.continue-reuse')}
              </Button>
            </div>
          </div>
        )}
      </div>
    </div>
  );
});

export default ReuseProperties;
