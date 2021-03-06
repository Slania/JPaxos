package lsr.paxos.replica;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.logging.Level;
import java.util.logging.Logger;

import lsr.common.ClientCommand;
import lsr.common.ClientReply;
import lsr.common.nio.PacketHandler;
import lsr.common.nio.ReaderAndWriter;
import lsr.common.nio.SelectorThread;

/**
 * This class is used to handle one client connection. It uses
 * <code>ReaderAndWriter</code> for writing and reading packets from clients.
 * <p>
 * First it initializes client connection reading client id (or granting him a
 * new one). After successful initialization commands will be received, and
 * reply can be send to clients.
 * 
 * @see ReaderAndWriter
 */
public class NioClientProxy {
    private final ClientRequestManager requestManager;
    private boolean initialized = false;
    private long clientId;
    private final IdGenerator idGenerator;
    private final ByteBuffer readBuffer = ByteBuffer.allocate(1024);
    private final ReaderAndWriter readerAndWriter;

    /**
     * Creates new client proxy.
     * 
     * @param readerAndWriter - used to send and receive data from clients
     * @param requestManager - callback for executing command from clients
     * @param idGenerator - generator used to generate id's for clients
     */
    public NioClientProxy(ReaderAndWriter readerAndWriter, ClientRequestManager requestManager,
                          IdGenerator idGenerator) {
        this.readerAndWriter = readerAndWriter;
        this.requestManager = requestManager;
        this.idGenerator = idGenerator;

        logger.info("New client connection: " + readerAndWriter.socketChannel.socket());
        this.readerAndWriter.setPacketHandler(new InitializePacketHandler(readBuffer));
    }

    /**
     * Sends the reply to client held by this proxy. This method has to be
     * called after client is initialized.
     * 
     * @param clientReply - reply send to underlying client
     * @throws IllegalStateException if called before client is initialized
     */
    public void send(ClientReply clientReply) throws IOException {
        if (!initialized) {
            throw new IllegalStateException("Connection not initialized yet");
        }
        logger.info("******** in method send, sending client reply at time: " + System.currentTimeMillis() + " ********");
        readerAndWriter.send(clientReply.toByteArray());
    }

    /** executes command from byte buffer 
     * @throws InterruptedException */
    private void execute(ByteBuffer buffer) throws InterruptedException {
        logger.info("******** in execute method in client proxy at time: " + System.currentTimeMillis() + " ********");
        ClientCommand command = new ClientCommand(buffer);
        requestManager.onClientRequest(command, this);
    }

    /**
     * Waits for first byte, 'T' or 'F' which specifies whether we should grant
     * new id for this client, or it has one already.
     */
    private class InitializePacketHandler implements PacketHandler {
        private final ByteBuffer buffer;

        public InitializePacketHandler(ByteBuffer buffer) {
            this.buffer = buffer;
            this.buffer.clear();
            this.buffer.limit(1);
        }

        public void finished() {
            buffer.rewind();
            byte b = buffer.get();

            if (b == 'T') {
                // grant new id for client
                clientId = idGenerator.next();
                byte[] bytesClientId = new byte[8];
                ByteBuffer.wrap(bytesClientId).putLong(clientId);
                readerAndWriter.send(bytesClientId);
                readerAndWriter.setPacketHandler(new MyClientCommandPacketHandler(buffer));
                initialized = true;
            } else if (b == 'F') {
                // wait for receiving id from client
                readerAndWriter.setPacketHandler(new ClientIdPacketHandler(buffer));
            } else {
                // command client is incorrect; close the underlying connection
                logger.log(Level.WARNING,
                        "Incorrect initialization header. Expected 'T' or 'F but received " + b);
                readerAndWriter.scheduleClose();
            }
        }

        public ByteBuffer getByteBuffer() {
            return buffer;
        }
    }

    /**
     * Waits for the id from client. After receiving it starts receiving client
     * commands packets.
     */
    private class ClientIdPacketHandler implements PacketHandler {
        private final ByteBuffer buffer;

        public ClientIdPacketHandler(ByteBuffer buffer) {
            this.buffer = buffer;
            this.buffer.clear();
            this.buffer.limit(8);
            initialized = true;
        }

        public void finished() {
            buffer.rewind();
            clientId = buffer.getLong();
            readerAndWriter.setPacketHandler(new MyClientCommandPacketHandler(buffer));
        }

        public ByteBuffer getByteBuffer() {
            return buffer;
        }
    }

    /**
     * Waits for the header and then for the message from the client.
     */
    private class MyClientCommandPacketHandler implements PacketHandler {
        private final ByteBuffer defaultBuffer;
        private ByteBuffer buffer;
        private boolean header = true;

        public MyClientCommandPacketHandler(ByteBuffer buffer) {
            defaultBuffer = buffer;
            this.buffer = defaultBuffer;
            this.buffer.clear();
            this.buffer.limit(8);
        }

        public void finished() throws InterruptedException {

            if (header) {
                assert buffer == defaultBuffer : "Default buffer should be used for reading header";
                defaultBuffer.rewind();
                int firstNumber = defaultBuffer.getInt();
                int sizeOfValue = defaultBuffer.getInt();
                if (8 + sizeOfValue <= defaultBuffer.capacity()) {
                    defaultBuffer.limit(8 + sizeOfValue);
                } else {
                    buffer = ByteBuffer.allocate(8 + sizeOfValue);
                    buffer.putInt(firstNumber);
                    buffer.putInt(sizeOfValue);
                }
            } else {
                buffer.flip();
                execute(buffer);
                // for reading header we can use default buffer
                buffer = defaultBuffer;
                defaultBuffer.clear();
                defaultBuffer.limit(8);
            }
            header = !header;
            readerAndWriter.setPacketHandler(this);
        }

        public ByteBuffer getByteBuffer() {
            return buffer;
        }
    }

    /**
     * Assumes that first received integer represent the size of value.
     */
    private class UniversalClientCommandPacketHandler implements PacketHandler {
        private final ByteBuffer defaultBuffer;
        private ByteBuffer buffer;
        private boolean readSize = true;

        public UniversalClientCommandPacketHandler(ByteBuffer buffer) {
            defaultBuffer = buffer;
            this.buffer = defaultBuffer;
            this.buffer.clear();
            this.buffer.limit(/* sizeof(int) */4);
        }

        public void finished() throws InterruptedException {
            if (readSize) {
                assert buffer == defaultBuffer : "Default buffer should be used for reading header";

                defaultBuffer.rewind();
                int size = defaultBuffer.getInt();
                if (size <= defaultBuffer.capacity()) {
                    defaultBuffer.limit(size);
                } else {
                    buffer = ByteBuffer.allocate(size);
                }
                defaultBuffer.rewind();
            } else {
                execute(buffer);
                // for reading header we can use default buffer
                buffer = defaultBuffer;
                defaultBuffer.clear();
                defaultBuffer.limit(4);
            }
            readSize = !readSize;
            readerAndWriter.setPacketHandler(this);
        }

        public ByteBuffer getByteBuffer() {
            return buffer;
        }
    }

    public String toString() {
        return "client: " + clientId + " - " + readerAndWriter.socketChannel.socket().getPort();
    }

    public SelectorThread getSelectorThread() {
        return readerAndWriter.getSelectorThread();        
    }
    
    public void closeConnection() {
        readerAndWriter.close();
    }

    private final static Logger logger = Logger.getLogger(NioClientProxy.class.getCanonicalName());
}
