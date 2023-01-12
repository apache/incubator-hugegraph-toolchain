SELECT 1;
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

-- INSERT INTO `graph_connection`(name, graph, host, port, timeout, create_time) VALUES ('s', 'hugegraph', 'localhost', 8080, 60, sysdate);

-- INSERT INTO `gremlin_collection`(name, content, create_time) VALUES ('first_gremlin', 'g.V().limit(10)', sysdate);

-- INSERT INTO `execute_history`(execute_type, content, execute_status, duration, create_time) VALUES (0, 'g.V().limit(100)', 0, 20, sysdate);
