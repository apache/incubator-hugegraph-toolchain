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

package org.apache.hugegraph.service.load;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.regex.Pattern;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.hugegraph.exception.InternalException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import org.apache.hugegraph.config.HugeConfig;
import org.apache.hugegraph.entity.enums.FileMappingStatus;
import org.apache.hugegraph.entity.load.FileMapping;
import org.apache.hugegraph.entity.load.FileSetting;
import org.apache.hugegraph.entity.load.FileUploadResult;
import org.apache.hugegraph.mapper.load.FileMappingMapper;
import org.apache.hugegraph.options.HubbleOptions;
import org.apache.hugegraph.util.Ex;
import org.apache.hugegraph.util.HubbleUtil;
import org.apache.hugegraph.util.StringUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import lombok.extern.log4j.Log4j2;

@Log4j2
@Service
public class FileMappingService {

    public static final String CONN_PREIFX = "graph-connection-";
    public static final String JOB_PREIFX = "job-";
    public static final String FILE_PREIFX = "file-mapping-";

    @Autowired
    private HugeConfig config;
    @Autowired
    private FileMappingMapper mapper;

    private final Map<String, ReadWriteLock> uploadingTokenLocks;

    public FileMappingService() {
        this.uploadingTokenLocks = new ConcurrentHashMap<>();
    }

    public Map<String, ReadWriteLock> getUploadingTokenLocks() {
        return this.uploadingTokenLocks;
    }

    public FileMapping get(int id) {
        return this.mapper.selectById(id);
    }

    public FileMapping get(int connId, int jobId, String fileName) {
        QueryWrapper<FileMapping> query = Wrappers.query();
        query.eq("conn_id", connId)
             .eq("job_id", jobId)
             .eq("name", fileName);
        return this.mapper.selectOne(query);
    }

    public List<FileMapping> listAll() {
        return this.mapper.selectList(null);
    }

    public IPage<FileMapping> list(int connId, int jobId, int pageNo, int pageSize) {
        QueryWrapper<FileMapping> query = Wrappers.query();
        query.eq("conn_id", connId);
        query.eq("job_id", jobId);
        query.eq("file_status", FileMappingStatus.COMPLETED.getValue());
        query.orderByDesc("create_time");
        Page<FileMapping> page = new Page<>(pageNo, pageSize);
        return this.mapper.selectPage(page, query);
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void save(FileMapping mapping) {
        if (this.mapper.insert(mapping) != 1) {
            throw new InternalException("entity.insert.failed", mapping);
        }
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void update(FileMapping mapping) {
        if (this.mapper.updateById(mapping) != 1) {
            throw new InternalException("entity.update.failed", mapping);
        }
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void remove(int id) {
        if (this.mapper.deleteById(id) != 1) {
            throw new InternalException("entity.delete.failed", id);
        }
    }

    public String generateFileToken(String fileName) {
        return HubbleUtil.md5(fileName) + "-" +
               HubbleUtil.nowTime().getEpochSecond();
    }

    public FileUploadResult uploadFile(MultipartFile srcFile, int index,
                                       String dirPath) {
        FileUploadResult result = new FileUploadResult();
        // Current part saved path
        String partName = srcFile.getOriginalFilename();
        result.setName(partName);
        result.setSize(srcFile.getSize());

        File destFile = new File(dirPath, partName + "-" + index);
        // File all parts saved path
        File dir = new File(dirPath);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        if (destFile.exists()) {
            destFile.delete();
        }

        log.debug("Uploading file {} length {}", partName, srcFile.getSize());
        try {
            // transferTo should accept absolute path
            srcFile.transferTo(destFile.getAbsoluteFile());
            result.setStatus(FileUploadResult.Status.SUCCESS);
            log.debug("Uploaded file part {}-{}", partName, index);
        } catch (Exception e) {
            log.error("Failed to save upload file and insert " +
                      "file mapping record", e);
            result.setStatus(FileUploadResult.Status.FAILURE);
            result.setCause(e.getMessage());
        }
        return result;
    }

    public boolean tryMergePartFiles(String dirPath, int total) {
        File dir = new File(dirPath);
        File[] partFiles = dir.listFiles();
        if (partFiles == null) {
            throw new InternalException("The part files can't be null");
        }
        if (partFiles.length != total) {
            return false;
        }

        File newFile = new File(dir.getPath() + ".all");
        File destFile = new File(dir.getPath());
        if (partFiles.length == 1) {
            try {
                // Rename file to dest file
                FileUtils.moveFile(partFiles[0], newFile);
            } catch (IOException e) {
                log.error("Failed to rename file from {} to {}",
                          partFiles[0], newFile, e);
                throw new InternalException("load.upload.move-file.failed", e);
            }
        } else {
            Arrays.sort(partFiles, (o1, o2) -> {
                String file1Idx = StringUtils.substringAfterLast(o1.getName(),
                                                                 "-");
                String file2Idx = StringUtils.substringAfterLast(o2.getName(),
                                                                 "-");
                Integer idx1 = Integer.valueOf(file1Idx);
                Integer idx2 = Integer.valueOf(file2Idx);
                return idx1.compareTo(idx2);
            });
            try (OutputStream os = new FileOutputStream(newFile, true)) {
                for (int i = 0; i < partFiles.length; i++) {
                    File partFile = partFiles[i];
                    try (InputStream is = new FileInputStream(partFile)) {
                        IOUtils.copy(is, os);
                    } catch (IOException e) {
                        log.error("Failed to copy file stream from {} to {}",
                                  partFile, newFile, e);
                        throw new InternalException(
                                  "load.upload.merge-file.failed", e);
                    }
                }
            } catch (IOException e) {
                log.error("Failed to copy all file-parts stream to {}",
                          newFile, e);
                throw new InternalException("load.upload.merge-file.failed", e);
            }
        }
        // Delete origin directory
        try {
            FileUtils.forceDelete(dir);
        } catch (IOException e) {
            log.error("Failed to force delete file {}", dir, e);
            throw new InternalException("load.upload.delete-temp-dir.failed", e);
        }
        // Rename file to dest file
        if (!newFile.renameTo(destFile)) {
            throw new InternalException("load.upload.rename-file.failed");
        }
        return true;
    }

    public void extractColumns(FileMapping mapping) {
        File file = FileUtils.getFile(mapping.getPath());
        BufferedReader reader;
        try {
            reader = new BufferedReader(new FileReader(file));
        } catch (FileNotFoundException e) {
            throw new InternalException("The file '%s' is not found", file);
        }
        FileSetting setting = mapping.getFileSetting();
        String delimiter = setting.getDelimiter();
        Pattern pattern = Pattern.compile(setting.getSkippedLine());

        String[] columnNames;
        String[] columnValues;
        try {
            String line;
            while ((line = reader.readLine()) != null) {
                if (!pattern.matcher(line).matches()) {
                    break;
                }
            }
            Ex.check(line != null,
                     "The file has no data line can treat as header");

            String[] firstLine = StringUtils.split(line, delimiter);
            if (setting.isHasHeader()) {
                // The first line as column names
                columnNames = firstLine;
                // The second line as column values
                line = reader.readLine();
                columnValues = StringUtil.split(line, delimiter);
            } else {
                // Let columns names as: column-1, column-2 ...
                columnNames = new String[firstLine.length];
                for (int i = 1; i <= firstLine.length; i++) {
                    columnNames[i - 1] = "col-" + i;
                }
                // The first line as column values
                columnValues = firstLine;
            }
        } catch (IOException e) {
            throw new InternalException("Failed to read header and sample " +
                                        "data from file '%s'", file);
        } finally {
            IOUtils.closeQuietly(reader);
        }

        setting.setColumnNames(Arrays.asList(columnNames));
        setting.setColumnValues(Arrays.asList(columnValues));
    }

    public String moveToNextLevelDir(FileMapping mapping) {
        File currFile = new File(mapping.getPath());
        String destPath = Paths.get(currFile.getParentFile().getPath(),
                                    FILE_PREIFX + mapping.getId())
                               .toString();
        File destDir = new File(destPath);
        try {
            FileUtils.moveFileToDirectory(currFile, destDir, true);
        } catch (IOException e) {
            this.remove(mapping.getId());
            throw new InternalException(
                      "Failed to move file to next level directory");
        }
        return Paths.get(destPath, currFile.getName()).toString();
    }

    public void deleteDiskFile(FileMapping mapping) {
        File file = new File(mapping.getPath());
        if (file.isDirectory()) {
            log.info("Prepare to delete directory {}", file);
            try {
                FileUtils.forceDelete(file);
            } catch (IOException e) {
                throw new InternalException("Failed to delete directory " +
                                            "corresponded to the file id %s, " +
                                            "please delete it manually",
                                            e, mapping.getId());
            }
        } else {
            File parentDir = file.getParentFile();
            log.info("Prepare to delete directory {}", parentDir);
            try {
                FileUtils.forceDelete(parentDir);
            } catch (IOException e) {
                throw new InternalException("Failed to delete parent directory " +
                                            "corresponded to the file id %s, " +
                                            "please delete it manually",
                                            e, mapping.getId());
            }
        }
    }

    @Async
    @Scheduled(fixedRate = 10 * 60 * 1000)
    public void deleteUnfinishedFile() {
        QueryWrapper<FileMapping> query = Wrappers.query();
        query.in("file_status", FileMappingStatus.UPLOADING.getValue());
        List<FileMapping> mappings = this.mapper.selectList(query);
        long threshold = this.config.get(
                         HubbleOptions.UPLOAD_FILE_MAX_TIME_CONSUMING) * 1000;
        Date now = HubbleUtil.nowDate();
        for (FileMapping mapping : mappings) {
            Date updateTime = mapping.getUpdateTime();
            long duration = now.getTime() - updateTime.getTime();
            if (duration > threshold) {
                String filePath = mapping.getPath();
                try {
                    FileUtils.forceDelete(new File(filePath));
                } catch (IOException e) {
                    log.warn("Failed to delete expired uploading file {}",
                             filePath, e);
                }
                this.remove(mapping.getId());
                // Delete corresponding uploading tokens
                Iterator<Map.Entry<String, ReadWriteLock>> iter;
                iter = this.uploadingTokenLocks.entrySet().iterator();
                iter.forEachRemaining(entry -> {
                    String token = entry.getKey();
                    if (token.startsWith(mapping.getName())) {
                        iter.remove();
                    }
                });
            }
        }
    }
}
