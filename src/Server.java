import Identity.ChatRoom;
import Identity.User;
import com.google.gson.Gson;
import jsonFile.General;
import jsonFile.Room;
import jsonFile.Types;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author JiazheHou
 * @date 2021/9/1
 * @apiNote
 */
public class Server {
    private static final String MAINHALL = "MainHall";
    private boolean handler_alive = false;
    private ArrayList<User> users;
    private ArrayList<ChatRoom> chatRooms;
    private volatile List<ChatConnection> connectionList;

    public static void main(String[] args) {
        CmdCommand cmdCommand = new CmdCommand();
        CmdLineParser parser = new CmdLineParser(cmdCommand);

        try {
            parser.parseArgument(args);

            new Server().handle(cmdCommand.portNum);
        } catch (CmdLineException e) {
            System.out.println("command lien error");
            e.printStackTrace();
        }

    }

    public static class CmdCommand {
        @Option(name = "-p", usage = "client port number")
        public int portNum = 4444;
    }

    public Server() {
        users = new ArrayList<>();
        chatRooms = new ArrayList<>();
        chatRooms.add(new ChatRoom(MAINHALL));
        ChatRoom.selectById(chatRooms,MAINHALL).setOwner("");
        connectionList = new ArrayList<>();
    }

    public void handle(int portNum) {
        ServerSocket serverSocket;
        try {
            serverSocket = new ServerSocket(portNum);

            System.out.printf("Listening on port %d\n", portNum);
            handler_alive = true;

            while (handler_alive) {
                Socket newSocket = serverSocket.accept();
                ChatConnection conn = new ChatConnection(newSocket);

                if (conn != null) {
                    System.out.printf("Accepted new connection from %s:%d\n", newSocket.getLocalAddress().getCanonicalHostName(), newSocket.getPort());
                    enter(conn);
                    conn.start();
                    conn.interrupt();
                } else {
                    handler_alive = false;
                }

            }
        } catch (IOException e) {
            System.out.printf("Error handling conns, %s\n", e.getMessage());
            handler_alive = false;
        }
    }

    private void enter(Server.ChatConnection connection) {
        connectionList.add(connection);
    }

    private void leave(Server.ChatConnection connection) {
        connectionList.remove(connection);
        users.remove(connection.user);
    }

    private synchronized void broadCast(String message, Server.ChatConnection ignored) {
        for (Server.ChatConnection connection : connectionList) {
            if (ignored == null || !ignored.equals(connection)) {
                connection.sendMessage(message);
            }
        }
    }

    private synchronized void broadCast(String message, ArrayList<ChatRoom> chatRoom, String roomName) {
        ChatRoom room = ChatRoom.selectById(chatRoom, roomName);
        for (User user : room.getRoomUsers()) {
            getByUser(connectionList,user).sendMessage(message);
        }
    }

    private ChatConnection getByUser(List<ChatConnection> chatConnections,User user){
        for(ChatConnection connection : chatConnections){
            if(connection.user.equals(user)){
                return connection;
            }
        }
        return null;
    }

    private synchronized void checkEmptyRoom(){
        for(ChatRoom chatRoom : chatRooms){
            if(chatRoom.getOwner().equals("") && !chatRoom.getId().equals(MAINHALL) && chatRoom.getRoomUsers().size()==0){
                chatRooms.remove(chatRoom);
            }
        }
    }

    class ChatConnection extends Thread {
        private Socket socket;
        private PrintWriter writer;
        private BufferedReader reader;
        private boolean connection_alive;
        private Gson gson = new Gson();
        private User user;

        public ChatConnection(Socket socket) throws IOException {
            this.socket = socket;
            this.reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            this.writer = new PrintWriter(socket.getOutputStream());
        }

        public void initialize() {
            String tempName = getTempIdentity(users);
            user = new User(tempName, socket.getLocalAddress().getCanonicalHostName(), socket.getPort());
            users.add(user);
            ChatRoom.selectById(chatRooms, MAINHALL).addRoomUser(user);
            user.setRoomid(MAINHALL);

            General newidentity = new General(Types.NEWIDENTITY.type);
            newidentity.setIdentity(tempName);
            newidentity.setFormer("");
            System.out.println(gson.toJson(newidentity));
            writer.print(gson.toJson(newidentity));
            writer.println();
            writer.flush();
        }

        public void close() {
            try {
                leave(this);
                reader.close();
                writer.close();
                socket.close();
            } catch (IOException e) {
                System.out.println("Error closing conn");
            }
        }

        public void run() {
            connection_alive = true;
            initialize();
            while (connection_alive) {
                try {
                    String inputLine = reader.readLine();
                    General message = gson.fromJson(inputLine, General.class);
                    if (inputLine == null) {
                        connection_alive = false;
                    } else if (Types.IDENTITYCHANGE.type.equals(message.getType())) {
                        identityChange(message);
                    } else if (Types.CREATEROOM.type.equals(message.getType())) {
                        createRoom(message);
                    } else if (Types.DELETE.type.equals(message.getType())){
                        deleteRoom(message);
                    } else if(Types.JOIN.type.equals(message.getType())) {
                        joinRoom(message);
                    } else if(Types.WHO.type.equals(message.getType())) {
                        replyForWho(message);
                    } else if(Types.LIST.type.equals(message.getType())) {
                        General roomList = new General(Types.ROOMLIST.type);
                        roomList.setRooms(Room.fromChatRoomToRoom(chatRooms));
                        sendMessage(gson.toJson(roomList));
                    } else if(Types.QUIT.type.equals(message.getType())){
                        checkEmptyRoom();
                        replyForQuit();
                        connection_alive = false;
                    } else if(Types.MESSAGE.type.equals(message.getType())){
                        broadcastMessage(message);
                    }
                } catch (IOException e) {
                    connection_alive = false;
                }
            }
            close();
        }

        public synchronized void sendMessage(String message) {
            writer.print(message);
            writer.println();
            writer.flush();
        }

        String getTempIdentity(ArrayList<User> userList) {
            int i = 1;
            List<Integer> usedNum = userList.stream().map(User::getIdentity).map(identity -> Integer.parseInt(identity.substring(5))).collect(Collectors.toList());
            while (true) {
                if (usedNum.contains(i)) {
                    i++;
                } else
                    break;
            }
            return "guest" + i;
        }

        public synchronized void identityChange(General inputLine){

            Boolean flag = true;
            General message = new General(Types.NEWIDENTITY.type);
            String tempName = inputLine.getFormer();
            String newIdentity = inputLine.getIdentity();
            for(User user:users){
                if(user.getIdentity().equals(newIdentity)){
                    flag = false;
                }
            }
            if (!flag) {
                message.setFormer(tempName);
                message.setIdentity(tempName);
                sendMessage(gson.toJson(message));
            } else {
                message.setFormer(tempName);
                message.setIdentity(newIdentity);
                this.user.setFormer(tempName);
                this.user.setIdentity(newIdentity);
                sendMessage(gson.toJson(message));
                message.setContent(tempName + " is now " + newIdentity);
                broadCast(gson.toJson(message), this);
                for(ChatRoom room : chatRooms){
                    if(room.getOwner().equals(tempName)){
                        room.setOwner(newIdentity);
                    }
                }
            }
        }

        public synchronized void createRoom(General inputLine){
            String roomName = inputLine.getRoomid();
            General message = new General(Types.ROOMLIST.type);
            for (ChatRoom room : chatRooms) {
                if (room.getId().equals(roomName)) {
                    message.setContent("Room " + roomName + " is invalid or already in use.");
                    message.setRooms(Room.fromChatRoomToRoom(chatRooms));
                    sendMessage(gson.toJson(message));
                    return;
                }
            }
            chatRooms.add(new ChatRoom(roomName,this.user.getIdentity()));
            ChatRoom.selectById(chatRooms,roomName).setOwner(this.user.getIdentity());
            message.setContent("Room " + roomName + " created");
            message.setRooms(Room.fromChatRoomToRoom(chatRooms));
            sendMessage(gson.toJson(message));
        }

        public synchronized void deleteRoom(General inputLine){
            String userName = this.user.getIdentity();
            String roomID = inputLine.getRoomid();
            ChatRoom room = ChatRoom.selectById(chatRooms,roomID);

            if(!room.getOwner().equals(userName)){
                General failToDelete = new General(Types.MESSAGE.type);
                failToDelete.setContent("The requested room is invalid or non existent");
                sendMessage(gson.toJson(failToDelete));
                return;
            }


            for(User subUser: room.getRoomUsers()){
                String targetRoomId = MAINHALL;
                    General successfulChange = new General(Types.ROOMCHANGE.type);
                    successfulChange.setIdentity(subUser.getIdentity());
                    successfulChange.setFormer(subUser.getRoomid());
                    successfulChange.setRoomid(targetRoomId);
                    getByUser(connectionList,subUser).sendMessage(gson.toJson(successfulChange));
                    ChatRoom.selectById(chatRooms,targetRoomId).addRoomUser(subUser);
                    subUser.setRoomid(targetRoomId);
            }
            chatRooms.remove(room);
            General roomList = new General(Types.ROOMLIST.type);
            roomList.setRooms(Room.fromChatRoomToRoom(chatRooms));
            sendMessage(gson.toJson(roomList));
        }

        public synchronized void broadcastMessage(General inputLine) {
            General message = new General(Types.MESSAGE.type);
            message.setIdentity(this.user.getIdentity());
            message.setContent(inputLine.getContent());
            broadCast(gson.toJson(message), chatRooms, this.user.getRoomid());
        }

        public synchronized void joinRoom(General inputLine) {
            String targetRoomId = inputLine.getRoomid();
            if(!chatRooms.stream().map(ChatRoom::getId).collect(Collectors.toList()).contains(targetRoomId)) {
                General failToChange = new General(Types.ROOMCHANGE.type);
                failToChange.setIdentity(this.user.getIdentity());
                failToChange.setFormer(this.user.getRoomid());
                failToChange.setRoomid(this.user.getRoomid());
                sendMessage(gson.toJson(failToChange));
            }
            else {
                General successfulChange = new General(Types.ROOMCHANGE.type);
                successfulChange.setIdentity(this.user.getIdentity());
                successfulChange.setFormer(this.user.getRoomid());
                successfulChange.setRoomid(targetRoomId);
                broadCast(gson.toJson(successfulChange), chatRooms, this.user.getRoomid());
                broadCast(gson.toJson(successfulChange), chatRooms, targetRoomId);
                ChatRoom.selectById(chatRooms,this.user.getRoomid()).removeRoomUser(this.user);
                ChatRoom.selectById(chatRooms,targetRoomId).addRoomUser(this.user);
                this.user.setRoomid(targetRoomId);

                if (targetRoomId.equals(MAINHALL)) {
                    General mainHallContent = new General(Types.ROOMCONTENTS.type);
                    mainHallContent.setRoomid(MAINHALL);
                    mainHallContent.setIdentities(getRoomUserIds(ChatRoom.selectById(chatRooms, MAINHALL)));
                    mainHallContent.setOwner(ChatRoom.selectById(chatRooms, MAINHALL).getOwner());
                    General roomList = new General(Types.ROOMLIST.type);
                    roomList.setRooms(Room.fromChatRoomToRoom(chatRooms));
                    sendMessage(gson.toJson(mainHallContent));
                    sendMessage(gson.toJson(roomList));
                }
            }
        }

        public synchronized void replyForWho(General inputLine){
            String targetRoomId = inputLine.getRoomid();
            General reply = new General(Types.ROOMCONTENTS.type);
            if(!chatRooms.stream().map(ChatRoom::getId).collect(Collectors.toList()).contains(targetRoomId)){
                reply.setRoomid("");
                sendMessage(gson.toJson(reply));
                return;
            }
            ArrayList<String> userIds = new ArrayList<>();
            ChatRoom chatRoom = ChatRoom.selectById(chatRooms, targetRoomId);
            userIds.addAll(chatRoom.getRoomUsers().stream().map(User::getIdentity).collect(Collectors.toList())) ;
            reply.setRoomid(targetRoomId);
            reply.setIdentities(userIds);
            reply.setOwner(chatRoom.getOwner());
            sendMessage(gson.toJson(reply));
        }

        public synchronized void replyForQuit(){
            General quitEntity = new General(Types.ROOMCHANGE.type);
            quitEntity.setIdentity(this.user.getIdentity());
            quitEntity.setFormer(this.user.getRoomid());
            quitEntity.setRoomid("");
            broadCast(gson.toJson(quitEntity),chatRooms,this.user.getRoomid());
            for(ChatRoom chatRoom:chatRooms){
                if(chatRoom.getOwner().equals(this.user.getIdentity())){
                    chatRoom.setOwner("");
                }
            }
            ChatRoom currentRoom = ChatRoom.selectById(chatRooms,this.user.getRoomid());
            currentRoom.removeRoomUser(this.user);
            if(currentRoom.getRoomUsers().size() == 0 && currentRoom.getOwner().equals("") && !currentRoom.getId().equals(MAINHALL)){
                chatRooms.remove(currentRoom);
            }
        }

        ArrayList<String> getRoomUserIds(ChatRoom chatRoom){
            ArrayList<String> result = new ArrayList<>();
            result.addAll(chatRoom.getRoomUsers().stream().map(User::getIdentity).collect(Collectors.toList()));
            return result;
        }

    }
}
