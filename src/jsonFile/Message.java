package jsonFile;

/**
 * @author JiazheHou
 * @date 2021/9/4
 * @apiNote
 */
public class Message {
    String type = Types.MESSAGE.type;
    String content;

    public Message(String content) {
        this.content = content;
    }
}
