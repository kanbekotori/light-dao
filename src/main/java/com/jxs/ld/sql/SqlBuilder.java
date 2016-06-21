package com.jxs.ld.sql;

import com.jxs.ld.BaseDao;
import com.jxs.ld.bean.Beans;
import org.apache.commons.lang3.StringUtils;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * SQL语句的构建工具，多用于查询语句的构建，目的为解决硬编码sql的字段表名与条件查询。
 *
 * @author jiangxingshang
 * @see BaseDao
 */
public class SqlBuilder {

    private StringBuilder builder = new StringBuilder();
    private Map<String, String> mapper = new HashMap<>();
    private Map<String, Map<String, String>> varMap = new HashMap<>();
    private boolean hasWhere = false;
    private List<Object> values = new LinkedList<>();
    private Map<String, Object> namedParams = new HashMap<>();
    private Pattern namedPattern = Pattern.compile(":([a-zA-Z_]+)");
    private Comparator<String> stringComparableWithLength = new Comparator<String>() {
        @Override
        public int compare(String o1, String o2) {
            if(o1.length() == o2.length()) {
                return 0;
            } else {
                return o1.length() > o2.length() ? -1 : 1;
            }
        }
    };

    /**
     * <p>Constructor for SqlBuilder.</p>
     */
    public SqlBuilder() {

    }

    /**
     * 构建一个带有属性字段映射的sql构造器，这个mapper会在转化sql时将带有@字符的单词替换成对应的字符串。
     *
     * @param mapper key是@变量名（不带@字符），value是你要替换的值。
     */
    public SqlBuilder(Map<String, String> mapper) {
        this.mapper = new HashMap<>(mapper.size());
        for(Map.Entry<String, String> entry : mapper.entrySet()) {
            this.mapper.put(entry.getKey(), entry.getValue());
        }
    }

    /**
     * 添加变量映射，在{@link #toSql()}的时候会将sql字符串中带有@前缀的单词替换成对应的变量值。
     * 如：
     * new SqlBuilder().addVar("tableName", "t_user").sql("select * from @tableName").toSql();
     * //select * from t_user
     *
     * @param key a {@link java.lang.String} object.
     * @param value a {@link java.lang.String} object.
     * @see #SqlBuilder(Map)
     * @return a {@link com.jxs.ld.sql.SqlBuilder} object.
     */
    public SqlBuilder addVar(String key, String value) {
        this.mapper.put(key, value);
        return this;
    }

    /**
     * <p>addVar</p>
     *
     * @param vars a {@link java.util.Map} object.
     * @return a {@link com.jxs.ld.sql.SqlBuilder} object.
     */
    public SqlBuilder addVar(Map<String, String>...vars) {
        for(Map<String, String> tmp : vars) {
            this.mapper.putAll(tmp);
        }
        return this;
    }

    /**
     * 添加分组变量。
     * 场景：
     * 当做一些连接查询时，需要用到其他表的变量，如果一个个变量添加就比较麻烦，如果使用map的方式添加又
     * 可能会和现有的发生冲突，为了解决这个冲突就有了分组变量。先看下面这个sql。
     *
     * <code>
     *     select * from @tableName t1 left join @v!tableName t2 on t1.@categoryId = t2.@v!id
     * </code>
     *
     * 第二个tableName的前面有一个"v!"，当在@变量符号中出现!感叹号时，就表示感叹号的左边的字母是一个变量集合
     * 的key，使用v这个key去查找一个变量集合，然后用tableName这个变量名从这个集合中查找对应的值，这样就不会和
     * 默认的（第一个）tableName发生冲突了。
     *
     * <code>
     *   Map&lt;String, String&gt; map = ...;
     *   map.put("tableName", "t_product_category");
     *   new SqlBuilder()
     *     .addVar("tableName", "t_product")
     *     .addVar("v", map)
     *     .sql(...)
     * </code>
     *
     * @param prefix
     * @param vars
     * @return
     */
    public SqlBuilder addVar(String prefix, Map<String, String> vars) {
        this.varMap.put(prefix, vars);
        return this;
    }

    /**
     * @param prefix
     * @param dao
     * @param <T>
     * @return
     * @see #addVar(String, Map)
     */
    public <T> SqlBuilder addVar(String prefix, BaseDao<T> dao) {
        return this.addVar(prefix, dao.getPropertiesMapper());
    }

    /**
     * <p>sql</p>
     *
     * @param text a {@link java.lang.String} object.
     * @return a {@link com.jxs.ld.sql.SqlBuilder} object.
     */
    public SqlBuilder sql(String text) {
        builder.append((builder.length() > 0 ? " " : "") + text.trim());
        return this;
    }

    /**
     * 将表名追加到sql后面。
     *
     * @param table a {@link java.lang.String} object.
     * @return a {@link com.jxs.ld.sql.SqlBuilder} object.
     */
    public SqlBuilder table(String table) {
        return table(table, null);
    }

    /**
     * 将表名追加到sql后面，附加表别名
     *
     * @param table a {@link java.lang.String} object.
     * @param alias 表别名
     * @return a {@link com.jxs.ld.sql.SqlBuilder} object.
     */
    public SqlBuilder table(String table, String alias) {
        builder.append(" " + table.trim() + (StringUtils.isBlank(alias) ? "" : " " + alias));
        return this;
    }

    /**
     * 将实体对应的表名追加到sql后面。
     *
     * @param beanClass a {@link java.lang.Class} object.
     * @see #table(Class, String)
     * @return a {@link com.jxs.ld.sql.SqlBuilder} object.
     */
    public SqlBuilder table(Class<?> beanClass) {
        return table(beanClass, null);
    }

    /**
     * 将实体对应的表名追加到sql后面，从javabean中获取对应的表名，bean必须使用了{@link com.jxs.ld.bean.TableName}注解。
     *
     * @param beanClass 任何带有{@link com.jxs.ld.bean.TableName}注解的类。
     * @param alias 表别名。
     * @return a {@link com.jxs.ld.sql.SqlBuilder} object.
     */
    public SqlBuilder table(Class<?> beanClass, String alias) {
        return table(Beans.getTable(beanClass), alias);
    }

    /**
     * <p>where</p>
     *
     * @param text a {@link java.lang.String} object.
     * @param use a boolean.
     * @param values a {@link java.lang.Object} object.
     * @return a {@link com.jxs.ld.sql.SqlBuilder} object.
     */
    public SqlBuilder where(String text, boolean use, Object...values) {
        if(use) {
            value(null, values);
            return appendCondition("where", text);
        } else {
            return this;
        }
    }

    public SqlBuilder where(String text) {
        return where(text, true);
    }

    /**
     * <p>and</p>
     *
     * @param text a {@link java.lang.String} object.
     * @param use a boolean.
     * @param values a {@link java.lang.Object} object.
     * @return a {@link com.jxs.ld.sql.SqlBuilder} object.
     */
    public SqlBuilder and(String text, boolean use, Object...values) {
        if(use) {
            value(null, values);
            return appendCondition("and", text);
        } else {
            return this;
        }
    }

    public SqlBuilder and(String text) {
        return and(text, true);
    }

    /**
     * <p>or</p>
     *
     * @param text a {@link java.lang.String} object.
     * @param use a boolean.
     * @param values a {@link java.lang.Object} object.
     * @return a {@link com.jxs.ld.sql.SqlBuilder} object.
     */
    public SqlBuilder or(String text, boolean use, Object...values) {
        if(use) {
            value(null, values);
            return appendCondition("or", text);
        } else {
            return this;
        }
    }

    public SqlBuilder or(String text) {
        return or(text, true);
    }

    private SqlBuilder appendCondition(String prefix, String text) {
        text = text.trim();
        if(!hasWhere) {
            hasWhere = true;
            prefix = "where";
        }
        builder.append(String.format(" %s %s", prefix, text));
        return this;
    }

    /**
     * 排序分段，如果field前带有一个"-"符号，表示desc排序，否则asc排序。
     * order("name", "@age", "-@age");
     * 第一个是表字段名，第二个是映射字段，第三个会降序排序。
     *
     * @param fields 排序字段。
     * @return a {@link com.jxs.ld.sql.SqlBuilder} object.
     */
    public SqlBuilder order(String...fields) {
        List<String> orders = new LinkedList<>();
        for(String f : fields) {
            String o = "asc";
            if(f.startsWith("-")) {
                o = "desc";
                f = f.substring(1);
            }
            if(f.startsWith("@")) {
                f = f.substring(1);
                String tmp = mapper.get(f);
                if(tmp != null) {
                    f = tmp;
                }
            }
            orders.add(f + " " + o);
        }
        builder.append(" order by " + StringUtils.join(orders, ", "));
        return this;
    }

    /**
     * 添加参数到队列中。
     *
     * @param values a {@link java.lang.Object} object.
     * @return a {@link com.jxs.ld.sql.SqlBuilder} object.
     */
    public SqlBuilder value(Object...values) {
        for(Object o : values) {
            this.values.add(o);
        }
        return this;
    }

    public SqlBuilder value(String name, Object value) {
        namedParams.put(name, value);
        return this;
    }

    private SqlBuilder value(String sqlText, Object...values) {
        if(sqlText == null) {
            value(values);
            return this;
        }
        Matcher m = namedPattern.matcher(sqlText);
        int i = 0;
        while(m.find()) {
            String var = m.group(1);
            try {
                namedParams.put(var, values[i++]);
            } catch (ArrayIndexOutOfBoundsException e) {
                throw new RuntimeException("参数个数与sql语句中的命名变量不匹配。", e);
            }
        }
        return this;
    }

    /**
     * <p>Getter for the field <code>value</code>.</p>
     *
     * @return a {@link java.util.List} object.
     */
    public List<Object> getValues() {
        return this.values;
    }

    /**
     * <p>getValueArray</p>
     *
     * @return an array of {@link java.lang.Object} objects.
     */
    public Object[] getValueArray() {
        return this.values.toArray();
    }

    public Map<String, Object> getValueMap() {
        return this.namedParams;
    }

    /**
     * <p>toSql</p>
     *
     * @return a {@link java.lang.String} object.
     */
    public String toSql() {
        String str = builder.toString();
        Pattern p = Pattern.compile("(@[a-zA-Z_!]+)");
        Matcher m = p.matcher(builder);
        List<String> finds = new LinkedList<>();
        while(m.find()) {
            finds.add(m.group());
        }
        Collections.sort(finds, stringComparableWithLength);
        for(String group : finds) {
            String prop = group.substring(1);
            String field;
            if(prop.contains("!")) {
                String[] tmp = prop.split("!");
                String prefix = tmp[0];
                Map<String, String> vars = varMap.get(prefix);
                if(vars == null) {
                    throw new RuntimeException("Cannot find var mapper for prefix '" + prefix + "' near " + group);
                }
                field = vars.get(tmp[1]);
            } else {
                field = mapper.get(prop);
            }
            if(field == null) {
                throw new RuntimeException("Cannot find value by " + group + ", you should use addVar(key, value) to add a var.");
            };
            str = str.replaceAll(group, field);
        }
        return str.trim();
    }

    /**
     * 返回分页查询SQL语句。
     *
     * @param start 记录起始行，从0开始。
     * @param limit 返回多少条记录。
     * @return a {@link java.lang.String} object.
     */
    public String toSql(int start, int limit) {
        return String.format("%s limit %d,%d", toSql(), start, limit);
    }

    /**
     * 返回查询总数的SQL语句。
     *
     * @return a {@link java.lang.String} object.
     */
    public String toSqlCount() {
        String sql = toSql();
        Pattern p = Pattern.compile("^select(.*?)from");
        Matcher m = p.matcher(sql);
        if(m.find()) {
            String g = m.group(1);
            sql = StringUtils.replaceOnce(sql, g, " count(0) ");
        }
        return sql;
    }

    /**
     * {@inheritDoc}
     *
     * 返回原始sql组装语句（未格式化）。
     */
    @Override
    public String toString() {
        return builder.toString();
    }
}
