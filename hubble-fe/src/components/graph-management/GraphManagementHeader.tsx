import React, { useContext, useCallback } from 'react';
import { observer } from 'mobx-react';
import { Input, Button } from '@baidu/one-ui';
import { GraphManagementStoreContext } from '../../stores';

const styles = {
  marginLeft: '20px',
  width: 88
};

const GraphManagementHeader: React.FC = observer(() => {
  const graphManagementStore = useContext(GraphManagementStoreContext);

  const handleLayoutSwitch = useCallback(
    (flag: boolean) => () => {
      graphManagementStore.switchCreateNewGraph(flag);
    },
    [graphManagementStore]
  );

  const handleSearchChange = useCallback(
    (e: React.ChangeEvent<HTMLInputElement>) => {
      graphManagementStore.mutateSearchWords(e.target.value);
    },
    [graphManagementStore]
  );

  const handleSearch = useCallback(() => {
    graphManagementStore.mutatePageNumber(1);
    graphManagementStore.swtichIsSearchedStatus(true);
    graphManagementStore.fetchGraphDataList();
  }, [graphManagementStore]);

  const handleClearSearch = useCallback(() => {
    graphManagementStore.mutateSearchWords('');
    graphManagementStore.mutatePageNumber(1);
    graphManagementStore.swtichIsSearchedStatus(false);
    graphManagementStore.fetchGraphDataList();
  }, [graphManagementStore]);

  return (
    <div className="graph-management-header">
      {graphManagementStore.licenseInfo &&
      graphManagementStore.licenseInfo.edition === 'community' ? (
        <div className="graph-management-header-description-community">
          <div>图管理</div>
          <div>
            {graphManagementStore.licenseInfo.edition === 'community'
              ? '社区版'
              : '商业版'}
            ：支持图上限 {graphManagementStore.licenseInfo.allowed_graphs + ' '}
            个，图磁盘上限 {graphManagementStore.licenseInfo.allowed_datasize}
          </div>
        </div>
      ) : (
        <span className="graph-management-header-description">图管理</span>
      )}
      {/* <span>图管理</span> */}
      {/* <div className="graph-management-header-description-community">
        <div>图管理</div>
        <div>社区版：支持图上限3个，图磁盘上限100G</div>
      </div> */}
      <Input.Search
        size="medium"
        width={200}
        placeholder="搜索图名称或ID"
        value={graphManagementStore.searchWords}
        onChange={handleSearchChange}
        onSearch={handleSearch}
        onClearClick={handleClearSearch}
        isShowDropDown={false}
        disabled={
          graphManagementStore.showCreateNewGraph ||
          graphManagementStore.selectedEditIndex !== null
        }
      />
      <Button
        type="primary"
        size="medium"
        style={styles}
        onClick={handleLayoutSwitch(true)}
        disabled={
          (graphManagementStore.licenseInfo &&
            graphManagementStore.graphData.length >=
              graphManagementStore.licenseInfo.allowed_graphs) ||
          graphManagementStore.showCreateNewGraph ||
          graphManagementStore.selectedEditIndex !== null
        }
      >
        创建图
      </Button>
    </div>
  );
});

export default GraphManagementHeader;
