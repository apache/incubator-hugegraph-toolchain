/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.hugegraph.service.algorithm;

import java.util.Collections;

import org.junit.Test;
import org.mockito.Mockito;

import org.apache.hugegraph.driver.ComputerManager;
import org.apache.hugegraph.driver.HugeClient;
import org.apache.hugegraph.entity.algorithm.OlapEntity;
import org.apache.hugegraph.exception.ServerCapabilityUnavailableException;
import org.apache.hugegraph.testutil.Assert;

public class OlapAlgoServiceTest {

    @Test
    public void testComputerAvailabilityUsesTaskEndpoint() {
        HugeClient client = Mockito.mock(HugeClient.class);
        ComputerManager computer = Mockito.mock(ComputerManager.class);
        Mockito.when(client.computer()).thenReturn(computer);
        Mockito.when(computer.list(1L)).thenReturn(Collections.emptyList());

        Assert.assertTrue(new OlapAlgoService().computerAvailable(client));

        Mockito.when(computer.list(1L))
               .thenThrow(new RuntimeException("request_failure"));
        Assert.assertFalse(new OlapAlgoService().computerAvailable(client));
    }

    @Test
    public void testMissingComputerErrorExplainsRequiredEnvironment() {
        HugeClient client = Mockito.mock(HugeClient.class);
        ComputerManager computer = Mockito.mock(ComputerManager.class);
        Mockito.when(client.computer()).thenReturn(computer);
        Mockito.when(computer.create(
                Mockito.anyString(), Mockito.anyLong(), Mockito.anyMap()))
               .thenThrow(new RuntimeException((String) null));

        Throwable error = Assert.assertThrows(
                ServerCapabilityUnavailableException.class,
                () -> new OlapAlgoService().olapView(
                        client, "DEFAULT", entity()));

        Assert.assertEquals("server.capability.computer.unavailable",
                            error.getMessage());
    }

    @Test
    public void testMeaningfulComputerErrorRemainsVisible() {
        HugeClient client = Mockito.mock(HugeClient.class);
        ComputerManager computer = Mockito.mock(ComputerManager.class);
        Mockito.when(client.computer()).thenReturn(computer);
        RuntimeException failure = new RuntimeException("invalid worker");
        Mockito.when(computer.create(
                Mockito.anyString(), Mockito.anyLong(), Mockito.anyMap()))
               .thenThrow(failure);

        Throwable error = Assert.assertThrows(
                RuntimeException.class,
                () -> new OlapAlgoService().olapView(
                        client, "DEFAULT", entity()));

        Assert.assertSame(failure, error);
    }

    private static OlapEntity entity() {
        return OlapEntity.builder()
                         .algorithm("page-rank")
                         .worker(2L)
                         .params(Collections.emptyMap())
                         .build();
    }
}
