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

package org.apache.hugegraph.mapper.load;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.springframework.stereotype.Component;

import org.apache.hugegraph.entity.load.JobManager;
import org.apache.hugegraph.entity.load.JobManagerItem;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;

@Mapper
@Component
public interface JobManagerMapper extends BaseMapper<JobManager> {

    @Select("SELECT ISNULL(SUM(f.total_size),0) as total_size, " +
            "ISNULL(SUM(l.duration),0) as duration " +
            "FROM `load_task` as l LEFT JOIN `file_mapping` as f " +
            "ON l.file_id=f.id WHERE l.job_id = #{job_id}")
    JobManagerItem computeSizeDuration(@Param("job_id") int jobId);
}
