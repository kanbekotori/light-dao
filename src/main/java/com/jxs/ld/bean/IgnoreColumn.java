package com.jxs.ld.bean;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 设置属性是否应被忽略，某些属性可能会希望在某个操作（insert，update，query）中不被处理，
 * 那么就为该属性设置此注解，默认的此注解表示所有操作都被忽略，如果想在指定的操作才被忽略，
 * 可使用{@link IgnoreColumnType}。
 *
 * @author jiangxingshang
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD})
public @interface IgnoreColumn {

    IgnoreColumnType[] value() default {};
}
