import Identity.ChatRoom;
import Identity.User;
import com.google.gson.Gson;
import jsonFile.General;
import jsonFile.NewIdentity;
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
        chatRooms.add(new ChatRoom(MAINHALL));
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
        broadCast(String.format("%d has joined the chat",connection.socket.getPort()), null);
        connectionList.add(connection);
    }

    private void leave(Server.ChatConnection connection) {
        broadCast(String.format("%d has left the chat",connection.socket.getPort()), connection);
        connectionList.remove(connection);
        users.remove(connection.user);
    }

    private synchronized void broadCast(String message, Server.ChatConnection ignored) {
        for(Server.ChatConnection connection:connectionList) {
            if(ignored == null || !ignored.equals(connection)){
                connection.sendMessage(message);
            }
        }
    }

    class ChatConnection extends Thread{
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
            user = new User(tempName,socket.getLocalAddress().getCanonicalHostName(),socket.getPort());
            System.out.println(user.getIdentity());
            users.add(user);
            ChatRoom.selectById(chatRooms,MAINHALL).addRoomUser(user);

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
                    General message = gson.fromJson(inputLine,General.class);
                    System.out.println(message);
                    if (inputLine == null) {
                        connection_alive = false;
                    }else if("identityChange".equals(message.getType())){
                        identityChange(message);
                    }

                    else {
                        System.out.printf("%d: %s\n", socket.getPort(), inputLine);
                        writer.print(inputLine);
                        writer.println();
                        writer.flush();
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

        String getTempIdentity(ArrayList<User> userList){
            int i = 1;
            List<Integer> usedNum = userList.stream().map(User::getIdentity).map(identity -> Integer.parseInt(identity.substring(5,identity.length()))).collect(Collectors.toList());
            while(true){
                if(usedNum.contains(i)){
                    i++;
                }
                else
                    break;
            }
            return "guest"+i;
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
            for(Server.ChatConnection connection : connectionList){
                if(connection.user.getIdentity().equals(newIdentity)){
                    flag = false;
                    if(connection.user.getIdentity().equals(tempName)){
                        client = connection;
                    }
                }
                else if(connection.user.getIdentity().equals(tempName)){
                    client = connection;
                }
            }
            if(!flag){
                message.setFormer("");
                message.setIdentity(tempName);
                message.setContent("Requested identity invalid or in use");
                client.sendMessage(gson.toJson(message));
            }else{
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
    }
}
