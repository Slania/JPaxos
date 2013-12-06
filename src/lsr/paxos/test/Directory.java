package lsr.paxos.test;

import lsr.paxos.ReplicationException;
import lsr.paxos.client.Client;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.logging.Logger;

public class Directory {

    private ServerSocketChannel serverSocketChannel;

    private Client client;
    private final ByteBuffer readBuffer = ByteBuffer.allocate(1024);

    private HashMap<String, String> objectReplicaSetMap = new HashMap<String, String>();

    public void start(int port, String hostAddress) throws IOException, ReplicationException {
        Selector selector = null;

        try {
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

            selector = Selector.open();

            serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);
            outerLoop: while (true) {
                readBuffer.clear();
                selector.select();

                for (Iterator<SelectionKey> i = selector.selectedKeys().iterator(); i.hasNext();) {

                    SelectionKey key = i.next();
                    i.remove();

                    if (key.isConnectable()){
                        ((SocketChannel)key.channel()).finishConnect();
                    }

                    if (key.isAcceptable()) {
                        SocketChannel client = serverSocketChannel.accept();
                        logger.info("New connection from: " + client.socket());
                        client.configureBlocking(false);
                        client.socket().setTcpNoDelay(true);
                        client.register(selector, SelectionKey.OP_READ);
                    }

                    if (key.isReadable()) {
                        int readBytes = 0;
                        while (true) {
                            readBytes += ((SocketChannel)key.channel()).read(readBuffer);
                            readBuffer.flip();
                            if (readBytes == readBuffer.getInt() + 4) {
                                readBuffer.rewind();
                                break;
                            } else if (readBytes == 0) {
                                logger.info("Not yet received message fully");
                                continue outerLoop;
                            } else {
                                readBuffer.flip();
                            }
                        }

                        if (readBytes == 0) {
                            break;
                        }

                        // EOF - that means that the other side close his socket, so we
                        // should close this connection too.
                        if (readBytes == -1) {
                            ((SocketChannel)key.channel()).close();
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

                        ByteBuffer wrap = ByteBuffer.allocate(1);
                        wrap.putInt(1);
                        wrap.flip();
                        ((SocketChannel)key.channel()).write(wrap);
                    }
                }
            }
        } catch (Exception e) {
            logger.info("Directory failure: " + e.getMessage());
        } finally {
            try {
                selector.close();
                serverSocketChannel.socket().close();
                serverSocketChannel.close();
            } catch (Exception e) {
                // do nothing - server failed
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
