package Identity;

import java.util.ArrayList;

/**
 * @author JiazheHou
 * @date 2021/9/4
 * @apiNote
 */
public class ChatRoom {
    private String id;
    private ArrayList<User> roomUsers = new ArrayList<>();
    private String owner;

    public ChatRoom(String id) {
        this.id = id;
        this.roomUsers = new ArrayList<>();
    }

    public ChatRoom(String id, String owner){
        this.id = id;
        this.owner = owner;
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public ArrayList<User> getRoomUsers() {
        return roomUsers;
    }

    public void setRoomUsers(ArrayList<User> roomUsers) {
        this.roomUsers = roomUsers;
    }

    public void addRoomUser(User user){
        this.roomUsers.add(user);
    }

    public void removeRoomUser(User user){
        this.roomUsers.remove(user);
    }

    public static ChatRoom selectById(ArrayList<ChatRoom> chatRooms, String id){
        for (ChatRoom chatRoom:chatRooms){
            if(chatRoom.getId().equals(id)){
                return chatRoom;
            }
        }
        return null;
    }
}
