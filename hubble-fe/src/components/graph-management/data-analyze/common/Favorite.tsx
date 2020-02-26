import React, { useState, useContext, useCallback } from 'react';
import { observer } from 'mobx-react';
import { Button, Input, Message } from '@baidu/one-ui';

import { DataAnalyzeStoreContext } from '../../../../stores';

export interface FavoriteProps {
  handlePop: (flag: boolean) => void;
  queryStatement?: string;
  isEdit?: boolean;
  id?: number;
  name?: string;
}

const styles = {
  primaryButton: {
    width: 72,
    marginRight: 12
  },
  alert: {
    width: 320,
    fontSize: 12,
    marginTop: 4,
    color: '#f5535b'
  }
};

const Favorite: React.FC<FavoriteProps> = observer(
  ({ handlePop, queryStatement = '', isEdit = false, id, name = '' }) => {
    const dataAnalyzeStore = useContext(DataAnalyzeStoreContext);
    const initialText = isEdit ? name : '';
    const [inputValue, setInputValue] = useState(initialText);

    const handleChange = useCallback(({ value }) => {
      setInputValue(value);
    }, []);

    const handleAddQueryCollection = useCallback(async () => {
      await dataAnalyzeStore.addQueryCollection(inputValue, queryStatement);

      if (dataAnalyzeStore.requestStatus.addQueryCollection === 'success') {
        dataAnalyzeStore.setFavoritePopUp('');
        handlePop(false);
        setInputValue('');

        Message.success({
          content: isEdit ? '修改成功' : '收藏成功',
          size: 'medium',
          showCloseIcon: false
        });

        dataAnalyzeStore.fetchFavoriteQueries();
      }
    }, [dataAnalyzeStore, handlePop, inputValue, isEdit, queryStatement]);

    const handleEditQueryCollection = useCallback(async () => {
      await dataAnalyzeStore.editQueryCollection(
        id as number,
        inputValue,
        queryStatement
      );

      if (dataAnalyzeStore.requestStatus.editQueryCollection === 'success') {
        dataAnalyzeStore.setFavoritePopUp('');
        handlePop(false);
        setInputValue('');

        Message.success({
          content: '修改成功',
          size: 'medium',
          showCloseIcon: false
        });

        dataAnalyzeStore.fetchFavoriteQueries();
      }
    }, [dataAnalyzeStore, handlePop, id, inputValue, queryStatement]);

    const handleCancel = useCallback(
      (type: 'add' | 'edit') => () => {
        handlePop(false);
        dataAnalyzeStore.setFavoritePopUp('');
        setInputValue(initialText);
        dataAnalyzeStore.resetFavoriteRequestStatus(type);
      },
      [dataAnalyzeStore, handlePop, initialText]
    );

    return (
      <div className="data-analyze">
        <div className="query-tab-favorite">
          <span>{isEdit ? '修改名称' : '收藏语句'}</span>
          <Input
            size="large"
            width={320}
            maxLen={48}
            countMode="en"
            placeholder="请输入收藏名称"
            value={inputValue}
            onChange={handleChange}
            errorLocation="bottom"
          />
          {dataAnalyzeStore.requestStatus.addQueryCollection === 'failed' &&
            isEdit === false && (
              <div style={styles.alert}>
                {dataAnalyzeStore.errorInfo.addQueryCollection.message}
              </div>
            )}
          {dataAnalyzeStore.requestStatus.editQueryCollection === 'failed' &&
            isEdit === true && (
              <div style={styles.alert}>
                {dataAnalyzeStore.errorInfo.editQueryCollection.message}
              </div>
            )}
          <div className="query-tab-favorite-footer">
            <Button
              type="primary"
              size="medium"
              style={{ width: 60 }}
              disabled={inputValue.length === 0 || inputValue.length > 48}
              onClick={
                isEdit ? handleEditQueryCollection : handleAddQueryCollection
              }
            >
              {isEdit ? '保存' : '收藏'}
            </Button>
            <Button
              size="medium"
              style={{
                marginLeft: 12,
                width: 60
              }}
              onClick={handleCancel(isEdit ? 'edit' : 'add')}
            >
              取消
            </Button>
          </div>
        </div>
      </div>
    );
  }
);

export default Favorite;
