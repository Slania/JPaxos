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
import java.nio.channels.SocketChannel;
import java.sql.*;
import java.util.*;
import java.util.logging.Logger;

public class Directory {

    private ServerSocketChannel serverSocketChannel;

    private Client client;
    private final ByteBuffer readBuffer = ByteBuffer.allocate(1024);

    private HashMap<String, String> objectReplicaSetMap = new HashMap<String, String>();

    public void start(int port, String hostAddress) throws IOException, ReplicationException {
        serverSocketChannel = ServerSocketChannel.open();
        InetSocketAddress address = new InetSocketAddress(hostAddress, port);
        serverSocketChannel.socket().bind(address);
        serverSocketChannel.configureBlocking(false);

        client = new Client();
        client.connect();

        DirectoryServiceCommand command = new DirectoryServiceCommand(hostAddress.getBytes(), port);
        byte[] response = client.execute(command.toByteArray());
        ByteBuffer buffer = ByteBuffer.wrap(response);
        String status = new String(buffer.array());
        logger.info("*********" + status + "*********");

        while (true) {
            SocketChannel socketChannel = serverSocketChannel.accept();

            if (socketChannel != null) {
                int readBytes = socketChannel.read(readBuffer);

                if (readBytes == 0) {
                    break;
                }

                // EOF - that means that the other side close his socket, so we
                // should close this connection too.
                if (readBytes == -1) {
                    socketChannel.close();
                    return;
                }

                int objectIdLength = readBuffer.getInt();
                int replicaSetLength = readBuffer.getInt();

                logger.info(String.valueOf(objectIdLength));
                logger.info(String.valueOf(replicaSetLength));

                byte[] objectId = new byte[objectIdLength];
                byte[] replicaSet = new byte[replicaSetLength];

                readBuffer.get(objectId);
                readBuffer.get(replicaSet);

                logger.info(new String(objectId));
                logger.info(new String(replicaSet));

                objectReplicaSetMap.put(new String(objectId), new String(replicaSet));

                for (String object : objectReplicaSetMap.keySet()) {
                    System.out.println("Contents of map:");
                    System.out.println("Object: " + object + ", Replicas: " + objectReplicaSetMap.get(object));
                }
                System.out.println("********-------------------------------********");

                byte[] ok = new byte[1];
                ok[0] = 1;
                ByteBuffer wrap = ByteBuffer.wrap(ok);
                wrap.flip();
                socketChannel.write(wrap);
            }
        }
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
