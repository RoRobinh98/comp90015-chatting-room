package jsonFile;

/**
 * @author JiazheHou
 * @date 2021/9/4
 * @apiNote
 */
public enum Types {
    MESSAGE("message"),
    NEWIDENTITY("newidentity"),
    IDENTITYCHANGE("identitychange"),
    CREATEROOM("createroom"),
    DELETEROOM("deleteroom"),
    ROOMLIST("roomlist"),
    ROOMCHANGE("roomchange"),
    JOIN("join"),
    ROOMCONTENTS("roomcontents"),
    WHO("who"),
    LIST("list"),
    QUIT("quit");

    public String type;
    Types(String type) {
        this.type = type;
    }
}
