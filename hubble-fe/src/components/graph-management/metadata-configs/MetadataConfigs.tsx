import React, { useState, useEffect, useContext } from 'react';
import { observer } from 'mobx-react';
import { useRoute, useLocation, Params } from 'wouter';
import classnames from 'classnames';
import { AnimatePresence } from 'framer-motion';
import { Radio, Menu, Modal, Button } from '@baidu/one-ui';

import { MetadataProperties } from './property';
import { VertexTypeList } from './vertex-type';
import { EdgeTypeList } from './edge-type';
import { PropertyIndex } from './property-index';
import { GraphView } from './graph-view';
import DataAnalyzeStore from '../../../stores/GraphManagementStore/dataAnalyzeStore/dataAnalyzeStore';
import MetadataConfigsRootStore from '../../../stores/GraphManagementStore/metadataConfigsStore/metadataConfigsStore';
import './MetadataConfigs.less';
import { AppStoreContext, GraphManagementStoreContext } from '../../../stores';
import ActiveTableIcon from '../../../assets/imgs/ic_liebiaomoshi_white.svg';
import TableIcon from '../../../assets/imgs/ic_liebiaomoshi_black.svg';
import ActiveShowGraphIcon from '../../../assets/imgs/ic_tumoshi_white.svg';
import ShowGraphIcon from '../../../assets/imgs/ic_tumoshi_black.svg';

const MetadataConfig: React.FC = observer(() => {
  const appStore = useContext(AppStoreContext);
  const dataAnalyzeStore = useContext(DataAnalyzeStore);
  const graphManagementStore = useContext(GraphManagementStoreContext);
  const metadataConfigRootStore = useContext(MetadataConfigsRootStore);
  const [viewMode, setViewMode] = useState('list');
  const [selectedMenuItem, setSelectedMenuItem] = useState('property');
  const [match, params] = useRoute('/graph-management/:id/metadata-configs');
  const [_, setLocation] = useLocation();

  const handleRadioGroupChange = (e: React.ChangeEvent<HTMLButtonElement>) => {
    setViewMode(e.target.value);
  };

  const handleMenuItemChange = ({ key }: { key: string }) => {
    // reset store current tab status to default
    switch (selectedMenuItem) {
      case 'vertex-type':
        metadataConfigRootStore.vertexTypeStore.changeCurrentTabStatus('list');
      case 'edge-type':
        metadataConfigRootStore.edgeTypeStore.changeCurrentTabStatus('list');
    }

    setSelectedMenuItem(key);
  };

  const wrapperClassName = classnames({
    'metadata-configs': true,
    'metadata-configs-with-expand-sidebar': graphManagementStore.isExpanded
  });

  const renderListView = () => {
    switch (selectedMenuItem) {
      case 'property':
        return <MetadataProperties key="metadata-property" />;
      case 'vertex-type':
        return <VertexTypeList key="vertex-type-list" />;
      case 'edge-type':
        return <EdgeTypeList key="edge-type-list" />;
      case 'property-index':
        return <PropertyIndex key="property-index-list" />;
    }
  };

  // Caution: Preitter will automatically add 'params' behind 'match' in array,
  // which is not equal each time
  /* eslint-disable */
  useEffect(() => {
    window.scrollTo(0, 0);
    graphManagementStore.fetchIdList();

    if (match && params !== null) {
      appStore.setCurrentId(Number(params.id));
      // fetch node colors
      dataAnalyzeStore.setCurrentId(Number(params.id));
      dataAnalyzeStore.fetchAllNodeStyle();
      dataAnalyzeStore.fetchAllEdgeStyle();
      metadataConfigRootStore.setCurrentId(Number(params.id));
      metadataConfigRootStore.fetchIdList();
    }

    return () => {
      metadataConfigRootStore.dispose();
    };
  }, [metadataConfigRootStore, match, params?.id]);

  return (
    <section className={wrapperClassName}>
      <div className="metadata-configs-content">
        <div className="metadata-configs-content-mode">
          <Radio.Group value={viewMode} onChange={handleRadioGroupChange}>
            <Radio.Button value="list">
              <div className="metadata-configs-content-mode-button">
                <img
                  src={viewMode === 'list' ? ActiveTableIcon : TableIcon}
                  alt="table mode"
                />
                <span>列表模式</span>
              </div>
            </Radio.Button>
            <Radio.Button value="graph">
              <div className="metadata-configs-content-mode-button">
                <img
                  src={
                    viewMode === 'graph' ? ActiveShowGraphIcon : ShowGraphIcon
                  }
                  alt="graph mode"
                />
                <span>图模式</span>
              </div>
            </Radio.Button>
          </Radio.Group>
        </div>
        {viewMode === 'list' ? (
          <>
            <Menu
              mode="horizontal"
              menuLevel={2}
              selectedKeys={[selectedMenuItem]}
              onClick={handleMenuItemChange}
            >
              <Menu.Item key="property">属性</Menu.Item>
              <Menu.Item key="vertex-type">顶点类型</Menu.Item>
              <Menu.Item key="edge-type">边类型</Menu.Item>
              <Menu.Item key="property-index">属性索引</Menu.Item>
            </Menu>
            <AnimatePresence exitBeforeEnter>
              {renderListView()}
            </AnimatePresence>
          </>
        ) : (
          <GraphView />
        )}
      </div>
      <Modal
        title="无法访问"
        footer={[
          <Button
            size="medium"
            type="primary"
            style={{ width: 88 }}
            onClick={() => {
              setLocation('/');
            }}
          >
            返回首页
          </Button>
        ]}
        visible={
          !metadataConfigRootStore.metadataPropertyStore
            .validateLicenseOrMemories ||
          !metadataConfigRootStore.vertexTypeStore.validateLicenseOrMemories ||
          !metadataConfigRootStore.edgeTypeStore.validateLicenseOrMemories ||
          !metadataConfigRootStore.metadataPropertyIndexStore
            .validateLicenseOrMemories
        }
        destroyOnClose
        needCloseIcon={false}
      >
        <div style={{ color: '#333' }}>
          {metadataConfigRootStore.metadataPropertyStore.errorMessage}
        </div>
      </Modal>
    </section>
  );
});

export default MetadataConfig;
