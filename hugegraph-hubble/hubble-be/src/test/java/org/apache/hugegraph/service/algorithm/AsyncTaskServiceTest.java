/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.hugegraph.service.algorithm;

import java.util.List;

import org.apache.hugegraph.driver.HugeClient;
import org.apache.hugegraph.driver.TaskManager;
import org.apache.hugegraph.exception.ServerException;
import org.apache.hugegraph.service.space.DirectVermeerService;
import org.apache.hugegraph.structure.Task;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;

public class AsyncTaskServiceTest {

    private AsyncTaskService service;
    private HugeClient client;
    private TaskManager tasks;

    @Before
    public void setup() {
        this.service = new AsyncTaskService();
        this.client = Mockito.mock(HugeClient.class);
        this.tasks = Mockito.mock(TaskManager.class);
        Mockito.when(this.client.task()).thenReturn(this.tasks);
        Mockito.when(this.tasks.list((String) null)).thenReturn(List.of());
    }

    @Test
    public void testFindsSubmittedTaskBeforeItAppearsInList() {
        Task submitted = task(80001L, "g.V().limit(100002)", "running");
        Mockito.when(this.tasks.list(List.of(80001L))).thenReturn(List.of(submitted));

        Assert.assertEquals(List.of(submitted), this.service.list(
                this.client, 1, 10, " 80001 ", "", "").getRecords());
        Mockito.verify(this.tasks, Mockito.never()).get(Mockito.anyLong());
    }

    @Test
    public void testKeepsNumericNameMatchesAndAppliesStatusAfterIdLookup() {
        Task named = task(70001L, "query 80001", "failed");
        Task submitted = task(80001L, "g.V()", "running");
        Mockito.when(this.tasks.list("FAILED")).thenReturn(List.of(named));
        Mockito.when(this.tasks.list(List.of(80001L))).thenReturn(List.of(submitted));

        Assert.assertEquals(List.of(named), this.service.list(
                this.client, 1, 10, "80001", "gremlin", "FAILED").getRecords());
        Mockito.when(this.tasks.list((String) null)).thenReturn(List.of(named));
        Assert.assertEquals(2, this.service.list(
                this.client, 1, 10, "80001", "", "").getTotal());
    }

    @Test
    public void testDoesNotFetchAnIdAlreadyInTheList() {
        Task submitted = task(80001L, "g.V()", "failed");
        Mockito.when(this.tasks.list((String) null)).thenReturn(List.of(submitted));

        Assert.assertEquals(List.of(submitted), this.service.list(
                this.client, 1, 10, "80001", "", "").getRecords());
        Mockito.verify(this.tasks, Mockito.never()).list(List.of(80001L));
    }

    @Test
    public void testMissingIdDoesNotHideNumericNameMatches() {
        Task named = task(70001L, "query 80001", "failed");
        Mockito.when(this.tasks.list((String) null)).thenReturn(List.of(named));
        ServerException missing = new ServerException("Task not found");
        missing.status(404);
        Mockito.when(this.tasks.list(List.of(80001L))).thenThrow(missing);

        Assert.assertEquals(List.of(named), this.service.list(
                this.client, 1, 10, "80001", "", "").getRecords());
    }

    @Test(expected = ServerException.class)
    public void testDoesNotTurnLookupFailureIntoEmptySuccess() {
        ServerException unavailable = new ServerException("Server unavailable");
        unavailable.status(503);
        Mockito.when(this.tasks.list(List.of(80001L))).thenThrow(unavailable);
        this.service.list(this.client, 1, 10, "80001", "", "");
    }

    @Test
    public void testIdsPollingTreatsAnUndiscoveredServerTaskAsEmpty() {
        ServerException missing = new ServerException("Task not found");
        missing.status(404);
        Mockito.when(this.tasks.list(List.of(80001L))).thenThrow(missing);

        Assert.assertTrue(this.service.list(this.client, List.of(80001L)).isEmpty());
    }

    @Test
    public void testIdsPollingPreservesPermissionAndServerFailures() {
        for (int status : List.of(403, 503)) {
            ServerException failure = new ServerException("Task lookup failed");
            failure.status(status);
            Mockito.doThrow(failure).when(this.tasks).list(List.of(80001L));

            try {
                this.service.list(this.client, List.of(80001L));
                Assert.fail("Expected task lookup failure to propagate");
            } catch (ServerException e) {
                Assert.assertSame(failure, e);
            }
        }
    }

    @Test
    public void testVermeerIdPollingDoesNotFetchResultValues() {
        DirectVermeerService vermeer = Mockito.mock(DirectVermeerService.class);
        ReflectionTestUtils.setField(this.service, "vermeer", vermeer);
        Task submitted = task(-7L, "pagerank", "success");
        Mockito.when(vermeer.task(this.client, -7, false)).thenReturn(submitted);

        Assert.assertEquals(List.of(submitted), this.service.list(this.client, List.of(-7L)));
        Mockito.verify(vermeer, Mockito.never()).task(this.client, -7, true);
        Mockito.verifyNoInteractions(this.tasks);
    }

    private static Task task(long id, String name, String status) {
        Task task = new Task();
        ReflectionTestUtils.setField(task, "id", id);
        ReflectionTestUtils.setField(task, "name", name);
        ReflectionTestUtils.setField(task, "status", status);
        task.type("gremlin");
        return task;
    }
}
