package lsr.paxos.test;

import lsr.common.*;
import lsr.paxos.ReplicationException;
import lsr.paxos.statistics.ClientStats;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.logging.Logger;

public class DirectoryProtocol {

    private ServerSocketChannel serverSocketChannel;

    private long clientId = -1;

    private int sequenceId = 0;

    private final Properties configuration = new Properties();

    private ByteBuffer byteBuffer = ByteBuffer.allocate(100);
    private Socket potentialLeader;
    private DataOutputStream output;
    private DataInputStream input;
    private boolean isLeader = false;

    public void start(int localId) throws IOException {
        serverSocketChannel = ServerSocketChannel.open();
        InetSocketAddress address = new InetSocketAddress(1111);
        serverSocketChannel.socket().bind(address);
        serverSocketChannel.configureBlocking(false);

        FileInputStream fis = new FileInputStream("paxos.properties");
        configuration.load(fis);
        fis.close();

        List<PID> processes = loadProcessList();
        potentialLeader = new Socket(processes.get(localId).getHostname(), processes.get(localId).getClientPort());
        output = new DataOutputStream(potentialLeader.getOutputStream());
        input = new DataInputStream(potentialLeader.getInputStream());

        initConnection();

        byte[] byteArray = "Dummy message".getBytes();
        ClientRequest request = new ClientRequest(nextRequestId(), byteArray);
        ClientCommand command = new ClientCommand(ClientCommand.CommandType.LEADER, request);

        ByteBuffer bb = ByteBuffer.allocate(command.byteSize());
        command.writeTo(bb);
        bb.flip();
        Connection connection = null;
        PreparedStatement preparedStatement = null;
        ResultSet rs = null;

        String url = "jdbc:postgresql://128.46.76.108/paxos";
        String user = "postgres";
        String password = "password";
        String sql = "SELECT object_id, old_replica_set, new_replica_set, migration_acks FROM migrations where migration_complete = 'false' limit 10";

        while (true) {
            try {
                connection = DriverManager.getConnection(url, user, password);
                preparedStatement = connection.prepareStatement(sql);
                break;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        while (true) {
            rs = null;
//            SocketChannel socketChannel = null;
//            try {
//                socketChannel = serverSocketChannel.accept();
//            } catch (IOException e) {
//                // TODO: probably too many open files exception,
//                // but i don't know what to do then; is server socket channel valid
//                // after throwing this exception?; if yes can we just ignore it and
//                // wait for new connections?
//                e.printStackTrace();
//            }
//
//            if (socketChannel != null) {
//                socketChannel.read(byteBuffer);
//                String objectId = new String(byteBuffer.array());
//                System.out.println("*******" + "Got string: " + objectId + "*******");
//            }

            output.write(bb.array());
            output.flush();

            ClientReply clientReply = new ClientReply(input);

            try {
                rs = preparedStatement.executeQuery();
                while (rs.next()) {
                    System.out.print(rs.getString(1));
                    System.out.print(": ");
                    System.out.println(rs.getString(2));
                    System.out.print("--->");
                    System.out.println(rs.getString(3));
                    System.out.print(".Progress: ");
                    System.out.println(rs.getString(4));
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }

            if (clientReply.getResult().equals(ClientReply.Result.OK)) {
                isLeader = clientReply.getValue()[0] == 1;
                logger.info("*******" + processes.get(localId).getHostname() + " is leader? " + isLeader);
            }

            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private List<PID> loadProcessList() {
        List<PID> processes = new ArrayList<PID>();
        int i = 0;
        while (true) {
            String line = configuration.getProperty("process." + i);
            if (line == null) {
                break;
            }
            StringTokenizer st = new StringTokenizer(line, ":");
            PID pid = new PID(i, st.nextToken(), Integer.parseInt(st.nextToken()),
                    Integer.parseInt(st.nextToken()));
            processes.add(pid);
            i++;
        }
        return processes;
    }

    private void initConnection() throws IOException {
        if (clientId == -1) {
            output.write('T'); // True
            output.flush();
            clientId = input.readLong();
        } else {
            output.write('F'); // False
            output.writeLong(clientId);
            output.flush();
        }
    }
    private RequestId nextRequestId() {
        return new RequestId(clientId, ++sequenceId);
    }


    public static void main(String[] args) throws IOException, ReplicationException, InterruptedException {
        DirectoryProtocol directoryProtocol = new DirectoryProtocol();
        if (args.length > 2) {
            System.exit(1);
        }
        int localId = Integer.parseInt(args[0]);
        directoryProtocol.start(localId);
    }

    private final static Logger logger = Logger.getLogger(DirectoryProtocol.class.getCanonicalName());

}
