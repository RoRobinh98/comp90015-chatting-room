package Identity;

/**
 * @author JiazheHou
 * @date 2021/9/4
 * @apiNote
 */
public class User {
    private String identity;
    private String hostName;
    private String former; //new added 2021/9/6
    private int port;

    public User(String identity, String hostName, int port) {
        this.identity = identity;
        this.hostName = hostName;
        this.port = port;
        this.former = "";
    }

    public String getIdentity() {
        return identity;
    }

    public void setIdentity(String identity) {
        this.identity = identity;
    }

    public String getFormer() {
        return former;
    }

    public void setFormer(String former) {
        this.former = former;
    }
}
