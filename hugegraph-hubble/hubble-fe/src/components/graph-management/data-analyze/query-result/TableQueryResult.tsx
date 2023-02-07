/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. The ASF
 * licenses this file to You under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

import React, { useContext, useCallback } from 'react';
import { observer } from 'mobx-react';
import { Table } from 'hubble-ui';
import { size } from 'lodash-es';

import { DataAnalyzeStoreContext } from '../../../../stores';

const TableQueryResult: React.FC = observer(() => {
  const dataAnalyzeStore = useContext(DataAnalyzeStoreContext);

  const columnConfigs = dataAnalyzeStore.originalGraphData.data.table_view.header.map(
    (title) => ({
      title,
      dataIndex: title,
      width:
        100 / size(dataAnalyzeStore.originalGraphData.data.table_view.header) +
        '%',
      render(text: any) {
        if (title === 'path') {
          return <PathItem items={text} />;
        }

        return JSON.stringify(text);
      }
    })
  );

  const handlePageChange = useCallback(
    (e: React.ChangeEvent<HTMLInputElement>) => {
      dataAnalyzeStore.mutatePageNumber('tableResult', Number(e.target.value));
    },
    [dataAnalyzeStore]
  );

  return (
    <div
      style={{
        position: 'absolute',
        top: 0,
        left: 0,
        padding: 8,
        width: '100%'
      }}
    >
      <Table
        columns={columnConfigs}
        dataSource={dataAnalyzeStore.originalGraphData.data.table_view.rows}
        pagination={{
          size: 'medium',
          pageSize: 10,
          hideOnSinglePage: true,
          showSizeChange: false,
          showPageJumper: false,
          total: dataAnalyzeStore.pageConfigs.tableResult.pageTotal,
          pageNo: dataAnalyzeStore.pageConfigs.tableResult.pageNumber,
          onPageNoChange: handlePageChange
        }}
      />
    </div>
  );
});

// item could be obejct array which needs serialization as well
const PathItem: React.FC<{ items: string[] }> = observer(({ items }) => (
  <>
    {items.map((item: string) => (
      <span className="query-result-content-table-path-item">
        {JSON.stringify(item)}
      </span>
    ))}
  </>
));

export default TableQueryResult;
