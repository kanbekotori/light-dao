import com.jxs.ld.sql.SqlBuilder;

import java.util.HashMap;
import java.util.Map;

/**
 * @author jiangxingshang
 * @date 16/6/12
 */
public class Test {

    public static void main(String[] a) {
        String sql = new SqlBuilder()
                .addBeanInfo("u", new UserDao().getBeanInfo())
                .sql("select * from $u where $u.username = ? and $u.password = ?")
                .toSql();

        System.out.println(sql);
    }
}
