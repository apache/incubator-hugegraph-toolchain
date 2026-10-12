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

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import org.apache.hugegraph.controller.auth.RoleController;
import org.apache.hugegraph.controller.space.GraphSpaceController;
import org.apache.hugegraph.service.space.GraphSpaceService;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;

import com.google.common.collect.ImmutableSet;

public class LegacyFacadeRemovalTest {

    @Test
    public void testLegacyDefaultFacadesAreRemoved() {
        List<String> violations = new ArrayList<>();
        this.findObsoleteRoutes(RoleController.class,
                                ImmutableSet.of("setdefaultrole",
                                                "deldefaultrole"),
                                violations);
        this.findObsoleteRoutes(GraphSpaceController.class,
                                ImmutableSet.of("setdefault", "getdefault"),
                                violations);

        Set<String> obsoleteServiceMethods =
                ImmutableSet.of("setdefault", "getdefault");
        Arrays.stream(GraphSpaceService.class.getMethods())
              .filter(method -> Modifier.isPublic(method.getModifiers()))
              .filter(method -> obsoleteServiceMethods.contains(method.getName()))
              .forEach(method -> violations.add(GraphSpaceService.class.getSimpleName() +
                                                 "#" + method.getName()));

        Assert.assertTrue("Obsolete default facades remain: " + violations,
                          violations.isEmpty());
    }

    private void findObsoleteRoutes(Class<?> controller,
                                    Set<String> obsoleteRoutes,
                                    List<String> violations) {
        for (Method method : controller.getDeclaredMethods()) {
            GetMapping get = method.getAnnotation(GetMapping.class);
            if (get != null) {
                this.findObsoleteValues(controller, method, get.value(),
                                        obsoleteRoutes, violations);
                this.findObsoleteValues(controller, method, get.path(),
                                        obsoleteRoutes, violations);
            }
            DeleteMapping delete = method.getAnnotation(DeleteMapping.class);
            if (delete != null) {
                this.findObsoleteValues(controller, method, delete.value(),
                                        obsoleteRoutes, violations);
                this.findObsoleteValues(controller, method, delete.path(),
                                        obsoleteRoutes, violations);
            }
        }
    }

    private void findObsoleteValues(Class<?> controller, Method method,
                                    String[] mappings,
                                    Set<String> obsoleteRoutes,
                                    List<String> violations) {
        Arrays.stream(mappings)
              .filter(mapping -> obsoleteRoutes.contains(routeName(mapping)))
              .forEach(mapping -> violations.add(controller.getSimpleName() +
                                                 "#" + method.getName() +
                                                 " -> " + mapping));
    }

    private static String routeName(String mapping) {
        int separator = mapping.lastIndexOf('/');
        return mapping.substring(separator + 1);
    }
}
