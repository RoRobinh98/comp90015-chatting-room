package jsonFile;

/**
 * @author JiazheHou
 * @date 2021/9/4
 * @apiNote
 */
public class NewIdentity {
    private String type = Types.NEWIDENTITY.type;
    private String former;
    private String identity;

    public NewIdentity(String former, String identity) {
        this.former = former;
        this.identity = identity;
    }

    public NewIdentity(String identity) {
        this.identity = identity;
    }

    public String getFormer() {
        return former;
    }

    public void setFormer(String former) {
        this.former = former;
    }

    public String getIdentity() {
        return identity;
    }

    public void setIdentity(String identity) {
        this.identity = identity;
    }
}
