package com.jxs.ld.bean;

import org.apache.commons.lang3.StringUtils;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

/**
 * 封装实体与表结构有关的信息，以及被{@link com.jxs.ld.BaseDao}处理的信息。
 * @author jiangxingshang
 */
public class BeanInfo {

    private String tableName;
    private Field primary;
    private String primaryColumn;
    private IdGenerator idGenerator;
    private int uuidLength;
    //实体属性到表字段的映射关系，key是实体属性，value是表字段。
    private Map<String, String> propertiesMapper;
    //实体属性的忽略设置。
    private Map<String, IgnoreColumn> propertiesIgnoreInfoMapper = new HashMap<>();

    public BeanInfo(Class<?> beanClass) {
        tableName = Beans.getTable(beanClass);
        if(StringUtils.isBlank(tableName)) {
            throw new RuntimeException("Table name not found on " + beanClass.getName());
        }
        primary = Beans.getPrimaryField(beanClass);
        if(primary == null) {
            throw new RuntimeException("Primary key not found on " + beanClass.getName());
        }
        uuidLength = primary.getAnnotation(Column.class).UUIDLength();
        primaryColumn = Beans.getPrimaryColumn(primary);
        idGenerator = Beans.getIdGenerator(beanClass);
        propertiesMapper = Beans.getMapper(beanClass);
        for(Field f : Beans.getFields(beanClass)) {
            propertiesIgnoreInfoMapper.put(f.getName(), f.getAnnotation(IgnoreColumn.class));
        }
    }

    public String getTableName() {
        return tableName;
    }

    /**
     * @return 主键的实体属性。
     */
    public Field getPrimary() {
        return primary;
    }

    /**
     * @return 主键字段。
     */
    public String getPrimaryColumn() {
        return primaryColumn;
    }

    public IdGenerator getIdGenerator() {
        return idGenerator;
    }

    /**
     * 当主键的{@link IdGenerator}设置为UUID时，限制UUID的长度。
     * @return UUID的字符长度。
     */
    public int getUuidLength() {
        return uuidLength;
    }

    /**
     * 使用属性名获取对应的字段名。
     * @param property 实体属性名称。
     * @return 表字段。
     */
    public String getColumn(String property) {
        return propertiesMapper.get(property);
    }

    /**
     * @return 实体属性与表字段的映射集合，key是属性名，value是字段名。
     */
    public Map<String, String> getPropertiesMapper() {
        return propertiesMapper;
    }

    /**
     * 实体属性可设置忽略类型，如某个属性在更新操作时应被忽略，但插入和查询时应被处理。
     * 此方法就是判断属性是否设置了ignoreType。
     * @param property 属性名。
     * @param ignoreType 忽略类型。
     * @return true表示property应忽略不被处理，false表示应被处理。
     */
    public boolean isIgnore(String property, IgnoreColumnType ignoreType) {
        IgnoreColumn ic = propertiesIgnoreInfoMapper.get(property);
        if(ic == null) return false;
        if(ic.value().length == 0) return true;
        for(IgnoreColumnType type : ic.value()) {
            if(type == ignoreType) return true;
        }
        return false;
    }
}
