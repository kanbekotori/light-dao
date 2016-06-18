package com.jxs.ld.bean;

import javax.lang.model.type.NullType;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD})
public @interface Column {

    String value() default "";

    boolean primaryKey() default false;

    Class<?> columnType() default NullType.class;

    IdGenerator idGenerator() default IdGenerator.UUID;

    int UUIDLength() default 32;
}
