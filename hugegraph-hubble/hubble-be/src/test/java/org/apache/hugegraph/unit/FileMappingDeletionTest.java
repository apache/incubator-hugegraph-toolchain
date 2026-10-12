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

import java.lang.reflect.Field;
import java.util.Collections;

import org.junit.Test;
import org.mockito.Mockito;

import org.apache.hugegraph.controller.load.FileMappingController;
import org.apache.hugegraph.driver.HugeClient;
import org.apache.hugegraph.entity.enums.LoadStatus;
import org.apache.hugegraph.entity.load.FileMapping;
import org.apache.hugegraph.entity.load.LoadTask;
import org.apache.hugegraph.exception.ExternalException;
import org.apache.hugegraph.service.load.FileMappingService;
import org.apache.hugegraph.service.load.JobManagerService;
import org.apache.hugegraph.service.load.LoadTaskService;
import org.apache.hugegraph.testutil.Assert;

public class FileMappingDeletionTest {

    @Test
    public void testDeleteProtectsEveryRecoverableTaskStatus()
            throws Exception {
        for (LoadStatus status : new LoadStatus[]{LoadStatus.RUNNING,
                                                  LoadStatus.PAUSED,
                                                  LoadStatus.FAILED,
                                                  LoadStatus.STOPPED}) {
            Fixture fixture = this.fixture(status);

            Assert.assertThrows(ExternalException.class, () ->
                fixture.controller.delete("DEFAULT", "hugegraph", 1, 7));

            Mockito.verify(fixture.jobService, Mockito.never())
                   .deleteMappings(Mockito.anyInt(), Mockito.anyList());
        }
    }

    @Test
    public void testDeleteAllowsTerminalTaskAndUsesTransactionalService()
            throws Exception {
        Fixture fixture = this.fixture(LoadStatus.SUCCEED);

        fixture.controller.delete("DEFAULT", "hugegraph", 1, 7);

        Mockito.verify(fixture.jobService).deleteMappings(
                1, Collections.singletonList(fixture.mapping));
        Mockito.verify(fixture.mappingService, Mockito.never())
               .deleteDiskFile(Mockito.any());
        Mockito.verify(fixture.mappingService, Mockito.never())
               .remove(Mockito.anyInt());
    }

    private Fixture fixture(LoadStatus status) throws Exception {
        Fixture fixture = new Fixture();
        fixture.controller = new TestFileMappingController();
        fixture.mappingService = Mockito.mock(FileMappingService.class);
        fixture.jobService = Mockito.mock(JobManagerService.class);
        LoadTaskService taskService = Mockito.mock(LoadTaskService.class);
        fixture.mapping = new FileMapping();
        fixture.mapping.setId(7);
        fixture.mapping.setJobId(1);
        fixture.mapping.setTotalSize(8L);
        LoadTask task = LoadTask.builder()
                                .jobId(1)
                                .fileId(7)
                                .status(status)
                                .build();
        Mockito.when(fixture.mappingService.get("DEFAULT", "hugegraph", 1, 7))
               .thenReturn(fixture.mapping);
        Mockito.when(taskService.taskListByJob(1))
               .thenReturn(Collections.singletonList(task));
        setField(fixture.controller, "service", fixture.mappingService);
        setField(fixture.controller, "jobService", fixture.jobService);
        setField(fixture.controller, "taskService", taskService);
        return fixture;
    }

    private static void setField(Object target, String name, Object value)
            throws Exception {
        Class<?> type = target.getClass();
        while (type != null) {
            try {
                Field field = type.getDeclaredField(name);
                field.setAccessible(true);
                field.set(target, value);
                return;
            } catch (NoSuchFieldException ignored) {
                type = type.getSuperclass();
            }
        }
        throw new NoSuchFieldException(name);
    }

    private static class TestFileMappingController
            extends FileMappingController {

        @Override
        protected HugeClient requireGraphSpaceWrite(String graphSpace) {
            return Mockito.mock(HugeClient.class);
        }
    }

    private static final class Fixture {

        private FileMappingController controller;
        private FileMappingService mappingService;
        private JobManagerService jobService;
        private FileMapping mapping;
    }
}
