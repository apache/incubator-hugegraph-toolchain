package org.apache.hugegraph.entity.space;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ComputerServiceEntity {
    @JsonProperty("id")
    public long id;
    @JsonProperty("task_type")
    public String type;
    @JsonProperty("task_name")
    public String name;
    @JsonProperty("task_status")
    public String status;
    @JsonProperty("task_progress")
    public long progress;
    @JsonProperty("task_algorithm")
    public String algorithm;
    @JsonProperty("task_description")
    public String description;
    @JsonProperty("task_create")
    public long create;
    @JsonProperty("task_input")
    public String input;
}
