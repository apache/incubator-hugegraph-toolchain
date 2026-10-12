/*
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

package org.apache.hugegraph.service.auth;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.hugegraph.driver.AuthManager;
import org.apache.hugegraph.driver.GraphsManager;
import org.apache.hugegraph.driver.HugeClient;
import org.apache.hugegraph.structure.auth.Access;
import org.apache.hugegraph.structure.auth.Belong;
import org.apache.hugegraph.structure.auth.Group;
import org.apache.hugegraph.structure.auth.HugePermission;
import org.apache.hugegraph.structure.auth.Target;
import org.apache.hugegraph.structure.auth.User;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

public class StandaloneAccountPermissionServiceTest {

    @Test
    public void testAssignReadWriteUsesOneSharedRole() {
        HugeClient client = Mockito.mock(HugeClient.class);
        AuthManager auth = Mockito.mock(AuthManager.class);
        GraphsManager graphs = Mockito.mock(GraphsManager.class);
        Group group = group();
        Target target = target();
        User user = new User();
        user.setId("user-id");

        Mockito.when(client.auth()).thenReturn(auth);
        Mockito.when(client.graphs()).thenReturn(graphs);
        Mockito.when(auth.listGroups())
               .thenReturn(Collections.singletonList(group));
        Mockito.when(auth.listTargets())
               .thenReturn(Collections.singletonList(target));
        Mockito.when(graphs.listGraph())
               .thenReturn(Collections.singletonList("hugegraph"));
        Mockito.when(auth.listAccessesByGroup("group-id", -1))
               .thenReturn(accesses(group, target));
        Mockito.when(auth.listBelongsByUser("user-id", -1))
               .thenReturn(Collections.emptyList());

        new StandaloneAccountPermissionService()
                .assignReadWrite(client, user);

        Mockito.verify(auth, Mockito.never())
               .createAccess(Mockito.any(Access.class));
        ArgumentCaptor<Belong> belong =
                ArgumentCaptor.forClass(Belong.class);
        Mockito.verify(auth).createBelong(belong.capture());
        Assert.assertEquals("user-id", belong.getValue().user());
        Assert.assertEquals("group-id", belong.getValue().group());
    }

    @Test
    public void testGrantFailureRollsBackNewRole() {
        HugeClient client = Mockito.mock(HugeClient.class);
        AuthManager auth = Mockito.mock(AuthManager.class);
        GraphsManager graphs = Mockito.mock(GraphsManager.class);
        Group group = group();
        Target target = target();
        User user = new User();
        user.setId("user-id");

        Mockito.when(client.auth()).thenReturn(auth);
        Mockito.when(client.graphs()).thenReturn(graphs);
        Mockito.when(graphs.listGraph())
               .thenReturn(Collections.singletonList("hugegraph"));
        Mockito.when(auth.listGroups()).thenReturn(Collections.emptyList());
        Mockito.when(auth.listTargets()).thenReturn(Collections.emptyList());
        Mockito.when(auth.createGroup(Mockito.any(Group.class)))
               .thenReturn(group);
        Mockito.when(auth.createTarget(Mockito.any(Target.class)))
               .thenReturn(target);
        Mockito.when(auth.createAccess(Mockito.any(Access.class)))
               .thenAnswer(invocation -> {
                   Access access = invocation.getArgument(0);
                   access.setId("access-" + access.permission());
                   return access;
               });
        Mockito.when(auth.listBelongsByUser("user-id", -1))
               .thenReturn(Collections.emptyList());
        RuntimeException failure = new RuntimeException("grant failed");
        Mockito.when(auth.createBelong(Mockito.any(Belong.class)))
               .thenThrow(failure);

        try {
            new StandaloneAccountPermissionService()
                    .assignReadWrite(client, user);
            Assert.fail("Expected the standalone grant to fail");
        } catch (RuntimeException error) {
            Assert.assertSame(failure, error);
        }

        Mockito.verify(auth, Mockito.times(4))
               .deleteAccess(Mockito.any());
        ArgumentCaptor<Target> createdTarget =
                ArgumentCaptor.forClass(Target.class);
        Mockito.verify(auth).createTarget(createdTarget.capture());
        Assert.assertEquals("", createdTarget.getValue().url());
        Mockito.verify(auth).deleteTarget("target-id");
        Mockito.verify(auth).deleteGroup("group-id");
    }

    private static Group group() {
        Group group = new Group();
        group.setId("group-id");
        group.name("hubble_standalone_read_write");
        return group;
    }

    private static Target target() {
        Target target = new Target();
        target.setId("target-id");
        target.name("hubble_standalone_read_write");
        target.graph("*");
        Map<String, Object> resource = new HashMap<>();
        resource.put("type", "ALL");
        resource.put("label", "*");
        resource.put("properties", null);
        target.resources(Collections.singletonList(resource));
        return target;
    }

    private static List<Access> accesses(Group group, Target target) {
        return Arrays.stream(HugePermission.values())
                     .filter(permission -> permission == HugePermission.READ ||
                                           permission == HugePermission.WRITE ||
                                           permission == HugePermission.DELETE ||
                                           permission == HugePermission.EXECUTE)
                     .map(permission -> {
                         Access access = new Access();
                         access.group(group);
                         access.target(target);
                         access.permission(permission);
                         return access;
                     })
                     .collect(java.util.stream.Collectors.toList());
    }
}
