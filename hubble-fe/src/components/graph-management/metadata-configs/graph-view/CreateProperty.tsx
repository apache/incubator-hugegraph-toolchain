import React, { useContext } from 'react';
import { observer } from 'mobx-react';
import { Drawer, Button, Input, Select, Message } from 'hubble-ui';
import { useTranslation } from 'react-i18next';

import MetadataConfigsRootStore from '../../../../stores/GraphManagementStore/metadataConfigsStore/metadataConfigsStore';

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

const CreateProperty: React.FC = observer(() => {
  const { metadataPropertyStore, graphViewStore } = useContext(
    MetadataConfigsRootStore
  );
  const { t } = useTranslation();
  const handleCloseDrawer = () => {
    graphViewStore.setCurrentDrawer('');
    metadataPropertyStore.resetNewProperties();
    metadataPropertyStore.resetValidateNewProperty();
  };

  return (
    <Drawer
      title={t('addition.operate.create-property')}
      width={548}
      destroyOnClose
      visible={graphViewStore.currentDrawer === 'create-property'}
      maskClosable={false}
      onClose={handleCloseDrawer}
      footer={[
        <Button
          type="primary"
          size="medium"
          style={{ width: 60 }}
          disabled={!metadataPropertyStore.isCreatedReady}
          onClick={async () => {
            metadataPropertyStore.validateNewProperty();

            if (!metadataPropertyStore.isCreatedReady) {
              return;
            }

            await metadataPropertyStore.addMetadataProperty();

            if (
              metadataPropertyStore.requestStatus.addMetadataProperty ===
              'success'
            ) {
              graphViewStore.setCurrentDrawer('');

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

            metadataPropertyStore.fetchMetadataPropertyList({ fetchAll: true });
            metadataPropertyStore.resetNewProperties();
          }}
        >
          {t('addition.newGraphConfig.create')}
        </Button>,
        <Button size="medium" style={{ width: 60 }} onClick={handleCloseDrawer}>
          {t('addition.common.cancel')}
        </Button>
      ]}
    >
      <div className="metadata-configs-drawer">
        <div className="metadata-graph-drawer-wrapper">
          <div className="metadata-graph-drawer">
            <div className="metadata-graph-drawer-options">
              <div
                className="metadata-graph-drawer-options-name"
                style={{ width: 67, marginRight: 14 }}
              >
                <span className="metdata-essential-form-options">*</span>
                <span>{t('addition.common.property-name')}:</span>
              </div>
              <Input
                size="medium"
                width={419}
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
                  // no autofocus here, it will automatically dispatch blur action
                  onBlur() {
                    metadataPropertyStore.validateNewProperty();
                  }
                }}
              />
            </div>
            <div className="metadata-graph-drawer-options">
              <div
                className="metadata-graph-drawer-options-name"
                style={{ width: 67, marginRight: 14 }}
              >
                <span className="metdata-essential-form-options">*</span>
                <span>{t('addition.common.data-type')}:</span>
              </div>
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
            </div>
            <div className="metadata-graph-drawer-options">
              <div
                className="metadata-graph-drawer-options-name"
                style={{ width: 67, marginRight: 14 }}
              >
                <span className="metdata-essential-form-options">*</span>
                <span>{t('addition.common.cardinal-number')}:</span>
              </div>
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
            </div>
          </div>
        </div>
      </div>
    </Drawer>
  );
});

export default CreateProperty;
