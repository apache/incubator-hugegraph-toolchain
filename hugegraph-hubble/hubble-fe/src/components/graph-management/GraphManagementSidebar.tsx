import React, { useState, useCallback, useContext, useEffect } from 'react';
import { observer } from 'mobx-react';
import classnames from 'classnames';
import { useLocation, useRoute } from 'wouter';
import { Select, Tooltip, PopLayer, Menu } from 'hubble-ui';

import {
  GraphManagementStoreContext,
  DataAnalyzeStoreContext,
  ImportManagerStoreContext
} from '../../stores';

import ArrowIcon from '../../assets/imgs/ic_arrow_white.svg';
import DataAnalyzeIconNormal from '../../assets/imgs/ic_shuju_normal.svg';
import DataAnalyzeIconPressed from '../../assets/imgs/ic_shuju_pressed.svg';
import MetaDataManagementIconNormal from '../../assets/imgs/ic_yuanshuju_normal.svg';
import MetaDataManagementIconPressed from '../../assets/imgs/ic_yuanshuju_pressed.svg';
import SidebarExpandIcon from '../../assets/imgs/ic_cebianzhankai.svg';
import SidebarCollapseIcon from '../../assets/imgs/ic_cebianshouqi.svg';
import DataImportIconNormal from '../../assets/imgs/ic_daorushuju_normal.svg';
import DataImportIconPressed from '../../assets/imgs/ic_daorushuju_pressed.svg';
import AsyncTaskManagerIconNormal from '../../assets/imgs/ic_renwuguanli_normal.svg';
import AsyncTaskManagerIconPressed from '../../assets/imgs/ic_renwuguanli_pressed.svg';
import { useTranslation } from 'react-i18next';

const GraphManagementSidebar: React.FC = observer(() => {
  const [match, params] = useRoute(
    '/graph-management/:id/:category/:subCategory?/:jobId?/:specific?/:importTasksSpecific?'
  );

  const graphManagementStore = useContext(GraphManagementStoreContext);
  const dataAnalyzeStore = useContext(DataAnalyzeStoreContext);
  const importManagerStore = useContext(ImportManagerStoreContext);
  const [_, setLocation] = useLocation();
  // caution
  const [sidebarKey, setSidebarKey] = useState('');
  const [isShowNamePop, switchShowNamePop] = useState(false);
  const { t } = useTranslation();

  const sidebarWrapperClassName = classnames({
    'data-analyze-sidebar': true,
    expand: graphManagementStore.isExpanded
  });

  const sidebarGraphSelectionClassName = classnames({
    'data-analyze-sidebar-graph-selection': true,
    expand: graphManagementStore.isExpanded
  });

  const sidebarGraphSelectionIconClassName = classnames({
    'data-analyze-sidebar-graph-selection-icon': true,
    expand: graphManagementStore.isExpanded
  });

  const sidebarMenuItemClassName = classnames({
    'data-analyze-sidebar-menu-item': true,
    expand: graphManagementStore.isExpanded
  });

  const handleOptionClick = useCallback(
    (key: string) => {
      setSidebarKey(key);

      switch (key) {
        case 'data-analyze':
          setLocation(`/graph-management/${params!.id}/data-analyze`);
          return;
        case 'metadata-configs':
          setLocation(`/graph-management/${params!.id}/metadata-configs`);
          return;
        case 'data-import':
          setLocation(
            `/graph-management/${params!.id}/data-import/import-manager`
          );
          // need to reset selectedJob here, it would change in the future
          importManagerStore.setSelectedJob(null);
          return;
        case 'async-tasks':
          setLocation(`/graph-management/${params!.id}/async-tasks`);
          return;
      }
    },
    [params, setLocation]
  );

  const handleSelectId = useCallback(
    (value: string) => {
      const id = graphManagementStore.idList.find(({ name }) => name === value)!
        .id;

      dataAnalyzeStore.resetIdState();
      setLocation(`/graph-management/${id}/data-analyze`);
    },
    [graphManagementStore.idList, dataAnalyzeStore, setLocation]
  );

  const handleExpandClick = useCallback(() => {
    graphManagementStore.switchExpanded(!graphManagementStore.isExpanded);
  }, [graphManagementStore]);

  // correctly highlight sidebar option
  useEffect(() => {
    if (params === null) {
      return;
    }

    switch (params.category) {
      case 'data-analyze':
        setSidebarKey('data-analyze');
        return;

      case 'metadata-configs':
        setSidebarKey('metadata-configs');
        return;

      case 'data-import': {
        setSidebarKey('data-import');

        return;
      }

      case 'async-tasks':
        setSidebarKey('async-tasks');
        return;
    }
  }, [params]);

  // prevent ERROR: Rendered more hooks than during the previous render
  // if this block stays before any usexxx hooks, p/n render hooks is not equal
  if (
    !match ||
    params?.subCategory === 'job-error-log' ||
    params?.jobId === 'task-error-log' ||
    (params?.category === 'async-tasks' && params?.jobId === 'result')
  ) {
    return null;
  }

  return (
    <ul className={sidebarWrapperClassName}>
      <li className={sidebarGraphSelectionClassName}>
        {!graphManagementStore.isExpanded ? (
          <PopLayer
            overlay={
              <GraphSelectMenu
                routeId={Number(params && params.id)}
                isShowNamePop={isShowNamePop}
                switchShowPop={switchShowNamePop}
              />
            }
            visible={isShowNamePop}
          >
            <div
              className="data-analyze-sidebar-dropdown-selection"
              onClick={() => {
                switchShowNamePop(!isShowNamePop);
              }}
            >
              <div className={sidebarGraphSelectionIconClassName}>G</div>
              <div className="data-analyze-sidebar-graph-selection-instruction">
                <img
                  src={ArrowIcon}
                  alt={t('addition.graphManagementSidebar.graph-select')}
                  style={{
                    transform: isShowNamePop ? 'rotate(180deg)' : 'rotate(0deg)'
                  }}
                />
              </div>
            </div>
          </PopLayer>
        ) : (
          <>
            <div className={sidebarGraphSelectionIconClassName}>G</div>
            <div>
              <Select
                options={graphManagementStore.idList.map(({ name }) => name)}
                size="medium"
                trigger="click"
                value={
                  graphManagementStore.idList.find(
                    ({ id }) => String(id) === params!.id
                  )!.name
                }
                width={150}
                onChange={handleSelectId}
                dropdownClassName="data-analyze-sidebar-select"
              >
                {graphManagementStore.idList.map(({ id, name }) => (
                  <Select.Option
                    value={name}
                    disabled={id === Number(params && params.id)}
                    key={id}
                  >
                    {name}
                  </Select.Option>
                ))}
              </Select>
            </div>
          </>
        )}
      </li>
      <div>
        <Menu
          mode="inline"
          selectedKeys={[sidebarKey]}
          inlineCollapsed={!graphManagementStore.isExpanded}
          onClick={(e: any) => {
            handleOptionClick(e.key);
          }}
          style={{
            width: graphManagementStore.isExpanded ? 200 : 60
          }}
        >
          <Menu.Item key="data-analyze">
            <div className={sidebarMenuItemClassName}>
              <img
                src={
                  sidebarKey === 'data-analyze'
                    ? DataAnalyzeIconPressed
                    : DataAnalyzeIconNormal
                }
                alt={t('addition.graphManagementSidebar.data-analysis')}
              />
              <div>{t('addition.graphManagementSidebar.data-analysis')}</div>
            </div>
          </Menu.Item>
          <Menu.Item key="metadata-configs">
            <div className={sidebarMenuItemClassName}>
              <img
                src={
                  sidebarKey === 'metadata-configs'
                    ? MetaDataManagementIconPressed
                    : MetaDataManagementIconNormal
                }
                alt={t('addition.graphManagementSidebar.metadata-config')}
              />
              <div>{t('addition.graphManagementSidebar.metadata-config')}</div>
            </div>
          </Menu.Item>
          <Menu.Item key="data-import">
            <div className={sidebarMenuItemClassName}>
              <img
                src={
                  sidebarKey === 'data-import'
                    ? DataImportIconPressed
                    : DataImportIconNormal
                }
                alt={t('addition.graphManagementSidebar.data-import')}
              />
              <div>{t('addition.graphManagementSidebar.data-import')}</div>
            </div>
          </Menu.Item>
          {/* <Menu.SubMenu
            key="sub-data-import"
            title={
              <div className={sidebarMenuItemClassName}>
                <img
                  src={
                    sidebarKey === 'import-tasks'
                      ? DataImportIconPressed
                      : DataImportIconNormal
                  }
                  alt="数据导入"
                />
                <div>数据导入</div>
              </div>
            }
          >
            <Menu.Item key="import-tasks">
              <div style={{ marginLeft: 24 }}>导入任务</div>
            </Menu.Item>
          </Menu.SubMenu> */}
          <Menu.Item key="async-tasks">
            <div className={sidebarMenuItemClassName}>
              <img
                src={
                  sidebarKey === 'async-tasks'
                    ? AsyncTaskManagerIconPressed
                    : AsyncTaskManagerIconNormal
                }
                alt={t('addition.graphManagementSidebar.task-management')}
              />
              <div>{t('addition.graphManagementSidebar.task-management')}</div>
            </div>
          </Menu.Item>
        </Menu>
      </div>
      <li
        className="data-analyze-sidebar-expand-control"
        onClick={handleExpandClick}
      >
        {graphManagementStore.isExpanded ? (
          <img src={SidebarCollapseIcon} alt={t('addition.common.fold')} />
        ) : (
          <img src={SidebarExpandIcon} alt={t('addition.common.open')} />
        )}
      </li>
    </ul>
  );
});

export interface GraphSelectMenuProps {
  routeId: number;
  isShowNamePop: boolean;
  switchShowPop: (flag: boolean) => void;
}

const GraphSelectMenu: React.FC<GraphSelectMenuProps> = observer(
  ({ routeId, switchShowPop }) => {
    const graphManagementStore = useContext(GraphManagementStoreContext);
    const dataAnalyzeStore = useContext(DataAnalyzeStoreContext);
    const [_, setLocation] = useLocation();

    const handleSelectDropdownId = useCallback(
      (id) => (e: any) => {
        const targetClassName = (e.target as Element).className;

        if (
          targetClassName !==
            'data-analyze-sidebar-dropdown-menu-item-wrapper disabled' &&
          targetClassName !== 'data-analyze-sidebar-dropdown-menu-item disabled'
        ) {
          switchShowPop(false);
          dataAnalyzeStore.resetIdState();
          setLocation(`/graph-management/${id}/data-analyze`);
        }
      },
      [dataAnalyzeStore, setLocation, switchShowPop]
    );

    useEffect(() => {
      const cb = (e: MouseEvent) => {
        if (
          (e.target as Element).className !==
            'data-analyze-sidebar-graph-selection-icon' &&
          (e.target as Element).className !==
            'data-analyze-sidebar-graph-selection-instruction' &&
          (e.target as Element).nodeName.toLowerCase() !== 'img' &&
          (e.target as Element).className !==
            'data-analyze-sidebar-dropdown-menu-item-wrapper disabled' &&
          (e.target as Element).className !==
            'data-analyze-sidebar-dropdown-menu-item disabled'
        ) {
          switchShowPop(false);
        }
      };

      window.addEventListener('click', cb, true);

      return () => {
        window.removeEventListener('click', cb);
      };
    }, [switchShowPop]);

    return (
      <div className="data-analyze">
        <div className="data-analyze-sidebar-dropdown-menu">
          {graphManagementStore.idList.map(({ id, name }) => {
            const dropdownItemWrapperClassName = classnames({
              'data-analyze-sidebar-dropdown-menu-item-wrapper': true,
              disabled: routeId === id
            });

            const dropdownItemClassName = classnames({
              'data-analyze-sidebar-dropdown-menu-item': true,
              disabled: routeId === id
            });

            return (
              <div
                className={dropdownItemWrapperClassName}
                onClick={handleSelectDropdownId(id)}
                key={id}
              >
                <div className={dropdownItemClassName}>{name}</div>
              </div>
            );
          })}
        </div>
      </div>
    );
  }
);

export default GraphManagementSidebar;
