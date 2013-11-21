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

        while (true) {
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

            if (clientReply.getResult().equals(ClientReply.Result.OK)) {
                isLeader = clientReply.getValue()[0] == 1;
                logger.info("*******" + processes.get(localId).getHostname() + " is leader? " + isLeader);
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
