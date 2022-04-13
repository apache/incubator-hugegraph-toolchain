import React, { useContext } from 'react';
import { observer } from 'mobx-react';
import { Alert } from 'hubble-ui';

import { GraphManagementStoreContext } from '../../stores';

const GraphManagementLimitHint: React.FC = observer(() => {
  const graphManagementStore = useContext(GraphManagementStoreContext);
  let limitMessage = '';

  for (const value of Object.values(graphManagementStore.errorInfo)) {
    if (value.code === 401) {
      limitMessage = value.message;
      break;
    }
  }

  if (limitMessage === '') {
    return null;
  }

  return (
    <Alert
      content={limitMessage}
      type="error"
      closable={true}
    />
  );
});

export default GraphManagementLimitHint;
