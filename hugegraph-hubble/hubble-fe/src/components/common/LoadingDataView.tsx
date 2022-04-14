import React from 'react';
import { observer } from 'mobx-react';
import { useTranslation } from 'react-i18next';

import LoadingBackIcon from '../../assets/imgs/ic_loading_back.svg';
import LoadingFrontIcon from '../../assets/imgs/ic_loading_front.svg';

import './LoadingDataView.less';

export interface LoadingDataViewProps {
  isLoading: boolean;
  emptyView?: React.ReactElement;
}

const LoadingDataView: React.FC<LoadingDataViewProps> = observer(
  ({ isLoading, emptyView }) => {
    const { t } = useTranslation();

    return isLoading ? (
      <div className="table-data-loading-wrapper">
        <div className="table-data-loading-bg">
          <img
            className="table-data-loading-back"
            src={LoadingBackIcon}
            alt="load background"
          />
          <img
            className="table-data-loading-front"
            src={LoadingFrontIcon}
            alt="load spinner"
          />
        </div>
        <span>{t('common.loading-data')}...</span>
      </div>
    ) : emptyView ? (
      emptyView
    ) : null;
  }
);

export default LoadingDataView;
