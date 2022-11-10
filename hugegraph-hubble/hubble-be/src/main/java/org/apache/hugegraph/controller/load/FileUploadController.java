/*
 * Copyright 2017 HugeGraph Authors
 *
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. The ASF
 * licenses this file to You under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

package org.apache.hugegraph.controller.load;

import static org.apache.hugegraph.service.load.FileMappingService.CONN_PREIFX;
import static org.apache.hugegraph.service.load.FileMappingService.JOB_PREIFX;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import org.apache.hugegraph.common.Constant;
import org.apache.hugegraph.config.HugeConfig;
import org.apache.hugegraph.entity.enums.FileMappingStatus;
import org.apache.hugegraph.entity.enums.JobStatus;
import org.apache.hugegraph.entity.load.FileMapping;
import org.apache.hugegraph.entity.load.FileUploadResult;
import org.apache.hugegraph.entity.load.JobManager;
import org.apache.hugegraph.exception.InternalException;
import org.apache.hugegraph.options.HubbleOptions;
import org.apache.hugegraph.service.load.FileMappingService;
import org.apache.hugegraph.service.load.JobManagerService;
import org.apache.hugegraph.util.CollectionUtil;
import org.apache.hugegraph.util.Ex;
import org.apache.hugegraph.util.FileUtil;
import org.apache.hugegraph.util.HubbleUtil;

import lombok.extern.log4j.Log4j2;

@Log4j2
@RestController
@RequestMapping(Constant.API_VERSION + "graph-connections/{connId}/job-manager/{jobId}/upload-file")
public class FileUploadController {

    @Autowired
    private HugeConfig config;
    @Autowired
    private FileMappingService service;
    @Autowired
    private JobManagerService jobService;

    @GetMapping("token")
    public Map<String, String> fileToken(@PathVariable("connId") int connId,
                                         @PathVariable("jobId") int jobId,
                                         @RequestParam("names")
                                         List<String> fileNames) {
        Ex.check(CollectionUtil.allUnique(fileNames),
                 "load.upload.file.duplicate-name");
        Map<String, String> tokens = new HashMap<>();
        for (String fileName : fileNames) {
            String token = this.service.generateFileToken(fileName);
            Ex.check(!this.uploadingTokenLocks().containsKey(token),
                     "load.upload.file.token.existed");
            this.uploadingTokenLocks().put(token, new ReentrantReadWriteLock());
            tokens.put(fileName, token);
        }
        return tokens;
    }

    @PostMapping
    public FileUploadResult upload(@PathVariable("connId") int connId,
                                   @PathVariable("jobId") int jobId,
                                   @RequestParam("file") MultipartFile file,
                                   @RequestParam("name") String fileName,
                                   @RequestParam("token") String token,
                                   @RequestParam("total") int total,
                                   @RequestParam("index") int index) {
        this.checkTotalAndIndexValid(total, index);
        this.checkFileNameMatchToken(fileName, token);
        JobManager jobEntity = this.jobService.get(jobId);
        this.checkFileValid(connId, jobId, jobEntity, file, fileName);
        if (jobEntity.getJobStatus() == JobStatus.DEFAULT) {
            jobEntity.setJobStatus(JobStatus.UPLOADING);
            this.jobService.update(jobEntity);
        }
        // Ensure location exist and generate file path
        String filePath = this.generateFilePath(connId, jobId, fileName);
        // Check this file deleted before
        ReadWriteLock lock = this.uploadingTokenLocks().get(token);
        FileUploadResult result;
        if (lock == null) {
            result = new FileUploadResult();
            result.setName(file.getOriginalFilename());
            result.setSize(file.getSize());
            result.setStatus(FileUploadResult.Status.FAILURE);
            result.setCause("File has been deleted");
            return result;
        }

        lock.readLock().lock();
        try {
            result = this.service.uploadFile(file, index, filePath);
            if (result.getStatus() == FileUploadResult.Status.FAILURE) {
                return result;
            }
            synchronized (this.service) {
                // Verify the existence of fragmented files
                FileMapping mapping = this.service.get(connId, jobId, fileName);
                if (mapping == null) {
                    mapping = new FileMapping(connId, fileName, filePath);
                    mapping.setJobId(jobId);
                    mapping.setFileStatus(FileMappingStatus.UPLOADING);
                    this.service.save(mapping);
                } else {
                    if (mapping.getFileStatus() == FileMappingStatus.COMPLETED) {
                        result.setId(mapping.getId());
                        // Remove uploading file token
                        this.uploadingTokenLocks().remove(token);
                        return result;
                    } else {
                        mapping.setUpdateTime(HubbleUtil.nowDate());
                    }
                }
                // Determine whether all the parts have been uploaded, then merge them
                boolean merged = this.service.tryMergePartFiles(filePath, total);
                if (!merged) {
                    this.service.update(mapping);
                    return result;
                }
                // Read column names and values then fill it
                this.service.extractColumns(mapping);
                mapping.setFileStatus(FileMappingStatus.COMPLETED);
                mapping.setTotalLines(FileUtil.countLines(mapping.getPath()));
                mapping.setTotalSize(FileUtils.sizeOf(new File(mapping.getPath())));

                // Move to the directory corresponding to the file mapping Id
                String newPath = this.service.moveToNextLevelDir(mapping);
                // Update file mapping stored path
                mapping.setPath(newPath);
                this.service.update(mapping);
                // Update Job Manager size
                long jobSize = jobEntity.getJobSize() + mapping.getTotalSize();
                jobEntity.setJobSize(jobSize);
                this.jobService.update(jobEntity);
                result.setId(mapping.getId());
                // Remove uploading file token
                this.uploadingTokenLocks().remove(token);
            }
            return result;
        } finally {
            lock.readLock().unlock();
        }
    }

    @DeleteMapping
    public Boolean delete(@PathVariable("connId") int connId,
                          @PathVariable("jobId") int jobId,
                          @RequestParam("name") String fileName,
                          @RequestParam("token") String token) {
        JobManager jobEntity = this.jobService.get(jobId);
        Ex.check(jobEntity != null, "job-manager.not-exist.id", jobId);
        Ex.check(jobEntity.getJobStatus() == JobStatus.UPLOADING ||
                 jobEntity.getJobStatus() == JobStatus.MAPPING ||
                 jobEntity.getJobStatus() == JobStatus.SETTING,
                 "deleted.file.no-permission");
        FileMapping mapping = this.service.get(connId, jobId, fileName);
        Ex.check(mapping != null, "load.file-mapping.not-exist.name", fileName);

        ReadWriteLock lock = this.uploadingTokenLocks().get(token);
        if (lock != null) {
            lock.writeLock().lock();
        }
        try {
            this.service.deleteDiskFile(mapping);
            log.info("Prepare to remove file mapping {}", mapping.getId());
            this.service.remove(mapping.getId());
            long jobSize = jobEntity.getJobSize() - mapping.getTotalSize();
            jobEntity.setJobSize(jobSize);
            this.jobService.update(jobEntity);
            if (lock != null) {
                this.uploadingTokenLocks().remove(token);
            }
            return true;
        } finally {
            if (lock != null) {
                lock.writeLock().unlock();
            }
        }
    }

    @PutMapping("next-step")
    public JobManager nextStep(@PathVariable("jobId") int jobId) {
        JobManager jobEntity = this.jobService.get(jobId);
        Ex.check(jobEntity != null, "job-manager.not-exist.id", jobId);
        Ex.check(jobEntity.getJobStatus() == JobStatus.UPLOADING,
                 "job.manager.status.unexpected",
                 JobStatus.UPLOADING, jobEntity.getJobStatus());
        jobEntity.setJobStatus(JobStatus.MAPPING);
        this.jobService.update(jobEntity);
        return jobEntity;
    }

    private Map<String, ReadWriteLock> uploadingTokenLocks() {
        return this.service.getUploadingTokenLocks();
    }

    private void checkTotalAndIndexValid(int total, int index) {
        if (total <= 0) {
            throw new InternalException("The request params 'total' must > 0");
        }
        if (index < 0) {
            throw new InternalException("The request params 'index' must >= 0");
        }
    }

    private void checkFileNameMatchToken(String fileName, String token) {
        String md5Prefix = HubbleUtil.md5(fileName);
        Ex.check(StringUtils.isNotEmpty(token) && token.startsWith(md5Prefix),
                 "load.upload.file.name-token.unmatch");
    }

    private void checkFileValid(int connId, int jobId, JobManager jobEntity,
                                MultipartFile file, String fileName) {
        Ex.check(jobEntity != null, "job-manager.not-exist.id", jobId);
        Ex.check(jobEntity.getJobStatus() == JobStatus.DEFAULT ||
                 jobEntity.getJobStatus() == JobStatus.UPLOADING ||
                 jobEntity.getJobStatus() == JobStatus.MAPPING ||
                 jobEntity.getJobStatus() == JobStatus.SETTING,
                 "load.upload.file.no-permission");
        // Now allowed to upload empty file
        Ex.check(!file.isEmpty(), "load.upload.file.cannot-be-empty");
        // Difficult: how to determine whether the file is csv or text
        log.debug("File content type: {}", file.getContentType());

        String format = FilenameUtils.getExtension(fileName);
        List<String> formatWhiteList = this.config.get(
                                       HubbleOptions.UPLOAD_FILE_FORMAT_LIST);
        Ex.check(formatWhiteList.contains(format),
                 "load.upload.file.format.unsupported");

        long fileSize = file.getSize();
        long singleFileSizeLimit = this.config.get(
                                   HubbleOptions.UPLOAD_SINGLE_FILE_SIZE_LIMIT);
        Ex.check(fileSize <= singleFileSizeLimit,
                 "load.upload.file.exceed-single-size",
                 FileUtils.byteCountToDisplaySize(singleFileSizeLimit));

        // Check is there a file with the same name
        FileMapping oldMapping = this.service.get(connId, jobId, fileName);
        Ex.check(oldMapping == null ||
                 oldMapping.getFileStatus() == FileMappingStatus.UPLOADING,
                 "load.upload.file.existed", fileName);

        long totalFileSizeLimit = this.config.get(
                                  HubbleOptions.UPLOAD_TOTAL_FILE_SIZE_LIMIT);
        List<FileMapping> fileMappings = this.service.listAll();
        long currentTotalSize = fileMappings.stream()
                                            .map(FileMapping::getTotalSize)
                                            .reduce(0L, (Long::sum));
        Ex.check(fileSize + currentTotalSize <= totalFileSizeLimit,
                 "load.upload.file.exceed-single-size",
                 FileUtils.byteCountToDisplaySize(totalFileSizeLimit));
    }

    private String generateFilePath(int connId, int jobId, String fileName) {
        String location = this.config.get(HubbleOptions.UPLOAD_FILE_LOCATION);
        String path = Paths.get(CONN_PREIFX + connId, JOB_PREIFX + jobId)
                           .toString();
        this.ensureLocationExist(location, path);
        // Before merge: upload-files/conn-1/verson_person.csv/part-1
        // After merge: upload-files/conn-1/file-mapping-1/verson_person.csv
        return Paths.get(location, path, fileName).toString();
    }

    private void ensureLocationExist(String location, String connPath) {
        String path = Paths.get(location, connPath).toString();
        File locationDir = new File(path);
        if (!locationDir.exists()) {
            try {
                FileUtils.forceMkdir(locationDir);
            } catch (IOException e) {
                throw new InternalException("failed to create location dir", e);
            }
        }
    }
}
