package com.jxs.ld.bean;

import javax.lang.model.type.NullType;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 *
 * @author jiangxingshang
 * @date 15/11/18
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD})
public @interface Column {

    String value() default "";

    /**
     * 字段是否为主键。
     * @return Boolean
     */
    boolean primaryKey() default false;

    /**
     * 设置属性对应的字段在数据库中应该是什么类型，从{@link java.sql.ResultSet}获取值时用到，如果不提供则不使用类型去获取值。
     * @return Class
     */
    Class<?> columnType() default NullType.class;

    IdGenerator idGenerator() default IdGenerator.UUID;
}
