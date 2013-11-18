package lsr.paxos.test;

import lsr.paxos.ReplicationException;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;

public class DirectoryProtocol {

    private ServerSocketChannel serverSocketChannel;

    private ByteBuffer byteBuffer = ByteBuffer.allocate(100);

    public void start() throws IOException {
        serverSocketChannel = ServerSocketChannel.open();
        InetSocketAddress address = new InetSocketAddress(1111);
        serverSocketChannel.socket().bind(address);
        serverSocketChannel.configureBlocking(false);

        while (true) {
            SocketChannel socketChannel = null;
            try {
                socketChannel = serverSocketChannel.accept();
            } catch (IOException e) {
                // TODO: probably too many open files exception,
                // but i don't know what to do then; is server socket channel valid
                // after throwing this exception?; if yes can we just ignore it and
                // wait for new connections?
                e.printStackTrace();
            }

            if (socketChannel != null) {
                socketChannel.read(byteBuffer);
                String objectId = byteBuffer.toString();
                System.out.println("*******" + "Got string: " + objectId + "*******");
            }
        }
    }

    public static void main(String[] args) throws IOException, ReplicationException, InterruptedException {
        DirectoryProtocol directoryProtocol = new DirectoryProtocol();
        directoryProtocol.start();
    }

}
