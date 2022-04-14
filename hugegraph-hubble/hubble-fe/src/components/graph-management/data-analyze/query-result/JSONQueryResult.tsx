import React, { useContext } from 'react';
import { observer } from 'mobx-react';
import ReactJsonView from 'react-json-view';
import 'codemirror/mode/javascript/javascript';

import { DataAnalyzeStoreContext } from '../../../../stores';

const JSONQueryResult: React.FC = observer(() => {
  const dataAnalyzeStore = useContext(DataAnalyzeStoreContext);

  return (
    <div style={{ position: 'absolute', top: 0, left: 0, padding: 8 }}>
      <ReactJsonView
        src={dataAnalyzeStore.originalGraphData.data.json_view.data}
        name={false}
        displayObjectSize={false}
        displayDataTypes={false}
        groupArraysAfterLength={50}
      />
    </div>
  );
});

export default JSONQueryResult;
