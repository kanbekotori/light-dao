import com.jxs.ld.bean.Column;
import com.jxs.ld.bean.IgnoreColumn;
import com.jxs.ld.bean.IgnoreColumnType;
import com.jxs.ld.bean.TableName;

/**
 * Created by jiangxingshang on 15/11/24.
 *
 * @author jiangxingshang
 * @version $Id: $
 * @since 1.0
 */
@TableName("t_user")
public class User {

    @Column(primaryKey = true, UUIDLength = 8)
    private String id;
    @IgnoreColumn(value = IgnoreColumnType.INSERT)
    private String username;
    private String password;
    private String name;
    @IgnoreColumn(IgnoreColumnType.UPDATE)
    private Integer age;

    /**
     * <p>Getter for the field <code>id</code>.</p>
     *
     * @return a {@link java.lang.String} object.
     */
    public String getId() {
        return id;
    }

    /**
     * <p>Setter for the field <code>id</code>.</p>
     *
     * @param id a {@link java.lang.String} object.
     */
    public void setId(String id) {
        this.id = id;
    }

    /**
     * <p>Getter for the field <code>username</code>.</p>
     *
     * @return a {@link java.lang.String} object.
     */
    public String getUsername() {
        return username;
    }

    /**
     * <p>Setter for the field <code>username</code>.</p>
     *
     * @param username a {@link java.lang.String} object.
     */
    public void setUsername(String username) {
        this.username = username;
    }

    /**
     * <p>Getter for the field <code>password</code>.</p>
     *
     * @return a {@link java.lang.String} object.
     */
    public String getPassword() {
        return password;
    }

    /**
     * <p>Setter for the field <code>password</code>.</p>
     *
     * @param password a {@link java.lang.String} object.
     */
    public void setPassword(String password) {
        this.password = password;
    }

    /**
     * <p>Getter for the field <code>name</code>.</p>
     *
     * @return a {@link java.lang.String} object.
     */
    public String getName() {
        return name;
    }

    /**
     * <p>Setter for the field <code>name</code>.</p>
     *
     * @param name a {@link java.lang.String} object.
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * <p>Getter for the field <code>age</code>.</p>
     *
     * @return a {@link java.lang.Integer} object.
     */
    public Integer getAge() {
        return age;
    }

    /**
     * <p>Setter for the field <code>age</code>.</p>
     *
     * @param age a {@link java.lang.Integer} object.
     */
    public void setAge(Integer age) {
        this.age = age;
    }
}
