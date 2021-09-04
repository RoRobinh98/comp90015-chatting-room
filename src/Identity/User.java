package Identity;

/**
 * @author JiazheHou
 * @date 2021/9/4
 * @apiNote
 */
public class User {
    private String identity;
    private String hostName;
    private int port;

    public User(String identity, String hostName, int port) {
        this.identity = identity;
        this.hostName = hostName;
        this.port = port;
    }

    public String getIdentity() {
        return identity;
    }

    public void setIdentity(String identity) {
        this.identity = identity;
    }
}
