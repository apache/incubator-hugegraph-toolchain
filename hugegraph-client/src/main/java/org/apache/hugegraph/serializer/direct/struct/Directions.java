package org.apache.hugegraph.serializer.direct.struct;

public enum Directions  {

    // TODO: add NONE enum for non-directional edges

    BOTH(0, "both"),

    OUT(1, "out"),

    IN(2, "in");
    private byte code = 0;
    private String name = null;

    Directions(int code, String name) {
        assert code < 256;
        this.code = (byte) code;
        this.name = name;
    }
}
