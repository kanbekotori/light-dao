package com.jxs.ld;

import com.jxs.ld.bean.BeanInfo;
import com.jxs.ld.bean.Beans;
import com.jxs.ld.bean.IdGenerator;
import com.jxs.ld.sql.SqlBuilder;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
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
 * Created by jiangxingshang on 15/11/23.
 */
public abstract class BaseDao<T> {

    private static Logger log = Logger.getLogger(BaseDao.class);

    protected interface BeanSetter<T> {
        void bean(T bean, ResultSet rs, int rowNum) throws SQLException;
    }

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
     * 实体的属性->字段映射，key是属性名，value是表字段。
     */
    protected Map<String, String> propertiesMapper;
    /**
     * 实体的字段->属性映射，key是表字段，value是属性。
     */
    protected Map<String, String> columnsMapper;
    /**
     * 实体属性对应字段的类型。
     */
    protected Map<String, Class<?>> columnTypes;
    protected BeanInfo beanInfo;

    protected BaseDao(final Class<T> beanClass) {
        this.beanClass = beanClass;
        propertiesMapper = Beans.getMapper(beanClass);
        columnTypes = new HashMap<>();
        for(String prop : propertiesMapper.keySet()) {
            columnTypes.put(prop, Beans.getColumnType(beanClass, prop));
        }
        columnsMapper = Beans.reverse(propertiesMapper);
        beanInfo = new BeanInfo(beanClass);
        defaultRowMapper = createRowMapper(null);
        SQL_GET_BY_ID = String.format("select * from %s where %s = ?", beanInfo.getTableName(), beanInfo.getPrimaryColumn());

        //debug
        log.debug("=================== " + beanClass.getSimpleName() + "(" + beanInfo.getTableName() + ") ====================");
        log.debug("Primary key -> " + beanInfo.getPrimaryColumn());
        for(Map.Entry<String, String> entry : propertiesMapper.entrySet()) {
            log.debug(entry.getKey() + " -> " + entry.getValue());
        }
        log.debug("=============================================================");
    }

    protected RowMapper<T> getRowMapper() {
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

    public Map<String, String> getPropertiesMapper() {
        return propertiesMapper;
    }

    public String getColumn(String property) {
        return propertiesMapper.get(property);
    }

    /**
     * 创建一个行到实体的转换器，这个转换器默认会将字段填充到实体中，如果你提供了{@link com.jxs.ld.BaseDao.BeanSetter}，
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
        Object idValue = null;
        if(IdGenerator.AUTO_INCREMENT == idg) {
            idValue = insert.executeAndReturnKey(map);
        } else if(IdGenerator.UUID == idg) {
            idValue = uuid();
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

    protected T getOne(String sql, RowMapper<T> mapper, Object...values) {
        try {
            return jdbc.queryForObject(sql, mapper, values);
        } catch(EmptyResultDataAccessException e) {
            return null;
        }
    }

    protected int getCount(String sql, Object...values) {
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
    protected Page<T> query(Page<T> page, RowMapper<T> mapper, SqlBuilder sqlBuilder, Object...values) {
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
    protected Page<T> query(Page<T> page, MapSqlParameterSource parameters, RowMapper<T> mapper, SqlBuilder sqlBuilder) {
        int total = namedJdbc.queryForObject(sqlBuilder.toSqlCount(), parameters, Integer.class);
        if(total == 0) {
            page.setTotal(0);
            page.setData(new LinkedList<T>());
        } else {
            List<T> list = namedJdbc.query(sqlBuilder.toSql(page.getStart(), page.getLimit()), parameters, mapper);
            page.setTotal(total);
            page.setData(list);
        }
        return page;
    }

    protected Collection<T> query(String sql, RowMapper<T> mapper, Object...values) {
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
    protected Collection<T> query(int max, String sql, RowMapper<T> mapper, Object...values) {
        return jdbc.query(new SqlBuilder().sql(sql).toSql(0, max), mapper, values);
    }

    public Page<T> query(Page<T> page) {
        throw new RuntimeException("Empty query implements.");
    }

    /**
     * 创建一个包含{@link #beanClass}对应关系（属性->字段）和一个表名变量（tableName）的SQL构建器。
     * 这个构建器里已经有了{@linkplain #propertiesMapper}和{@linkplain #beanInfo}的表名的变量。
     * @param propertiesMapper 提供给{@link SqlBuilder}的变量。
     * @return
     * @see #propertiesMapper
     * @see {@linkplain BeanInfo#getTableName()}
     */
    public SqlBuilder createSqlBuilder(Map<String, String>...propertiesMapper) {
        return new SqlBuilder(this.propertiesMapper)
                .addVar(propertiesMapper)
                .addVar("tableName", beanInfo.getTableName());
    }
}
