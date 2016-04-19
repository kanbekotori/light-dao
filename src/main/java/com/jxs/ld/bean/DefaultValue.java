package com.jxs.ld.bean;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 当实体属性的值为Null时就分配一个默认值。
 * @see {@link Beans#initDefaultValue(Object)}
 * Created by jiangxingshang on 15/11/18.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD})
public @interface DefaultValue {
    String value();
}
