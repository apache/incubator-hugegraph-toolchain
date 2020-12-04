import React, { useContext, useCallback, useLayoutEffect } from 'react';
import { observer } from 'mobx-react';
import { Embedded, Input, Button, Message, Tooltip } from '@baidu/one-ui';

import { GraphManagementStoreContext } from '../../stores';
import HintIcon from '../../assets/imgs/ic_question_mark.svg';
import { isNull } from 'lodash-es';

const commonInputProps = {
  size: 'medium',
  width: 420
};

const isRequiredInputProps = {
  ...commonInputProps,
  isRequired: true,
  requiredErrorMessage: '必填项'
};

const NewGraphConfig: React.FC = observer(() => {
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
        content: '创建成功',
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
      title="创建图"
      className="graph-management-list-data-config"
      onClose={handleCancel}
      visible={graphManagementStore.showCreateNewGraph}
    >
      <div className="graph-management-list-create-content">
        <div>
          <div>
            <span>图ID:</span>
            <Tooltip
              placement="right"
              title="为创建的图设置唯一标识的ID"
              type="dark"
            >
              <img src={HintIcon} alt="hint" />
            </Tooltip>
            <Input
              {...isRequiredInputProps}
              maxLen={48}
              placeholder="必须以字母开头，允许出现英文、数字、下划线"
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
            <span>图名称:</span>
            <Tooltip
              placement="right"
              title="填写需要连接的图的名称"
              type="dark"
            >
              <img src={HintIcon} alt="hint" />
            </Tooltip>
            <Input
              {...isRequiredInputProps}
              maxLen={48}
              placeholder="必须以字母开头，允许出现英文、数字、下划线"
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
            <span>主机名:</span>
            <Input
              {...isRequiredInputProps}
              placeholder="请输入主机名"
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
            <span>端口号:</span>
            <Input
              {...isRequiredInputProps}
              placeholder="请输入端口号"
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
            <span>用户名:</span>
            <Input
              {...commonInputProps}
              placeholder="未设置则无需填写"
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
            <span>密码:</span>
            <Input
              {...commonInputProps}
              placeholder="未设置则无需填写"
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
                创建
              </Button>
              <Button
                size="medium"
                style={{
                  marginLeft: 12,
                  width: 78
                }}
                onClick={handleCancel}
              >
                取消
              </Button>
            </div>
          </div>
        </div>
      </div>
    </Embedded>
  );
});

export default NewGraphConfig;
