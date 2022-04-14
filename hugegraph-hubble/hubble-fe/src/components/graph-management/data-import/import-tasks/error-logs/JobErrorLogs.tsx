import React, { useContext, useEffect, useState } from 'react';
import { useRoute } from 'wouter';
import { observer } from 'mobx-react';
import { motion } from 'framer-motion';
import { Menu } from 'hubble-ui';
import { useTranslation } from 'react-i18next';

import { ImportManagerStoreContext } from '../../../../../stores';

import './JobErrorLogs.less';

const failedReasonVariants = {
  initial: {
    opacity: 0,
    y: -10
  },
  animate: {
    opacity: 1,
    y: 0,
    transition: {
      duration: 0.8,
      ease: 'easeInOut'
    }
  }
};

const JobErrorLogs: React.FC = observer(() => {
  const importManagerStore = useContext(ImportManagerStoreContext);
  const { t } = useTranslation();
  const [, params] = useRoute(
    '/graph-management/:id/data-import/job-error-log/:jobId'
  );
  const [selectedFileName, setSelectedFileName] = useState('');

  useEffect(() => {
    const init = async () => {
      await importManagerStore.fetchFailedReason(
        Number(params!.id),
        Number(params!.jobId)
      );

      if (importManagerStore.requestStatus.fetchFailedReason === 'success') {
        setSelectedFileName(importManagerStore.failedReason[0].file_name);
      }
    };

    init();
  }, [params!.id, params!.jobId]);

  return (
    <section className="job-error-logs">
      <div className="job-error-logs-title">
        {t('addition.message.fail-reason')}
      </div>
      <motion.div
        initial="initial"
        animate="animate"
        variants={failedReasonVariants}
      >
        <div className="job-error-logs-content-wrapper">
          {importManagerStore.requestStatus.fetchFailedReason === 'failed' ? (
            <div className="job-error-logs-content-with-error-only">
              {importManagerStore.errorInfo.fetchFailedReason.message}
            </div>
          ) : (
            <>
              <Menu
                mode="inline"
                needBorder={true}
                style={{ width: 200, height: 'calc(100vh - 194px)' }}
                selectedKeys={[selectedFileName]}
                onClick={(e: any) => {
                  setSelectedFileName(e.key);
                }}
              >
                {importManagerStore.failedReason.map(({ file_name }) => (
                  <Menu.Item key={file_name}>
                    <span>{file_name}</span>
                  </Menu.Item>
                ))}
              </Menu>
              <div className="job-error-logs-content">
                {importManagerStore.failedReason
                  .find(({ file_name }) => file_name === selectedFileName)
                  ?.reason.split('\n')
                  .filter((reason) => reason !== '')
                  .map((text, index) => (
                    <div className="job-error-logs-content-item">
                      <div className="job-error-logs-content-item-title">
                        {index % 2 === 0
                          ? `${t('addition.message.fail-reason')}：`
                          : `${t('addition.message.fail-position')}：`}
                      </div>
                      <div className="job-error-logs-content-item-text">
                        {text.replace('#### INSERT ERROR:', '')}
                      </div>
                    </div>
                  ))}
              </div>
            </>
          )}
        </div>
      </motion.div>
    </section>
  );
});

export default JobErrorLogs;
