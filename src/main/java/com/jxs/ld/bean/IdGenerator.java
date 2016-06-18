package com.jxs.ld.bean;

public enum IdGenerator {

    UUID(0),
    AUTO_INCREMENT(1),
    ASSIGNED(2);

    private int value;
    private IdGenerator(int v) {
        value = v;
    }
}
