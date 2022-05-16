import React, {
  useContext,
  useCallback,
  useEffect,
  useState,
  useLayoutEffect
} from 'react';
import { observer } from 'mobx-react';
import {
  Embedded,
  Input,
  Button,
  Dropdown,
  Pagination,
  Modal,
  Message,
  Tooltip
} from 'hubble-ui';
import { useLocation } from 'wouter';
import { isNull } from 'lodash-es';
import { motion } from 'framer-motion';
import Highlighter from 'react-highlight-words';

import { GraphManagementStoreContext } from '../../stores';
import HintIcon from '../../assets/imgs/ic_question_mark.svg';
import { GraphData } from '../../stores/types/GraphManagementStore/graphManagementStore';
import { useTranslation } from 'react-i18next';

const commonInputProps = {
  size: 'medium',
  width: 420
};

const listVariants = {
  initial: {
    opacity: 0,
    y: -10
  },
  animate: {
    opacity: 1,
    y: 0,
    transition: {
      duration: 1,
      ease: 'easeInOut'
    }
  }
};

const GraphManagementList: React.FC = observer(() => {
  const graphManagementStore = useContext(GraphManagementStoreContext);
  const [width, setWidth] = useState(0);

  const handlePageChange = useCallback(
    (e: React.ChangeEvent<HTMLInputElement>) => {
      graphManagementStore.mutatePageNumber(Number(e.target.value));
      graphManagementStore.fetchGraphDataList();
    },
    [graphManagementStore]
  );

  useLayoutEffect(() => {
    const inputElement = document.querySelector('.input-password input');

    if (!isNull(inputElement)) {
      inputElement.setAttribute('type', 'password');
    }
  }, [graphManagementStore.showCreateNewGraph]);

  useEffect(() => {
    const pg = document.querySelector(
      '.new-fc-one-pagination-pager'
    ) as Element;

    if (!!pg) {
      const styles = getComputedStyle(pg);
      const width = styles.width as string;

      setWidth(+width.split('px')[0]);
    }
  }, [
    graphManagementStore.graphDataPageConfig.pageTotal,
    graphManagementStore.graphDataPageConfig.pageNumber
  ]);

  return (
    <motion.div initial="initial" animate="animate" variants={listVariants}>
      {graphManagementStore.graphData.map((data, index) => (
        <GraphManagementListItem {...data} index={index} key={data.id} />
      ))}
      <div style={{ position: 'relative', marginTop: 16 }}>
        {graphManagementStore.graphDataPageConfig.pageTotal > 1 &&
          (graphManagementStore.showCreateNewGraph ||
            graphManagementStore.selectedEditIndex !== null) && (
            <div
              style={{
                position: 'absolute',
                background: 'rgba(255, 255, 255, 0.5)',
                width,
                height: 33,
                left: `calc(540px - ${width / 2}px)`,
                zIndex: 9
              }}
            ></div>
          )}
        <Pagination
          size="medium"
          pageSize={10}
          hideOnSinglePage
          showSizeChange={false}
          showPageJumper={false}
          total={graphManagementStore.graphDataPageConfig.pageTotal}
          pageNo={graphManagementStore.graphDataPageConfig.pageNumber}
          onPageNoChange={handlePageChange}
        />
      </div>
    </motion.div>
  );
});

const GraphManagementListItem: React.FC<
  GraphData & {
    index: number;
  }
> = observer(({ id, name, graph, host, port, enabled, create_time, index }) => {
  const { t } = useTranslation();
  const graphManagementStore = useContext(GraphManagementStoreContext);
  const [_, setLocation] = useLocation();
  const [isEditing, setEditingState] = useState(false);

  const handleCancel = useCallback(() => {
    graphManagementStore.resetGraphDataConfig('edit');
    graphManagementStore.changeSelectedEditIndex(null);
    graphManagementStore.switchValidateStatus(false);
    graphManagementStore.resetValidateErrorMessage();
    setEditingState(false);
  }, [graphManagementStore]);

  const handleSave = useCallback(async () => {
    graphManagementStore.switchValidateStatus(true);

    if (!graphManagementStore.validate('edit')) {
      return;
    }

    await graphManagementStore.upgradeGraphData(id);

    if (graphManagementStore.requestStatus.upgradeGraphData === 'success') {
      Message.success({
        content: t('addition.graphManagementList.save-scuccess'),
        size: 'medium',
        showCloseIcon: false
      });

      graphManagementStore.fetchGraphDataList();
      handleCancel();
    }

    if (graphManagementStore.requestStatus.upgradeGraphData === 'failed') {
      Message.error({
        content: graphManagementStore.errorInfo.upgradeGraphData.message,
        size: 'medium',
        showCloseIcon: false
      });
    }
  }, [graphManagementStore, handleCancel, id]);

  const handleDropdownClick = useCallback(
    (index: number) => (e: { key: string; item: object }) => {
      const { name } = graphManagementStore.graphData[index];

      if (e.key === 'edit') {
        setEditingState(true);
        graphManagementStore.fillInGraphDataConfig(index);
        graphManagementStore.changeSelectedEditIndex(index);
      }

      if (e.key === 'delete') {
        Modal.confirm({
          title: t('addition.graphManagementList.graph-del'),
          content: (
            <div className="graph-data-delete-confirm">
              {`${t('addition.common.del-comfirm')} ${name} ${t(
                'addition.common.ask'
              )}`}
              <br />
              {t('addition.graphManagementList.graph-del-comfirm-msg')}
            </div>
          ),
          buttonSize: 'medium',
          okText: t('addition.common.confirm'),
          onOk: async () => {
            await graphManagementStore.deleteGraphData(id);

            if (
              graphManagementStore.requestStatus.deleteGraphData === 'success'
            ) {
              Message.success({
                content: t('addition.common.del-success'),
                size: 'medium',
                showCloseIcon: false
              });

              graphManagementStore.fetchGraphDataList();
            }

            if (
              graphManagementStore.requestStatus.deleteGraphData === 'failed'
            ) {
              Message.error({
                content: graphManagementStore.errorInfo.deleteGraphData.message,
                size: 'medium',
                showCloseIcon: false
              });
            }
          }
        });
      }
    },
    [graphManagementStore, id]
  );

  const handleVisit = useCallback(() => {
    setLocation(`/graph-management/${id}/data-analyze`);
  }, [id, setLocation]);

  const dropdownList = [
    {
      label: t('addition.common.edit'),
      value: 'edit'
    },
    {
      label: t('addition.common.del'),
      value: 'delete'
    }
  ];

  const disabledDropdownList = [
    {
      label: t('addition.common.edit'),
      value: 'edit',
      disabled: true
    },
    {
      label: t('addition.common.del'),
      value: 'delete'
    }
  ];
  return isEditing ? (
    <Embedded
      title={t('addition.graphManagementList.graph-edit')}
      className="graph-management-list-data-config"
      onClose={handleCancel}
      visible={isEditing}
    >
      <div className="graph-management-list-create-content">
        <div>
          <div>
            <span>{t('addition.graphManagementList.id')}:</span>
            <Tooltip
              placement="right"
              title={t('addition.graphManagementList.id-desc')}
              type="dark"
            >
              <img src={HintIcon} alt="hint" />
            </Tooltip>
            <Input
              {...commonInputProps}
              disabled
              value={graphManagementStore.editGraphData.name}
            />
          </div>
          <div>
            <span>{t('addition.graphManagementList.name')}:</span>
            <Tooltip
              placement="right"
              title={t('addition.graphManagementList.name-desc')}
              type="dark"
            >
              <img src={HintIcon} alt="hint" />
            </Tooltip>
            <Input
              {...commonInputProps}
              disabled
              value={graphManagementStore.editGraphData.graph}
            />
          </div>
          <div>
            <span>{t('addition.graphManagementList.host')}:</span>
            <Input
              {...commonInputProps}
              disabled
              value={graphManagementStore.editGraphData.host}
            />
          </div>
          <div>
            <span>{t('addition.graphManagementList.port')}:</span>
            <Input
              {...commonInputProps}
              placeholder={t('addition.graphManagementList.port-desc')}
              disabled
              value={graphManagementStore.editGraphData.port}
            />
          </div>
          <div>
            <span>{t('addition.graphManagementList.username')}:</span>
            <Input
              {...commonInputProps}
              errorMessage={
                graphManagementStore.isValidated &&
                graphManagementStore.validateErrorMessage
                  .usernameAndPassword !== ''
                  ? graphManagementStore.validateErrorMessage
                      .usernameAndPassword
                  : null
              }
              value={graphManagementStore.editGraphData.username}
              onChange={graphManagementStore.mutateGraphDataConfig(
                'username',
                'edit'
              )}
            />
          </div>
          <div id="graph-password">
            <span>{t('addition.graphManagementList.password')}:</span>
            <Input
              {...commonInputProps}
              errorMessage={
                graphManagementStore.isValidated &&
                graphManagementStore.validateErrorMessage
                  .usernameAndPassword !== ''
                  ? graphManagementStore.validateErrorMessage
                      .usernameAndPassword
                  : null
              }
              value={graphManagementStore.editGraphData.password}
              onChange={graphManagementStore.mutateGraphDataConfig(
                'password',
                'edit'
              )}
              originInputProps={{
                type: 'password'
              }}
            />
          </div>
          <div>
            <div style={{ width: 420 }}>
              <Button
                type="primary"
                size="medium"
                style={{ width: 78 }}
                onClick={handleSave}
              >
                {t('addition.common.save')}
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
  ) : (
    <div className="graph-management-list" key={id}>
      <div className="graph-management-list-item">
        <div className="graph-management-list-title">
          {t('addition.graphManagementList.id')}
        </div>
        <div className="graph-management-list-data">
          <span title={name}>
            <Highlighter
              highlightClassName="graph-management-list-highlight"
              searchWords={[graphManagementStore.isSearched.value]}
              autoEscape={true}
              textToHighlight={name}
            />
          </span>
        </div>
      </div>
      <div className="graph-management-list-item">
        <div className="graph-management-list-title">
          {t('addition.graphManagementList.name')}
        </div>
        <div className="graph-management-list-data">
          <span title={graph}>
            <Highlighter
              highlightClassName="graph-management-list-highlight"
              searchWords={[graphManagementStore.isSearched.value]}
              autoEscape={true}
              textToHighlight={graph}
            />
          </span>
        </div>
      </div>
      <div className="graph-management-list-item">
        <div className="graph-management-list-title">
          {t('addition.graphManagementList.host')}
        </div>
        <div className="graph-management-list-data" title={host}>
          {host}
        </div>
      </div>
      <div className="graph-management-list-item">
        <div className="graph-management-list-title">
          {t('addition.graphManagementList.port')}
        </div>
        <div className="graph-management-list-data" title={String(port)}>
          {port}
        </div>
      </div>
      <div className="graph-management-list-item">
        <div className="graph-management-list-title">
          {t('addition.graphManagementList.creation-time')}
        </div>
        <div className="graph-management-list-data" title={create_time}>
          {create_time}
        </div>
      </div>
      <div className="graph-management-list-manipulation">
        <Button
          size="medium"
          style={{ width: 78, marginRight: 12 }}
          disabled={
            !enabled ||
            graphManagementStore.showCreateNewGraph === true ||
            (graphManagementStore.selectedEditIndex !== null &&
              graphManagementStore.selectedEditIndex !== index)
          }
          onClick={handleVisit}
        >
          {t('addition.graphManagementList.visit')}
        </Button>
        <Dropdown.Button
          options={enabled ? dropdownList : disabledDropdownList}
          title={t('addition.common.more')}
          size="medium"
          trigger={['click']}
          width={78}
          onHandleMenuClick={handleDropdownClick(index)}
          disabled={
            graphManagementStore.showCreateNewGraph === true ||
            (graphManagementStore.selectedEditIndex !== null &&
              graphManagementStore.selectedEditIndex !== index)
          }
        />
      </div>
    </div>
  );
});

export default GraphManagementList;
