package com.baidu.hugegraph.structure.constant;

public enum DataType {

    OBJECT(1, "object"),
    BOOLEAN(2, "boolean"),
    BYTE(3, "byte"),
    BLOB(4, "blob"),
    DOUBLE(5, "double"),
    FLOAT(6, "float"),
    INT(7, "int"),
    LONG(8, "long"),
    TEXT(9, "text"),
    TIMESTAMP(10, "timestamp"),
    UUID(11, "uuid");

    private byte code = 0;
    private String name = null;

    private DataType(int code, String name) {
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
