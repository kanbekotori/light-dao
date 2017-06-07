light-dao是一个基于spring的JdbcTemplate模块封装的DAO层辅助框架，主要用于构建中小型项目，解决DAO层对数据库的处理（实体插入，更新，删除，动态条件查询语句构建，数据自动填充）。

# Demo

## Model

```java
@TableName("t_user")
public class User {
	@Column(primaryKey = true)
	private String id;
	private String username;
	private String password;
	private Integer age;
	private String name;
	private String roleId;
	@IgnoreColumn({IgnoreColumn.UPDATE, IgnoreColumn.INSERT})
	private String roleName;
	...
}
```

## Dao

```java
public class UserDao extend BaseDao<User> {

	@Autowire
	private RoleDao roleDao;

	public UserDao() {
		super(User.class);
	}
	
	public User get(String username, String password) {
		String sql = sql("select * from @tableName where @username = ? and @password = ?").toSql();
		return super.getOne(sql, getRowMapper(), username, password);
	}
	
	//与角色表连接查询，并动态添加name和age条件
	@Override
	public Page<User> query(Page<User> page) {
        	ParamKit p = new ParamKit(page.getParams());
        	SqlBuilder builder =
                	sql("select u.*, $r.name role_name from @tableName u")
			.sql("left join $r on $r.id = u.@roleId")
	                .where("@name like ?", p.has("name"), p.asStr("name", "%", "%"))
			.and("@age > ?", p.has("age"), p.asInt("age"))
			.addBeanInfo("r", roleDao.getBeanInfo());

        	return super.query(page, getRowMapper(), builder, builder.getValueArray());
    	}
}
```

## Service

```java
public class UserService {
	@Autowire
	private UserDao dao;
	
	public void add(User user) {
		dao.insert(user);
	}
	
	public void update(User user) {
		dao.update(user);
	}
	
	public void updatePassword(String id, String pwd) {
		dao.update(id, "password", pwd);//便捷的属性更新方法
	}
}
```

# SQL构建工具
你可以使用`SqlBuilder`来辅助你构建sql语句，它能帮你解决数据库表结构的硬编码和条件组装查询问题。

## 基本用法

```java
String sql = new SqlBuilder().sql("select * from t_user").toSql();
```

这看起来没什么意义，如果你想构建这么个sql那么你确实没必要使用`SqlBuilder`。

## 变量
如果你不想在sql语句中硬编码表名或字段等信息，你可以像下面这样使用。

```java
Map<String, String> map = new HashMap<>();
map.put("username", "f_username");
map.put("password", "f_password");
String sql = new SqlBuilder(map)
	.addVar("tableName", "t_user")
	.sql("select * from @tableName where @username=? and @password=?")
	.toSql();
```

在sql语句中凡是使用`@`符号标注的都会被替换，你可以在创建`SqlBuilder`时提供一个map，key表示变量名，value表示被替换的值，你也可以通过`addVar(String, String)`方法增加变量。

**“那map的值我还是硬编码了呀！”**。通常实体对应的表字段和表名已经提供了工具类获取了，在下面的章节我们会详细讲这部分内容，目前你只需要知道怎么使用变量即可。

## 分组变量
分组变量是为了解决有同名变量冲突的问题，为什么会有同名变量呢？通常我们在做连表查询的时候，会用到其他DAO的变量（从`BaseDao`中我们可以得到一个表结构的map映射），所以当两个表有同名字段的时候也就意味着有同名的变量。

```java
//Deprecated
Map<String, String> map1 = new HashMap<>();
map1.put("tableName", "t_product");
map1.put("categoryId", "category_id");
map1.put("name", "name");

Map<String, String> map2 = new HashMap<>();
map2.put("tableName", "t_product_category");
map2.put("name", "name");
map2.put("id", "id");

new SqlBuilder()
	.addVar(map1)//默认的变量集合
	.addVar("a", map2)//分组a的变量集合
	.sql("select p.*, c.@a!name category_name from @tableName p left join @a!tableName c on p.@categoryId = p.@a!id")
	.toSql();
```

可以发现有些变量中有一个"!"感叹号，这个感叹号的左边就是分组的key，右边是变量集合的变量名，所以带有"a!"的变量都会从map2中查找，而不会从map1中查找。

上面的使用方式已经放弃，从1.4版本开始使用下面的方式，简洁了其他表属性的引用。
```java
new SqlBuilder()
	.addBeanInfo("c", categoryDao.getBeanInfo())
	.autoAppendTableAlias(true)
	.sql("select p.*, $c.name category_name from @tableName p left join $c on $c.id = p.@categoryId");
```

## 条件查询

```java
Map<String, String> map = ...;
map.put("age", 18);
ParamKit p = new ParamKit(map);
SqlBuilder sb = new SqlBuilder(map)
	.sql("select * from @tableName")
	.where("@age > ?", p.has("age"), p.asInt("age"))
	.and("@sex = ?", p.has("sex"), p.asStr("sex"));
String sql = sb.toSql();
Object[] params = sql.getValueArray();
//select * from t_user where age > ?
//params只有一个age参数值。
```

以上代码帮你解决了if语句和处理sql的拼接等麻烦的问题。

## 命名式参数

```java
SqlBuilder sb = new SqlBuilder(map)
	.sql("select * from @tableName")
	.where("@age > :age", age != null, age)
	.and("@sex = :sex", "m".equals(sex) || "f".equals(sex), sex);
Map<String, Object> params = sb.getValueMap();
//使用namedJdbc来处理
```

## 表名
```java
new SqlBuilder().addVar("tableName", "t_user").sql("select * from @tableName");
```

或

```java
new SqlBuilder().sql("select * from").table("t_user");
```

或

```java
new SqlBuilder().sql("select * from").table(User.class);
```


第三种的方式你的`User`必须要添加一个`@TableName`注解。

## 排序
```java
SqlBuilder sb = ...;
sb.order("@age", "-@birthday");
```

上面的排序的意思是按照年龄升序然后降序出生日期，未带"-"号的表示升序，带有"-"号的表示降序。

## 分页查询
`SqlBuilder`目前只支持mysql的分页查询。

```java
String sql = new SqlBuilder().sql(...).toSql(10, 20);
//select * from (select * from xxx) limit 10, 20
```

## 在sql中直接访问实体信息
在**分组变量**中提到如何简化变量的设置问题，但在sql中使用分组变量会导致sql阅读困难，使用"$"符号可以解决这个问题，看例子：

```java
new SqlBuilder()
	.addBeanInfo("e", this.getBeanInfo())
	.autoAppendTableAlias(true)
	.sql("select * from $e where $e.username = ? and $e.password = ?")
```

"$"符号和"."符号之间的字符表示实体变量，"."符号后面的表示属性名，如果你单单使用`$e`则表示表名。

## DAO
你只需要继承`BaseDao`就可以为你实现基础的CRUD，在`BaseDao`中使用了`@Autowire`注入一个`javax.sql.DataSource`，所以在你的应用中需要有一个数据源实现。
**注：从2.x版本开始，要求你的应用有一个JdbcTemplate的bean，而不是javax.sql.DataSource实现了。**

```java
    @Autowired
    public void setJdbcTemplate(JdbcTemplate jdbcTemplate) {
        jdbc = jdbcTemplate;
        ...
    }
```

```java
public class UserDao extends BaseDao<User> {
  public UserDao() {
    super(User.class);
  }
  
  @Override
  public Page<User> query(Page<User> page) {
    SqlBuilder sqlBuilder = sql("select * from @tableName");
    return super.query(page, getRowMapper(), sqlBuilder);
  }
}
```

你必须提供一个beanclass给父类，这样`BaseDao`才能知道怎么处理这个`Bean`的信息。你需要实现自己的查询方法提供给业务层，因为很少情况会出现无条件查询，所以`BaseDao`不给出这样的一个`query`方法，但提供了很多便捷的查询方法。

查询方法都需要你提供一个`RowMapper`，这样`BaseDao`才能将行记录转换成实体，你可以从`BaseDao#getRowMapper()`获取到一个默认的mapper，这个mapper会简单的填充行记录到实体上，空值会忽略，你可以提供你自己的mapper。比如你做了连表查询，多出了一个额外字段。

```java
RowMapper<User> mapper = super.createRowMapper(new BeanSetter<User>() {
  @Override
  public void bean(User bean, ResultSet rs, int rowNum) {
    //此时的bean已经设置了基本的属性
    //设置额外的属性
  }
});
super.query(sql, mapper);
```

### 在DAO中使用SqlBuilder
当在DAO中写sql时，我建议你使用`sql()`来获取一个sql构建器，这个构建器已经具备了实体的属性变量和表名变量。

```java
SqlBuilder sb = sql("select * from @tableName where @username = ? and @password = ?");
```

上面的代码中，`tableName`是固定为实体的表名变量，其他则跟你在实体中定义的属性有关。

**从2.x开始，提供了一个简化的sql方法，并设置了`.autoAppendTableAlias(true)`。**

### 在DAO中使用SqlBuilder做连表查询

```java
SqlBuilder builder = sql("select p.*, $c.name category_name from @tableName p left join $c on $c.id = p.@categoryId")
	.addBeanInfo("c", categoryDao.getBeanInfo());
```

使用其他DAO获取属性到字段的映射作为变量添加到sql构建器中。

### ID生成
`BaseDao`提供了自增id和uuid的实现，你只需要在实体的主键上加上`@Column(idGenerator = IdGenerator.AUTO_INCREMENT)`就可以实现id自增，前提是你的主键是个整数类型，在调用`BaseDao#insert(Object)`方法时，会根据ID生成器类型生成id值，不过目前也就支持自增和uuid而已，第三种就是你自己分配值。

### 实体（Entity）
在使用dao之前我们得先为实体和表做映射，我们的映射方式是在实体属性上标注注解。

```java
@TableName("t_user")
public class User {
  @Column(primaryKey=true)
  private String id;
  private String username;
  @Column("pwd")
  private String password;
  @Column(columnType = java.sql.Date.class)
  private Date birthday;
  @IgnoreColumn
  private String leaderName;
  //getter and setter
}
```

如果你的属性和字段名称一样，那么可以不用为属性添加`@Column`注解，没有`@Column`注解的属性都按照驼峰下划线分割方式来做对应关系，如：userName(属性) -> user_name(字段)。

每个实体都应该有一个业务无关的主键，你需要明确给某个属性标识为“主键”，使用`@Column(primaryKey = true)`来将某个属性标识为主键。

如果你想忽略某个属性的映射关系，那么可以使用`@IgnoreColumn`标注，这样`BaseDao`在做CRU操作时会忽略这个属性，其实在应用启动后`BaseDao`就会将bean的信息缓存在map中，但不会缓存被`@IgnoreColumn`的属性。

在查询的时候，`BaseDao`会帮你将行数据填充到实体，从`ResultSet`获取值的时候使用的是`getObject(String)`方法，但某些时候无法获取到正确的类型，这使用你需要在注解里指定字段的类型。
```java
@Column(columnType = java.sql.Timestamp.class)
private Date regTime;
```
