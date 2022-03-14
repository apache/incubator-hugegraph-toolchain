import React, { useContext, useCallback, useLayoutEffect } from 'react';
import { observer } from 'mobx-react';
import { Embedded, Input, Button, Message, Tooltip } from 'hubble-ui';

import { GraphManagementStoreContext } from '../../stores';
import HintIcon from '../../assets/imgs/ic_question_mark.svg';
import { isNull } from 'lodash-es';
import { useTranslation } from 'react-i18next';

const commonInputProps = {
  size: 'medium',
  width: 420
};

const NewGraphConfig: React.FC = observer(() => {
  const { t } = useTranslation();
  const isRequiredInputProps = {
    ...commonInputProps,
    isRequired: true,
    requiredErrorMessage: t('addition.common.required')
  };
  const graphManagementStore = useContext(GraphManagementStoreContext);

  const handleCancel = useCallback(() => {
    graphManagementStore.switchCreateNewGraph(false);
    graphManagementStore.resetGraphDataConfig('new');
    graphManagementStore.switchValidateStatus(false);
    graphManagementStore.resetValidateErrorMessage();
  }, [graphManagementStore]);

  const handleCreate = useCallback(async () => {
    graphManagementStore.switchValidateStatus(true);

    if (!graphManagementStore.validate('new')) {
      return;
    }

    await graphManagementStore.AddGraphData();

    if (graphManagementStore.requestStatus.AddGraphData === 'success') {
      Message.success({
        content: t('addition.newGraphConfig.create-success'),
        size: 'medium',
        showCloseIcon: false
      });

      graphManagementStore.fetchGraphDataList();
      handleCancel();
    }

    if (graphManagementStore.requestStatus.AddGraphData === 'failed') {
      Message.error({
        content: graphManagementStore.errorInfo.AddGraphData.message,
        size: 'medium',
        showCloseIcon: false
      });
    }
  }, [graphManagementStore, handleCancel]);

  useLayoutEffect(() => {
    const inputElement = document.querySelector('.input-password input');

    if (!isNull(inputElement)) {
      inputElement.setAttribute('type', 'password');
    }
  }, [graphManagementStore.showCreateNewGraph]);

  return (
    <Embedded
      title={t('addition.newGraphConfig.graph-create')}
      className="graph-management-list-data-config"
      onClose={handleCancel}
      visible={graphManagementStore.showCreateNewGraph}
    >
      <div className="graph-management-list-create-content">
        <div>
          <div>
            <span>{t('addition.newGraphConfig.id')}:</span>
            <Tooltip
              placement="right"
              title={t('addition.newGraphConfig.id-desc')}
              type="dark"
            >
              <img src={HintIcon} alt="hint" />
            </Tooltip>
            <Input
              {...isRequiredInputProps}
              maxLen={48}
              placeholder={t('addition.common.format-error-desc')}
              errorMessage={
                graphManagementStore.isValidated &&
                graphManagementStore.validateErrorMessage.name !== ''
                  ? graphManagementStore.validateErrorMessage.name
                  : null
              }
              value={graphManagementStore.newGraphData.name}
              onChange={graphManagementStore.mutateGraphDataConfig(
                'name',
                'new'
              )}
            />
          </div>
          <div>
            <span>{t('addition.newGraphConfig.name')}:</span>
            <Tooltip
              placement="right"
              title={t('addition.newGraphConfig.name-desc')}
              type="dark"
            >
              <img src={HintIcon} alt="hint" />
            </Tooltip>
            <Input
              {...isRequiredInputProps}
              maxLen={48}
              placeholder={t('addition.common.format-error-desc')}
              errorMessage={
                graphManagementStore.isValidated &&
                graphManagementStore.validateErrorMessage.graph !== ''
                  ? graphManagementStore.validateErrorMessage.graph
                  : null
              }
              value={graphManagementStore.newGraphData.graph}
              onChange={graphManagementStore.mutateGraphDataConfig(
                'graph',
                'new'
              )}
            />
          </div>
          <div>
            <span>{t('addition.newGraphConfig.host')}:</span>
            <Input
              {...isRequiredInputProps}
              placeholder={t('addition.newGraphConfig.host-desc')}
              errorMessage={
                graphManagementStore.isValidated &&
                graphManagementStore.validateErrorMessage.host !== ''
                  ? graphManagementStore.validateErrorMessage.host
                  : null
              }
              value={graphManagementStore.newGraphData.host}
              onChange={graphManagementStore.mutateGraphDataConfig(
                'host',
                'new'
              )}
            />
          </div>
          <div>
            <span>{t('addition.newGraphConfig.port')}:</span>
            <Input
              {...isRequiredInputProps}
              placeholder={t('addition.newGraphConfig.host-desc')}
              errorMessage={
                graphManagementStore.isValidated &&
                graphManagementStore.validateErrorMessage.port !== ''
                  ? graphManagementStore.validateErrorMessage.port
                  : null
              }
              value={graphManagementStore.newGraphData.port}
              onChange={graphManagementStore.mutateGraphDataConfig(
                'port',
                'new'
              )}
            />
          </div>
          <div>
            <span>{t('addition.newGraphConfig.username')}:</span>
            <Input
              {...commonInputProps}
              placeholder={t('addition.newGraphConfig.not-required-desc')}
              errorMessage={
                graphManagementStore.isValidated &&
                graphManagementStore.validateErrorMessage
                  .usernameAndPassword !== ''
                  ? graphManagementStore.validateErrorMessage
                      .usernameAndPassword
                  : null
              }
              value={graphManagementStore.newGraphData.username}
              onChange={graphManagementStore.mutateGraphDataConfig(
                'username',
                'new'
              )}
            />
          </div>
          <div className="input-password">
            <span>{t('addition.newGraphConfig.password')}:</span>
            <Input
              {...commonInputProps}
              placeholder={t('addition.newGraphConfig.not-required-desc')}
              errorMessage={
                graphManagementStore.isValidated &&
                graphManagementStore.validateErrorMessage
                  .usernameAndPassword !== ''
                  ? graphManagementStore.validateErrorMessage
                      .usernameAndPassword
                  : null
              }
              value={graphManagementStore.newGraphData.password}
              onChange={graphManagementStore.mutateGraphDataConfig(
                'password',
                'new'
              )}
            />
          </div>
          <div>
            <div style={{ width: 420 }}>
              <Button
                type="primary"
                size="medium"
                style={{ width: 78 }}
                onClick={handleCreate}
              >
                {t('addition.newGraphConfig.create')}
              </Button>
              <Button
                size="medium"
                style={{
                  marginLeft: 12,
                  width: 78
                }}
                onClick={handleCancel}
              >
                {t('addition.common.cancel')}
              </Button>
            </div>
          </div>
        </div>
      </div>
    </Embedded>
  );
});

export default NewGraphConfig;
