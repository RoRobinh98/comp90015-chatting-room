/**
 * @author JiazheHou
 * @date 2021/9/3
 * @apiNote
 */

import com.google.gson.Gson;
import jsonFile.General;
import jsonFile.Room;
import jsonFile.Types;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;

import java.io.*;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Scanner;

public class Client {
    public static void main(String[] args){

        CmdCommand cmdCommand = new CmdCommand();
        CmdLineParser parser = new CmdLineParser(cmdCommand);
        try {
            parser.parseArgument(args);

            new Client().handle(cmdCommand.hostName, cmdCommand.portNum);

        } catch (CmdLineException e) {
            System.out.println("command line error");
            e.printStackTrace();
        }
    }

    public static class CmdCommand {
        @Option(name = "-p", usage = "client port number")
        public int portNum = 4444;

        @Option(name = "-h", usage = "client hostname")
        public String hostName = "localhost";
    }

    public void handle(String hostName, int portNum){
        Socket socket;
        try {
            socket=new Socket(hostName, portNum);
            ChatClient conn = new ChatClient(socket);
            if (conn != null) {
                conn.run();
            }
            else
                System.out.println("Connection failed");

        } catch (UnknownHostException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    class ChatClient{
        private Socket socket;
        private PrintWriter writer;
        private BufferedReader reader;
        private boolean connection_alive;
        private Gson gson = new Gson();
        private String identity;
        private String currentRoomId;

        public ChatClient(Socket socket) throws IOException {
            this.socket = socket;
            this.reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            this.writer = new PrintWriter(socket.getOutputStream());

        }

        public void close() {
            try {
                System.out.printf("Disconnected from %s",socket.getLocalAddress().getCanonicalHostName());
                System.out.println();
                reader.close();
                writer.close();
                socket.close();
            } catch (IOException e) {
                System.out.println("Error closing conn");
            }
        }

        public void run() {
            connection_alive = true;

                new Thread(new InputMsg()).start();
                new Thread(new OutputMsg()).start();

        }

        public synchronized void identityChange(String input){
            if (input.length() >= 3 && input.length() <= 16) {
                if( input.substring(0,1).matches("[A-Za-z]") && input.matches("^[A-Za-z0-9]+$")){
                    General command = new General(Types.IDENTITYCHANGE.type);
                    command.setFormer(this.identity);
                    command.setIdentity(input);
                    input = gson.toJson(command);
                    writer.print(input);
                    writer.println();
                    writer.flush();
                    return;
                }
                else{
                    System.out.println("Starting with an upper or lower case character");
                    System.out.println("And upper and lower case characters only and digits");
                    System.out.printf("[%s] %s>", currentRoomId, identity);
                }
            } else {
                System.out.println("The identity must be at least 3 characters " +
                        "and no more than 16 characters");
                System.out.printf("[%s] %s>", currentRoomId, identity);
            }
            return;
        }

        public synchronized void createRoom(String input){
            if (input.length() >= 3 && input.length() <= 32) {
                if( input.substring(0,1).matches("[A-Za-z]") && input.matches("^[A-Za-z0-9]+$")){
                    General command = new General(Types.CREATEROOM.type);
                    command.setRoomid(input);
                    input = gson.toJson(command);
                    writer.print(input);
                    writer.println();
                    writer.flush();

                    return;
                }
                else{
                    System.out.println("Starting with an upper or lower case character");
                    System.out.println("And upper and lower case characters only and digits");
                    System.out.printf("[%s] %s>", currentRoomId, identity);
                }
            }
            else {
                System.out.println("The room name must be at least 3 characters " +
                        "and no more than 32 characters");
                System.out.printf("[%s] %s>", currentRoomId, identity);
            }
            return;
        }

        public synchronized void deleteRoom(String input){
            General command = new General(Types.DELETE.type);
            command.setRoomid(input);
            input = gson.toJson(command);
            writer.print(input);
            writer.println();
            writer.flush();
        }

        public synchronized void joinRoom(String input){
            General command = new General(Types.JOIN.type);
            command.setRoomid(input);
            writer.print(gson.toJson(command));
            writer.println();
            writer.flush();
            return;
        }

        public synchronized void askWho(String input){
            General command = new General(Types.WHO.type);
            command.setRoomid(input);
            writer.print(gson.toJson(command));
            writer.println();
            writer.flush();
            return;
        }

        public synchronized void askList(){
            General command = new General(Types.LIST.type);
            writer.print(gson.toJson(command));
            writer.println();
            writer.flush();
            return;
        }

        public void askQuit(){
            General command = new General(Types.QUIT.type);
            writer.print(gson.toJson(command));
            writer.println();
            writer.flush();
            return;
        }

        String allUserIds(ArrayList<String> identities, String owner){
            String result = "";
            for (String identity:identities){
                result = result+" "+identity;
                if(identity.equals(owner))
                    result = result+"*";
            }
            return result;
        }

        void listAllRooms(ArrayList<Room> rooms){
            for (Room room:rooms){
                System.out.println(room.toString() + " guest(s)");;
            }
        }

        private class InputMsg implements Runnable{

            @Override
            public void run() {
                while (connection_alive == true) {
                    try {
                        String inputLine = reader.readLine();
                        if (inputLine == null){
                            System.out.println("Connection failed");
                            connection_alive = false;
                        }
                        else {
                            General fromServer = gson.fromJson(inputLine, General.class);
                            if (fromServer.getType().equals(Types.NEWIDENTITY.type) && fromServer.getFormer().equals("")) {
                                identity = fromServer.getIdentity();
                                currentRoomId = "MainHail";
                                System.out.printf("Connected to %s as %s", socket.getLocalAddress().getCanonicalHostName(), identity);
                                System.out.println();

                            }
                            else if (fromServer.getType().equals(Types.NEWIDENTITY.type)) {
                                if (fromServer.getIdentity().equals(fromServer.getFormer())) {
                                    System.out.println("Requested identity invalid or in use");
                                } else {
                                    System.out.println(fromServer.getFormer() + " is now " + fromServer.getIdentity());
                                    if(identity.equals(fromServer.getFormer())){
                                        identity = fromServer.getIdentity();
                                    }
                                }
                            } else if (fromServer.getType().equals(Types.MESSAGE.type)) {
                                System.out.println();
                                System.out.printf("%s: %s", fromServer.getIdentity(), fromServer.getContent());
                                System.out.println();
                            } else if (fromServer.getType().equals(Types.ROOMCHANGE.type)) {
                                if (fromServer.getFormer().equals(fromServer.getRoomid())) {
                                    System.out.println("The requested room is invalid or non existent");
                                } else if (fromServer.getRoomid().equals("")) {
                                    System.out.printf("%s leaves %s", fromServer.getIdentity(), fromServer.getFormer());
                                    System.out.println();
                                    if (fromServer.getIdentity().equals(identity)) {
                                        connection_alive = false;
                                    }
                                } else {
                                    System.out.printf("%s moved from %s to %s", fromServer.getIdentity(), fromServer.getFormer(), fromServer.getRoomid());
                                    System.out.println();
                                    if (fromServer.getIdentity().equals(identity))
                                        currentRoomId = fromServer.getRoomid();
                                }
                            } else if (fromServer.getType().equals(Types.ROOMCONTENTS.type)) {
                                System.out.println("Owner: " + fromServer.getOwner());
                                if (fromServer.getRoomid().equals("")) {
                                    System.out.println("The requested room is invalid or non existent");
                                }
                                else {
                                    if (fromServer.getIdentities().size() == 0)
                                        System.out.println(fromServer.getRoomid() + " has no one in the room currently");
                                    else
                                        System.out.println(fromServer.getRoomid() + " contains " + allUserIds(fromServer.getIdentities(), fromServer.getOwner()));
                                }
                            } else if (fromServer.getType().equals(Types.ROOMLIST.type)) {
                                if (null != fromServer.getContent()) {
                                    System.out.println(fromServer.getContent());
                                    listAllRooms(fromServer.getRooms());

                                } else {
                                    listAllRooms(fromServer.getRooms());
                                }
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

        private class OutputMsg implements Runnable{
            public void run() {
                while (connection_alive) {
                    Scanner scanner = new Scanner(System.in);
                    String input = scanner.nextLine();
                    String[] input1 = input.split(" ");
                    if (input != null && !"#".equals(input.substring(0, 1))) {
                        General message = new General(Types.MESSAGE.type);
                        message.setContent(input);
                        input = gson.toJson(message);
                        writer.print(input);
                        writer.println();
                        writer.flush();
                    } else {
                        if ("#".equals(input.substring(0, 1))) {
                            switch (input1[0].substring(1)) {
                                case "identitychange":
                                    identityChange(input1[1]);
                                    break;
                                case "createroom":
                                    createRoom(input1[1]);
                                    break;
                                case "delete":
                                    deleteRoom(input1[1]);
                                case "join":
                                    joinRoom(input1[1]);
                                    break;
                                case "who":
                                    askWho(input1[1]);
                                    break;
                                case "list":
                                    askList();
                                    break;
                                case "quit":
                                    askQuit();
                                    break;
                                default:
                                    System.out.println("Invalid Command");
                            }
                        }

                    }
                }
            }
        }
    }
}
