package com.baidu.hugegraph.structure.constant;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Created by liningrui on 2017/5/19.
 */
public class EdgeLink {

    @JsonProperty
    private String source;
    @JsonProperty
    private String target;

    public static EdgeLink of(String source, String target) {
        return new EdgeLink(source, target);
    }

    public EdgeLink() {
        super();
    }

    public EdgeLink(String source, String target) {
        super();
        this.source = source;
        this.target = target;
    }

    public String source() {
        return source;
    }

    public String target() {
        return target;
    }

    @Override
    public String toString() {
        return String.format("source=%s, target=%s", this.source, this.target);
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof EdgeLink)) {
            return false;
        }

        EdgeLink other = (EdgeLink) obj;
        if (this.source().equals(other.source())
                && this.target().equals(other.target())) {
            return true;
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return this.source().hashCode() ^ this.target().hashCode();
    }

}
