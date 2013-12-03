package lsr.paxos.test;

import lsr.common.*;
import lsr.paxos.ReplicationException;
import lsr.paxos.client.Client;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.channels.ServerSocketChannel;
import java.sql.*;
import java.util.*;
import java.util.logging.Logger;

public class Directory {

    private ServerSocketChannel serverSocketChannel;

    private long clientId = -1;

    private int sequenceId = 0;

    private final Properties configuration = new Properties();

    private ByteBuffer byteBuffer = ByteBuffer.allocate(100);
    private Socket potentialLeader;
    private DataOutputStream output;
    private DataInputStream input;
    private boolean isLeader = false;
    private Client client;

    public void start(int port, String hostAddress) throws IOException, ReplicationException {
        serverSocketChannel = ServerSocketChannel.open();
        InetSocketAddress address = new InetSocketAddress(1111);
        serverSocketChannel.socket().bind(address);
        serverSocketChannel.configureBlocking(false);

        client = new Client();
        client.connect();

        DirectoryServiceCommand command = new DirectoryServiceCommand(hostAddress.getBytes(), port);
        byte[] response = client.execute(command.toByteArray());
        ByteBuffer buffer = ByteBuffer.wrap(response);
        String status = new String(buffer.array());
        logger.info("*********" + status + "*********");
    }

    public static void main(String[] args) throws IOException, ReplicationException, InterruptedException {
        Directory directory = new Directory();
        if (args.length > 2) {
            System.exit(1);
        }
        int port = Integer.parseInt(args[0]);
        Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
        while (interfaces.hasMoreElements()) {
            NetworkInterface current = interfaces.nextElement();
            System.out.println(current);
            if (!current.isUp() || current.isLoopback() || current.isVirtual()) continue;
            Enumeration<InetAddress> addresses = current.getInetAddresses();
            while (addresses.hasMoreElements()) {
                InetAddress current_addr = addresses.nextElement();
                if (current_addr.isLoopbackAddress()) continue;
                if (current_addr instanceof Inet4Address) {
                    directory.start(port, current_addr.getHostAddress());
                }
            }
        }
    }

    private final static Logger logger = Logger.getLogger(Directory.class.getCanonicalName());

}
