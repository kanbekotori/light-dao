import com.jxs.ld.BaseDao;
import com.jxs.ld.sql.SqlBuilder;
import org.apache.commons.lang3.StringUtils;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import java.sql.DriverManager;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by jiangxingshang on 15/11/24.
 *
 * @author jiangxingshang
 * @version $Id: $
 * @since 1.0
 */
public class UserDao extends BaseDao<User> {

    /**
     * <p>Constructor for UserDao.</p>
     */
    public UserDao() {
        super(User.class);
    }

    /**
     * <p>main</p>
     *
     * @param a an array of {@link java.lang.String} objects.
     * @throws java.lang.Exception if any.
     */
    public static void main(String[] a) throws Exception {
//        DriverManagerDataSource ds = new DriverManagerDataSource();
//        ds.setUrl("jdbc:mysql://localhost:3306/test");
//        ds.setDriverClassName("com.mysql.jdbc.Driver");
//        ds.setUsername("root");
//        ds.setPassword("admin");
//        UserDao dao = new UserDao();
//        dao.setDataSource(ds);
////        User u = dao.getById("123456");
////        System.out.println(u);
////        u.setAge(4);
////        dao.update(u);
//        User u = new User();
//        u.setUsername("sizuru");
//        u.setPassword("123");
//        u.setName("啊");
//        u.setAge(10);
//        dao.insert(u);
        SqlBuilder sql = new SqlBuilder()
                .sql("select * from t_user")
                .where("name like :my_name", true, "Ko%")
                .and("age = :age and a = :ff", true, 15);
        for(String key : sql.getValueMap().keySet()) {
            System.out.println(key + " : " + sql.getValueMap().get(key));
        }
    }
}
