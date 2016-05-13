package com.jxs.ld.bean;

import org.apache.commons.lang3.StringUtils;

import java.lang.reflect.Field;

/**
 * 包装实体与表相关的信息。
 *
 * @author jiangxingshang
 * @date 15/11/26
 */
public class BeanInfo {

    private String tableName;
    private Field primary;
    private String primaryColumn;
    private IdGenerator idGenerator;

    /**
     * <p>Constructor for BeanInfo.</p>
     *
     * @param beanClass a {@link java.lang.Class} object.
     */
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

    /**
     * <p>Getter for the field <code>tableName</code>.</p>
     *
     * @return a {@link java.lang.String} object.
     */
    public String getTableName() {
        return tableName;
    }

    /**
     * <p>Getter for the field <code>primary</code>.</p>
     *
     * @return a {@link java.lang.reflect.Field} object.
     */
    public Field getPrimary() {
        return primary;
    }

    /**
     * <p>Getter for the field <code>primaryColumn</code>.</p>
     *
     * @return a {@link java.lang.String} object.
     */
    public String getPrimaryColumn() {
        return primaryColumn;
    }

    /**
     * <p>Getter for the field <code>idGenerator</code>.</p>
     *
     * @return a {@link com.jxs.ld.bean.IdGenerator} object.
     */
    public IdGenerator getIdGenerator() {
        return idGenerator;
    }
}
