/**
 * @author JiazheHou
 * @date 2021/9/3
 * @apiNote
 */

import com.google.gson.Gson;
import jsonFile.General;
import jsonFile.Types;

import java.io.*;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Scanner;

public class Client {
    public static void main(String[] args) {
       new Client().handle();
    }

    public void handle(){
        Socket socket;
        try {
            socket=new Socket("127.0.0.1", 6379);
            boolean handler_alive = true;
            while (handler_alive) {
                ChatClient conn = new ChatClient(socket);
                if (conn != null) {
                    // we have accepted a new connection, lets log it
                    System.out.printf("Accepted new connection from %s:%d\n", socket.getLocalAddress().getCanonicalHostName(), socket.getPort());

                    // finally, lets do some stuff with the socket inside the new object;
                    conn.run();
                } else {
                    handler_alive = false;
                }
            }

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
                reader.close();
                writer.close();
                socket.close();
            } catch (IOException e) {
                System.out.println("Error closing conn");
            }
        }

        public void run() throws IOException {
            connection_alive = true;
            while (connection_alive == true){

                Boolean messageSent = false;
                String inputLine = reader.readLine();
                if (inputLine == null) {
                    // bad write, close
                    connection_alive = false;
                } else {
                    General fromServer = gson.fromJson(inputLine,General.class);
                    if (fromServer.getType().equals(Types.NEWIDENTITY.type) && fromServer.getFormer().equals("")) {
                        this.identity = fromServer.getIdentity();
                        this.currentRoomId = "MainHail";
                        System.out.printf("Connected to %s as %s",socket.getLocalAddress().getCanonicalHostName(),this.identity);
                        System.out.println();

                    }
                    //identify changed and output message
                    else if(fromServer.getType().equals(Types.NEWIDENTITY.type) && !fromServer.getIdentity().equals(this.identity)){
                        this.identity = fromServer.getIdentity();
                        System.out.println(fromServer.getContent());

                    }else if(fromServer.getType().equals(Types.ROOMLIST.type) || fromServer.getType().equals(Types.MESSAGE.type))
                        System.out.println(fromServer.getContent());
                }

                while(messageSent == false) {
                    System.out.printf("[%s] %s>",this.currentRoomId, this.identity );
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
                        messageSent = true;
                    } else{
                        //TODO 不是发言，视为命令
                        if("#".equals(input.substring(0, 1))){
                            switch (input1[0].substring(1)){
                                case "identitychange":
                                    messageSent = identityChange(input1[1]);
                                    break;
                                case "creatroom":
                                    messageSent = createRoom(input1[1]);
                                    break;
                                case "roomchange":
                                    roomChange(input1[1]);
                                    break;
                                case "join":
                                    messageSent = joinRoom(input1[1]);
                                    break;

                            }
                        }

                    }
                }

            }
            
            close();
        }

        /**
        * @Description: identityChange
        * @Author: Yuanyi Zhang
        * @Date: 2021/9/6
        */
        public boolean identityChange(String input){
            if (input.length() >= 3 && input.length() <= 16) {
                if( input.substring(0,1).matches("[A-Za-z]") && input.matches("^[A-Za-z0-9]+$")){
                    General command = new General(Types.IDENTITYCHANGE.type);
                    command.setFormer(this.identity);
                    command.setIdentity(input);
                    input = gson.toJson(command);
                    writer.print(input);
                    writer.println();
                    writer.flush();
                    return true;
                }
                else{
                    System.out.println("Starting with an upper or lower case character");
                    System.out.println("And upper and lower case characters only and digits");
                }
            } else {
                System.out.println("The identity must be at least 3 characters " +
                        "and no more than 16 characters");
            }
            return false;
        }


        /**
         * @Description: roomChange
         * @Author: Yuanyi Zhang
         * @Date: 9/11/2021
         */
        public boolean createRoom(String input){
            if (input.length() >= 3 && input.length() <= 32) {
                if( input.substring(0,1).matches("[A-Za-z]") && input.matches("^[A-Za-z0-9]+$")){
                    General command = new General(Types.CREATEROOM.type);
                    command.setRoomid(input);
                    input = gson.toJson(command);
                    writer.print(input);
                    writer.println();
                    writer.flush();
                    return true;
                }
                else{
                    System.out.println("Starting with an upper or lower case character");
                    System.out.println("And upper and lower case characters only and digits");
                }
            }
            else {
                System.out.println("The room name must be at least 3 characters " +
                        "and no more than 32 characters");
            }
            return false;
        }

        public void roomChange(String input){
            General command = new General(Types.JOIN.type);
            command.setRoomid(input);
            input = gson.toJson(command);
            writer.print(input);
            writer.println();
            writer.flush();
        }

        public boolean joinRoom(String input){
            General command = new General(Types.JOIN.type);
            command.setRoomid(input);
            writer.print(gson.toJson(command));
            writer.println();
            writer.flush();
            return true;
        }



    }




}
