package jsonFile;

import java.util.ArrayList;

/**
 * @author JiazheHou
 * @date 2021/9/4
 * @apiNote
 */
public class General {
    private String type;
    private String identity;
    private String former;
    private String roomid;
    private ArrayList<String> identities;
    private String owner;
    private ArrayList<String> rooms;
    private String content;

    public General(String type) {
        this.type = type;
    }

    public General() {
    }

    public String getType() {
        return type;
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

    public String getRoomid() {
        return roomid;
    }

    public void setRoomid(String roomid) {
        this.roomid = roomid;
    }

    public ArrayList<String> getIdentities() {
        return identities;
    }

    public void setIdentities(ArrayList<String> identities) {
        this.identities = identities;
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    public ArrayList<String> getRooms() {
        return rooms;
    }

    public void setRooms(ArrayList<String> rooms) {
        this.rooms = rooms;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }
}
