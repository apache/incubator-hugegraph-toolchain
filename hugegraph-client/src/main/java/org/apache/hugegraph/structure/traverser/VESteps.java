package org.apache.hugegraph.structure.traverser;

import java.util.ArrayList;
import java.util.List;

import org.apache.hugegraph.api.API;
import org.apache.hugegraph.api.traverser.TraversersAPI;
import org.apache.hugegraph.structure.constant.Direction;
import org.apache.hugegraph.structure.constant.Traverser;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;

public class VESteps {
    @JsonProperty("direction")
    public Direction direction;
    @JsonAlias("degree")
    @JsonProperty("max_degree")
    public long maxDegree;
    @JsonProperty("skip_degree")
    public long skipDegree;
    @JsonProperty("vertex_steps")
    public List<VEStepEntity> vSteps;
    @JsonProperty("edge_steps")
    public List<VEStepEntity> eSteps;

    protected VESteps() {
        this.direction = Direction.BOTH;
        this.maxDegree = Traverser.DEFAULT_MAX_DEGREE;
        this.skipDegree = Traverser.DEFAULT_SKIP_DEGREE;
        this.vSteps = new ArrayList<>();
        this.eSteps = new ArrayList<>();
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public String toString() {
        return String.format("Steps{direction=%s,maxDegree=%s," +
                             "skipDegree=%s,vSteps=%s,eSteps=%s}",
                             this.direction, this.maxDegree,
                             this.skipDegree, this.vSteps, this.eSteps);
    }

    public static class Builder {
        protected VESteps steps;

        private Builder() {
            this.steps = new VESteps();
        }

        public VESteps.Builder direction(Direction direction) {
            this.steps.direction = direction;
            return this;
        }

        public VESteps.Builder vSteps(List<VEStepEntity> vSteps) {
            this.steps.vSteps = vSteps;
            return this;
        }

        public VESteps.Builder vSteps(VEStepEntity vStepEntity) {
            this.steps.vSteps.add(vStepEntity);
            return this;
        }

        public VESteps.Builder eSteps(List<VEStepEntity> eSteps) {
            this.steps.eSteps = eSteps;
            return this;
        }

        public VESteps.Builder eSteps(VEStepEntity eStepEntity) {
            this.steps.eSteps.add(eStepEntity);
            return this;
        }

        public VESteps.Builder degree(long degree) {
            TraversersAPI.checkDegree(degree);
            this.steps.maxDegree = degree;
            return this;
        }

        public VESteps.Builder skipDegree(long skipDegree) {
            TraversersAPI.checkSkipDegree(skipDegree, this.steps.maxDegree,
                                          API.NO_LIMIT);
            this.steps.skipDegree = skipDegree;
            return this;
        }

        public VESteps build() {
            TraversersAPI.checkDegree(this.steps.maxDegree);
            TraversersAPI.checkSkipDegree(this.steps.skipDegree,
                                          this.steps.maxDegree, API.NO_LIMIT);
            return this.steps;
        }
    }

}