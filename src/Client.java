/**
 * @author JiazheHou
 * @date 2021/9/3
 * @apiNote
 */

import com.google.gson.Gson;
import jsonFile.Message;

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
                Scanner scanner = new Scanner(System.in);
                String input = scanner.nextLine();
                if (input != null && input.substring(0,1) != "#") {
                    input = gson.toJson(new Message(input));
                    writer.print(input);
                    writer.println();
                    writer.flush();
                }
                else {
                    //TODO 不是发言，视为命令
                }

                String inputLine = reader.readLine();
                if (inputLine == null) {
                    // bad write, close
                    connection_alive = false;
                } else {
                    System.out.printf("%d: %s\n", socket.getPort(), inputLine);
            
                }
            }
            
            close();
        }
    }




}
