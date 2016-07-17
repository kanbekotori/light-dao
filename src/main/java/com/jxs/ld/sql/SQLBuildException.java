package com.jxs.ld.sql;

/**
 * @author jiangxingshang
 * @since 16/7/17
 */
@SuppressWarnings("serial")
public class SQLBuildException extends RuntimeException {

    public SQLBuildException(String msg) {
        super(msg);
    }

    public SQLBuildException(String msg, Throwable e) {
        super(msg, e);
    }
}
