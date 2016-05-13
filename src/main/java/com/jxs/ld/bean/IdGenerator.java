package com.jxs.ld.bean;

/**
 * @author jiangxingshang
 * @date 15/11/24
 */
public enum IdGenerator {

    UUID(0),
    AUTO_INCREMENT(1),
    /**已分配，开发员自己设置ID*/
    ASSIGNED(2);

    private int value;
    private IdGenerator(int v) {
        value = v;
    }
}
