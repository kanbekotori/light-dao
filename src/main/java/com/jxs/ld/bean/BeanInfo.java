package com.jxs.ld.bean;

import org.apache.commons.lang3.StringUtils;

import java.lang.reflect.Field;

/**
 * 包装实体与表相关的信息。
 * Created by jiangxingshang on 15/11/26.
 */
public class BeanInfo {

    private String tableName;
    private Field primary;
    private String primaryColumn;
    private IdGenerator idGenerator;

    public BeanInfo(Class<?> beanClass) {
        tableName = Beans.getTable(beanClass);
        if(StringUtils.isBlank(tableName)) {
            throw new RuntimeException("Table name not found on " + beanClass.getName());
        }
        primary = Beans.getPrimaryField(beanClass);
        if(primary == null) {
            throw new RuntimeException("Primary key not found on " + beanClass.getName());
        }
        primaryColumn = Beans.getPrimaryColumn(primary);
        idGenerator = Beans.getIdGenerator(beanClass);
    }

    public String getTableName() {
        return tableName;
    }

    public Field getPrimary() {
        return primary;
    }

    public String getPrimaryColumn() {
        return primaryColumn;
    }

    public IdGenerator getIdGenerator() {
        return idGenerator;
    }
}
