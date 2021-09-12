import Identity.ChatRoom;
import Identity.User;
import com.google.gson.Gson;
import jsonFile.General;
import jsonFile.Types;

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
    public static final int port = 6379;
    private static final String MAINHALL = "MainHall";
    private boolean handler_alive = false;
    private ArrayList<User> users;
    private ArrayList<ChatRoom> chatRooms;
    private volatile List<ChatConnection> connectionList;

    public static void main(String[] args) {
        new Server().handle();
    }

    public Server() {
        users = new ArrayList<>();
        chatRooms = new ArrayList<>();
        chatRooms.add(new ChatRoom(MAINHALL,""));
        connectionList = new ArrayList<>();
    }

    public void handle() {
        ServerSocket serverSocket;
        try {
            serverSocket = new ServerSocket(port);

            System.out.printf("Listening on port %d\n", port);
            handler_alive = true;

            while (handler_alive) {
                Socket newSocket = serverSocket.accept();
                ChatConnection conn = new ChatConnection(newSocket);

                if (conn != null) {
                    System.out.printf("Accepted new connection from %s:%d\n", newSocket.getLocalAddress().getCanonicalHostName(), newSocket.getPort());
                    enter(conn);
                    conn.start();
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
        broadCast(String.format("%d has joined the chat", connection.socket.getPort()), null);
        connectionList.add(connection);
    }

    private void leave(Server.ChatConnection connection) {
        broadCast(String.format("%d has left the chat", connection.socket.getPort()), connection);
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
            connectionList.get(connectionList.indexOf(user)).sendMessage(message);
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
                    } else if (Types.ROOMCHANGE.type.equals(message.getType())) {
                        roomChange(message);
                    } else if(Types.JOIN.type.equals(message.getType())) {

                    }

                    else if(Types.MESSAGE.type.equals(message.getType())){
                        broadcastMessage(message);
                    }
                } catch (IOException e) {
                    connection_alive = false;
                }
            }
            close();
        }

        public void sendMessage(String message) {
            writer.print(message);
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

        /**
         * @Description: identityChange
         * @Author: Yuanyi Zhang
         * @Date: 2021/9/6
         */
        public void identityChange(General inputLine) throws IOException {
            Boolean flag = true;
            Server.ChatConnection client = null;
            General message = new General(Types.NEWIDENTITY.type);
            String tempName = inputLine.getFormer();
            String newIdentity = inputLine.getIdentity();
            for (Server.ChatConnection connection : connectionList) {
                if (connection.user.getIdentity().equals(newIdentity)) {
                    flag = false;
                    if (connection.user.getIdentity().equals(tempName)) {
                        client = connection;
                    }
                } else if (connection.user.getIdentity().equals(tempName)) {
                    client = connection;
                }
            }
            if (!flag) {
                message.setFormer("");
                message.setIdentity(tempName);
                message.setContent("Requested identity invalid or in use");
                client.sendMessage(gson.toJson(message));
            } else {
                message.setFormer(tempName);
                message.setIdentity(newIdentity);
                message.setContent(tempName + " is now " + newIdentity);
                client.user.setFormer(tempName);
                client.user.setIdentity(newIdentity);
                client.sendMessage(gson.toJson(message));
                message.setContent("User " + tempName + " has changed his username to: " + newIdentity);
                broadCast(gson.toJson(message), client);
            }

            socket.shutdownOutput();

        }

        /**
         * @Description: roomChange
         * @Author: Yuanyi Zhang
         * @Date: 9/11/2021
         */
        public void createRoom(General inputLine) throws IOException {
            Boolean flag = false;
            String roomName = inputLine.getRoomid();
            Server.ChatConnection client = null;
            System.out.println("test");
            System.out.println(socket.getPort());

            for (Server.ChatConnection connection : connectionList) {
                if (connection.socket.getPort() == (socket.getPort())) {
                    client = connection;
                }
            }

            for (ChatRoom room : chatRooms) {
                if (room.getId().equals(roomName)) {
                    General message = new General(Types.MESSAGE.type);
                    message.setContent("Room " + roomName + " is invalid or already in use.");
                    client.sendMessage(gson.toJson(message));
                    return;
                }
            }
            chatRooms.add(new ChatRoom(roomName));
            General message = new General(Types.ROOMLIST.type);
            message.setContent("Room " + roomName + " created");

            //TODO reply a ROOMLIST message
            for(ChatRoom room: chatRooms){
                System.out.println(room.getId());
                System.out.println(room.getRoomUsers().size());
            }
            client.sendMessage(gson.toJson(message));


            socket.shutdownOutput();
        }


        //TODO
        public void roomChange(General inputLine) {
            Boolean flag = false;
            General message = new General(Types.ROOMCHANGE.type);
            Server.ChatConnection client = null;
            String roomName = inputLine.getRoomid();
            for (Server.ChatConnection connection : connectionList) {
                if (connection.socket.getPort() == (socket.getPort())) {
                    client = connection;
                }
                for (ChatRoom room : chatRooms) {
                    if (room.equals(roomName)) {
                        flag = true;
                    }
                }
                if (!flag) {
                    message.setIdentity(client.user.getIdentity());
                    message.setContent("The requested room is invalid or non existent");
                    client.sendMessage(gson.toJson(message));
                } else {
                    message.setIdentity(client.user.getIdentity());
                    if (client.user.getRoomid().equals("")) {
                        message.setFormer(MAINHALL);
                        message.setContent(client.user.getIdentity() + " moved from MainHall to " + roomName);
                    } else {
                        message.setFormer(client.user.getRoomid());
                        message.setContent(client.user.getIdentity() + " moved from + " + client.user.getRoomid() + " to " + roomName);
                    }
                    message.setRoomid(roomName);

                    ChatRoom.selectById(chatRooms, roomName).addRoomUser(client.user);
//                ChatRoom.selectById(chatRooms,MAINHALL);
                    broadCast(gson.toJson(message), chatRooms, roomName);
                }
            }
        }

        public void broadcastMessage(General inputLine) {
            General message = new General(Types.MESSAGE.type);
            message.setIdentity(this.user.getIdentity());
            message.setContent(inputLine.getContent());
            broadCast(gson.toJson(message), chatRooms, this.user.getRoomid());
        }

        public void joinRoom(General inputLine) {
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
                this.user.setRoomid(targetRoomId);
                if (targetRoomId.equals(MAINHALL)) {
                    //TODO mainhall
                    General mainHallContent = new General(Types.ROOMCONTENTS.type);
                    mainHallContent.setRoomid(MAINHALL);
                    mainHallContent.setIdentities(getRoomUserIds(ChatRoom.selectById(chatRooms, MAINHALL)));
                    mainHallContent.setOwner(ChatRoom.selectById(chatRooms, MAINHALL).getOwner());
                    General roomList = new General(Types.ROOMLIST.type);

                }
            }
        }

        ArrayList<String> getRoomUserIds(ChatRoom chatRoom){
            ArrayList<String> result = new ArrayList<>();
            result.addAll(chatRoom.getRoomUsers().stream().map(User::getIdentity).collect(Collectors.toList()));
            return result;
        }

    }
}
