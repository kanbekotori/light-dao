package com.jxs.ld.utils;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.apache.commons.lang3.time.DateUtils;

import javax.servlet.http.HttpServletRequest;
import java.lang.reflect.Array;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * 键值参数工具类。
 * @author jiangxingshang
 */
public class ParamKit {

    private Map<String, String> params;

    public ParamKit(Map<String, String> params) {
        this.params = params;
    }

    public ParamKit(HttpServletRequest request) {
        params = new HashMap<>();
        Map<String, String[]> tmp = request.getParameterMap();
        if(tmp == null) return;
        for(Map.Entry<String, String[]> entry : tmp.entrySet()) {
            String[] value = entry.getValue();
            if(value == null) continue;
            if(value.length == 1) {
                params.put(entry.getKey(), value[0]);
            } else {
                params.put(entry.getKey(), StringUtils.join(value, ","));
            }
        }
    }

    private String val(String name) {
        return params.get(name);
    }

    public ParamKit add(String key, String value) {
        params.put(key, value);
        return this;
    }

    /**
     * “1”或“true”都为true，其他为false。
     * @param name
     */
    public boolean asBool(String name) {
        String str = val(name);
        return "1".equals(str) || "true".equals(str);
    }

    public int asInt(String name, int def) {
        try {
            return Integer.decode(val(name));
        } catch(Exception e) {
            return def;
        }
    }

    public int asInt(String name) {
        return asInt(name, 0);
    }

    public float asFloat(String name, float def) {
        try {
            return Float.valueOf(val(name));
        } catch(Exception e) {
            return def;
        }
    }

    public float asFloat(String name) {
        return asFloat(name, 0f);
    }

    public double asDouble(String name, double def) {
        try {
            return Double.parseDouble(val(name));
        } catch(Exception e) {
            return def;
        }
    }

    public double asDouble(String name) {
        return asFloat(name, 0f);
    }


    public String asStr(String name, String def) {
        String r = val(name);
        return r == null ? def : r;
    }

    public String asStr(String name) {
        return asStr(name, "");
    }

    public String asStr(String name, String prefix, String suffix) {
        return prefix + asStr(name) + suffix;
    }

    /**
     *
     * @param name
     * @param cls 只支持String， Integer，Float，Double类型。
     * @param sper 分隔符
     * @param <T>
     * @return
     */
    public <T> T[] asArray(String name, Class<T> cls, String sper) {
        String tmp = asStr(name);
        String[] arr = tmp.split(sper);
        T[] t = (T[]) Array.newInstance(cls, arr.length);
        if(cls == String.class) {
            return (T[])arr;
        } else if(cls == Integer.class) {
            for(int i = 0; i < arr.length; i++) {
                t[i] = (T) Integer.decode(arr[i]);
            }
        } else if(cls == Float.class) {
            for(int i = 0; i < arr.length; i++) {
                t[i] = (T) Float.valueOf(arr[i]);
            }
        } else if(cls == Double.class) {
            for(int i = 0; i < arr.length; i++) {
                t[i] = (T) Double.valueOf(arr[i]);
            }
        } else {
            throw new RuntimeException(cls.getSimpleName() + " is not support.");
        }
        return t;
    }

    /**
     * 默认分隔符为","
     * @param name
     * @param cls 只支持String， Integer，Float，Double类型。
     * @param <T>
     * @see #asArray(String, Class, String)
     */
    public <T> T[] asArray(String name, Class<T> cls) {
        return asArray(name, cls, ",");
    }

    /**
     *
     * @param name
     * @param cls 只支持String， Integer，Float，Double类型。
     * @param sper
     * @param <T>
     */
    public <T> Set<T> asSet(String name, Class<T> cls, String sper) {
        String tmp = asStr(name);
        String[] arr = tmp.split(sper);
        Set<T> set = new HashSet<>();
        if(cls == String.class) {
            for(int i = 0; i < arr.length; i++) {
                set.add((T)arr[i]);
            }
        } else if(cls == Integer.class) {
            for(int i = 0; i < arr.length; i++) {
                set.add((T) Integer.decode(arr[i]));
            }
        } else if(cls == Float.class) {
            for(int i = 0; i < arr.length; i++) {
                set.add((T) Float.valueOf(arr[i]));
            }
        } else if(cls == Double.class) {
            for(int i = 0; i < arr.length; i++) {
                set.add((T) Double.valueOf(arr[i]));
            }
        } else {
            throw new RuntimeException(cls.getSimpleName() + " is not support.");
        }
        return set;
    }

    /**
     * 默认分隔符","
     * @param name
     * @param cls
     * @param <T>
     * @return
     * @see #asSet(String, Class, String)
     */
    public <T> Set<T> asSet(String name, Class<T> cls) {
        return asSet(name, cls, ",");
    }

    public Date asDate(String name) {
        return asDate(name, (Date) null);
    }

    public Date asDate(String name, Date def) {
        return asDate(name, "yyyy-MM-dd", def);
    }

    public Date asDate(String name, String format) {
        return asDate(name, format, null);
    }

    public Date asDate(String name, String format, Date def) {
        SimpleDateFormat fmt = new SimpleDateFormat(format);
        try {
            return fmt.parse(val(name));
        } catch (Exception e) {
            return def;
        }
    }

    public Date[] asDates(String format, Date def, String...name) {
        Date[] tmp = new Date[name.length];
        for(int i = 0; i < name.length; i++) {
            tmp[i] = asDate(name[i], format, def);
        }
        return tmp;
    }

    /**
     * 所有的参数是否存在，参数必须有值才被认为存在。
     * @param name
     */
    public boolean has(String...name) {
        for(String n : name) {
            String data = val(n);
            if(data == null || data.trim().length() == 0) {
                return false;
            }
        }
        return true;
    }
}
