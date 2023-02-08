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

package org.apache.hugegraph.api;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.hugegraph.api.gremlin.GremlinRequest;
import org.apache.hugegraph.api.task.TasksWithPage;
import org.apache.hugegraph.structure.Task;
import org.apache.hugegraph.structure.gremlin.ResultSet;
import org.apache.hugegraph.structure.schema.IndexLabel;
import org.apache.hugegraph.testutil.Assert;
import org.apache.hugegraph.testutil.Utils;
import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;

public class TaskApiTest extends BaseApiTest {

    @BeforeClass
    public static void prepareSchema() {
        BaseApiTest.initPropertyKey();
        BaseApiTest.initVertexLabel();
        BaseApiTest.initEdgeLabel();
        BaseApiTest.initIndexLabel();
    }

    @After
    public void teardown() throws Exception {
        taskAPI.list(null, -1).forEach(task -> taskAPI.delete(task.id()));
    }

    @Test
    public void testListAll() {
        IndexLabel personByCity = schema().getIndexLabel("personByCity");
        IndexLabel personByAge = schema().getIndexLabel("personByAge");
        IndexLabel knowsByDate = schema().getIndexLabel("knowsByDate");
        IndexLabel createdByDate = schema().getIndexLabel("createdByDate");

        Set<Long> taskIds = new HashSet<>();
        taskIds.add(rebuildAPI.rebuild(personByCity));
        taskIds.add(rebuildAPI.rebuild(personByAge));
        taskIds.add(rebuildAPI.rebuild(knowsByDate));
        taskIds.add(rebuildAPI.rebuild(createdByDate));

        List<Task> tasks = taskAPI.list(null, -1);

        Assert.assertEquals(4, tasks.size());

        Set<Long> listedTaskIds = new HashSet<>();
        for (Task task : tasks) {
            listedTaskIds.add(task.id());
        }
        Assert.assertEquals(taskIds.size(), listedTaskIds.size());
        Assert.assertTrue(taskIds.containsAll(listedTaskIds));

        taskIds.forEach(BaseApiTest::waitUntilTaskCompleted);

        taskIds.forEach(id -> taskAPI.delete(id));

        tasks = taskAPI.list(null, -1);
        Assert.assertEquals(0, tasks.size());
    }

    @Test
    public void testListByIds() {
        IndexLabel personByCity = schema().getIndexLabel("personByCity");
        IndexLabel personByAge = schema().getIndexLabel("personByAge");
        IndexLabel knowsByDate = schema().getIndexLabel("knowsByDate");
        IndexLabel createdByDate = schema().getIndexLabel("createdByDate");

        List<Long> taskIds = new ArrayList<>();
        taskIds.add(rebuildAPI.rebuild(personByCity));
        taskIds.add(rebuildAPI.rebuild(personByAge));
        taskIds.add(rebuildAPI.rebuild(knowsByDate));
        taskIds.add(rebuildAPI.rebuild(createdByDate));

        taskIds.forEach(BaseApiTest::waitUntilTaskCompleted);

        List<Task> tasks = taskAPI.list(taskIds);

        Assert.assertEquals(4, tasks.size());
        Set<Long> listedTaskIds = new HashSet<>();
        for (Task task : tasks) {
            listedTaskIds.add(task.id());
        }
        Assert.assertEquals(taskIds.size(), listedTaskIds.size());
        Assert.assertTrue(taskIds.containsAll(listedTaskIds));
    }

    @Test
    public void testListByStatus() {
        IndexLabel personByCity = schema().getIndexLabel("personByCity");
        IndexLabel personByAge = schema().getIndexLabel("personByAge");
        IndexLabel knowsByDate = schema().getIndexLabel("knowsByDate");
        IndexLabel createdByDate = schema().getIndexLabel("createdByDate");

        Set<Long> taskIds = new HashSet<>();
        taskIds.add(rebuildAPI.rebuild(personByCity));
        taskIds.add(rebuildAPI.rebuild(personByAge));
        taskIds.add(rebuildAPI.rebuild(knowsByDate));
        taskIds.add(rebuildAPI.rebuild(createdByDate));

        taskIds.forEach(BaseApiTest::waitUntilTaskCompleted);

        List<Task> tasks = taskAPI.list("SUCCESS", -1);

        Assert.assertEquals(4, tasks.size());
        Set<Long> listedTaskIds = new HashSet<>();
        for (Task task : tasks) {
            listedTaskIds.add(task.id());
        }
        Assert.assertEquals(taskIds.size(), listedTaskIds.size());
        Assert.assertTrue(taskIds.containsAll(listedTaskIds));

        tasks = taskAPI.list("SUCCESS", 3);

        Assert.assertEquals(3, tasks.size());
        Set<Long> listedTaskIds1 = new HashSet<>();
        for (Task task : tasks) {
            listedTaskIds1.add(task.id());
        }
        Assert.assertEquals(3, listedTaskIds1.size());
        Assert.assertTrue(taskIds.containsAll(listedTaskIds));
    }

    @Test
    public void testListByStatusAndPage() {
        IndexLabel personByCity = schema().getIndexLabel("personByCity");
        IndexLabel personByAge = schema().getIndexLabel("personByAge");
        IndexLabel knowsByDate = schema().getIndexLabel("knowsByDate");
        IndexLabel createdByDate = schema().getIndexLabel("createdByDate");

        Set<Long> taskIds = new HashSet<>();
        taskIds.add(rebuildAPI.rebuild(personByCity));
        taskIds.add(rebuildAPI.rebuild(personByAge));
        taskIds.add(rebuildAPI.rebuild(knowsByDate));
        taskIds.add(rebuildAPI.rebuild(createdByDate));

        taskIds.forEach(BaseApiTest::waitUntilTaskCompleted);

        TasksWithPage tasksWithPage = taskAPI.list("SUCCESS", "", 2);

        List<Task> tasks = tasksWithPage.tasks();
        Assert.assertEquals(2, tasks.size());
        Set<Long> listedTaskIds = new HashSet<>();
        for (Task task : tasks) {
            listedTaskIds.add(task.id());
        }

        tasksWithPage = taskAPI.list("SUCCESS", tasksWithPage.page(), 2);

        List<Task> tasks1 = tasksWithPage.tasks();
        Assert.assertEquals(2, tasks1.size());
        Assert.assertNull(tasksWithPage.page());
        for (Task task : tasks1) {
            listedTaskIds.add(task.id());
        }
        Assert.assertEquals(taskIds.size(), listedTaskIds.size());
        Assert.assertTrue(taskIds.containsAll(listedTaskIds));
    }

    @Test
    public void testGet() {
        IndexLabel personByCity = schema().getIndexLabel("personByCity");
        long taskId = rebuildAPI.rebuild(personByCity);

        Task task = taskAPI.get(taskId);

        Assert.assertNotNull(task);
        Assert.assertEquals(taskId, task.id());
        waitUntilTaskCompleted(taskId);
    }

    @Test
    public void testDelete() {
        IndexLabel personByCity = schema().getIndexLabel("personByCity");
        long taskId = rebuildAPI.rebuild(personByCity);

        waitUntilTaskCompleted(taskId);
        taskAPI.delete(taskId);

        Utils.assertResponseError(404, () -> {
            taskAPI.get(taskId);
        });
    }

    @Test
    public void testCancel() {
        schema().vertexLabel("man").useAutomaticId().ifNotExist().create();

        String groovy = "for (int i = 0; i < 10; i++) {" +
                        "hugegraph.addVertex(T.label, 'man');" +
                        "hugegraph.tx().commit();" +
                        "}";
        // Insert 10 records in sync mode
        GremlinRequest request = new GremlinRequest(groovy);
        gremlin().execute(request);
        // Verify insertion takes effect
        groovy = "g.V()";
        request = new GremlinRequest(groovy);
        ResultSet resultSet = gremlin().execute(request);
        Assert.assertEquals(10, resultSet.size());
        // Delete to prepare for insertion in async mode
        groovy = "g.V().drop()";
        request = new GremlinRequest(groovy);
        gremlin().execute(request);

        /*
         * The asyn task scripts need to be able to handle interrupts,
         * otherwise they cannot be cancelled
         */
        groovy = "for (int i = 0; i < 10; i++) {" +
                 "    hugegraph.addVertex(T.label, 'man');" +
                 "    hugegraph.tx().commit();" +
                 "    try {" +
                 "        sleep(1000);" +
                 "    } catch (InterruptedException e) {" +
                 "        break;" +
                 "    }" +
                 "}";
        request = new GremlinRequest(groovy);
        long taskId = gremlin().executeAsTask(request);

        groovy = "g.V()";
        request = new GremlinRequest(groovy);
        // Wait async task running
        while (true) {
            resultSet = gremlin().execute(request);
            if (resultSet.size() > 0) {
                break;
            } else {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException ignored) {
                }
            }
        }
        // Cancel async task
        Task task = taskAPI.cancel(taskId);
        Assert.assertTrue(task.cancelling());

        try {
            Thread.sleep(1000L);
        } catch (InterruptedException e) {
            // ignored
        }

        task = taskAPI.get(taskId);
        Assert.assertTrue(task.cancelled());

        resultSet = gremlin().execute(request);
        Assert.assertTrue(resultSet.size() < 10);
    }

    @Test
    public void testTaskAsMap() {
        IndexLabel personByCity = schema().getIndexLabel("personByCity");
        long taskId = rebuildAPI.rebuild(personByCity);

        Task task = taskAPI.get(taskId);

        Assert.assertNotNull(task);
        Assert.assertEquals(taskId, task.id());
        waitUntilTaskCompleted(taskId);

        task = taskAPI.get(taskId);
        Map<String, Object> taskMap = task.asMap();
        Assert.assertEquals("rebuild_index", taskMap.get(Task.P.TYPE));
        Assert.assertEquals("success", taskMap.get(Task.P.STATUS));
    }
}
