package com.jxs.ld.bean;

import org.apache.commons.lang3.StringUtils;

import javax.lang.model.type.NullType;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author jiangxingshang
 * @date 15/11/18
 */
public class Beans {

    private interface FieldFilter {
        boolean filter(Field field);
    }

    private static List<Field> getFields(Class<?> beanClass, FieldFilter filter) {
        List<Field> fields = new LinkedList<>();
        Class<?> superClass = beanClass.getSuperclass();
        if(superClass != Object.class) {
            fields.addAll(getFields(superClass));
        }
        for(Field f : beanClass.getDeclaredFields()) {
            if(filter != null) {
                if(filter.filter(f)) {
                    fields.add(f);
                }
            } else {
                fields.add(f);
            }
        }
        return fields;
    }

    /**
     * 获取bean类的属性（包含父类定义的属性），返回的列表只包含有效属性（拥有对应的getter方法）。
     * @param beanClass
     */
    private static List<Field> getFields(Class<?> beanClass) {
        return getFields(beanClass, new FieldFilter() {
            @Override
            public boolean filter(Field field) {
                if (field.getAnnotation(IgnoreColumn.class) != null) return false;
                if (getMethod(field, true) != null) {
                    return true;
                }
                return false;
            }
        });
    }

    /**
     * 获取属性对应的表字段名，属性或getter方法上需放置{@link Column}注解，否则会返回null。
     * @param f
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
     * 根据属性获取getter或setter
     * @param f
     * @param gs true表示getter，false表示setter
     */
    private static Method getMethod(Field f, boolean gs, Class<?>... parameterTypes) {
        String property = f.getName();
        Class<?> fieldType = f.getType();
        Class<?> boolType = fieldType.isPrimitive() ? boolean.class : Boolean.class;
        String prefix = gs ? (fieldType == boolType ? "is" : "get") : "set";
        String getter = prefix + property.substring(0, 1).toUpperCase() + property.substring(1);
        try {
            return f.getDeclaringClass().getMethod(getter, parameterTypes);
        } catch (NoSuchMethodException e) {
            if(gs && f.getType() == Boolean.class) {
                //boolean类型的getter如果用isXXX没有找到的话再用getXXX尝试找一次
                getter = "get" + property.substring(0, 1).toUpperCase() + property.substring(1);
                try {
                    return f.getDeclaringClass().getMethod(getter, parameterTypes);
                } catch(NoSuchMethodException x) {
                    return null;
                }
            }
            return null;
        }
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
     * 获取主键属性。
     * @param beanClass
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
     * 获取bean的属性字段映射，属性上必须有{@link Column}注解或对应的getter方法才会被添加到映射集中。
     * @param beanClass bean类。
     * @return key是bean的属性名，value是表字段名。
     */
    public static Map<String, String> getMapper(Class<?> beanClass) {
        Map<String, String> map = new HashMap<>();
        for(java.lang.reflect.Field prop : getFields(beanClass)) {
            map.put(prop.getName(), getColumnName(prop));
        }
        return map;
    }

    /**
     * 将key和value的位置对调。
     * @param map
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
     * 获取bean定义的表名，bean必须设置了{@linkplain com.jxs.ld.bean.TableName}注解。
     * @param beanClass
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
     * 获取bean定义的主键表字段名称。
     * @param beanClass
     * @see #getPrimaryColumn(Field)
     */
    public static String getPrimaryColumn(Class<?> beanClass) {
        Field f = getPrimaryField(beanClass);
        return getPrimaryColumn(f);
    }

    /**
     * 获取这个属性对应的字段
     * @param primary
     */
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
     * 返回一个map，key是属性对应的表字段名，value是属性的值（通过getter获得）。
     * @param bean
     * @param includePrimaryKey true表示返回的map包含了主键，false表示不包含。
     * @param <E>
     */
    public static <E> Map<String, Object> getValueMap(E bean, boolean includePrimaryKey) {
        String primaryKey = getPrimaryColumn(bean.getClass());
        List<Field> fields = getFields(bean.getClass());
        Map<String, Object> map = new HashMap<>(fields.size());
        for(Field f : fields) {
            String columnName = getColumnName(f);
            if(!includePrimaryKey && columnName.equals(primaryKey)) continue;
            Method m = getMethod(f, true);
            if(m != null) {
                try {
                    map.put(columnName, m.invoke(bean));
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

    /**
     * 调用bean的setter来设置值。
     * @param bean
     * @param prop
     * @param data
     */
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

    /**
     * 获取属性对应的字段的类型。
     * @param beanClass
     * @param property
     */
    public static Class<?> getColumnType(Class<?> beanClass, String property) {
        Field f = getField(beanClass, property);
        if(f == null) return null;
        Column c = f.getAnnotation(Column.class);
        if(c == null) return f.getType();
        return c.columnType() == NullType.class ? f.getType() : c.columnType();
    }

    /**
     * 初始化实体的空值，如果该属性的值为空，则分配一个默认值，属性必须加上{@link DefaultValue}注解。
     * 注：你的属性必须提供getter和setter。
     * @param bean
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
