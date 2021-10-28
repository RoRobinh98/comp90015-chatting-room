package jsonFile;

import java.util.ArrayList;
import java.util.List;

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
    private ArrayList<Room> rooms;
    private String content;
    private String host;
    private List<String> neighbors;

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

    public ArrayList<Room> getRooms() {
        return rooms;
    }

    public void setRooms(ArrayList<Room> rooms) {
        this.rooms = rooms;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public List<String> getNeighbors() {
        return neighbors;
    }

    public void setNeighbors(List<String> neighbors) {
        this.neighbors = neighbors;
    }

    @Override
    public String toString() {
        return "General{" +
                "type='" + type + '\'' +
                ", identity='" + identity + '\'' +
                ", former='" + former + '\'' +
                ", roomid='" + roomid + '\'' +
                ", identities=" + identities +
                ", owner='" + owner + '\'' +
                ", rooms=" + rooms +
                ", content='" + content + '\'' +
                '}';
    }
}
