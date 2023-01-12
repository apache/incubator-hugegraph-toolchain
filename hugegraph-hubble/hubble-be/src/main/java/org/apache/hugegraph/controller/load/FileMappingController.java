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

package org.apache.hugegraph.controller.load;

import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import org.apache.hugegraph.common.Constant;
import org.apache.hugegraph.controller.BaseController;
import org.apache.hugegraph.entity.enums.JobStatus;
import org.apache.hugegraph.entity.load.EdgeMapping;
import org.apache.hugegraph.entity.load.ElementMapping;
import org.apache.hugegraph.entity.load.FileMapping;
import org.apache.hugegraph.entity.load.FileSetting;
import org.apache.hugegraph.entity.load.JobManager;
import org.apache.hugegraph.entity.load.LoadParameter;
import org.apache.hugegraph.entity.load.VertexMapping;
import org.apache.hugegraph.entity.schema.EdgeLabelEntity;
import org.apache.hugegraph.entity.schema.VertexLabelEntity;
import org.apache.hugegraph.exception.ExternalException;
import org.apache.hugegraph.service.load.FileMappingService;
import org.apache.hugegraph.service.load.JobManagerService;
import org.apache.hugegraph.service.schema.EdgeLabelService;
import org.apache.hugegraph.service.schema.VertexLabelService;
import org.apache.hugegraph.util.CollectionUtil;
import org.apache.hugegraph.util.Ex;
import org.apache.hugegraph.util.HubbleUtil;
import com.baomidou.mybatisplus.core.metadata.IPage;

import lombok.extern.log4j.Log4j2;

@Log4j2
@RestController
@RequestMapping(Constant.API_VERSION +
                "graph-connections/{connId}/job-manager/{jobId}/file-mappings")
public class FileMappingController extends BaseController {

    @Autowired
    private VertexLabelService vlService;
    @Autowired
    private EdgeLabelService elService;
    @Autowired
    private FileMappingService service;
    @Autowired
    private JobManagerService jobService;

    @GetMapping
    public IPage<FileMapping> list(@PathVariable("connId") int connId,
                                   @PathVariable("jobId") int jobId,
                                   @RequestParam(name = "page_no",
                                                 required = false,
                                                 defaultValue = "1")
                                   int pageNo,
                                   @RequestParam(name = "page_size",
                                                 required = false,
                                                 defaultValue = "10")
                                   int pageSize) {
        return this.service.list(connId, jobId, pageNo, pageSize);
    }

    @GetMapping("{id}")
    public FileMapping get(@PathVariable("id") int id) {
        FileMapping mapping = this.service.get(id);
        if (mapping == null) {
            throw new ExternalException("load.file-mapping.not-exist.id", id);
        }
        return mapping;
    }

    @DeleteMapping("{id}")
    public void delete(@PathVariable("id") int id) {
        FileMapping mapping = this.service.get(id);
        if (mapping == null) {
            throw new ExternalException("load.file-mapping.not-exist.id", id);
        }

        this.service.deleteDiskFile(mapping);
        this.service.remove(id);
    }

    @DeleteMapping
    public void clear() {
        List<FileMapping> mappings = this.service.listAll();
        for (FileMapping mapping : mappings) {
            this.service.remove(mapping.getId());
        }
    }

    @PostMapping("{id}/file-setting")
    public FileMapping fileSetting(@PathVariable("id") int id,
                                   @RequestBody FileSetting newEntity) {
        Ex.check(!StringUtils.isEmpty(newEntity.getDelimiter()),
                 "load.file-mapping.file-setting.delimiter-cannot-be-empty");
        Ex.check(!StringUtils.isEmpty(newEntity.getCharset()),
                 "load.file-mapping.file-setting.charset-cannot-be-empty");
        Ex.check(!StringUtils.isEmpty(newEntity.getDateFormat()),
                 "load.file-mapping.file-setting.dateformat-cannot-be-empty");
        Ex.check(!StringUtils.isEmpty(newEntity.getTimeZone()),
                 "load.file-mapping.file-setting.timezone-cannot-be-empty");
        Ex.check(!StringUtils.isEmpty(newEntity.getSkippedLine()),
                 "load.file-mapping.file-setting.skippedline-cannot-be-empty");

        FileMapping mapping = this.service.get(id);
        if (mapping == null) {
            throw new ExternalException("load.file-mapping.not-exist.id", id);
        }
        // Change format to TEXT if needed
        newEntity.changeFormatIfNeeded();
        FileSetting oldEntity = mapping.getFileSetting();
        FileSetting entity = this.mergeEntity(oldEntity, newEntity);
        mapping.setFileSetting(entity);
        // Read column names and values then fill it
        this.service.extractColumns(mapping);
        this.service.update(mapping);
        return mapping;
    }

    @PostMapping("{id}/vertex-mappings")
    public FileMapping addVertexMapping(@PathVariable("connId") int connId,
                                        @PathVariable("id") int id,
                                        @RequestBody VertexMapping newEntity) {
        FileMapping mapping = this.service.get(id);
        if (mapping == null) {
            throw new ExternalException("load.file-mapping.not-exist.id", id);
        }
        this.checkVertexMappingValid(connId, newEntity, mapping);

        newEntity.setId(HubbleUtil.generateSimpleId());
        mapping.getVertexMappings().add(newEntity);
        this.service.update(mapping);
        return mapping;
    }

    @PutMapping("{id}/vertex-mappings/{vmid}")
    public FileMapping updateVertexMapping(@PathVariable("connId") int connId,
                                           @PathVariable("id") int id,
                                           @PathVariable("vmid") String vmId,
                                           @RequestBody VertexMapping newEntity) {
        FileMapping mapping = this.service.get(id);
        if (mapping == null) {
            throw new ExternalException("load.file-mapping.not-exist.id", id);
        }
        this.checkVertexMappingValid(connId, newEntity, mapping);

        VertexMapping vertexMapping = mapping.getVertexMapping(vmId);
        Ex.check(vertexMapping != null,
                 "load.file-mapping.vertex-mapping.not-exist.id", vmId);

        newEntity.setId(vmId);
        Set<VertexMapping> vertexMappings = mapping.getVertexMappings();
        vertexMappings.remove(vertexMapping);
        vertexMappings.add(newEntity);
        this.service.update(mapping);
        return mapping;
    }

    @DeleteMapping("{id}/vertex-mappings/{vmid}")
    public FileMapping deleteVertexMapping(@PathVariable("id") int id,
                                           @PathVariable("vmid") String vmid) {
        FileMapping mapping = this.service.get(id);
        if (mapping == null) {
            throw new ExternalException("load.file-mapping.not-exist.id", id);
        }

        VertexMapping vertexMapping = mapping.getVertexMapping(vmid);
        boolean removed = mapping.getVertexMappings().remove(vertexMapping);
        if (!removed) {
            throw new ExternalException(
                      "load.file-mapping.vertex-mapping.not-exist.id", vmid);
        }
        this.service.update(mapping);
        return mapping;
    }

    @PostMapping("{id}/edge-mappings")
    public FileMapping addEdgeMapping(@PathVariable("connId") int connId,
                                      @PathVariable("id") int id,
                                      @RequestBody EdgeMapping newEntity) {
        FileMapping mapping = this.service.get(id);
        if (mapping == null) {
            throw new ExternalException("load.file-mapping.not-exist.id", id);
        }
        this.checkEdgeMappingValid(connId, newEntity, mapping);

        newEntity.setId(HubbleUtil.generateSimpleId());
        mapping.getEdgeMappings().add(newEntity);
        this.service.update(mapping);
        return mapping;
    }

    @PutMapping("{id}/edge-mappings/{emid}")
    public FileMapping updateEdgeMapping(@PathVariable("connId") int connId,
                                         @PathVariable("id") int id,
                                         @PathVariable("emid") String emId,
                                         @RequestBody EdgeMapping newEntity) {
        FileMapping mapping = this.service.get(id);
        if (mapping == null) {
            throw new ExternalException("load.file-mapping.not-exist.id", id);
        }
        this.checkEdgeMappingValid(connId, newEntity, mapping);

        EdgeMapping edgeMapping = mapping.getEdgeMapping(emId);
        Ex.check(edgeMapping != null,
                 "load.file-mapping.edge-mapping.not-exist.id", emId);

        newEntity.setId(emId);
        Set<EdgeMapping> edgeMappings = mapping.getEdgeMappings();
        edgeMappings.remove(edgeMapping);
        edgeMappings.add(newEntity);
        this.service.update(mapping);
        return mapping;
    }

    @DeleteMapping("{id}/edge-mappings/{emid}")
    public FileMapping deleteEdgeMapping(@PathVariable("id") int id,
                                         @PathVariable("emid") String emid) {
        FileMapping mapping = this.service.get(id);
        if (mapping == null) {
            throw new ExternalException("load.file-mapping.not-exist.id", id);
        }

        EdgeMapping edgeMapping = mapping.getEdgeMapping(emid);
        boolean removed = mapping.getEdgeMappings().remove(edgeMapping);
        if (!removed) {
            throw new ExternalException(
                      "load.file-mapping.edge-mapping.not-exist.id", emid);
        }
        this.service.update(mapping);
        return mapping;
    }

    /**
     * TODO: All file mapping share one load paramter now, should be separated
     *  in actually
     */
    @PostMapping("load-parameter")
    public void loadParameter(@RequestBody LoadParameter newEntity) {
        this.checkLoadParameter(newEntity);
        List<FileMapping> mappings = this.service.listAll();
        for (FileMapping mapping : mappings) {
            LoadParameter oldEntity = mapping.getLoadParameter();
            LoadParameter entity = this.mergeEntity(oldEntity, newEntity);
            mapping.setLoadParameter(entity);
            this.service.update(mapping);
        }
    }

    @PutMapping("next-step")
    public JobManager nextStep(@PathVariable("jobId") int jobId) {
        JobManager jobEntity = this.jobService.get(jobId);
        Ex.check(jobEntity != null, "job-manager.not-exist.id", jobId);
        Ex.check(jobEntity.getJobStatus() == JobStatus.MAPPING,
                 "job.manager.status.unexpected",
                 JobStatus.MAPPING, jobEntity.getJobStatus());
        jobEntity.setJobStatus(JobStatus.SETTING);
        this.jobService.update(jobEntity);
        return jobEntity;
    }

    private void checkVertexMappingValid(int connId, VertexMapping vertexMapping,
                                         FileMapping fileMapping) {
        VertexLabelEntity vl = this.vlService.get(vertexMapping.getLabel(),
                                                  connId);
        Ex.check(!vl.getIdStrategy().isAutomatic(),
                 "load.file-mapping.vertex.automatic-id-unsupported");

        Ex.check(!CollectionUtils.isEmpty(vertexMapping.getIdFields()),
                 "load.file-mapping.vertex.id-fields-cannot-be-empty");
        FileSetting fileSetting = fileMapping.getFileSetting();
        List<String> columnNames = fileSetting.getColumnNames();
        Ex.check(columnNames.containsAll(vertexMapping.getIdFields()),
                 "load.file-mapping.vertex.id-fields-should-in-column-names",
                 vertexMapping.getIdFields(), columnNames);
        if (vl.getIdStrategy().isPrimaryKey()) {
            Ex.check(vertexMapping.getIdFields().size() ==
                     vl.getPrimaryKeys().size(),
                     "load.file-mapping.vertex.id-fields-should-same-size-pks");
            Ex.check(!CollectionUtils.containsAny(
                     vertexMapping.fieldMappingToMap().values(),
                     vl.getPrimaryKeys()),
                     "load.file-mapping.vertex.mapping-fields-cannot-contains-pk");
        } else {
            Ex.check(vertexMapping.getIdFields().size() == 1,
                     "load.file-mapping.vertex.id-fields-should-only-one");
        }
        Ex.check(CollectionUtil.allUnique(
                 vertexMapping.fieldMappingToMap().values()),
                 "load.file-mapping.mapping-fields-should-no-duplicate");
        this.checkMappingValid(vertexMapping, fileMapping);
    }

    private void checkEdgeMappingValid(int connId, EdgeMapping edgeMapping,
                                       FileMapping fileMapping) {
        EdgeLabelEntity el = this.elService.get(edgeMapping.getLabel(), connId);
        VertexLabelEntity source = this.vlService.get(el.getSourceLabel(), connId);
        VertexLabelEntity target = this.vlService.get(el.getTargetLabel(), connId);
        Ex.check(!source.getIdStrategy().isAutomatic(),
                 "load.file-mapping.vertex.automatic-id-unsupported");
        Ex.check(!target.getIdStrategy().isAutomatic(),
                 "load.file-mapping.vertex.automatic-id-unsupported");

        Ex.check(!CollectionUtils.isEmpty(edgeMapping.getSourceFields()),
                 "load.file-mapping.edge.source-fields-cannot-be-empty");
        Ex.check(!CollectionUtils.isEmpty(edgeMapping.getTargetFields()),
                 "load.file-mapping.edge.target-fields-cannot-be-empty");
        FileSetting fileSetting = fileMapping.getFileSetting();
        List<String> columnNames = fileSetting.getColumnNames();
        Ex.check(columnNames.containsAll(edgeMapping.getSourceFields()),
                 "load.file-mapping.edge.source-fields-should-in-column-names",
                 edgeMapping.getSourceFields(), columnNames);
        Ex.check(columnNames.containsAll(edgeMapping.getTargetFields()),
                 "load.file-mapping.edge.target-fields-should-in-column-names",
                 edgeMapping.getTargetFields(), columnNames);
        Ex.check(CollectionUtil.allUnique(
                 edgeMapping.fieldMappingToMap().values()),
                 "load.file-mapping.mapping-fields-should-no-duplicate");
        this.checkMappingValid(edgeMapping, fileMapping);
    }

    private void checkMappingValid(ElementMapping elementMapping,
                                   FileMapping fileMapping) {
        FileSetting fileSetting = fileMapping.getFileSetting();
        List<String> columnNames = fileSetting.getColumnNames();
        if (elementMapping.getFieldMappings() != null) {
            Set<String> keys = elementMapping.fieldMappingToMap().keySet();
            Ex.check(columnNames.containsAll(keys),
                     "load.file-mapping.field.keys-should-in-column-names");
        }
        if (elementMapping.getValueMappings() != null) {
            Set<String> keys = elementMapping.valueMappingToMap().keySet();
            Ex.check(columnNames.containsAll(keys),
                     "load.file-mapping.value.keys-should-in-column-names");
        }
    }

    private void checkLoadParameter(LoadParameter newEntity) {
        Ex.check(newEntity.getMaxParseErrors() == Constant.NO_LIMIT ||
                 newEntity.getMaxParseErrors() > 0,
                 "load.file-mapping.load-parameter.max-parse-error.illegal");
        Ex.check(newEntity.getMaxInsertErrors() == Constant.NO_LIMIT ||
                 newEntity.getMaxInsertErrors() > 0,
                 "load.file-mapping.load-parameter.max-insert-error.illegal");
        Ex.check(newEntity.getInsertTimeout() > 0,
                 "load.file-mapping.load-parameter.insert-timeout.illegal");
        Ex.check(newEntity.getRetryTimes() > 0,
                 "load.file-mapping.load-parameter.retry-times.illegal");
        Ex.check(newEntity.getRetryInterval() > 0,
                 "load.file-mapping.load-parameter.retry-interval.illegal");
    }
}
