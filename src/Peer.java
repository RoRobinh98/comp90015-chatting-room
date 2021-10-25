import Identity.ChatRoom;
import Identity.User;
import com.google.gson.Gson;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;

import java.io.BufferedReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class Peer {
    private boolean handler_alive = false;
    private ArrayList<User> users;
    private ArrayList<ChatRoom> chatRooms;

    public static void main(String[] args) {
        CmdCommand cmdCommand = new CmdCommand();
        CmdLineParser parser = new CmdLineParser(cmdCommand);

        try {
            parser.parseArgument(args);

            new Server().handle(cmdCommand.portP);
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

    public class PeerServer implements Runnable{
        private boolean handler_alive = false;
        private ArrayList<User> users;
        private ArrayList<ChatRoom> chatRooms;
        //private volatile List<ChatConnection> connectionList;

        @Override
        public void run() {

        }
    }

    public class PeerClient implements Runnable{
        private Socket socket;
        private PrintWriter writer;
        private BufferedReader reader;
        private boolean connection_alive;
        private Gson gson = new Gson();
        private String identity;
        private String currentRoomId;

        @Override
        public void run() {

        }
    }
}
