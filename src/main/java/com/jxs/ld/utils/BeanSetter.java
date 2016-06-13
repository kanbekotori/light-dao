package com.jxs.ld.utils;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * @author jiangxingshang
 * @date 16/6/12
 */
public interface BeanSetter<T> {
    void bean(T bean, ResultSet rs, int rowNum) throws SQLException;
}
