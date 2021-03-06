package com.jxs.ld;

import com.jxs.ld.bean.BeanInfo;
import com.jxs.ld.bean.Beans;
import com.jxs.ld.bean.IdGenerator;
import com.jxs.ld.bean.IgnoreColumnType;
import com.jxs.ld.sql.SqlBuilder;
import com.jxs.ld.utils.BeanSetter;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementSetter;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.simple.SimpleJdbcInsert;

import javax.sql.DataSource;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

/**
 * @author jiangxingshang
 */
public abstract class BaseDao<T> {

    private String SQL_GET_BY_ID;

    private SimpleJdbcInsert insert;
    protected JdbcTemplate jdbc;
    protected NamedParameterJdbcTemplate namedJdbc;
    /**
     * 默认的行到实体的转换器。
     */
    protected RowMapper<T> defaultRowMapper;
    protected Class<T> beanClass;
    /**
     * 实体的属性-字段映射，key是属性名，value是表字段。
     */
    protected Map<String, String> propertiesMapper;
    /**
     * 实体的字段-属性映射，key是表字段，value是属性。
     */
    protected Map<String, String> columnsMapper;
    /**
     * 实体属性对应字段的类型。
     */
    protected Map<String, Class<?>> columnTypes;
    protected BeanInfo beanInfo;

    protected BaseDao(final Class<T> beanClass) {
        this.beanClass = beanClass;
        this.initModelInfo();
    }

    protected void initModelInfo() {
        beanInfo = new BeanInfo(beanClass);
        propertiesMapper = beanInfo.getPropertiesMapper();
        columnTypes = new HashMap<>();
        for(String prop : propertiesMapper.keySet()) {
            columnTypes.put(prop, Beans.getColumnType(beanClass, prop));
        }
        columnsMapper = Beans.reverse(propertiesMapper);

        defaultRowMapper = createRowMapper(null);
        SQL_GET_BY_ID = String.format("select * from %s where %s = ?", beanInfo.getTableName(), beanInfo.getPrimaryColumn());
    }

    public RowMapper<T> getRowMapper() {
        return defaultRowMapper;
    }

    protected String uuid() {
        return UUID.randomUUID().toString().toLowerCase().replaceAll("-", "");
    }

    public BeanInfo getBeanInfo() {
        return beanInfo;
    }

    public Map<String, Class<?>> getColumnTypes() {
        return columnTypes;
    }

    public Map<String, String> getColumnsMapper() {
        return columnsMapper;
    }

    /**
     * 获取实体属性到表字段的映射集合。
     * @return
     */
    public Map<String, String> getPropertiesMapper() {
        return propertiesMapper;
    }

    /**
     * @see #getPropertiesMapperWithTableName(String) 默认表变量名称为tableName
     * @return
     */
    public Map<String, String> getPropertiesMapperWithTableName() {
        return this.getPropertiesMapperWithTableName("tableName");
    }

    /**
     * 获取实体属性到表字段的映射集合，并且附加了一个tableName的变量。
     * @param tableName 表名的变量名称，默认是"tableName"。
     * @return
     */
    public Map<String, String> getPropertiesMapperWithTableName(String tableName) {
        if(tableName == null || tableName.length() == 0) tableName = "tableName";
        Map<String, String> tmp = new HashMap<>(this.propertiesMapper);
        tmp.put(tableName, this.getBeanInfo().getTableName());
        return tmp;
    }

    public String getColumn(String property) {
        return beanInfo.getColumn(property);
    }

    /**
     * 创建一个行到实体的转换器，这个转换器默认会将字段填充到实体中，如果你提供了{@link com.jxs.ld.utils.BeanSetter}，
     * 那么你可以在它填充完默认的字段后做自己额外的工作，例如填充别名字段。
     * @param setter
     * @return
     */
    public RowMapper<T> createRowMapper(final BeanSetter<T> setter) {
        return new RowMapper<T>() {

            @Override
            public T mapRow(ResultSet rs, int rowNum) throws SQLException {
                T bean;
                try {
                    bean = beanClass.newInstance();
                } catch (InstantiationException | IllegalAccessException e) {
                    throw new SQLException(beanClass.getName() + " new fail", e);
                }
                for(Map.Entry<String, String> entry : columnsMapper.entrySet()) {
                    if(beanInfo.isIgnore(entry.getValue(), IgnoreColumnType.QUERY)) continue;
                    try {
                        rs.findColumn(entry.getKey());
                    } catch(SQLException e) {
                        continue;
                    }
                    Class<?> type = columnTypes.get(entry.getValue());
                    Object value;
                    if(type == null) {
                        value = rs.getObject(entry.getKey());
                    } else {
                        value = rs.getObject(entry.getKey(), type);
                        if(rs.wasNull()) {
                            continue;
                        }
                    }
                    if(value == null) continue;
                    Beans.set(bean, Beans.getField(beanClass, entry.getValue()), value);
                }
                if(setter != null) {
                    setter.bean(bean, rs, rowNum);
                }
                return bean;
            }
        };
    }

    @Autowired
    public void setDataSource(DataSource dataSource) {
        jdbc = new JdbcTemplate(dataSource);
        insert = new SimpleJdbcInsert(jdbc);
        insert.withTableName(beanInfo.getTableName());
        if(beanInfo.getIdGenerator() == IdGenerator.AUTO_INCREMENT) {
            insert.usingGeneratedKeyColumns(beanInfo.getPrimaryColumn());
        }
        namedJdbc = new NamedParameterJdbcTemplate(jdbc);
    }

    public T getById(Object id) {
        try {
            return jdbc.queryForObject(SQL_GET_BY_ID, getRowMapper(), id);
        } catch(EmptyResultDataAccessException e) {
            return null;
        }
    }

    public void insert(T bean) {
        IdGenerator idg = beanInfo.getIdGenerator();
        String primaryColumn = beanInfo.getPrimaryColumn();
        Map<String, Object> map = Beans.getValueMap(bean, idg == IdGenerator.ASSIGNED);
        Map<String, Object> tmp = new HashMap<>();
        for(String col : map.keySet()) {
            if(!beanInfo.isIgnore(columnsMapper.get(col), IgnoreColumnType.INSERT)) {
                tmp.put(col, map.get(col));
            }
        }
        map = tmp;
        Object idValue = null;
        if(IdGenerator.AUTO_INCREMENT == idg) {
            idValue = insert.executeAndReturnKey(map);
        } else if(IdGenerator.UUID == idg) {
            String uuid = uuid();
            if(beanInfo.getUuidLength() > uuid.length()) {
                throw new RuntimeException(bean.getClass().getName() + "的UUIDLength长度溢出（不能超过" + uuid.length() + "位）。");
            }
            idValue = uuid.substring(uuid.length() - beanInfo.getUuidLength(), uuid.length());
            map.put(primaryColumn, idValue);
            insert.execute(map);
        } else if(IdGenerator.ASSIGNED == idg) {
            insert.execute(map);
        } else {
            throw new RuntimeException("Id generator not found on " + bean.getClass().getName() + ", you must add @Column to primary key and provider a id generator.");
        }
        if(idValue != null) {
            Beans.set(bean, beanInfo.getPrimary(), idValue);
        }
    }

    /**
     * 更新记录。
     * @param bean
     * @param includeNullValue true表示会把null值的字段也更新到数据库，false会排除null值的更新。
     * @param excludeProperties 排除的属性，表示不会更新这些字段，注意你需要提供的是属性名而不是字段名。
     * @throws RuntimeException bean的id为null时抛出。
     */
    public void update(T bean, boolean includeNullValue, String...excludeProperties) {
        Set<String> excludes = new HashSet<>();
        if(excludeProperties != null) {
            for(String p : excludeProperties) {
                excludes.add(propertiesMapper.get(p));
            }
        }
        Map<String, Object> map = Beans.getValueMap(bean, true);
        Map<String, Object> tmp = new HashMap<>();
        for(String col : map.keySet()) {
            if(!beanInfo.isIgnore(columnsMapper.get(col), IgnoreColumnType.UPDATE)) {
                tmp.put(col, map.get(col));
            }
        }
        map = tmp;
        Object id = map.get(beanInfo.getPrimaryColumn());
        if(id == null) throw new RuntimeException("Id must not be null.");
        map.remove(beanInfo.getPrimaryColumn());
        List<String> sets = new LinkedList<>();
        final List<Object> values = new LinkedList<>();
        for(Map.Entry<String, Object> entry : map.entrySet()) {
            if(excludes.contains(entry.getKey())) continue;
            Object value = entry.getValue();
            if(value == null && !includeNullValue) continue;
            sets.add(entry.getKey() + "=?");
            values.add(value);
        }
        values.add(id);
        String sql = String.format("update %s set %s where %s=?", beanInfo.getTableName(), StringUtils.join(sets, ","), beanInfo.getPrimaryColumn());
        jdbc.update(sql, new PreparedStatementSetter() {
            @Override
            public void setValues(PreparedStatement ps) throws SQLException {
                int i = 1;
                for(Object v : values) {
                    ps.setObject(i++, v);
                }
            }
        });
    }

    /**
     * 更新记录，如果实体的属性值为null则忽略该属性。
     * @param bean
     * @see #update(Object, boolean, String...)
     */
    public void update(T bean) {
        update(bean, false);
    }

    /**
     * 根据ID删除记录。
     * @param id
     */
    public void delete(Object id) {
        jdbc.update(String.format("delete from %s where %s=?", beanInfo.getTableName(), beanInfo.getPrimaryColumn()), id);
    }

    public T getOne(String sql, RowMapper<T> mapper, Object...values) {
        try {
            return jdbc.queryForObject(sql, mapper, values);
        } catch(EmptyResultDataAccessException e) {
            return null;
        }
    }

    public int getCount(String sql, Object...values) {
        return jdbc.queryForObject(sql, values, Integer.class);
    }

    /**
     * 分页查询。
     * @param page
     * @param mapper
     * @param sqlBuilder
     * @param values 查询参数。
     * @return
     */
    public Page<T> query(Page<T> page, RowMapper<T> mapper, SqlBuilder sqlBuilder, Object...values) {
        int total = getCount(sqlBuilder.toSqlCount(), values);
        if(total == 0) {
            page.setTotal(0);
            page.setData(new LinkedList<T>());
        } else {
            List<T> list = jdbc.query(sqlBuilder.toSql(page.getStart(), page.getLimit()), mapper, values);
            page.setTotal(total);
            page.setData(list);
        }
        return page;
    }

    /**
     * {@link org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate}的分页查询实现版。
     * @param page
     * @param parameters
     * @param mapper
     * @param sqlBuilder
     * @return
     * @see #query(Page, RowMapper, SqlBuilder, Object...)
     */
    public Page<T> query(Page<T> page, MapSqlParameterSource parameters, RowMapper<T> mapper, SqlBuilder sqlBuilder) {
        int total = namedJdbc.queryForObject(sqlBuilder.toSqlCount(), parameters, Integer.class);
        if(total != 0) {
            List<T> list = namedJdbc.query(sqlBuilder.toSql(page.getStart(), page.getLimit()), parameters, mapper);
            page.setTotal(total);
            page.setData(list);
        } else {
            page.setTotal(0);
            page.setData(new LinkedList<T>());
        }
        return page;
    }

    public List<T> query(String sql, RowMapper<T> mapper, Object...values) {
        return jdbc.query(sql ,mapper, values);
    }

    /**
     * 查询前MAX条记录。
     * @param max
     * @param sql
     * @param mapper
     * @param values
     * @return
     */
    public List<T> query(int max, String sql, RowMapper<T> mapper, Object...values) {
        return jdbc.query(new SqlBuilder().sql(sql).toSql(0, max), mapper, values);
    }

    public Page<T> query(Page<T> page) {
        throw new RuntimeException("Empty query implements.");
    }

    /**
     * 创建一个包含{@link #beanClass}对应关系（属性-字段）和一个表名变量（tableName）的SQL构建器。
     * 这个构建器里已经有了{@linkplain #propertiesMapper}和{@linkplain #beanInfo}的表名的变量。
     * @param propertiesMapper 提供给{@link SqlBuilder}的变量。
     * @return
     * @see #propertiesMapper
     */
    public SqlBuilder createSqlBuilder(Map<String, String>...propertiesMapper) {
        return new SqlBuilder(this.propertiesMapper)
                .addVar(propertiesMapper)
                .addVar("tableName", beanInfo.getTableName());
    }

    /**
     * 自动设置{@link SqlBuilder#autoAppendTableAlias(boolean)}为true。
     * @see #createSqlBuilder(Map[])
     * @param sql
     * @return
     * @since 2.x
     */
    public SqlBuilder sql(String sql) {
        return createSqlBuilder().autoAppendTableAlias(true).sql(sql);
    }

    /**
     * @see #sql(String)
     * @return
     * @since 2.x
     */
    public SqlBuilder sql() {
        return createSqlBuilder().autoAppendTableAlias(true);
    }

    /**
     * 根据主键获取某个字段值。
     * @param id
     * @param propertyName 属性名（非字段名）
     * @param propertyType 属性值的类型，如：String.class
     * @param <P>
     * @return
     * @since 2.x
     */
    public <P> P getPropertyValue(Object id, String propertyName, Class<P> propertyType) {
        String col = getColumn(propertyName);
        String sql = sql("select " + col + " from @tableName where @id = ?").toSql();
        try {
            return jdbc.queryForObject(sql, propertyType, id);
        } catch(EmptyResultDataAccessException e) {
            return null;
        }
    }

    /**
     * 根据主键更新某个字段的值。
     * @param id
     * @param property 属性名（非字段名）
     * @param value
     * @since 2.x
     */
    public void update(Object id, String property, Object value) {
        String column = getColumn(property);
        BeanInfo i = getBeanInfo();
        if(column != null) {
            String sql = String.format("update %s set %s = ? where %s = ?", i.getTableName(), column, i.getPrimaryColumn());
            jdbc.update(sql, value, id);
        }
    }

    /**
     * 更新数据模型，如果模型的属性值为空，则会更新字段为空。
     * @param bean
     * @since 2.x
     */
    public void updateIncludeNullData(T bean) {
        update(bean, true);
    }

    /**
     * 查询表的所有记录，谨慎使用。
     * @return
     * @since 2.x
     */
    public List<T> queryAll() {
        return query(sql("select * from @tableName").toSql(), getRowMapper());
    }
}
