import Identity.ChatRoom;
import Identity.User;
import com.google.gson.Gson;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.*;
import java.util.ArrayList;
import java.util.Scanner;

public class Peer {
    private Gson gson = new Gson();
    private boolean currentConnection = false;

    //Server side
    private boolean server_alive = false;
    private ArrayList<User> users;
    private ArrayList<ChatRoom> chatRooms;
    private ArrayList<ServerConnection> serverConnections;
    private final String COMMONSPACE = "commonSpace";

    //Client side
    private Socket socket;
    private boolean client_alive;
    private String identity;
    private String currentRoomId;

    public Peer() {
        users = new ArrayList<>();
        chatRooms = new ArrayList<>();
        //chatRooms.add(new ChatRoom(COMMONSPACE));
        serverConnections = new ArrayList<>();
    }

    public static void main(String[] args) {
        CmdCommand cmdCommand = new CmdCommand();
        CmdLineParser parser = new CmdLineParser(cmdCommand);

        try {
            parser.parseArgument(args);
            Peer peer = new Peer();
            new Thread(peer.new PeerServer()).start();
            new Thread(peer.new PeerClient()).start();
        } catch (CmdLineException e) {
            System.out.println("command lien error");
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
                serverHandle(4444);
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

                    System.out.println(newSocket.getInetAddress().getHostAddress());
                    System.out.println(newSocket.getPort());
                    ServerConnection conn = new ServerConnection(newSocket);

                    if (conn != null) {
                        System.out.printf("Accepted new connection from %s:%d\n", newSocket.getLocalAddress().getCanonicalHostName(), newSocket.getPort());
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
                    System.out.print(">");
                    Scanner scanner = new Scanner(System.in);
                    String input = scanner.nextLine();
                    String[] inputPart = input.split(" ");
                    if (input != null && "#".equals(input.substring(0, 1))) {
                        switch (inputPart[0].substring(1)) {
                            case "connect":
                                String target = inputPart[1];
                                String targetAddress  =target.split(":")[0];
                                int targetPort = Integer.parseInt(target.split(":")[1]);
                                String localPort;
                                if(inputPart.length == 3)
                                    localPort = inputPart[2];
                                else
                                    localPort = null;
                                clientHandle(targetAddress, targetPort,localPort);
                                break;
                            case "help":
                                break;
                            case "createroom":
                                break;
                            case "delete":
                                break;
                            case "list":
                                break;
                            case "listneighbors":
                                break;
                            case "searchneiwork":
                                break;
                        }
                    }
                }


            } catch (Exception e) {
                System.out.println("command line error");
                e.printStackTrace();
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


//            Client.ChatClient conn = new Client.ChatClient(socket);
//            if (conn != null) {
//                conn.run();
//            }
//            else
//                System.out.println("Connection failed");

        } catch (UnknownHostException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
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
            //ChatRoom.selectById(chatRooms, COMMONSPACE).addRoomUser(user);
            //user.setRoomid(COMMONSPACE);
        }

        @Override
        public void run() {

        }
    }

    class clientConnection extends Thread {
        private Socket socket;
        private PrintWriter clientWriter;
        private BufferedReader clientReader;
        private boolean connection_alive;
        private String identity;
        private String currentRoomId;

        public clientConnection(Socket socket) throws IOException {
            this.socket = socket;
            this.clientReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            this.clientWriter = new PrintWriter(socket.getOutputStream());

        }

        public void close() {
            try {
                System.out.printf("Disconnected from %s",socket.getLocalAddress().getCanonicalHostName());
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

        }
    }
}
