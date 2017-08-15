package com.jxs.ld.bean;

import org.apache.commons.lang3.StringUtils;

import javax.lang.model.type.NullType;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 操作实体的工具类。
 *
 * @author jiangxingshang
 */
public class Beans {

    private interface FieldFilter {
        boolean filter(Field field);
    }

    /**
     * @param beanClass 实体类型。
     * @param filter 过滤器，返回false会忽略当前属性。
     * @return 返回实体类的所有定义属性（包括父类的）。
     */
    private static List<Field> getFields(Class<?> beanClass, FieldFilter filter) {
        List<Field> fields = new LinkedList<>();

        while(beanClass != Object.class) {
            if(beanClass.getAnnotation(TableName.class) != null) {
                for(Field f : beanClass.getDeclaredFields()) {
                    if(filter != null) {
                        if(filter.filter(f)) {
                            fields.add(f);
                        }
                    } else {
                        fields.add(f);
                    }
                }
            }
            beanClass = beanClass.getSuperclass();
        }
        return fields;
    }

    /**
     * @param beanClass 实体类型。
     * @return 实体类型的所有（有对应getter）属性，如果某个属性没有对应的getter则会被忽略。
     */
    public static List<Field> getFields(Class<?> beanClass) {
        return getFields(beanClass, new FieldFilter() {
            @Override
            public boolean filter(Field field) {
                if (getMethod(field, true) != null) {
                    return true;
                }
                return false;
            }
        });
    }

    /**
     * 使用实体属性获取对应的表字段，默认的，属性名采用驼峰命名方式，字段名采用"_"分隔符，
     * 如果命名方式不正确，则无法正确的将属性名转换成字段名，你也可以显示的在属性上设置字段
     * 名，这样就不会自动转换。
     * @param f 实体属性。
     * @return 属性对应的表字段名。
     */
    private static String getColumnName(Field f) {
        String property = f.getName();
        Column column = f.getAnnotation(Column.class);
        if(column == null) {
            Method m = getMethod(f, true);
            if(m != null) column = m.getAnnotation(Column.class);
        }
        if(column == null || StringUtils.isBlank(column.value())) {
            String name = property;
            Pattern p = Pattern.compile("([A-Z])");
            Matcher m = p.matcher(name);
            while (m.find()) {
                String g = m.group(0);
                name = name.replace(g, "_" + g.toLowerCase());
            }
            return name;
        } else {
            return column.value();
        }
    }

    /**
     * @param f 实体属性。
     * @param gs true表示获取getter方法，false表示获取setter方法。
     * @param parameterTypes 属性对应getter或setter的参数类型。
     * @return 属性对应的getter或setter。
     */
    private static Method getMethod(Field f, boolean gs, Class<?>... parameterTypes) {
        String property = f.getName();
        Class<?> fieldType = f.getType();
        Class<?> boolType = fieldType.isPrimitive() ? boolean.class : Boolean.class;
        String prefix = gs ? (fieldType == boolType ? "is" : "get") : "set";
        String getter = prefix + property.substring(0, 1).toUpperCase() + property.substring(1);
        Class<?> cls = f.getDeclaringClass();
        while(true) {
            try {
                return cls.getMethod(getter, parameterTypes);
            } catch(NoSuchMethodException e) {
                cls = cls.getSuperclass();
                if(cls == Object.class) {
                    return null;
                }
            }
        }
//            if(gs && f.getType() == Boolean.class) {
//                //boolean类型的getter如果用isXXX没有找到的话再用getXXX尝试找一次
//                getter = "get" + property.substring(0, 1).toUpperCase() + property.substring(1);
//                try {
//                    return f.getDeclaringClass().getMethod(getter, parameterTypes);
//                } catch(NoSuchMethodException x) {
//                    return null;
//                }
//            }
    }

    public static Field getField(Class<?> beanClass, String property) {
        while(true) {
            Field field = null;
            try {
                field = beanClass.getDeclaredField(property);
            } catch (NoSuchFieldException e) {
            }
            if(field != null) {
                return field;
            } else {
                beanClass = beanClass.getSuperclass();
            }
            if(beanClass == Object.class) return null;
        }
    }

    /**
     * 从实体类型中获取主键的属性。
     * @param beanClass
     * @return
     */
    public static Field getPrimaryField(Class<?> beanClass) {
        for(Field f : getFields(beanClass)) {
            Column col = f.getAnnotation(Column.class);
            if(col != null && col.primaryKey()) {
                return f;
            }
        }
        return null;
    }

    /**
     * 将实体属性和表字段做映射，key是属性名，value是字段名。
     * @param beanClass 实体类型。
     * @return 实体类型的属性与表字段的集合。
     */
    public static Map<String, String> getMapper(Class<?> beanClass) {
        Map<String, String> map = new HashMap<>();
        for(java.lang.reflect.Field prop : getFields(beanClass)) {
            map.put(prop.getName(), getColumnName(prop));
        }
        return map;
    }

    /**
     * 将map的key和value对换位置。
     * @param map
     * @return
     */
    public static Map<String, String> reverse(Map<String, String> map) {
        Map<String, String> r = new HashMap<>(map.size());
        for(String key : map.keySet()) {
            String value = map.get(key);
            r.put(value, key);
        }
        return r;
    }

    /**
     * @param beanClass
     * @return 实体对应的表名。
     */
    public static String getTable(Class<?> beanClass) {
        TableName tn = beanClass.getAnnotation(TableName.class);
        if(tn == null) {
            throw new RuntimeException(beanClass.getName() + " not found table name");
        } else {
            return tn.value();
        }
    }

    /**
     * 获取实体类型中设置为主键的对应的字段名。
     * @param beanClass
     * @return 主键字段。
     */
    public static String getPrimaryColumn(Class<?> beanClass) {
        Field f = getPrimaryField(beanClass);
        return getPrimaryColumn(f);
    }

    public static String getPrimaryColumn(Field primary) {
        if (primary != null) {
            Column col = primary.getAnnotation(Column.class);
            String name = col == null ? null : col.value();
            return StringUtils.isBlank(name) ? primary.getName() : name;
        } else {
            throw new RuntimeException(primary.getDeclaringClass().getName() + " not found primary key");
        }
    }

    /**
     * 获取实体对应的表字段与属性值的映射，用于sql的插入或更新操作。
     * @param bean 实体对象。
     * @param includePrimaryKey true表示将主键添加到映射中，false则忽略主键。
     * @param <E> 实体类型。
     * @return 字段与值的映射集合。
     */
    public static <E> Map<String, Object> getValueMap(E bean, boolean includePrimaryKey) {
        String primaryKey = getPrimaryColumn(bean.getClass());
        List<Field> fields = getFields(bean.getClass());
        Map<String, Object> map = new HashMap<>(fields.size());
        for(Field f : fields) {
            Class<?> columnType = getColumnType(bean.getClass(), f.getName());
            String columnName = getColumnName(f);
            if(!includePrimaryKey && columnName.equals(primaryKey)) continue;
            Method m = getMethod(f, true);
            if(m != null) {
                try {
                    Object value = m.invoke(bean);

                    //日期类型处理
                    if(value instanceof java.util.Date || value instanceof Calendar) {
                        long time;
                        if(value instanceof java.util.Date) {
                            time = ((java.util.Date)value).getTime();
                        } else {
                            time = ((Calendar)value).getTimeInMillis();
                        }

                        if(columnType == java.sql.Date.class) {
                            value = new java.sql.Date(time);
                        } else if(columnType == java.sql.Timestamp.class) {
                            value = new java.sql.Timestamp(time);
                        }
                    }
                    map.put(columnName, value);
                } catch (IllegalAccessException | InvocationTargetException  e) {
                }
            }
        }
        return map;
    }

    public static IdGenerator getIdGenerator(Class<?> beanClass) {
        Field f = getPrimaryField(beanClass);
        if(f == null) {
            throw new RuntimeException(beanClass.getName() + " not found primary key");
        } else {
            return f.getAnnotation(Column.class).idGenerator();
        }
    }

    public static void set(Object bean, Field prop, Object data) {
        try {
            Method m = getMethod(prop, false, prop.getType());
            if(m != null) {
                m.invoke(bean, data);
            } else {
                throw new RuntimeException(String.format("No such method with field [%s %s %s]", bean.getClass().getName(), data.getClass().getName(), prop.getName()));
            }
        } catch (Exception e) {
            throw new RuntimeException("Cannot set value to " + bean.getClass().getName() + "." + prop.getName() + " with " + data + "(" + (data != null ? data.getClass().getName() : "null") + ")", e);
        }
    }

    public static Class<?> getColumnType(Class<?> beanClass, String property) {
        Field f = getField(beanClass, property);
        if(f == null) return null;
        Column c = f.getAnnotation(Column.class);
        if(c == null) return f.getType();
        return c.columnType() == NullType.class ? f.getType() : c.columnType();
    }

    /**
     * 初始化实体设置的默认值。
     * @param bean 实体实例。
     * @see DefaultValue
     */
    public static void initDefaultValue(final Object bean) {
        getFields(bean.getClass(), new FieldFilter() {
            @Override
            public boolean filter(Field f) {
                DefaultValue df = f.getAnnotation(DefaultValue.class);
                if(df == null) return false;
                try {
                    Object value = getMethod(f, true).invoke(bean);
                    if(value == null) {
                        String defVal = df.value();
                        Class<?> cls = f.getType();
                        Method m = getMethod(f, false, cls);
                        if (cls == String.class) {
                            m.invoke(bean, defVal);
                        } else if (cls == Double.class) {
                            m.invoke(bean, Double.valueOf(defVal));
                        } else if (cls == Float.class) {
                            m.invoke(bean, Float.valueOf(defVal));
                        } else if (cls == Integer.class) {
                            m.invoke(bean, Integer.valueOf(defVal));
                        } else if (cls == Short.class) {
                            m.invoke(bean, Short.valueOf(defVal));
                        } else if (cls == Byte.class) {
                            m.invoke(bean, Byte.valueOf(defVal));
                        } else if (cls == Character.class) {
                            m.invoke(bean, defVal.charAt(0));
                        } else if (cls == Boolean.class) {
                            m.invoke(bean, "true".equals(defVal) || "1".equals(defVal));
                        } else if (cls == Long.class) {
                            m.invoke(bean, Long.valueOf(defVal));
                        } else {
                            throw new RuntimeException("Not support type for " + cls.getName());
                        }
                    }
                } catch (IllegalAccessException | InvocationTargetException e) {
                    throw new RuntimeException(e);
                }
                return false;
            }
        });
    }
}
