/*
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

package org.apache.hugegraph.unit;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.mock.web.MockMultipartFile;

import org.apache.hugegraph.config.HugeConfig;
import org.apache.hugegraph.controller.load.FileUploadController;
import org.apache.hugegraph.entity.enums.FileMappingStatus;
import org.apache.hugegraph.entity.enums.JobStatus;
import org.apache.hugegraph.entity.load.FileMapping;
import org.apache.hugegraph.entity.load.JobManager;
import org.apache.hugegraph.exception.ExternalException;
import org.apache.hugegraph.exception.InternalException;
import org.apache.hugegraph.options.HubbleOptions;
import org.apache.hugegraph.service.load.FileMappingService;
import org.apache.hugegraph.service.load.JobManagerService;
import org.apache.hugegraph.testutil.Assert;

public class FileUploadControllerTest {

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    @Test
    public void testCheckFileValidAcceptsUppercaseTrimmedFormat()
           throws Exception {
        FileUploadController controller = this.controller(" CSV ", " TXT ");
        MockMultipartFile file = new MockMultipartFile("file", "HLM.TXT",
                                                       "text/plain",
                                                       "name\nmarko".getBytes());
        JobManager job = JobManager.builder()
                                   .id(1)
                                   .jobStatus(JobStatus.UPLOADING)
                                   .build();

        this.checkFileValid(controller, job, file, "HLM.TXT");
    }

    @Test
    public void testCheckFileValidSkipsNullAndBlankWhitelistItems()
           throws Exception {
        FileUploadController controller = this.controller(" CSV ", null, " ",
                                                          " TXT ");
        MockMultipartFile file = new MockMultipartFile("file", "HLM.TXT",
                                                       "text/plain",
                                                       "name\nmarko".getBytes());
        JobManager job = JobManager.builder()
                                   .id(1)
                                   .jobStatus(JobStatus.UPLOADING)
                                   .build();

        this.checkFileValid(controller, job, file, "HLM.TXT");
    }

    @Test
    public void testCheckFileValidRejectsMissingExtension() throws Exception {
        FileUploadController controller = this.controller("csv", "txt");
        MockMultipartFile file = new MockMultipartFile("file", "HLM",
                                                       "text/plain",
                                                       "name\nmarko".getBytes());
        JobManager job = JobManager.builder()
                                   .id(1)
                                   .jobStatus(JobStatus.UPLOADING)
                                   .build();

        Assert.assertThrows(ExternalException.class, () -> {
            this.checkFileValid(controller, job, file, "HLM");
        });
    }

    @Test
    public void testCheckFileValidRejectsEmptyWhitelist() throws Exception {
        FileUploadController controller = this.controller((String[]) null);
        MockMultipartFile file = new MockMultipartFile("file", "HLM.TXT",
                                                       "text/plain",
                                                       "name\nmarko".getBytes());
        JobManager job = JobManager.builder()
                                   .id(1)
                                   .jobStatus(JobStatus.UPLOADING)
                                   .build();

        Assert.assertThrows(ExternalException.class, () -> {
            this.checkFileValid(controller, job, file, "HLM.TXT");
        });
    }

    @Test
    public void testReserveUploadQuotaUsesGraphRouteForNewMapping()
           throws Exception {
        FileUploadController controller = this.controller("csv");
        FileMappingService service = Mockito.mock(FileMappingService.class);
        Path uploadRoot = this.folder.newFolder("quota-upload").toPath();
        Path uploadPath = uploadRoot.resolve("data.csv");
        FileMappingService pathValidator = this.fileMappingService(uploadRoot);
        Mockito.when(service.requirePathUnderUploadRoot(Mockito.anyString()))
               .thenAnswer(call -> pathValidator.requirePathUnderUploadRoot(call.getArgument(0)));
        JobManagerService jobService = Mockito.mock(JobManagerService.class);
        JobManager job = JobManager.builder().id(1).jobSize(20L).build();
        Mockito.when(jobService.get("DEFAULT", "hugegraph", 1))
               .thenReturn(job);
        Mockito.when(service.get("DEFAULT", "hugegraph", 1, "data.csv"))
               .thenReturn(null);
        Mockito.when(service.listByJob(1)).thenReturn(Collections.emptyList());
        this.setField(controller, "service", service);
        this.setField(controller, "jobService", jobService);

        FileMapping mapping = this.reserveUploadQuota(controller, "DEFAULT",
                                                      "hugegraph", 1,
                                                      "data.csv",
                                                      uploadPath.toString(), 10L);

        ArgumentCaptor<FileMapping> captor = ArgumentCaptor.forClass(
                                             FileMapping.class);
        Mockito.verify(service).save(captor.capture());
        Assert.assertEquals(mapping, captor.getValue());
        Assert.assertEquals("DEFAULT", mapping.getGraphSpace());
        Assert.assertEquals("hugegraph", mapping.getGraph());
        Assert.assertEquals("data.csv", mapping.getName());
        Assert.assertEquals(uploadPath.toString(), mapping.getPath());
        Assert.assertTrue(Files.isDirectory(uploadPath));
        Assert.assertEquals(10L, mapping.getTotalSize());
        Assert.assertEquals(FileMappingStatus.UPLOADING,
                            mapping.getFileStatus());

        Path failedPath = uploadRoot.resolve("failed.csv");
        Mockito.doThrow(new InternalException("database failure")).when(service).save(Mockito.any());
        Assert.assertThrows(InternalException.class, () ->
                this.reserveUploadQuota(controller, "DEFAULT", "hugegraph", 1,
                                        "failed.csv", failedPath.toString(), 10L));
        Assert.assertFalse(Files.exists(failedPath));
        Assert.assertTrue(Files.isDirectory(uploadPath));
    }

    @Test
    public void testReserveUploadQuotaRejectsCompletedDuplicate()
           throws Exception {
        FileUploadController controller = this.controller("csv");
        FileMappingService service = Mockito.mock(FileMappingService.class);
        JobManagerService jobService = Mockito.mock(JobManagerService.class);
        JobManager job = JobManager.builder().id(1).jobSize(20L).build();
        FileMapping completed = new FileMapping("DEFAULT", "hugegraph",
                                                "data.csv",
                                                "upload/old.csv");
        completed.setFileStatus(FileMappingStatus.COMPLETED);
        Mockito.when(jobService.get("DEFAULT", "hugegraph", 1))
               .thenReturn(job);
        Mockito.when(service.get("DEFAULT", "hugegraph", 1, "data.csv"))
               .thenReturn(completed);
        this.setField(controller, "service", service);
        this.setField(controller, "jobService", jobService);

        Assert.assertThrows(ExternalException.class, () -> {
            this.reserveUploadQuota(controller, "DEFAULT", "hugegraph", 1,
                                    "data.csv", "upload/data.csv", 10L);
        });
        Mockito.verify(service, Mockito.never()).save(Mockito.any());
        Mockito.verify(service, Mockito.never()).update(Mockito.any());
    }

    @Test
    public void testReserveUploadQuotaUpdatesExistingUploadingMapping()
           throws Exception {
        FileUploadController controller = this.controller("csv");
        FileMappingService service = Mockito.mock(FileMappingService.class);
        Path uploadRoot = this.folder.newFolder("quota-upload").toPath();
        Path uploadPath = uploadRoot.resolve("data.csv");
        FileMappingService pathValidator = this.fileMappingService(uploadRoot);
        Mockito.when(service.requirePathUnderUploadRoot(Mockito.anyString()))
               .thenAnswer(call -> pathValidator.requirePathUnderUploadRoot(call.getArgument(0)));
        JobManagerService jobService = Mockito.mock(JobManagerService.class);
        JobManager job = JobManager.builder().id(1).jobSize(20L).build();
        FileMapping existing = new FileMapping("DEFAULT", "hugegraph",
                                               "data.csv",
                                               "upload/old.csv");
        existing.setId(2);
        existing.setJobId(1);
        existing.setTotalSize(5L);
        existing.setFileStatus(FileMappingStatus.UPLOADING);
        List<FileMapping> uploading = new ArrayList<>();
        uploading.add(existing);
        Mockito.when(jobService.get("DEFAULT", "hugegraph", 1))
               .thenReturn(job);
        Mockito.when(service.get("DEFAULT", "hugegraph", 1, "data.csv"))
               .thenReturn(existing);
        Mockito.when(service.listByJob(1)).thenReturn(uploading);
        this.setField(controller, "service", service);
        this.setField(controller, "jobService", jobService);

        FileMapping mapping = this.reserveUploadQuota(controller, "DEFAULT",
                                                      "hugegraph", 1,
                                                      "data.csv",
                                                      uploadPath.toString(), 10L);

        Assert.assertEquals(existing, mapping);
        Assert.assertEquals(uploadPath.toString(), mapping.getPath());
        Assert.assertTrue(Files.isDirectory(uploadPath));
        Assert.assertEquals(10L, mapping.getTotalSize());
        Assert.assertEquals(FileMappingStatus.UPLOADING,
                            mapping.getFileStatus());
        Mockito.verify(service).update(existing);
        Mockito.verify(service, Mockito.never()).save(Mockito.any());
    }

    @Test
    public void testChunkReservationsReuseDirectoryAndStillEnforceQuota()
           throws Exception {
        FileUploadController controller = this.controller("csv");
        Path uploadRoot = this.folder.newFolder("chunk-upload").toPath();
        Path uploadPath = uploadRoot.resolve("data.csv");
        FileMappingService service = Mockito.spy(this.fileMappingService(uploadRoot));
        JobManagerService jobService = Mockito.mock(JobManagerService.class);
        JobManager job = JobManager.builder().id(1).jobSize(20L).build();
        Mockito.when(jobService.get("DEFAULT", "hugegraph", 1)).thenReturn(job);
        List<FileMapping> mappings = new ArrayList<>();
        Path otherPath = Files.createDirectory(uploadRoot.resolve("other.csv"));
        FileMapping other = new FileMapping("DEFAULT", "hugegraph", "other.csv", otherPath.toString());
        other.setId(3);
        other.setFileStatus(FileMappingStatus.UPLOADING);
        other.setTotalSize(5L);
        mappings.add(other);
        Mockito.doReturn(mappings).when(service).listByJob(1);
        Mockito.doAnswer(call -> mappings.size() == 1 ? null : mappings.get(1))
               .when(service).get("DEFAULT", "hugegraph", 1, "data.csv");
        Mockito.doAnswer(call -> {
            FileMapping mapping = call.getArgument(0);
            mapping.setId(2);
            mappings.add(mapping);
            return null;
        }).when(service).save(Mockito.any());
        Mockito.doNothing().when(service).update(Mockito.any());
        this.setField(controller, "service", service);
        this.setField(controller, "jobService", jobService);

        for (int i = 0; i < 3; i++) {
            this.reserveUploadQuota(controller, "DEFAULT", "hugegraph", 1,
                                    "data.csv", uploadPath.toString(), 10L);
            MockMultipartFile chunk = new MockMultipartFile(
                    "file", "data.csv", "text/csv", ("part" + i).getBytes());
            service.uploadFile(chunk, "data.csv", i, uploadPath.toString());
        }
        // One quota scan per chunk and one physical alias scan for the first chunk.
        Mockito.verify(service, Mockito.times(4)).listByJob(1);
        Mockito.verify(service, Mockito.times(6))
               .requirePathUnderUploadRoot(Mockito.anyString());
        Mockito.verify(service).requirePathUnderUploadRoot(otherPath.toString());
        Path outsidePath = uploadRoot.getParent().resolve("outside.csv");
        Assert.assertThrows(InternalException.class, () ->
                this.reserveUploadQuota(controller, "DEFAULT", "hugegraph", 1,
                                        "data.csv", outsidePath.toString(), 10L));
        Assert.assertTrue(service.tryMergePartFiles(uploadPath.toString(), 3));
        Assert.assertEquals("part0part1part2", Files.readString(uploadPath));

        // Increasing the declared size and a concurrently increased job size are still checked.
        Assert.assertThrows(ExternalException.class, () ->
                this.reserveUploadQuota(controller, "DEFAULT", "hugegraph", 1,
                                        "data.csv", uploadPath.toString(), 1025L));
        job.setJobSize(2040L);
        Assert.assertThrows(ExternalException.class, () ->
                this.reserveUploadQuota(controller, "DEFAULT", "hugegraph", 1,
                                        "data.csv", uploadPath.toString(), 10L));
        Assert.assertEquals(10L, mappings.get(1).getTotalSize());
    }

    @Test
    public void testFirstReservationRejectsNativeAliases() throws Exception {
        Path uploadRoot = this.folder.newFolder("alias-upload").toPath();
        for (String[] names : new String[][]{{"data.csv", "DATA.csv"},
                                             {"caf\u00e9.csv", "cafe\u0301.csv"}}) {
            Path original = Files.createDirectory(uploadRoot.resolve(names[0]));
            Path alias = uploadRoot.resolve(names[1]);
            // Exercise case/Unicode aliases where the native filesystem actually aliases them.
            if (!Files.exists(alias)) {
                Files.createSymbolicLink(alias, original);
            }
            Assert.assertTrue(Files.isSameFile(original, alias));
            FileUploadController controller = this.controller("csv");
            FileMappingService service = Mockito.spy(this.fileMappingService(uploadRoot));
            FileMapping existing = new FileMapping("DEFAULT", "hugegraph", names[0], original.toString());
            existing.setFileStatus(FileMappingStatus.UPLOADING);
            Mockito.doReturn(null).when(service).get("DEFAULT", "hugegraph", 1, names[1]);
            Mockito.doReturn(Collections.singletonList(existing)).when(service).listByJob(1);
            JobManagerService jobService = Mockito.mock(JobManagerService.class);
            Mockito.when(jobService.get("DEFAULT", "hugegraph", 1))
                   .thenReturn(JobManager.builder().id(1).jobSize(0L).build());
            this.setField(controller, "service", service);
            this.setField(controller, "jobService", jobService);

            Assert.assertThrows(ExternalException.class, () ->
                    this.reserveUploadQuota(controller, "DEFAULT", "hugegraph", 1,
                                            names[1], alias.toString(), 10L));
            Mockito.verify(service, Mockito.never()).save(Mockito.any());
            Assert.assertTrue(Files.isDirectory(original));
        }
    }

    @Test
    public void testCheckTotalAndIndexValidRejectsIndexAtTotal()
           throws Exception {
        FileUploadController controller = this.controller("csv");

        Assert.assertThrows(InternalException.class, () -> {
            this.checkTotalAndIndexValid(controller, 2, 2);
        });
    }

    @Test
    public void testUploadFileUsesValidatedFileNameForPartPath()
           throws Exception {
        Path tempDir = Files.createTempDirectory("hubble-upload");
        try {
            FileMappingService service = new FileMappingService();
            MockMultipartFile file = new MockMultipartFile(
                    "file", "../evil.csv", "text/csv", "id,name".getBytes());

            service.uploadFile(file, "safe.csv", 0, tempDir.toString());

            Assert.assertTrue(Files.exists(tempDir.resolve("safe.csv-0")));
            Assert.assertFalse(Files.exists(tempDir.resolve("evil.csv-0")));
            Assert.assertFalse(Files.exists(tempDir.getParent()
                                                  .resolve("evil.csv-0")));
        } finally {
            FileUtils.deleteDirectory(tempDir.toFile());
        }
    }

    @Test
    public void testRestoreMovedUploadRestoresFileAndRemovesDirectory()
           throws Exception {
        Path uploadRoot = Files.createTempDirectory("hubble-upload-restore");
        try {
            Path movedDir = Files.createDirectories(
                            uploadRoot.resolve("file-mapping-2"));
            Path movedFile = Files.write(movedDir.resolve("data.csv"),
                                         "id,name".getBytes());
            Path originalFile = uploadRoot.resolve("data.csv");
            FileMappingService service = this.fileMappingService(uploadRoot);

            service.restoreMovedUpload(movedFile.toString(),
                                       originalFile.toString());

            Assert.assertTrue(Files.exists(originalFile));
            Assert.assertFalse(Files.exists(movedFile));
            Assert.assertFalse(Files.exists(movedDir));
        } finally {
            FileUtils.deleteDirectory(uploadRoot.toFile());
        }
    }

    @Test
    public void testRestoreMovedUploadRejectsSymlinkedDestinationParent()
           throws Exception {
        Path uploadRoot = Files.createTempDirectory("hubble-upload-root");
        Path outsideRoot = Files.createTempDirectory("hubble-upload-outside");
        try {
            Path movedDir = Files.createDirectories(
                            uploadRoot.resolve("file-mapping-2"));
            Path movedFile = Files.write(movedDir.resolve("data.csv"),
                                         "id,name".getBytes());
            Path linkedParent = uploadRoot.resolve("linked");
            Files.createSymbolicLink(linkedParent, outsideRoot);
            Path originalFile = linkedParent.resolve("data.csv");
            FileMappingService service = this.fileMappingService(uploadRoot);

            Assert.assertThrows(InternalException.class, () ->
                    service.restoreMovedUpload(movedFile.toString(),
                                               originalFile.toString()));
            Assert.assertTrue(Files.exists(movedFile));
            Assert.assertFalse(Files.exists(outsideRoot.resolve("data.csv")));
        } finally {
            FileUtils.deleteDirectory(uploadRoot.toFile());
            FileUtils.deleteDirectory(outsideRoot.toFile());
        }
    }

    @Test
    public void testTryMergePartFilesReplacesStaleAllFile() throws Exception {
        Path parent = Files.createTempDirectory("hubble-merge");
        File dest = parent.resolve("data.csv").toFile();
        File all = parent.resolve("data.csv.all").toFile();
        File partDir = dest;
        try {
            FileUtils.forceMkdir(partDir);
            Files.write(all.toPath(), "stale".getBytes());
            Files.write(new File(partDir, "data.csv-0").toPath(),
                        "new".getBytes());

            FileMappingService service = new FileMappingService();
            Assert.assertTrue(service.tryMergePartFiles(partDir.toString(), 1));

            Assert.assertEquals("new",
                                new String(Files.readAllBytes(dest.toPath())));
        } finally {
            FileUtils.deleteDirectory(parent.toFile());
        }
    }

    @Test
    public void testFinalizeUploadRestoresMovedFileAfterDatabaseFailure()
           throws Exception {
        FileUploadController controller = this.controller("csv");
        FileMappingService service = Mockito.mock(FileMappingService.class);
        JobManagerService jobService = Mockito.mock(JobManagerService.class);
        FileMapping mapping = this.completedMapping("upload/data.csv");
        Mockito.when(service.moveToNextLevelDir(mapping))
               .thenReturn("upload/file-mapping-2/data.csv");
        Mockito.doThrow(new InternalException("database failure"))
               .when(jobService).completeUpload(mapping);
        this.setField(controller, "service", service);
        this.setField(controller, "jobService", jobService);

        Assert.assertThrows(InternalException.class,
                            () -> this.finalizeUpload(controller, mapping));

        Mockito.verify(service).restoreMovedUpload(
                "upload/file-mapping-2/data.csv", "upload/data.csv");
        Mockito.verify(service, Mockito.never()).update(mapping);
        Assert.assertEquals("upload/data.csv", mapping.getPath());
        Assert.assertEquals(FileMappingStatus.UPLOADING,
                            mapping.getFileStatus());
    }

    @Test
    public void testFinalizeUploadRetainsCleanupPathWhenRestoreFails()
           throws Exception {
        FileUploadController controller = this.controller("csv");
        FileMappingService service = Mockito.mock(FileMappingService.class);
        JobManagerService jobService = Mockito.mock(JobManagerService.class);
        FileMapping mapping = this.completedMapping("upload/data.csv");
        String movedPath = "upload/file-mapping-2/data.csv";
        Mockito.when(service.moveToNextLevelDir(mapping)).thenReturn(movedPath);
        Mockito.doThrow(new InternalException("database failure"))
               .when(jobService).completeUpload(mapping);
        Mockito.doThrow(new InternalException("restore failure"))
               .when(service).restoreMovedUpload(movedPath,
                                                 "upload/data.csv");
        this.setField(controller, "service", service);
        this.setField(controller, "jobService", jobService);

        Assert.assertThrows(InternalException.class,
                            () -> this.finalizeUpload(controller, mapping));

        Mockito.verify(service).update(mapping);
        Assert.assertEquals(movedPath, mapping.getPath());
        Assert.assertEquals(FileMappingStatus.FAILURE,
                            mapping.getFileStatus());

        JobManager job = JobManager.builder().id(1).jobSize(0L).build();
        Mockito.when(jobService.get("DEFAULT", "hugegraph", 1))
               .thenReturn(job);
        Mockito.when(service.get("DEFAULT", "hugegraph", 1, "data.csv"))
               .thenReturn(mapping);
        Assert.assertThrows(ExternalException.class, () ->
                this.reserveUploadQuota(controller, "DEFAULT", "hugegraph",
                                        1, "data.csv", "upload/data.csv",
                                        10L));
        Mockito.verify(service).update(mapping);
    }

    private FileMapping completedMapping(String path) {
        FileMapping mapping = new FileMapping("DEFAULT", "hugegraph",
                                              "data.csv", path);
        mapping.setId(2);
        mapping.setJobId(1);
        mapping.setFileStatus(FileMappingStatus.COMPLETED);
        mapping.setTotalSize(10L);
        return mapping;
    }

    private FileUploadController controller(String... formats) throws Exception {
        FileUploadController controller = new FileUploadController();
        HugeConfig config = Mockito.mock(HugeConfig.class);
        Mockito.when(config.get(HubbleOptions.UPLOAD_FILE_FORMAT_LIST))
               .thenReturn(formats == null ? null : Arrays.asList(formats));
        Mockito.when(config.get(HubbleOptions.UPLOAD_SINGLE_FILE_SIZE_LIMIT))
               .thenReturn(1024L);
        Mockito.when(config.get(HubbleOptions.UPLOAD_TOTAL_FILE_SIZE_LIMIT))
               .thenReturn(2048L);
        this.setField(controller, "config", config);
        return controller;
    }

    private FileMappingService fileMappingService(Path uploadRoot)
            throws Exception {
        FileMappingService service = new FileMappingService();
        HugeConfig config = Mockito.mock(HugeConfig.class);
        Mockito.when(config.get(HubbleOptions.UPLOAD_FILE_LOCATION))
               .thenReturn(uploadRoot.toString());
        this.setField(service, "config", config);
        return service;
    }

    private void checkFileValid(FileUploadController controller, JobManager job,
                                MockMultipartFile file, String fileName)
                                throws Exception {
        Method method = FileUploadController.class.getDeclaredMethod("checkFileValid",
                                                                    String.class,
                                                                    String.class,
                                                                    int.class,
                                                                    JobManager.class,
                                                            org.springframework.web.multipart.MultipartFile.class,
                                                                    String.class);
        method.setAccessible(true);
        try {
            method.invoke(controller, "DEFAULT", "hugegraph", 1, job, file,
                          fileName);
        } catch (InvocationTargetException e) {
            Throwable cause = e.getCause();
            if (cause instanceof Exception) {
                throw (Exception) cause;
            }
            throw e;
        }
    }

    private FileMapping reserveUploadQuota(FileUploadController controller,
                                           String graphSpace, String graph,
                                           int jobId, String fileName,
                                           String filePath,
                                           Long sourceFileSize)
                                           throws Exception {
        Method method = FileUploadController.class.getDeclaredMethod(
                        "reserveUploadQuota", String.class, String.class,
                        int.class, String.class, String.class, Long.class);
        method.setAccessible(true);
        try {
            return (FileMapping) method.invoke(controller, graphSpace, graph,
                                               jobId, fileName, filePath,
                                               sourceFileSize);
        } catch (InvocationTargetException e) {
            Throwable cause = e.getCause();
            if (cause instanceof Exception) {
                throw (Exception) cause;
            }
            throw e;
        }
    }

    private void finalizeUpload(FileUploadController controller,
                                FileMapping mapping) throws Exception {
        Method method = FileUploadController.class.getDeclaredMethod(
                        "finalizeUpload", FileMapping.class);
        method.setAccessible(true);
        try {
            method.invoke(controller, mapping);
        } catch (InvocationTargetException e) {
            Throwable cause = e.getCause();
            if (cause instanceof Exception) {
                throw (Exception) cause;
            }
            throw e;
        }
    }

    private void checkTotalAndIndexValid(FileUploadController controller,
                                         int total, int index)
                                         throws Exception {
        Method method = FileUploadController.class.getDeclaredMethod(
                        "checkTotalAndIndexValid", int.class, int.class);
        method.setAccessible(true);
        try {
            method.invoke(controller, total, index);
        } catch (InvocationTargetException e) {
            Throwable cause = e.getCause();
            if (cause instanceof Exception) {
                throw (Exception) cause;
            }
            throw e;
        }
    }

    private void setField(Object object, String name, Object value) throws Exception {
        Field field = object.getClass().getDeclaredField(name);
        field.setAccessible(true);
        field.set(object, value);
    }
}
