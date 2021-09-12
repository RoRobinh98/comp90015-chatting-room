package jsonFile;

import Identity.ChatRoom;

import java.util.ArrayList;
import java.util.List;

/**
 * @author JiazheHou
 * @date 2021/9/12
 * @apiNote
 */
public class Room {
    private String roomId;
    private int count;

    public Room(String roomId, int count) {
        this.roomId = roomId;
        this.count = count;
    }

    public String getRoomId() {
        return roomId;
    }

    public void setRoomId(String roomId) {
        this.roomId = roomId;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }

    public static List<Room> fromChatRoomToRoom(ArrayList<ChatRoom> chatRooms) {
        //TODO
        return null;
    }
}
