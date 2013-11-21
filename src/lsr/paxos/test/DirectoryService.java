package lsr.paxos.test;

import lsr.service.SimplifiedService;

import java.io.*;
import java.net.Socket;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DirectoryService extends SimplifiedService {

    private Socket socket;
    private DataOutputStream output;
    private DataInputStream input;

    private HashMap<DirectoryServiceCommand, Boolean> map = new HashMap<DirectoryServiceCommand, Boolean>();

    protected byte[] execute(byte[] value, boolean isLeader) {
        logger.info("******** in execute method of DirectoryService at time: " + System.currentTimeMillis() + " ********");
        DirectoryServiceCommand command;
        try {
            command = new DirectoryServiceCommand(value);
        } catch (IOException e) {
            logger.log(Level.WARNING, "Incorrect request", e);
            return null;
        }

        Boolean migrationStatus = command.isMigrationComplete();
        map.put(command, migrationStatus);

        ByteArrayOutputStream byteArrayOutput = new ByteArrayOutputStream();
        DataOutputStream dataOutput = new DataOutputStream(byteArrayOutput);

        logger.info(command.toString());
        try {
            dataOutput.write(command.toString().getBytes());
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
//        if (isLeader) {
//            try {
//                connectTo();
//                output.write(command.getObjectId());
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
//        }

        return byteArrayOutput.toByteArray();
    }

    protected byte[] makeSnapshot() {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        try {
            ObjectOutputStream objectOutputStream = new ObjectOutputStream(stream);
            objectOutputStream.writeObject(map);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
        return stream.toByteArray();
    }

    @SuppressWarnings("unchecked")
    protected void updateToSnapshot(byte[] snapshot) {
        ByteArrayInputStream stream = new ByteArrayInputStream(snapshot);
        ObjectInputStream objectInputStream;
        try {
            objectInputStream = new ObjectInputStream(stream);
            map = (HashMap<DirectoryServiceCommand, Boolean>) objectInputStream.readObject();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    private void connectTo() throws IOException {
        // close previous connection if any
        cleanClose();

//        String host = "localhost";
        String host = "localhost";
        int port = 1111;
        logger.info("Connecting to " + host + ":" + port);
        socket = new Socket(host, port);

//        socket.setSoTimeout(Math.min(timeout, MAX_TIMEOUT));
        socket.setSoTimeout(3000);
        socket.setReuseAddress(true);
        socket.setTcpNoDelay(true);
        output = new DataOutputStream(socket.getOutputStream());
        input = new DataInputStream(socket.getInputStream());

        logger.info("*****" + "Connected to localhost directory service" + "*****");
    }

    private void cleanClose() {
        try {
            if (socket != null) {
                socket.shutdownOutput();
                socket.close();
                socket = null;
                logger.info("Closing socket");
            }
        } catch (IOException e) {
            e.printStackTrace();
            logger.log(Level.WARNING, "Not clean socket closing.");
        }
    }

    private static final Logger logger = Logger.getLogger(DirectoryService.class.getCanonicalName());
}
