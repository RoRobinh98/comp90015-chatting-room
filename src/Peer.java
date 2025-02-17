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
import java.net.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.stream.Collectors;

public class Peer {
    private Gson gson = new Gson();
    private boolean currentConnection = false;
    private String target;
    private List<String> blackList;
    private int portP;
    private int portI;

    //Server side
    private boolean server_alive = false;
    private ArrayList<User> users;
    private ArrayList<ChatRoom> chatRooms;
    private ArrayList<ServerConnection> serverConnections;
    private ServerSocket serverSocket;

    //Client side
    private boolean client_alive = true;
    private String identity;
    private String currentRoomId;
    private ClientConnection clientConnection;

    public Peer(int portP, int portI) {
        users = new ArrayList<>();
        chatRooms = new ArrayList<>();
        //chatRooms.add(new ChatRoom(COMMONSPACE));
        serverConnections = new ArrayList<>();
        blackList = new ArrayList<>();
        this.portP = portP;
        this.portI = portI;
    }

    public static void main(String[] args) {
        CmdCommand cmdCommand = new CmdCommand();
        CmdLineParser parser = new CmdLineParser(cmdCommand);

        try {
            parser.parseArgument(args);
            Peer peer = new Peer(cmdCommand.portP, cmdCommand.portI);
            PeerClient peerClient = peer.new PeerClient();
            PeerServer peerServer = peer.new PeerServer();
            new Thread(peerClient).start();
            new Thread(peerServer).start();
        } catch (CmdLineException e) {
            System.out.println("command line error");
            e.printStackTrace();
        }

    }

    public static class CmdCommand {
        @Option(name = "-p", usage = "port listens")
        public int portP = 4444;

        @Option(name = "-i", usage = "port to connect")
        public int portI;
    }



    private class PeerServer implements Runnable{

        @Override
        public void run() {
            try {
                serverHandle(portP);
            } catch (Exception e) {
                System.out.println("command lien error");
                e.printStackTrace();
            }
        }

        public void serverHandle(int portNum) {
            ServerSocket serverSocket;
            try {
                serverSocket = new ServerSocket(portNum);

                //System.out.printf("Listening on port %d\n", portNum);
                server_alive = true;

                while (server_alive) {
                    Socket newSocket = serverSocket.accept();

                 //   System.out.println(newSocket.getInetAddress().getHostAddress());
                 //   System.out.println(newSocket.getPort());
                    ServerConnection conn = new ServerConnection(newSocket);

                    if (conn != null) {
                 //       System.out.printf("Accepted new connection from %s:%d\n", newSocket.getLocalAddress().getCanonicalHostName(), newSocket.getPort());
                        enter(conn);
                        conn.start();
                        conn.interrupt();
                    } else {
                        server_alive = false;
                    }

                }
            } catch (IOException e) {
                System.out.printf("Error handling conns, %s\n", e.getMessage());
                server_alive = false;
            }
        }
        private void enter(ServerConnection connection) {
            serverConnections.add(connection);
        }
    }

    private class PeerClient implements Runnable{

        @Override
        public void run() {
            try {
                while (client_alive) {
                    if (currentConnection == true)
                        continue;
                    System.out.print(">");
                    Scanner scanner = new Scanner(System.in);
                    String input = scanner.nextLine();
                    String[] inputPart = input.split(" ");
                    if (input != null && "#".equals(input.substring(0, 1))) {
                        switch (inputPart[0].substring(1)) {
                            case "connect":
                                target = inputPart[1];
                                String targetAddress  =target.split(":")[0];
                                int targetPort = Integer.parseInt(target.split(":")[1]);
                                String localPort;
                                if(inputPart.length == 3)
                                    localPort = inputPart[2];
                                else
                                    localPort = portI+"";
                                clientHandle(targetAddress, targetPort,localPort);

                                break;
                            case "help":
                                askHelp();
                                break;
                            case "createroom":
                                createRoom(inputPart[1]);
                                break;
                            case "delete":
                                askDelete(inputPart[1]);
                                break;
                            case "list":
                                General roomList = new General(Types.ROOMLIST.type);
                                roomList.setRooms(Room.fromChatRoomToRoom(chatRooms));
                                for(Room room: roomList.getRooms()){
                                    System.out.println(room.toString() + " guest(s)");
                                }
                                break;
                            case "listneighbors":
                                List<String> neighborList = users.stream().map(User::getIdentity).collect(Collectors.toList());
                                if (neighborList.size() == 0)
                                    System.out.println("No neighbors detected");
                                for(String neighbor:neighborList){
                                    System.out.println(neighbor);
                                }
                                break;
                            case "searchnetwork":
                                List<String> neighborList1 = users.stream().map(User::getIdentity).collect(Collectors.toList());
                                List<String> searchedList = new ArrayList<>();
                                searchNetwork(neighborList1, searchedList);
                                break;
                            case "who":
                                String askId = inputPart[1];
                                if (!chatRooms.stream().map(ChatRoom::getId).collect(Collectors.toList()).contains(askId)){
                                    System.out.println("The requested room is invalid or non existent");
                                   }
                                else {
                                    ArrayList<String> userIds = new ArrayList<>();
                                    userIds.addAll(ChatRoom.selectById(chatRooms, askId).getRoomUsers().stream().map(User::getIdentity).collect(Collectors.toList()));
                                    if (userIds.size() == 0)
                                    System.out.println(askId + " has no one in the room currently");
                                    else
                                    System.out.println(askId + " contains " + allUserIds(userIds));
                                }
                                break;
                            case "kick":
                                doKick(inputPart[1]);
                                break;
                            default:
                                System.out.println("Invalid command, use #help to see instructions");
                                break;
                        }
                    }
                }


            } catch (Exception e) {
                System.out.println("command line error");
                e.printStackTrace();
            }

        }

        private void createRoom(String input) {
            if (input.length() >= 3 && input.length() <= 32) {
                if (input.substring(0,1).matches("[A-Za-z]") && input.matches("^[A-Za-z0-9]+$")) {
                    if (chatRooms.stream().map(ChatRoom::getId).collect(Collectors.toList()).contains(input)) {
                        System.out.printf("Room %s is invalid or already in use.", input);
                        System.out.println();
                        return;
                    }
                   ChatRoom chatRoom = new ChatRoom(input);
                   chatRooms.add(chatRoom);
                    System.out.printf("Room %s created.", input);
                    System.out.println();
                   return;
                }

                else {
                    System.out.println("Starting with an upper or lower case character");
                    System.out.println("And upper and lower case characters only and digits");
                    System.out.print(">");
                }
            }
            else {
                System.out.println("The room name must be at least 3 characters " +
                        "and no more than 32 characters");
                System.out.print(">");
            }
            return;
        }

        private void doKick(String input) throws IOException {
                User kickOne = getByIdentity(input);
                if (kickOne == null){
                    System.out.println("No such user");
                    return;
                }
                ServerConnection serverConnection = getByUser(serverConnections,kickOne);
                serverConnection.socket.close();
                System.out.printf("User %s has been kicked off", input);
                System.out.println();
        }

        private void askDelete(String input) throws IOException {
            if (chatRooms.stream().map(ChatRoom::getId).collect(Collectors.toList()).contains(input)) {
                System.out.printf("Room %s is invalid.", input);
                System.out.println();
                return;
            }
            else {
                ChatRoom room = ChatRoom.selectById(chatRooms,input);
                for(User subUser: room.getRoomUsers()){
                    General roomChange = new General(Types.ROOMCHANGE.type);
                    roomChange.setIdentity(subUser.getIdentity());
                    roomChange.setFormer(subUser.getRoomid());
                    roomChange.setRoomid("");

                    ServerConnection serverConnection = getByUser(serverConnections,subUser);
                    serverConnection.serverWriter.print(roomChange);
                    serverConnection.serverWriter.println();
                    serverConnection.serverWriter.flush();
                    serverConnection.socket.close();
                }
            }
        }

    }



    public void clientHandle(String hostName, int portNum, String localPort){
        Socket socket;

        try {
            if (localPort == null){
                socket = new Socket(hostName, portNum);
            }
            else {
                int port = Integer.parseInt(localPort);
                socket=new Socket();
                socket.bind(new InetSocketAddress(port));
                socket.connect(new InetSocketAddress(hostName,portNum));
            }
            currentConnection = true;


            clientConnection = new ClientConnection(socket);
            if (clientConnection != null) {
                clientConnection.run();
            }
            else
                System.out.println("Connection failed");

        } catch (UnknownHostException e) {
            System.out.println("Connection failed");
            e.printStackTrace();
            currentConnection = false;
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("Connection failed");
            currentConnection = false;
        }
    }

    class ServerConnection extends Thread{
        private Socket socket;
        private PrintWriter serverWriter;
        private BufferedReader serverReader;
        private boolean connection_alive;
        private User user;

        public ServerConnection(Socket socket) throws IOException {
            this.socket = socket;
            serverReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            serverWriter = new PrintWriter(socket.getOutputStream());
        }
        public void initialize() {
            String clientAddress = socket.getInetAddress().getHostAddress();
            int clientPort = socket.getPort();
            user = new User(clientAddress+":"+clientPort, clientAddress, clientPort);
            users.add(user);
        }

        @Override
        public void run() {
            connection_alive = true;
            //initialize();
            while (connection_alive) {
                String inputLine = null;
                try {
                    inputLine = serverReader.readLine();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                General message = gson.fromJson(inputLine, General.class);
                if (inputLine == null) {
                    connection_alive = false;
                } else if (Types.MESSAGE.type.equals(message.getType())) {
                    broadcastMessage(message);
                } else if (Types.WHO.type.equals(message.getType())) {
                    replyForWho(message);
                } else if (Types.HOSTCHANGE.type.equals(message.getType())) {
                    String hostName = message.getHost();
                    String hostAddress = hostName.split(":")[0];
                    int hostPort = Integer.parseInt(hostName.split(":")[1]);
                    user = new User(message.getHost(), hostAddress, hostPort);
                    users.add(user);
                    //System.out.println(user.getIdentity());
                } else if (Types.LISTNEIGHBORS.type.equals(message.getType())) {
                    replyForListNeighbors();
                } else if (Types.LIST.type.equals(message.getType())){
                    replyForList();
                } else if (Types.QUIT.type.equals(message.getType())){
                    replyForQuit();
                    connection_alive = false;
                } else if (Types.JOIN.type.equals(message.getType())){
                    joinRoom(message);
                } else if (Types.SHOUT.type.equals(message.getType())) {
                    replyForShout(message);
                }
            }

        }

        public synchronized void broadcastMessage(General inputLine) {
            General message = new General(Types.MESSAGE.type);
            message.setIdentity(this.user.getIdentity());
            message.setContent(inputLine.getContent());
            broadCast(gson.toJson(message), chatRooms, this.user.getRoomid());
        }

        private synchronized void broadCast(String message, ArrayList<ChatRoom> chatRoom, String roomName) {
            ChatRoom room = ChatRoom.selectById(chatRoom, roomName);
            for (User user : room.getRoomUsers()) {
                getByUser(serverConnections,user).sendMessage(message);
            }
        }

        public synchronized void sendMessage(String message) {
            serverWriter.print(message);
            serverWriter.println();
            serverWriter.flush();
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
            sendMessage(gson.toJson(reply));
        }

        public synchronized void replyForListNeighbors() {
            List<String> neighbors = new ArrayList<>();
            if (currentConnection == true) {
                neighbors.add(target);
            }
            neighbors.addAll(users.stream().map(User::getIdentity).filter(string -> string.equals(user.getIdentity())).collect(Collectors.toList()));
            General reply = new General(Types.NEIGHBORS.type);
            reply.setNeighbors(neighbors);
            sendMessage(gson.toJson(reply));
        }

        public synchronized void replyForList() {
            General reply = new General(Types.ROOMLIST.type);
            reply.setRooms(Room.fromChatRoomToRoom(chatRooms));
            serverWriter.print(gson.toJson(reply));
            serverWriter.println();
            serverWriter.flush();
        }

        public synchronized void replyForQuit(){
            General quitEntity = new General(Types.ROOMCHANGE.type);
            quitEntity.setIdentity(this.user.getIdentity());
            quitEntity.setFormer(this.user.getRoomid());
            quitEntity.setRoomid(null);
            broadCast(gson.toJson(quitEntity),chatRooms,this.user.getRoomid());
            ChatRoom currentRoom = ChatRoom.selectById(chatRooms,this.user.getRoomid());
            currentRoom.removeRoomUser(this.user);
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
                if(this.user.getRoomid()!=""){
                    General roomContent = new General(Types.WHO.type);
                    roomContent.setRoomid(targetRoomId);
                    replyForWho(roomContent);
                    broadCast(gson.toJson(successfulChange), chatRooms, this.user.getRoomid());
                    ChatRoom.selectById(chatRooms,this.user.getRoomid()).removeRoomUser(this.user);
                } else{
                    sendMessage(gson.toJson(successfulChange));
                }
                broadCast(gson.toJson(successfulChange), chatRooms, targetRoomId);
                ChatRoom.selectById(chatRooms,targetRoomId).addRoomUser(this.user);
                this.user.setRoomid(targetRoomId);
            }
        }

        public synchronized void replyForShout(General message) {
            if (message.getShoutedList().contains(identity))
                return;

//            System.out.printf("%s shouted", message.getShoutIdentity());
            General command = new General(Types.SHOUT.type);
            command.setShoutIdentity(message.getShoutIdentity());

            command.setShoutedList(message.getShoutedList());
            if (clientConnection != null && !clientConnection.socket.isClosed()) {
                message.getShoutedList().add(identity);
                clientConnection.sendMessage(gson.toJson(command));
                System.out.printf("%s shouted",message.getShoutIdentity());
                System.out.println();
            }

            for (ServerConnection serverConnection:serverConnections){
                if(!serverConnection.user.getRoomid().equals(null) || !serverConnection.user.getRoomid().equals(""))
                serverConnection.sendMessage(gson.toJson(command));
            }
        }



    }

    class ClientConnection extends Thread {
        private Socket socket;
        private PrintWriter clientWriter;
        private BufferedReader clientReader;
        private String identity;
        private String currentRoomId;

        public ClientConnection(Socket socket) throws IOException {
            this.socket = socket;
            this.clientReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            this.clientWriter = new PrintWriter(socket.getOutputStream());

        }

        public void close() {
            try {
                System.out.printf("Disconnected from %s",socket.getInetAddress().getCanonicalHostName());
                System.out.println();
                clientReader.close();
                clientWriter.close();
                socket.close();
            } catch (IOException e) {
                System.out.println("Error closing conn");
            }
        }

        @Override
        public void run() {
            currentRoomId = "";
            identity = socket.getLocalAddress().getHostAddress() + ":" + socket.getLocalPort();
            System.out.println(identity);
            General hostChange = new General((Types.HOSTCHANGE.type));
            hostChange.setHost(identity);
            clientWriter.print(gson.toJson(hostChange));
            clientWriter.println();
            clientWriter.flush();

            System.out.printf("[%s] %s>", currentRoomId, identity);
            new Thread(new InputMsg()).start();
            new Thread(new OutputMsg()).start();
        }

        private void sendMessage(String message){
            clientWriter.print(message);
            clientWriter.println();
            clientWriter.flush();
        }

        private class InputMsg implements Runnable {

            @Override
            public void run() {
                while (currentConnection) {
                    try {
                        String inputLine = clientReader.readLine();
                        if (inputLine == null){
                            System.out.println("Connection failed");
                            currentConnection = false;
                        }
                        else {
                            General fromServer = gson.fromJson(inputLine, General.class);
                            if (fromServer.getType().equals(Types.HOSTCHANGE.type)) {
                                identity = fromServer.getHost();
                            }
                            else if(fromServer.getType().equals(Types.MESSAGE.type)) {
                                System.out.printf("%s: %s", fromServer.getIdentity(), fromServer.getContent());
                                System.out.println();
                            }
                            else if(fromServer.getType().equals(Types.ROOMCONTENTS.type)){
                                if (fromServer.getRoomid().equals("")) {
                                    System.out.println("The requested room is invalid or non existent");
                                }
                                else {
                                    if (fromServer.getIdentities().size() == 0)
                                        System.out.println(fromServer.getRoomid() + " has no one in the room currently");
                                    else
                                        System.out.println(fromServer.getRoomid() + " contains " + allUserIds(fromServer.getIdentities()));
                                }
                            }
                            else if(fromServer.getType().equals(Types.NEIGHBORS.type)){
                                if (fromServer.getNeighbors().size() == 0) {
                                    System.out.println("No neighbors detected");
                                }
                                else {
                                    for(String neighbor: fromServer.getNeighbors()){
                                        System.out.println(neighbor);
                                    }
                                }
                            } else if(fromServer.getType().equals(Types.ROOMLIST.type)){
                                for(Room room:fromServer.getRooms()){
                                    System.out.println(room.toString() + " guest(s)");
                                }
                            } else if(fromServer.getType().equals(Types.ROOMCHANGE.type)){
                                if (fromServer.getFormer().equals(fromServer.getRoomid())) {
                                    System.out.println("The requested room is invalid or non existent");
                                } else if (fromServer.getRoomid()==(null)) {
                                    System.out.println("in the null");
                                    System.out.printf("%s leaves %s", fromServer.getIdentity(), fromServer.getFormer());
                                    System.out.println();
                                    if (fromServer.getIdentity().equals(identity)) {
                                        currentConnection = false;
                                    }
                                } else if(fromServer.getFormer().equals("")){
                                    System.out.printf("Successfully join the room %s",fromServer.getRoomid());
                                    System.out.println();
                                    if (fromServer.getIdentity().equals(identity))
                                        currentRoomId = fromServer.getRoomid();
                                }
                                else {
                                    System.out.printf("%s moved from %s to %s", fromServer.getIdentity(), fromServer.getFormer(), fromServer.getRoomid());
                                    System.out.println();
                                    if (fromServer.getIdentity().equals(identity))
                                        currentRoomId = fromServer.getRoomid();
                                }
                            } else if (fromServer.getType().equals(Types.SHOUT.type)) {
                                if (fromServer.getShoutedList().contains(identity))
                                    continue;

                                System.out.printf("%s shouted", fromServer.getShoutIdentity());
                                System.out.println();
                                General command = new General(Types.SHOUT.type);
                                command.setShoutIdentity(identity);
                                fromServer.getShoutedList().add(identity);
                                command.setShoutedList(fromServer.getShoutedList());
                                for (ServerConnection serverConnection:serverConnections){
                                    if(!serverConnection.user.getRoomid().equals(null) || !serverConnection.user.getRoomid().equals(""))
                                    serverConnection.sendMessage(gson.toJson(command));
                                }
//                                clientWriter.print(gson.toJson(command));
//                                clientWriter.println();
//                                clientWriter.flush();
                            }

                            System.out.printf("[%s] %s>", currentRoomId, identity);
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                close();

            }
        }

        private class OutputMsg implements Runnable {

            @Override
            public void run() {
                while (currentConnection) {
                    Scanner scanner = new Scanner(System.in);
                    String input = scanner.nextLine();
                    String[] inputPart = input.split(" ");
                    if (input != null && !"#".equals(input.substring(0, 1))) {
                        if (currentRoomId.equals("")){
                            System.out.println("Please join a room before sending messages");
                            System.out.printf("[%s] %s>", currentRoomId, identity);
                        }
                        else {
                            General message = new General(Types.MESSAGE.type);
                            message.setContent(input);
                            input = gson.toJson(message);
                            clientWriter.print(input);
                            clientWriter.println();
                            clientWriter.flush();
                        }
                    }
                    else {
                        if ("#".equals(input.substring(0, 1))) {
                            switch (inputPart[0].substring(1)) {
                                case "who":
                                    askWho (inputPart[1]);
                                    break;
                                case "listneighbors" :
                                    General command = new General(Types.LISTNEIGHBORS.type);
                                    clientWriter.print(gson.toJson(command));
                                    clientWriter.println();
                                    clientWriter.flush();
                                    break;
                                case "list":
                                    askList();
                                    break;
                                case "join":
                                    joinRoom(inputPart[1]);
                                    break;
                                case "quit":
                                    askQuit();
                                    currentConnection = false;
                                    break;
                                case "help":
                                    askHelp();
                                    break;
                                case  "shout":
                                    askShout();
                                    break;
                                default:
                                    System.out.println("Invalid command, use #help to see instructions");
                                    break;
                            }
                        }
                    }
                }
            }

            public synchronized void askWho(String input){
                General command = new General(Types.WHO.type);
                command.setRoomid(input);
                clientWriter.print(gson.toJson(command));
                clientWriter.println();
                clientWriter.flush();
                return;
            }

            public synchronized void askList(){
                General command = new General(Types.LIST.type);
                clientWriter.print(gson.toJson(command));
                clientWriter.println();
                clientWriter.flush();
                return;
            }

            public synchronized void joinRoom(String input){
                General command = new General(Types.JOIN.type);
                command.setRoomid(input);
                clientWriter.print(gson.toJson(command));
                clientWriter.println();
                clientWriter.flush();
                return;
            }

            public synchronized void askQuit(){
                General command = new General(Types.QUIT.type);
                clientWriter.print(gson.toJson(command));
                clientWriter.println();
                clientWriter.flush();
                return;
            }

            public synchronized void askShout() {
                General command = new General(Types.SHOUT.type);
                command.setShoutIdentity(identity);
                command.setShoutedList(new ArrayList<>());
                clientWriter.print(gson.toJson(command));
                clientWriter.println();
                clientWriter.flush();
                return;
            }
        }


    }

    String allUserIds(ArrayList<String> identities){
        String result = "";
        for (String identity:identities){
            result = result+" "+identity;
        }
        return result;
    }

    private ServerConnection getByUser(ArrayList<ServerConnection> chatConnections, User user){
        for(ServerConnection connection : chatConnections){
            if(connection.user.equals(user)){
                return connection;
            }
        }
        return null;
    }

    private User getByIdentity(String identity){
        for (User user:users){
            if (user.getIdentity().equals(identity))
                return user;
        }

        return null;
    }

    private void searchNetwork(List<String> hostList, List<String> searchedList) {
        if (hostList == null || hostList.size() == 0)
            return;
        else {
            for(String host:hostList) {
                if (searchedList.contains(host))
                    continue;
                searchedList.add(host);
                String hostAddress = host.split(":")[0];
                int hostPort = Integer.parseInt(host.split(":")[1]);
                searchNetwork(responseForSearch(hostAddress, hostPort), searchedList);
            }
        }
    }

    public List<String> responseForSearch(String hostAddress, int hostPort){
        Socket socket;

        try {
                socket = new Socket(hostAddress, hostPort);
            BufferedReader clientReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            PrintWriter clientWriter = new PrintWriter(socket.getOutputStream());
            identity = socket.getLocalAddress().getHostAddress() + ":" + socket.getPort();
            General hostChange = new General((Types.HOSTCHANGE.type));
            hostChange.setHost(identity);
            clientWriter.print(gson.toJson(hostChange));
            clientWriter.println();
            clientWriter.flush();

            General command1 = new General(Types.LIST.type);
            clientWriter.print(gson.toJson(command1));
            clientWriter.println();
            clientWriter.flush();
            String inputLine = clientReader.readLine();
            //TODO print list

            General command2 = new General(Types.LISTNEIGHBORS.type);
            clientWriter.print(gson.toJson(command2));
            clientWriter.println();
            clientWriter.flush();

            inputLine = clientReader.readLine();
            General fromServer = gson.fromJson(inputLine, General.class);
            if (fromServer.getNeighbors().size() == 0 || fromServer.getNeighbors() == null) {
                System.out.println("No neighbors detected");
            }
            else {
                for(String neighbor: fromServer.getNeighbors()){
                    System.out.println(neighbor);
                }
            }

            General command3 = new General(Types.QUIT.type);
            clientWriter.print(gson.toJson(command3));
            clientWriter.println();
            clientWriter.flush();

            clientReader.close();
            clientWriter.close();
            socket.close();

            return fromServer.getNeighbors();

        } catch (UnknownHostException e) {
            System.out.println("Connection failed");
            e.printStackTrace();
            currentConnection = false;
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("Connection failed");
            currentConnection = false;
        }

        return null;
    }

    public void askHelp(){
        System.out.println("Local Command:");
        System.out.println("#createroom - create a chat room");
        System.out.println("#delete [room identity] - delete a chat room:");
        System.out.println("#kick [user identity] - kick the user and block them from reconnecting");
        System.out.println("#help - list the command information");
        System.out.println("Remote Command:");
        System.out.println("#join [room identity] - join a chat room within connected peer");
        System.out.println("#who [room identity] - list all users in chat room:");
        System.out.println("#quit - disconnect from a peer");
        System.out.println("#listneighbors - list all currently connected peers' network address");
        System.out.println("#connect IP[:port] [local port] - connect to another peer");
        System.out.println("Special Command:");
        System.out.println("#searchnetwork - list all connected IP address in the network");
    }
}
