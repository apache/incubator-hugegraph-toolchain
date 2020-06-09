import React, { useContext, useCallback, useRef } from 'react';
import { observer } from 'mobx-react';
import classnames from 'classnames';
import { isEmpty, size, isUndefined, range } from 'lodash-es';
import { DndProvider, useDrop, DropTargetMonitor } from 'react-dnd';
import { useTranslation } from 'react-i18next';
import Backend, { NativeTypes } from 'react-dnd-html5-backend';
import { Button, Progress, Message } from '@baidu/one-ui';

import { DataImportRootStoreContext } from '../../../../stores';
import CloseIcon from '../../../../assets/imgs/ic_close_16.svg';
import RefreshIcon from '../../../../assets/imgs/ic_refresh.svg';

import './UploadEntry.less';
import { CancellablePromise } from 'mobx/lib/api/flow';
import { FileUploadResult } from '../../../../stores/types/GraphManagementStore/dataImportStore';

const KB = 1024;
const MB = 1024 * 1024;
const GB = 1024 * 1024 * 1024;
const MAX_CONCURRENT_UPLOAD = 5;

const UploadEntry: React.FC = observer(() => {
  const dataImportRootStore = useContext(DataImportRootStoreContext);
  const { dataMapStore, serverDataImportStore } = dataImportRootStore;
  const { t } = useTranslation();

  return (
    <>
      <DndProvider backend={Backend}>
        <FileDropZone />
      </DndProvider>
      <div className="import-tasks-manipulation-wrapper">
        <Button
          size="medium"
          style={{ width: 88, marginRight: 16 }}
          onClick={() => {
            dataImportRootStore.fileUploadQueue.forEach(
              ({ fileName, status }) => {
                if (status === 'uploading') {
                  dataImportRootStore.mutateFileUploadTasks(
                    'status',
                    'failed',
                    fileName
                  );
                }
              }
            );
          }}
        >
          {t('upload-files.cancel')}
        </Button>
        <Button
          type="primary"
          size="medium"
          style={{ width: 74 }}
          disabled={
            size(dataImportRootStore.fileUploadTasks) === 0 ||
            dataImportRootStore.fileUploadTasks.some(
              ({ status }) => status === 'uploading'
            )
          }
          onClick={() => {
            dataMapStore.setSelectedFileId(
              Number(
                dataMapStore.fileMapInfos.filter(({ name }) =>
                  dataImportRootStore.successFileUploadTaskNames.includes(name)
                )[0].id
              )
            );
            dataMapStore.setSelectedFileInfo();
            serverDataImportStore.syncImportConfigs(
              dataMapStore.selectedFileInfo!.load_parameter
            );
            dataImportRootStore.setCurrentStep(2);
          }}
        >
          {t('upload-files.next')}
        </Button>
      </div>
    </>
  );
});

export const FileDropZone: React.FC = observer(() => {
  const dataImportRootStore = useContext(DataImportRootStoreContext);
  const { dataMapStore, serverDataImportStore } = dataImportRootStore;
  const [{ canDrop, isOver }, drop] = useDrop({
    accept: [NativeTypes.FILE],
    drop(item, monitor) {
      handleFileDrop(monitor);
    },
    collect: (monitor) => ({
      isOver: monitor.isOver(),
      canDrop: monitor.canDrop()
    })
  });
  const { t } = useTranslation();

  const uploadRef = useRef<HTMLInputElement>(null);

  const validateFileCondition = (files: File[]) => {
    const validatedFiles = [];
    const currentUploadFileNames = dataImportRootStore.fileUploadTasks.map(
      ({ name }) => name
    );

    files = files.filter(({ name }) => !currentUploadFileNames.includes(name));

    const totalSize = files
      .map(({ size }) => size)
      .reduce((prev, curr) => prev + curr, 0);

    if (totalSize / GB > 10) {
      Message.error({
        content: `${t('upload-files.over-all-size-limit')}`,
        size: 'medium',
        showCloseIcon: false
      });

      return [];
    }

    for (const file of files) {
      const { name, size } = file;
      const sizeGB = size / GB;

      if (size === 0) {
        Message.error({
          content: `${name} ${t('upload-files.empty-file')}`,
          size: 'medium',
          showCloseIcon: false
        });

        break;
      }

      if (name.slice(-4) !== '.csv') {
        Message.error({
          content: `${name}: ${t('upload-files.wrong-format')}`,
          size: 'medium',
          showCloseIcon: false
        });

        break;
      }

      if (sizeGB > 1) {
        Message.error({
          content: `${name}: ${t('upload-files.over-single-size-limit')}`,
          size: 'medium',
          showCloseIcon: false
        });

        break;
      }

      validatedFiles.push(file);
    }

    dataImportRootStore.updateFileList(validatedFiles);
    return validatedFiles;
  };

  const handleFileChange = (files: File[]) => {
    for (const file of files) {
      const { name, size } = file;
      const sizeMB = size / MB;

      const chunkList = [];
      // start byte to slice
      let currentChunkStartIndex = 0;
      // size of each chunk
      let chunkSizeMB: number;
      let chunkIndex = 0;

      if (sizeMB > 2 && sizeMB <= 128) {
        chunkSizeMB = 2;
      } else if (sizeMB > 128 && size <= 512) {
        chunkSizeMB = 4;
      } else {
        chunkSizeMB = 5;
      }

      while (currentChunkStartIndex < size) {
        chunkList.push({
          chunkIndex,
          chunk: file.slice(
            currentChunkStartIndex,
            currentChunkStartIndex + chunkSizeMB * MB
          )
        });
        currentChunkStartIndex += chunkSizeMB * MB;
        ++chunkIndex;
      }

      dataImportRootStore.initFileUploadTask({
        name,
        size,
        status: 'uploading',
        chunkList,
        chunkTotal: Math.ceil(size / (chunkSizeMB * MB)),
        uploadedChunkTotal: 0,
        pendingChunkIndexes: [],
        failedChunkIndexes: [],
        uploadedChunksIndexes: []
      });
    }
  };

  const initFileTaskQueue = () => {
    const firstRequestTasks = dataImportRootStore.fileUploadTasks.slice(
      0,
      MAX_CONCURRENT_UPLOAD
    );

    firstRequestTasks.forEach(({ name, chunkList, chunkTotal }) => {
      const task = dataImportRootStore.uploadFiles2({
        fileName: name,
        fileChunkList: chunkList[0],
        fileChunkTotal: chunkTotal
      });

      dataImportRootStore.addFileUploadQueue({
        fileName: name,
        status: 'uploading',
        task
      });

      scheduler(name, 0, task);
    });

    if (size(firstRequestTasks) < MAX_CONCURRENT_UPLOAD) {
      let loopCount = 1;

      // Traverse firstRequestTasks to add extra items to queue
      while (loopCount) {
        const currentQueueItemSize = size(dataImportRootStore.fileUploadQueue);

        firstRequestTasks.forEach(({ chunkTotal, chunkList, name }) => {
          if (
            chunkTotal > loopCount &&
            size(dataImportRootStore.fileUploadQueue) < MAX_CONCURRENT_UPLOAD
          ) {
            const task = dataImportRootStore.uploadFiles2({
              fileName: name,
              fileChunkList: chunkList[loopCount],
              fileChunkTotal: chunkTotal
            });

            dataImportRootStore.addFileUploadQueue({
              fileName: name,
              status: 'uploading',
              task
            });

            scheduler(name, loopCount, task);
          }
        });

        if (
          size(dataImportRootStore.fileUploadQueue) === MAX_CONCURRENT_UPLOAD ||
          // which means no other items pushed into queue
          currentQueueItemSize === size(dataImportRootStore.fileUploadQueue)
        ) {
          break;
        } else {
          loopCount += 1;
        }
      }
    }
  };

  const scheduler = useCallback(
    async (
      fileName: string,
      fileChunkIndex: number,
      task: CancellablePromise<FileUploadResult | undefined>,
      retryMode: boolean = false
    ) => {
      const fileUploadTask = dataImportRootStore.fileUploadTasks.find(
        ({ name }) => name === fileName
      )!;

      // the index of fileChunk is pending
      dataImportRootStore.mutateFileUploadTasks(
        'pendingChunkIndexes',
        [...fileUploadTask.pendingChunkIndexes, fileChunkIndex],
        fileName
      );

      let result;

      // cancel all uploads by user
      if (fileUploadTask.status === 'failed') {
        task.cancel();
        result = undefined;
      } else {
        result = await task;
      }

      if (isUndefined(result) || result.status === 'FAILURE') {
        if (fileUploadTask.status !== 'failed') {
          if (dataImportRootStore.errorInfo.uploadFiles.message !== '') {
            Message.error({
              content: dataImportRootStore.errorInfo.uploadFiles.message,
              size: 'medium',
              showCloseIcon: false
            });
          }

          dataImportRootStore.mutateFileUploadTasks(
            'status',
            'failed',
            fileName
          );
        }

        dataImportRootStore.mutateFileUploadTasks(
          'failedChunkIndexes',
          [...fileUploadTask.failedChunkIndexes, fileChunkIndex],
          fileName
        );

        dataImportRootStore.mutateFileUploadTasks(
          'pendingChunkIndexes',
          [],
          fileName
        );

        dataImportRootStore.removeFileUploadQueue(fileName);
        return;
      }

      if (result.status === 'SUCCESS') {
        dataImportRootStore.mutateFileUploadTasks(
          'uploadedChunkTotal',
          fileUploadTask.uploadedChunkTotal + 1,
          fileName
        );

        dataImportRootStore.mutateFileUploadTasks(
          'uploadedChunksIndexes',
          [...fileUploadTask.uploadedChunksIndexes, fileChunkIndex],
          fileName
        );

        if (retryMode) {
          // remove failed chunk index after succeed
          dataImportRootStore.mutateFileUploadTasks(
            'failedChunkIndexes',
            fileUploadTask.failedChunkIndexes.filter(
              (failedChunkIndex) => failedChunkIndex !== fileChunkIndex
            ),
            fileName
          );

          // if there are no longer existed failed chunk index,
          // set retry mode to false
          if (size(fileUploadTask.failedChunkIndexes) === 0) {
            retryMode = false;
          }
        }

        // all file chunks are uploaded
        if (fileUploadTask.chunkTotal === fileUploadTask.uploadedChunkTotal) {
          // remove fully uploaded file from queue
          dataImportRootStore.removeFileUploadQueue(fileName);
          // change the status of fully uploaded file to success
          dataImportRootStore.mutateFileUploadTasks(
            'status',
            'success',
            fileName
          );
          // clear chunkList of fully uploaded file to release memory
          dataImportRootStore.mutateFileUploadTasks('chunkList', [], fileName);

          // no uploading files
          if (
            dataImportRootStore.fileUploadTasks.every(
              ({ status }) => status !== 'uploading'
            )
          ) {
            dataMapStore.fetchDataMaps();
            return;
          }

          if (size(dataImportRootStore.fileRetryUploadList) !== 0) {
            // check if there are some failed uploads waiting in retry queue
            const fileName = dataImportRootStore.pullRetryFileUploadQueue()!;
            const retryFileUploadTask = dataImportRootStore.fileUploadTasks.find(
              ({ name }) => name === fileName
            )!;

            const task = dataImportRootStore.uploadFiles2({
              fileName,
              fileChunkList:
                retryFileUploadTask.chunkList[
                  retryFileUploadTask.failedChunkIndexes[0]
                ],
              fileChunkTotal: retryFileUploadTask.chunkTotal
            });

            scheduler(
              retryFileUploadTask.name,
              retryFileUploadTask.failedChunkIndexes[0],
              task,
              true
            );

            return;
          }

          for (const [
            fileIndex,
            fileUploadTask
          ] of dataImportRootStore.fileUploadTasks.entries()) {
            // if there still has files which are fully not being uploaded
            if (
              fileUploadTask.uploadedChunkTotal === 0 &&
              size(fileUploadTask.pendingChunkIndexes) === 0
            ) {
              const task = dataImportRootStore.uploadFiles2({
                fileName: fileUploadTask.name,
                fileChunkList:
                  dataImportRootStore.fileUploadTasks[fileIndex].chunkList[0],
                fileChunkTotal:
                  dataImportRootStore.fileUploadTasks[fileIndex].chunkTotal
              });

              dataImportRootStore.addFileUploadQueue({
                fileName: fileUploadTask.name,
                status: 'uploading',
                task
              });

              scheduler(fileUploadTask.name, 0, task);

              // if queue is full, do not loop to add task
              if (
                size(dataImportRootStore.fileUploadQueue) ===
                MAX_CONCURRENT_UPLOAD
              ) {
                break;
              }
            }
          }

          return;
        }

        let nextUploadChunkIndex;

        if (retryMode) {
          nextUploadChunkIndex = fileUploadTask.failedChunkIndexes[0];
        } else {
          nextUploadChunkIndex =
            Math.max(
              ...fileUploadTask.pendingChunkIndexes,
              // maybe it just turns retry mode to false
              // and the failed chunk index could be less than
              // the one which uploads success
              // we have to compare uploaded chunk index either
              ...fileUploadTask.uploadedChunksIndexes
            ) + 1;
        }

        // remove pending here to get right result of nextUploadChunkIndex
        dataImportRootStore.mutateFileUploadTasks(
          'pendingChunkIndexes',
          fileUploadTask.pendingChunkIndexes.filter(
            (pendingChunkIndex) => pendingChunkIndex !== fileChunkIndex
          ),
          fileName
        );

        // no recursion
        if (
          nextUploadChunkIndex > fileUploadTask.chunkTotal - 1 ||
          fileUploadTask.uploadedChunksIndexes.includes(nextUploadChunkIndex)
        ) {
          return;
        }

        scheduler(
          fileName,
          nextUploadChunkIndex,
          dataImportRootStore.uploadFiles2({
            fileName,
            fileChunkList: fileUploadTask.chunkList[nextUploadChunkIndex],
            fileChunkTotal: fileUploadTask.chunkTotal
          }),
          retryMode
        );

        return;
      }
    },
    []
  );

  const handleFileDrop = (monitor: DropTargetMonitor) => {
    if (monitor) {
      const fileList = monitor.getItem().files;
      const currentValidateFileList = validateFileCondition(fileList);
      const isFirstBatchUpload =
        size(dataImportRootStore.fileUploadTasks) === 0;

      if (isEmpty(currentValidateFileList)) {
        return;
      }

      handleFileChange(currentValidateFileList);

      if (isFirstBatchUpload) {
        initFileTaskQueue();
      } else {
        const spareQueueItems =
          MAX_CONCURRENT_UPLOAD - size(dataImportRootStore.fileUploadQueue);

        if (spareQueueItems === 0) {
          return;
        }

        // if new selected files are less than spare spaces from queue
        if (spareQueueItems >= size(currentValidateFileList)) {
          currentValidateFileList.forEach(({ name }) => {
            const fileUploadTask = dataImportRootStore.fileUploadTasks.find(
              ({ name: fileName }) => fileName === name
            )!;

            const task = dataImportRootStore.uploadFiles2({
              fileName: name,
              fileChunkList: fileUploadTask.chunkList[0],
              fileChunkTotal: fileUploadTask.chunkTotal
            });

            scheduler(name, 0, task);
          });
        } else {
          range(spareQueueItems).forEach((fileUploadTaskIndex) => {
            const fileUploadTask = dataImportRootStore.fileUploadTasks.find(
              ({ name: fileName }) =>
                fileName === currentValidateFileList[fileUploadTaskIndex].name
            )!;

            const task = dataImportRootStore.uploadFiles2({
              fileName: fileUploadTask.name,
              fileChunkList: fileUploadTask.chunkList[0],
              fileChunkTotal: fileUploadTask.chunkTotal
            });

            scheduler(fileUploadTask.name, 0, task);
          });
        }
      }
    }
  };

  const handleFileSelect = async (e: React.ChangeEvent<HTMLInputElement>) => {
    if (e.target.files) {
      const fileList = Array.from(e.target.files);
      const currentValidateFileList = validateFileCondition(fileList);
      const isFirstBatchUpload =
        size(dataImportRootStore.fileUploadTasks) === 0;

      if (isEmpty(currentValidateFileList)) {
        return;
      }

      handleFileChange(currentValidateFileList);

      if (isFirstBatchUpload) {
        initFileTaskQueue();
      } else {
        const spareQueueItems =
          MAX_CONCURRENT_UPLOAD - size(dataImportRootStore.fileUploadQueue);

        if (spareQueueItems === 0) {
          return;
        }

        // if new selected files are less than spare spaces from queue
        if (spareQueueItems >= size(currentValidateFileList)) {
          currentValidateFileList.forEach(({ name }) => {
            const fileUploadTask = dataImportRootStore.fileUploadTasks.find(
              ({ name: fileName }) => fileName === name
            )!;

            const task = dataImportRootStore.uploadFiles2({
              fileName: name,
              fileChunkList: fileUploadTask.chunkList[0],
              fileChunkTotal: fileUploadTask.chunkTotal
            });

            scheduler(name, 0, task);
          });
        } else {
          range(spareQueueItems).forEach((fileUploadTaskIndex) => {
            const fileUploadTask = dataImportRootStore.fileUploadTasks.find(
              ({ name: fileName }) =>
                fileName === currentValidateFileList[fileUploadTaskIndex].name
            )!;

            const task = dataImportRootStore.uploadFiles2({
              fileName: fileUploadTask.name,
              fileChunkList: fileUploadTask.chunkList[0],
              fileChunkTotal: fileUploadTask.chunkTotal
            });

            scheduler(fileUploadTask.name, 0, task);
          });
        }
      }
    }

    // select same file will not trigger onChange on input[type="file"]
    // need to reset its value here
    e.target.value = '';
  };

  const dragAreaClassName = classnames({
    'import-tasks-upload-drag-area': true,
    'file-above': canDrop && isOver
  });

  return (
    <div className="import-tasks-upload-wrapper">
      <label htmlFor="import-tasks-file-upload">
        <div className={dragAreaClassName} ref={drop}>
          <span>{t('upload-files.description')}</span>
          <input
            ref={uploadRef}
            type="file"
            id="import-tasks-file-upload"
            multiple={true}
            onChange={handleFileSelect}
            style={{
              display: 'none'
            }}
          />
        </div>
      </label>
      <FileList scheduler={scheduler} />
    </div>
  );
});

export interface FileListProps {
  scheduler: (
    fileName: string,
    fileChunkIndex: number,
    task: CancellablePromise<FileUploadResult | undefined>,
    retryMode?: boolean
  ) => Promise<void>;
}

export const FileList: React.FC<FileListProps> = observer(({ scheduler }) => {
  const dataImportRootStore = useContext(DataImportRootStoreContext);
  const { dataMapStore } = dataImportRootStore;

  const handleRetry = (fileName: string) => () => {
    // when fileUploadQueue is empty, directly add a task to the Queue
    if (size(dataImportRootStore.fileUploadQueue) === 0) {
      const fileUploadTasks = dataImportRootStore.fileUploadTasks.find(
        ({ name }) => name === fileName
      )!;

      const task = dataImportRootStore.uploadFiles2({
        fileName,
        fileChunkList:
          fileUploadTasks.chunkList[fileUploadTasks.failedChunkIndexes[0]],
        fileChunkTotal: fileUploadTasks.chunkTotal
      });

      dataImportRootStore.mutateFileUploadTasks(
        'status',
        'uploading',
        fileName
      );

      scheduler(fileName, fileUploadTasks.failedChunkIndexes[0], task, true);
    } else {
      // or just add filename to retry queue,
      // let other scheduler decide when to call it
      dataImportRootStore.addRetryFileUploadQueue(fileName);
    }
  };

  const handleDelete = (name: string) => async () => {
    await dataImportRootStore.deleteFiles([name]);
    dataImportRootStore.removeFileUploadTasks(name);

    // if no uploading files, fetch data maps again
    if (
      !dataImportRootStore.fileUploadTasks.some(
        ({ status }) => status === 'uploading'
      )
    ) {
      dataMapStore.fetchDataMaps();
    }
  };

  return (
    <div className="import-tasks-upload-file-list">
      {dataImportRootStore.fileUploadTasks.map(
        ({ name, size, chunkTotal, uploadedChunkTotal, status }) => {
          const [sizeKB, sizeMB, sizeGB] = [size / KB, size / MB, size / GB];
          const convertedSize =
            sizeGB > 1
              ? String(sizeGB.toFixed(2)) + ' GB'
              : sizeMB > 1
              ? String(sizeMB.toFixed(2)) + ' MB'
              : sizeKB > 1
              ? String(sizeKB.toFixed(2)) + ' KB'
              : String(size) + ' Byte';
          const progress = Number(
            ((uploadedChunkTotal / chunkTotal) * 100).toFixed(2)
          );

          return (
            <div className="import-tasks-upload-file-info" key={name}>
              <div className="import-tasks-upload-file-info-titles">
                <span>{name}</span>
                <span>{convertedSize}</span>
              </div>
              <div className="import-tasks-upload-file-info-progress-status">
                <Progress
                  width={390}
                  percent={progress}
                  status={
                    progress === 100
                      ? 'success'
                      : status === 'failed'
                      ? 'exception'
                      : 'normal'
                  }
                  onCancel={handleDelete(name)}
                />
                {status === 'failed' && progress !== 100 && (
                  <img
                    src={RefreshIcon}
                    alt="retry-upload-file"
                    className="import-tasks-upload-file-info-progress-status-refresh-icon"
                    onClick={handleRetry(name)}
                  />
                )}
                <img
                  src={CloseIcon}
                  alt="delete-file"
                  onClick={handleDelete(name)}
                  className={
                    progress !== 100 && status !== 'failed'
                      ? 'import-tasks-upload-file-info-progress-status-close-icon in-progress'
                      : status === 'failed'
                      ? 'import-tasks-upload-file-info-progress-status-close-icon in-error'
                      : 'import-tasks-upload-file-info-progress-status-close-icon'
                  }
                />
              </div>
            </div>
          );
        }
      )}
      {/* {dataMapStore.fileMapInfos.map(({ name, total_size }) => {
        return (
          <div className="import-tasks-upload-file-info">
            <div className="import-tasks-upload-file-info-titles">
              <span>{name}</span>
              <span>{total_size}</span>
            </div>
            <div className="import-tasks-upload-file-info-progress-status">
              <Progress
                width={410}
                percent={100}
                status="success"
                onCancel={handleDelete}
              />
              <img
                src={CloseIcon}
                alt="delete-file"
                onClick={handleDelete(name)}
              />
            </div>
          </div>
        );
      })} */}
    </div>
  );
});

export default UploadEntry;
