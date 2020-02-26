import React, { useState, useCallback, useContext, useEffect } from 'react';
import { observer } from 'mobx-react';
import classnames from 'classnames';
import { useLocation, useRoute } from 'wouter';

import { Select, Tooltip, PopLayer } from '@baidu/one-ui';

import BackIcon from '../../assets/imgs/ic_topback.svg';
import ArrowIcon from '../../assets/imgs/ic_arrow_white.svg';
import DataAnalyzeIconNormal from '../../assets/imgs/ic_shuju_normal.svg';
import DataAnalyzeIconPressed from '../../assets/imgs/ic_shuju_pressed.svg';
import MetaDataManagementIconNormal from '../../assets/imgs/ic_yuanshuju_normal.svg';
import MetaDataManagementIconPressed from '../../assets/imgs/ic_yuanshuju_pressed.svg';
import SidebarExpandIcon from '../../assets/imgs/ic_cebianzhankai.svg';
import SidebarCollapseIcon from '../../assets/imgs/ic_cebianshouqi.svg';
import {
  GraphManagementStoreContext,
  DataAnalyzeStoreContext
} from '../../stores';

const GraphManagementSidebar: React.FC = observer(() => {
  const [match, params] = useRoute('/graph-management/:id/:category');

  const graphManagementStore = useContext(GraphManagementStoreContext);
  const dataAnalyzeStore = useContext(DataAnalyzeStoreContext);
  const [_, setLocation] = useLocation();
  // caution
  const [sidebarIndex, setSidebarIndex] = useState(0);
  const [isShowNamePop, switchShowNamePop] = useState(false);
  const optionClassName = 'data-analyze-sidebar-options';
  const activeOptionClassName = optionClassName + ' selected';

  const sidebarWrapperClassName = classnames({
    'data-analyze-sidebar': true,
    expand: graphManagementStore.isExpanded
  });

  const sidebarGoBackClassName = classnames({
    'data-analyze-sidebar-go-back': true,
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

  const handleOptionClick = useCallback(
    (index: number) => () => {
      setSidebarIndex(index);

      switch (index) {
        case 0:
          setLocation(`/graph-management/${params!.id}/data-analyze`);
          return;
        case 1:
          setLocation(`/graph-management/${params!.id}/metadata-configs`);
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
        setSidebarIndex(0);
        return;

      case 'metadata-configs':
        setSidebarIndex(1);
        return;
    }
  }, [params]);

  // prevent ERROR: Rendered more hooks than during the previous render
  // if this block stays before any usexxx hooks, p/n render hooks is not equal
  if (!match) {
    return null;
  }

  return (
    <ul className={sidebarWrapperClassName}>
      <li
        className={sidebarGoBackClassName}
        onClick={() => {
          setLocation('/');
        }}
      >
        <Tooltip
          placement="right"
          title={graphManagementStore.isExpanded ? '' : '返回图管理'}
          type="dark"
        >
          <img src={BackIcon} alt="返回" />
        </Tooltip>
        {graphManagementStore.isExpanded && <span>返回图管理</span>}
      </li>
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
                  alt="选择图"
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
      {sidebarIndex === 0 ? (
        <li className={activeOptionClassName} onClick={handleOptionClick(0)}>
          <Tooltip
            placement="right"
            title={graphManagementStore.isExpanded ? '' : '数据分析'}
            type="dark"
          >
            <img src={DataAnalyzeIconPressed} alt="数据分析" />
          </Tooltip>
          {graphManagementStore.isExpanded && <span>数据分析</span>}
        </li>
      ) : (
        <li className={optionClassName} onClick={handleOptionClick(0)}>
          <Tooltip
            placement="right"
            title={graphManagementStore.isExpanded ? '' : '数据分析'}
            type="dark"
          >
            <img src={DataAnalyzeIconNormal} alt="数据分析" />
          </Tooltip>
          {graphManagementStore.isExpanded && <span>数据分析</span>}
        </li>
      )}
      {sidebarIndex === 1 ? (
        <li className={activeOptionClassName} onClick={handleOptionClick(1)}>
          <Tooltip
            placement="right"
            title={graphManagementStore.isExpanded ? '' : '元数据配置'}
            type="dark"
          >
            <img src={MetaDataManagementIconPressed} alt="元数据配置" />
          </Tooltip>
          {graphManagementStore.isExpanded && <span>元数据配置</span>}
        </li>
      ) : (
        <li className={optionClassName} onClick={handleOptionClick(1)}>
          <Tooltip
            placement="right"
            title={graphManagementStore.isExpanded ? '' : '元数据配置'}
            type="dark"
          >
            <img src={MetaDataManagementIconNormal} alt="元数据配置" />
          </Tooltip>
          {graphManagementStore.isExpanded && <span>元数据配置</span>}
        </li>
      )}
      {/* {sidebarIndex === 2 ? (
          <li className={activeOptionClassName} onClick={handleOptionClick(2)}>
            <Tooltip
              placement="right"
              title={isExpand ? '' : '数据管理'}
              type="dark"
            >
              <img src={ManagementIconPressed} alt="数据管理" />
            </Tooltip>
            {isExpand && <span>数据管理</span>}
          </li>
        ) : (
          <li className={optionClassName} onClick={handleOptionClick(2)}>
            <Tooltip
              placement="right"
              title={isExpand ? '' : '数据管理'}
              type="dark"
            >
              <img src={ManagementIconNormal} alt="数据管理" />
            </Tooltip>
            {isExpand && <span>数据管理</span>}
          </li>
        )} */}
      <li
        className="data-analyze-sidebar-expand-control"
        onClick={handleExpandClick}
      >
        {graphManagementStore.isExpanded ? (
          <img src={SidebarCollapseIcon} alt="折叠" />
        ) : (
          <img src={SidebarExpandIcon} alt="展开" />
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
      id => (e: any) => {
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
