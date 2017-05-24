package com.baidu.hugegraph.structure.constant;

public enum Cardinality {

    SINGLE(1, "single"),

    LIST(2, "list"),
    
    SET(3, "set");

    // HugeKeys define
    private byte code = 0;
    private String name = null;

    private Cardinality(int code, String name) {
        assert code < 256;
        this.code = (byte) code;
        this.name = name;
    }

    public byte code() {
        return this.code;
    }

    public String string() {
        return this.name;
    }

}