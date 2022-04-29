import React, { useState, useContext, useCallback, useEffect } from 'react';
import { observer } from 'mobx-react';
import classnames from 'classnames';
import { isUndefined, update } from 'lodash-es';
import { Drawer, Input, Button, Message } from 'hubble-ui';

import { DataAnalyzeStoreContext } from '../../../stores';
import { convertArrayToString } from '../../../stores/utils';
import { useTranslation } from 'react-i18next';

const DataAnalyzeInfoDrawer: React.FC = observer(() => {
  const { t } = useTranslation();
  const dataAnalyzeStore = useContext(DataAnalyzeStoreContext);
  const [isEdit, switchEdit] = useState(false);

  const handleDrawerClose = useCallback(() => {
    dataAnalyzeStore.switchShowScreenInfo(false);
    switchEdit(false);
  }, [dataAnalyzeStore]);

  const premitSave =
    !isEdit ||
    ((dataAnalyzeStore.editedSelectedGraphDataProperties.nonNullable.size ===
      0 ||
      [
        ...dataAnalyzeStore.validateEditableGraphDataPropertyErrorMessage!.nonNullable.values()
      ].every((value) => value === '')) &&
      (dataAnalyzeStore.editedSelectedGraphDataProperties.nullable.size === 0 ||
        [
          ...dataAnalyzeStore.validateEditableGraphDataPropertyErrorMessage!.nullable.values()
        ].every((value) => value === '')));

  const graphInfoItemClassName = classnames({
    'data-analyze-graph-node-info-item': true,
    'data-analyze-graph-node-info-item-disabled': isEdit
  });

  useEffect(() => {
    const handleOutSideClick = (e: MouseEvent) => {
      const drawerWrapper = document.querySelector(
        '.new-fc-one-drawer-content-wrapper'
      );

      if (
        !isEdit &&
        dataAnalyzeStore.isShowGraphInfo &&
        !dataAnalyzeStore.isClickOnNodeOrEdge &&
        drawerWrapper &&
        !drawerWrapper.contains(e.target as Element)
      ) {
        dataAnalyzeStore.switchShowScreenInfo(false);
      }
    };

    document.addEventListener('click', handleOutSideClick, false);

    return () => {
      document.removeEventListener('click', handleOutSideClick, false);
    };
  }, [dataAnalyzeStore, isEdit]);

  return (
    <Drawer
      title={
        isEdit
          ? t('addition.dataAnalyzeInfoDrawer.edit-details')
          : t('addition.dataAnalyzeInfoDrawer.data-details')
      }
      visible={dataAnalyzeStore.isShowGraphInfo}
      onClose={handleDrawerClose}
      mask={isEdit}
      maskClosable={false}
      footer={[
        <Button
          type="primary"
          size="medium"
          style={{ width: 60 }}
          disabled={!premitSave}
          onClick={async () => {
            if (!isEdit) {
              switchEdit(true);
              return;
            }

            const updatedInfo = await dataAnalyzeStore.updateGraphProperties();

            if (!isUndefined(updatedInfo)) {
              if (dataAnalyzeStore.graphInfoDataSet === 'node') {
                dataAnalyzeStore.visDataSet?.nodes.update({
                  id: updatedInfo.id,
                  properties: updatedInfo.properties,
                  title: `
                    <div class="tooltip-fields">
                      <div>${t('addition.common.vertex-type')}：</div>
                      <div>${updatedInfo.label}</div>
                    </div>
                    <div class="tooltip-fields">
                      <div>${t('addition.common.vertex-id')}：</div>
                      <div>${updatedInfo.id}</div>
                    </div>
                    ${Object.entries(updatedInfo.properties)
                      .map(([key, value]) => {
                        return `<div class="tooltip-fields">
                                  <div>${key}: </div>
                                  <div>${convertArrayToString(value)}</div>
                                </div>`;
                      })
                      .join('')}
                  `
                });
              }

              if (dataAnalyzeStore.graphInfoDataSet === 'edge') {
                dataAnalyzeStore.visDataSet?.edges.update({
                  id: updatedInfo.id,
                  properties: updatedInfo.properties,
                  title: `
                      <div class="tooltip-fields">
                        <div>${t('addition.common.edge-type')}：</div>
                        <div>${updatedInfo.label}</div>
                      </div>
                      <div class="tooltip-fields">
                        <div>${t('addition.common.edge-id')}：</div>
                        <div>${updatedInfo.id}</div>
                      </div>
                      ${Object.entries(updatedInfo.properties)
                        .map(([key, value]) => {
                          return `<div class="tooltip-fields">
                                    <div>${key}: </div>
                                    <div>${convertArrayToString(value)}</div>
                                  </div>
                                `;
                        })
                        .join('')}
                    `
                });
              }
            }

            if (
              dataAnalyzeStore.requestStatus.updateGraphProperties === 'success'
            ) {
              Message.success({
                content: t('addition.common.save-scuccess'),
                size: 'medium',
                showCloseIcon: false
              });
            } else {
              Message.error({
                content: t('addition.common.save-fail'),
                size: 'medium',
                showCloseIcon: false
              });
            }

            handleDrawerClose();
          }}
          key="save"
        >
          {isEdit ? t('addition.common.save') : t('addition.common.edit')}
        </Button>,
        <Button
          size="medium"
          style={{ width: 60 }}
          onClick={handleDrawerClose}
          key="close"
        >
          {t('addition.common.close')}
        </Button>
      ]}
    >
      <div className="data-analyze-graph-node-info">
        {dataAnalyzeStore.graphInfoDataSet === 'node' ? (
          <>
            <div className={graphInfoItemClassName}>
              <div>{t('addition.common.vertex-type')}：</div>
              <div>{dataAnalyzeStore.selectedGraphData.label}</div>
            </div>
            <div className={graphInfoItemClassName}>
              <div>{t('addition.common.vertex-id')}：</div>
              <div>{dataAnalyzeStore.selectedGraphData.id}</div>
            </div>
          </>
        ) : (
          <>
            <div className={graphInfoItemClassName}>
              <div>{t('addition.common.edge-type')}：</div>
              <div>{dataAnalyzeStore.selectedGraphLinkData.label}</div>
            </div>
            <div className={graphInfoItemClassName}>
              <div>{t('addition.common.edge-id')}：</div>
              <div>{dataAnalyzeStore.selectedGraphLinkData.id}</div>
            </div>
            <div className={graphInfoItemClassName}>
              <div>{t('addition.common.source')}：</div>
              <div>{dataAnalyzeStore.selectedGraphLinkData.source}</div>
            </div>
            <div className={graphInfoItemClassName}>
              <div>{t('addition.common.target')}：</div>
              <div>{dataAnalyzeStore.selectedGraphLinkData.target}</div>
            </div>
          </>
        )}

        {isEdit &&
          (dataAnalyzeStore.editedSelectedGraphDataProperties.primary.size !==
            0 ||
            dataAnalyzeStore.editedSelectedGraphDataProperties.nonNullable
              .size !== 0) && (
            <div style={{ marginTop: 24, marginBottom: 3 }}>
              {t('addition.common.required-property')}：
            </div>
          )}
        {[...dataAnalyzeStore.editedSelectedGraphDataProperties.primary].map(
          ([key, value], primaryKeyIndex) => (
            <div className={graphInfoItemClassName} key={key}>
              <div
                title={`${key}(${
                  dataAnalyzeStore.graphInfoDataSet === 'node'
                    ? t('addition.common.primary-key')
                    : t('addition.common.distinguishing-key')
                })${`${primaryKeyIndex + 1}`}`}
              >
                {key}(
                {dataAnalyzeStore.graphInfoDataSet === 'node'
                  ? t('addition.common.primary-key')
                  : t('addition.common.distinguishing-key')}
                {`${primaryKeyIndex + 1}`})：
              </div>
              <div>{value}</div>
            </div>
          )
        )}
        {[
          ...dataAnalyzeStore.editedSelectedGraphDataProperties.nonNullable
        ].map(([key, value], nonNullableIndex) => (
          <div
            className="data-analyze-graph-node-info-item"
            key={key}
            style={{
              alignItems: 'flex-start',
              marginTop: !isEdit ? 0 : nonNullableIndex === 0 ? 8 : 32
            }}
          >
            <div title={key}>{key}: </div>
            {!isEdit ? (
              <div>{convertArrayToString(value)}</div>
            ) : (
              <div>
                <Input
                  size="medium"
                  width={268}
                  placeholder={t('addition.common.property-input-desc')}
                  errorLocation="layer"
                  errorMessage={dataAnalyzeStore.validateEditableGraphDataPropertyErrorMessage!.nonNullable.get(
                    key
                  )}
                  value={dataAnalyzeStore.editedSelectedGraphDataProperties.nonNullable.get(
                    key
                  )}
                  onChange={(e: any) => {
                    dataAnalyzeStore.editGraphDataProperties(
                      'nonNullable',
                      key,
                      e.value
                    );

                    dataAnalyzeStore.validateGraphDataEditableProperties(
                      'nonNullable',
                      key
                    );
                  }}
                  onBlur={() => {
                    dataAnalyzeStore.validateGraphDataEditableProperties(
                      'nonNullable',
                      key
                    );
                  }}
                />
              </div>
            )}
          </div>
        ))}

        {isEdit &&
          dataAnalyzeStore.editedSelectedGraphDataProperties.nullable.size !==
            0 && (
            <div style={{ marginTop: 24 }}>
              {t('addition.common.nullable-property')}：
            </div>
          )}
        {[...dataAnalyzeStore.editedSelectedGraphDataProperties.nullable].map(
          ([key, value], nullableIndex) => (
            <div
              className="data-analyze-graph-node-info-item"
              key={key}
              style={{
                alignItems: 'flex-start',
                marginTop: !isEdit ? 0 : nullableIndex === 0 ? 8 : 32
              }}
            >
              <div title={key}>{key}: </div>
              {!isEdit ? (
                <div>{convertArrayToString(value)}</div>
              ) : (
                <div>
                  <Input
                    size="medium"
                    width={268}
                    placeholder={t('addition.common.property-input-desc')}
                    errorLocation="layer"
                    errorMessage={dataAnalyzeStore.validateEditableGraphDataPropertyErrorMessage!.nullable.get(
                      key
                    )}
                    value={dataAnalyzeStore.editedSelectedGraphDataProperties.nullable.get(
                      key
                    )}
                    onChange={(e: any) => {
                      dataAnalyzeStore.editGraphDataProperties(
                        'nullable',
                        key,
                        e.value
                      );

                      dataAnalyzeStore.validateGraphDataEditableProperties(
                        'nullable',
                        key
                      );
                    }}
                    onBlur={() => {
                      dataAnalyzeStore.validateGraphDataEditableProperties(
                        'nullable',
                        key
                      );
                    }}
                  />
                </div>
              )}
            </div>
          )
        )}
      </div>
    </Drawer>
  );
});

export default DataAnalyzeInfoDrawer;
