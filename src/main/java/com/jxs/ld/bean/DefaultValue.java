package com.jxs.ld.bean;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 为实体属性设置默认值，通过调用{@link Beans#initDefaultValue(Object)}方法可初始化实体实例的默认值。
 *
 * @author jiangxingshang
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD})
public @interface DefaultValue {
    String value();
}
