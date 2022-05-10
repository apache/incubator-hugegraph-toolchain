import React, { useContext } from 'react';
import { observer } from 'mobx-react';
import { range, rangeRight } from 'lodash-es';
import { useTranslation } from 'react-i18next';
import classnames from 'classnames';
import { Radio, Switch, Input, Select, Button, Message } from 'hubble-ui';

import { DataImportRootStoreContext } from '../../../../../stores';

import ArrowIcon from '../../../../../assets/imgs/ic_arrow_16.svg';

const separators = [',', ';', '\\t', ' '];
const charsets = ['UTF-8', 'GBK', 'ISO-8859-1', 'US-ASCII'];
const dateFormat = [
  'yyyy-MM-dd',
  'yyyy-MM-dd HH:mm:ss',
  'yyyy-MM-dd HH:mm:ss.SSS'
];

const timezones = rangeRight(1, 13)
  .map((num) => `GMT-${num}`)
  .concat(['GMT'])
  .concat(range(1, 13).map((num) => `GMT+${num}`));

const styles = {
  smallGap: {
    marginBottom: 18
  },
  mediumGap: {
    marginBottom: 23
  }
};

const FileConfigs: React.FC = observer(() => {
  const { dataMapStore, serverDataImportStore } = useContext(
    DataImportRootStoreContext
  );
  const { t } = useTranslation();

  const expandClassName = classnames({
    'import-tasks-step-content-header-expand': dataMapStore.isExpandFileConfig,
    'import-tasks-step-content-header-collpase': !dataMapStore.isExpandFileConfig
  });

  const handleExpand = () => {
    dataMapStore.switchExpand('file', !dataMapStore.isExpandFileConfig);
  };

  return (
    dataMapStore.selectedFileInfo && (
      <div className="import-tasks-data-map">
        <div className="import-tasks-step-content-header">
          <span>{t('data-configs.file.title')}</span>
          <img
            src={ArrowIcon}
            alt="collpaseOrExpand"
            className={expandClassName}
            onClick={handleExpand}
          />
        </div>
        {dataMapStore.isExpandFileConfig && (
          <>
            <div className="import-tasks-data-options" style={styles.smallGap}>
              <span className="import-tasks-data-options-title">
                {t('data-configs.file.include-header')}:
              </span>
              <Switch
                size="large"
                disabled={dataMapStore.readOnly || dataMapStore.lock}
                checked={dataMapStore.selectedFileInfo!.file_setting.has_header}
                onChange={(checked: boolean) => {
                  dataMapStore.setFileConfig('has_header', checked);
                }}
              />
            </div>
            <div className="import-tasks-data-options" style={styles.smallGap}>
              <span className="import-tasks-data-options-title">
                {t('data-configs.file.delimiter.title')}:
              </span>
              <div style={{ display: 'flex', alignItems: 'center' }}>
                <Radio.Group
                  disabled={dataMapStore.readOnly || dataMapStore.lock}
                  value={
                    separators.includes(
                      dataMapStore.selectedFileInfo!.file_setting.delimiter
                    )
                      ? dataMapStore.selectedFileInfo!.file_setting.delimiter
                      : 'custom'
                  }
                  onChange={(e: React.ChangeEvent<HTMLSelectElement>) => {
                    if (e.target.value === 'custom') {
                      dataMapStore.setFileConfig('delimiter', '');
                    } else {
                      dataMapStore.setFileConfig('delimiter', e.target.value);
                      // reset validate error message of delimiter
                      dataMapStore.validateFileInfo('delimiter');
                    }
                  }}
                >
                  <Radio value=",">
                    {t('data-configs.file.delimiter.comma')}
                  </Radio>
                  <Radio value=";">
                    {t('data-configs.file.delimiter.semicolon')}
                  </Radio>
                  <Radio value="\t">
                    {t('data-configs.file.delimiter.tab')}
                  </Radio>
                  <Radio value=" ">
                    {t('data-configs.file.delimiter.space')}
                  </Radio>
                  <Radio value="custom">
                    {t('data-configs.file.delimiter.custom')}
                  </Radio>
                </Radio.Group>
              </div>
              {!separators.includes(
                dataMapStore.selectedFileInfo!.file_setting.delimiter
              ) && (
                <div style={{ marginLeft: 8 }}>
                  <Input
                    size="medium"
                    width={122}
                    disabled={dataMapStore.readOnly || dataMapStore.lock}
                    placeholder={t(
                      'data-configs.file.placeholder.input-delimiter'
                    )}
                    value={
                      dataMapStore.selectedFileInfo!.file_setting.delimiter
                    }
                    onChange={(e: any) => {
                      dataMapStore.setFileConfig('delimiter', e.value);
                      dataMapStore.validateFileInfo('delimiter');
                    }}
                    errorMessage={
                      dataMapStore.validateFileInfoErrorMessage.delimiter
                    }
                    errorLocation="layer"
                    originInputProps={{
                      onBlur: () => {
                        dataMapStore.validateFileInfo('delimiter');
                      }
                    }}
                  />
                </div>
              )}
            </div>
            <div className="import-tasks-data-options" style={styles.smallGap}>
              <span className="import-tasks-data-options-title">
                {t('data-configs.file.code-type.title')}:
              </span>
              <div style={{ display: 'flex', alignItems: 'center' }}>
                <Radio.Group
                  disabled={dataMapStore.readOnly || dataMapStore.lock}
                  value={
                    charsets.includes(
                      dataMapStore.selectedFileInfo!.file_setting.charset
                    )
                      ? dataMapStore.selectedFileInfo!.file_setting.charset
                      : 'custom'
                  }
                  onChange={(e: React.ChangeEvent<HTMLSelectElement>) => {
                    if (e.target.value === 'custom') {
                      dataMapStore.setFileConfig('charset', '');
                    } else {
                      dataMapStore.setFileConfig('charset', e.target.value);
                      dataMapStore.validateFileInfo('charset');
                    }
                  }}
                >
                  <Radio value="UTF-8">
                    {t('data-configs.file.code-type.UTF-8')}
                  </Radio>
                  <Radio value="GBK">
                    {t('data-configs.file.code-type.GBK')}
                  </Radio>
                  <Radio value="ISO-8859-1">
                    {t('data-configs.file.code-type.ISO-8859-1')}
                  </Radio>
                  <Radio value="US-ASCII">
                    {t('data-configs.file.code-type.US-ASCII')}
                  </Radio>
                  <Radio value="custom">
                    {t('data-configs.file.code-type.custom')}
                  </Radio>
                </Radio.Group>
              </div>
              {!charsets.includes(
                dataMapStore.selectedFileInfo!.file_setting.charset
              ) && (
                <div style={{ marginLeft: 8 }}>
                  <Input
                    size="medium"
                    width={122}
                    countMode="en"
                    disabled={dataMapStore.readOnly || dataMapStore.lock}
                    placeholder={t(
                      'data-configs.file.placeholder.input-charset'
                    )}
                    value={dataMapStore.selectedFileInfo!.file_setting.charset}
                    onChange={(e: any) => {
                      dataMapStore.setFileConfig('charset', e.value);
                      dataMapStore.validateFileInfo('charset');
                    }}
                    errorMessage={
                      dataMapStore.validateFileInfoErrorMessage.charset
                    }
                    errorLocation="layer"
                    originInputProps={{
                      onBlur: () => {
                        dataMapStore.validateFileInfo('charset');
                      }
                    }}
                  />
                </div>
              )}
            </div>
            <div className="import-tasks-data-options" style={styles.mediumGap}>
              <span className="import-tasks-data-options-title">
                {t('data-configs.file.date-type.title')}:
              </span>
              <div style={{ display: 'flex', alignItems: 'center' }}>
                <Radio.Group
                  disabled={dataMapStore.readOnly || dataMapStore.lock}
                  value={
                    dateFormat.includes(
                      dataMapStore.selectedFileInfo!.file_setting.date_format
                    )
                      ? dataMapStore.selectedFileInfo!.file_setting.date_format
                      : 'custom'
                  }
                  onChange={(e: React.ChangeEvent<HTMLSelectElement>) => {
                    if (e.target.value === 'custom') {
                      dataMapStore.setFileConfig('date_format', '');
                    } else {
                      dataMapStore.setFileConfig('date_format', e.target.value);
                      dataMapStore.validateFileInfo('date_format');
                    }
                  }}
                >
                  <Radio value="yyyy-MM-dd">yyyy-MM-dd</Radio>
                  <Radio value="yyyy-MM-dd HH:mm:ss">yyyy-MM-dd HH:MM:SS</Radio>
                  <Radio value="yyyy-MM-dd HH:mm:ss.SSS">
                    yyyy-MM-dd HH:mm:ss.SSS
                  </Radio>
                  <Radio value="custom">
                    {t('data-configs.file.code-type.custom')}
                  </Radio>
                </Radio.Group>
                {!dateFormat.includes(
                  dataMapStore.selectedFileInfo!.file_setting.date_format
                ) && (
                  <div style={{ marginLeft: 8 }}>
                    <Input
                      size="medium"
                      width={122}
                      countMode="en"
                      disabled={dataMapStore.readOnly || dataMapStore.lock}
                      placeholder={t(
                        'data-configs.file.placeholder.input-date-format'
                      )}
                      value={
                        dataMapStore.selectedFileInfo!.file_setting.date_format
                      }
                      onChange={(e: any) => {
                        dataMapStore.setFileConfig('date_format', e.value);
                        dataMapStore.validateFileInfo('date_format');
                      }}
                      errorMessage={
                        dataMapStore.validateFileInfoErrorMessage.date_format
                      }
                      errorLocation="layer"
                      originInputProps={{
                        onBlur: () => {
                          dataMapStore.validateFileInfo('date_format');
                        }
                      }}
                    />
                  </div>
                )}
              </div>
            </div>
            <div className="import-tasks-data-options" style={styles.mediumGap}>
              <span className="import-tasks-data-options-title">
                {t('data-configs.file.skipped-line')}:
              </span>
              <Input
                size="medium"
                width={356}
                maxLen={48}
                countMode="en"
                disabled={dataMapStore.readOnly || dataMapStore.lock}
                value={dataMapStore.selectedFileInfo!.file_setting.skipped_line}
                onChange={(e: any) => {
                  dataMapStore.setFileConfig('skipped_line', e.value);
                  dataMapStore.validateFileInfo('skipped_line');
                }}
                errorLocation="layer"
                errorMessage={
                  dataMapStore.validateFileInfoErrorMessage.skipped_line
                }
                originInputProps={{
                  onBlur: () => {
                    dataMapStore.validateFileInfo('skipped_line');
                  }
                }}
              />
            </div>
            <div className="import-tasks-data-options">
              <span className="import-tasks-data-options-title">
                {t('data-configs.file.timezone')}:
              </span>
              <Select
                disabled={dataMapStore.readOnly || dataMapStore.lock}
                width={140}
                size="medium"
                value={dataMapStore.selectedFileInfo!.file_setting.time_zone}
                onChange={(value: string) => {
                  dataMapStore.setFileConfig('time_zone', value);
                }}
              >
                {timezones.map((timezone) => (
                  <Select.Option value={timezone} key={timezone}>
                    {timezone}
                  </Select.Option>
                ))}
              </Select>
            </div>
            {!dataMapStore.readOnly && !dataMapStore.lock && (
              <div className="import-tasks-data-options">
                <span className="import-tasks-data-options-title"></span>
                <Button
                  type="primary"
                  size="medium"
                  disabled={!dataMapStore.isValidateFileInfo}
                  onClick={async () => {
                    dataMapStore.switchExpand('file', false);

                    await dataMapStore.updateFileConfig(
                      dataMapStore.selectedFileId
                    );

                    if (
                      dataMapStore.requestStatus.updateFileConfig === 'failed'
                    ) {
                      Message.error({
                        content:
                          dataMapStore.errorInfo.updateFileConfig.message,
                        size: 'medium',
                        showCloseIcon: false
                      });

                      return;
                    }

                    if (
                      dataMapStore.requestStatus.updateFileConfig === 'success'
                    ) {
                      Message.success({
                        content: t('data-configs.file.hint.save-succeed'),
                        size: 'medium',
                        showCloseIcon: false
                      });
                    }
                  }}
                >
                  {t('data-configs.file.save')}
                </Button>
              </div>
            )}
          </>
        )}
      </div>
    )
  );
});

export default FileConfigs;
