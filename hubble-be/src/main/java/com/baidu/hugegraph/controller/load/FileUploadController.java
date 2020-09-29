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

package com.baidu.hugegraph.controller.load;

import static com.baidu.hugegraph.service.load.FileMappingService.CONN_PREIFX;
import static com.baidu.hugegraph.service.load.FileMappingService.JOB_PREIFX;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.baidu.hugegraph.common.Constant;
import com.baidu.hugegraph.config.HugeConfig;
import com.baidu.hugegraph.entity.enums.FileMappingStatus;
import com.baidu.hugegraph.entity.enums.JobManagerStatus;
import com.baidu.hugegraph.entity.load.FileMapping;
import com.baidu.hugegraph.entity.load.FileUploadResult;
import com.baidu.hugegraph.entity.load.JobManager;
import com.baidu.hugegraph.exception.InternalException;
import com.baidu.hugegraph.options.HubbleOptions;
import com.baidu.hugegraph.service.load.FileMappingService;
import com.baidu.hugegraph.service.load.JobManagerService;
import com.baidu.hugegraph.util.Ex;
import com.baidu.hugegraph.util.FileUtil;
import com.baidu.hugegraph.util.HubbleUtil;

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

    @PostMapping
    public FileUploadResult upload(@PathVariable("connId") int connId,
                                   @PathVariable("jobId") int jobId,
                                   @RequestParam("file") MultipartFile file,
                                   @RequestParam("name") String fileName,
                                   @RequestParam("total") int total,
                                   @RequestParam("index") int index) {
        // When front-end use multipart-upload mode,
        // file.getOriginalFilename() is blob, not actual file name
        JobManager jobEntity = this.jobService.get(jobId);
        this.checkFileValid(connId, jobId, jobEntity, file, fileName);

        String location = this.config.get(HubbleOptions.UPLOAD_FILE_LOCATION);
        String path = Paths.get(CONN_PREIFX + connId, JOB_PREIFX + jobId)
                           .toString();
        this.ensureLocationExist(location, path);
        // Before merge: upload-files/conn-1/verson_person.csv/part-1
        // After merge: upload-files/conn-1/file-mapping-1/verson_person.csv
        String filePath = Paths.get(location, path, fileName).toString();
        // Check destFile exist
        // Ex.check(!destFile.exists(), "load.upload.file.existed", fileName);
        FileUploadResult result = this.service.uploadFile(file, index, filePath);
        if (result.getStatus() == FileUploadResult.Status.FAILURE) {
            return result;
        }
        // Verify the existence of fragmented files
        FileMapping mapping = this.service.get(connId, jobId, fileName);
        if (mapping != null) {
            mapping.setJobId(jobId);
            mapping.setFileStatus(FileMappingStatus.UPLOADING);
            mapping.setFileIndex(mapping.getFileIndex() + "," + index);
            mapping.setFileTotal(total);
            if (this.service.update(mapping) != 1) {
                throw new InternalException("entity.update.failed", mapping);
            }
        } else {
            mapping = new FileMapping(connId, fileName, filePath);
            mapping.setJobId(jobId);
            mapping.setFileStatus(FileMappingStatus.UPLOADING);
            mapping.setFileIndex(String.valueOf(index));
            mapping.setFileTotal(total);
            if (this.service.save(mapping) != 1) {
                throw new InternalException("entity.insert.failed", mapping);
            }
        }
       Integer mapId = mapping.getId();
        // Determine whether all the parts have been uploaded, then merge them
        boolean merged = this.service.tryMergePartFiles(filePath, total);
        if (merged) {
            // Save file mapping
            mapping = new FileMapping(connId, fileName, filePath);
            // Read column names and values then fill it
            this.service.extractColumns(mapping);
            mapping.setId(mapId);
            mapping.setFileStatus(FileMappingStatus.COMPLETED);
            mapping.setTotalLines(FileUtil.countLines(mapping.getPath()));
            mapping.setTotalSize(FileUtils.sizeOf(new File(mapping.getPath())));
            mapping.setCreateTime(HubbleUtil.nowDate());
            // Will generate mapping id

            // Move to the directory corresponding to the file mapping Id
            String newPath = this.service.moveToNextLevelDir(mapping);
            // Update file mapping stored path
            mapping.setPath(newPath);
            if (this.service.update(mapping) != 1) {
                throw new InternalException("entity.update.failed", mapping);
            }
            // Update Job Manager size
            long jobSize = jobEntity.getJobSize() + mapping.getTotalSize();
            jobEntity.setJobSize(jobSize);
            jobEntity.setJobStatus(JobManagerStatus.SETTING);
            if (this.jobService.update(jobEntity) != 1) {
                throw new InternalException("job-manager.entity.update.failed",
                                            jobEntity);
            }
            result.setId(mapping.getId());
        }
        return result;
    }

    @DeleteMapping
    public Map<String, Boolean> delete(@PathVariable("connId") int connId,
                                       @PathVariable("jobId") int jobId,
                                       @RequestParam("names")
                                       List<String> fileNames) {

        JobManager jobEntity = this.jobService.get(jobId);
        Ex.check(jobEntity != null,
                 "job-manager.not-exist.id", jobId);
        Ex.check(jobEntity.getJobStatus() == JobManagerStatus.SETTING,
                 "deleted.file.no-permission" );
        Ex.check(fileNames.size() > 0, "load.upload.files.at-least-one");
        Map<String, Boolean> result = new LinkedHashMap<>();
        for (String fileName : fileNames) {
            FileMapping mapping = this.service.get(connId, fileName);
            Ex.check(mapping != null, "load.file-mapping.not-exist.name",
                     fileName);
            File destFile = new File(mapping.getPath());
            boolean deleted = destFile.delete();
            if (deleted) {
                log.info("deleted file {}, prepare to remove file mapping {}",
                         destFile, mapping.getId());
                if (this.service.remove(mapping.getId()) != 1) {
                    throw new InternalException("entity.delete.failed", mapping);
                }
                log.info("removed file mapping {}", mapping.getId());
                long jobSize = jobEntity.getJobSize() - mapping.getTotalSize();
                jobEntity.setJobSize(jobSize);
                if (this.jobService.update(jobEntity) != 1) {
                    throw new InternalException("job-manager.entity.update.failed",
                                                jobEntity);
                }
            }
            result.put(fileName, deleted);
        }
        return result;
    }


    private void checkFileValid(int connId, int jobId, JobManager jobEntity,
                                MultipartFile file, String fileName) {
        Ex.check(jobEntity != null,
                 "job-manager.not-exist.id", jobId);
        Ex.check(jobEntity.getJobStatus() == JobManagerStatus.DEFAULT ||
                 jobEntity.getJobStatus() == JobManagerStatus.SETTING,
                 "load.upload.file.no-permission" );
        // Now allowed to upload empty file
        Ex.check(!file.isEmpty(), "load.upload.file.cannot-be-empty");
        // Difficult: how to determine whether the file is csv or text
        log.info("File content type: {}", file.getContentType());

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
