package lsr.paxos.test;

import lsr.common.ProcessDescriptor;
import lsr.service.SimplifiedService;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import java.io.*;
import java.net.Socket;
import java.sql.*;
import java.util.HashMap;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DirectoryService extends SimplifiedService {

    private Socket socket;

    private final Properties configuration = new Properties();

    private HashMap<DirectoryServiceCommand, Boolean> map = new HashMap<DirectoryServiceCommand, Boolean>();

    protected byte[] execute(byte[] value, boolean isLeader) {
        FileInputStream fis = null;
        try {
            fis = new FileInputStream("paxos.properties");
            configuration.load(fis);
            fis.close();
        } catch (IOException e) {
        }

        logger.info("******** in execute method of DirectoryService at time: " + System.currentTimeMillis() + " ********");
        DirectoryServiceCommand command;
        try {
            command = new DirectoryServiceCommand(value);
        } catch (IOException e) {
            logger.log(Level.WARNING, "Incorrect request", e);
            return null;
        }

        if (!command.getDirectoryCommandType().equals(DirectoryServiceCommand.DirectoryCommandType.REGISTER_DIRECTORY)) {
            Boolean migrationStatus = command.isMigrationComplete();
            map.put(command, migrationStatus);

            ByteArrayOutputStream byteArrayOutput = new ByteArrayOutputStream();
            DataOutputStream dataOutput = new DataOutputStream(byteArrayOutput);

            logger.info(command.toString());

            Connection connection = null;
            PreparedStatement preparedStatement = null;

            logger.info(String.valueOf(ProcessDescriptor.getInstance().localId));
            logger.info("db" + ProcessDescriptor.getInstance().localId);
            logger.info(configuration.getProperty("db" + ProcessDescriptor.getInstance().localId));
            String url = "jdbc:postgresql://" + configuration.getProperty("db" + ProcessDescriptor.getInstance().localId);
            String user = "postgres";
            String password = "password";

            try {
                connection = DriverManager.getConnection(url, user, password);
                String sql = "INSERT INTO migrations(object_id, old_replica_set, new_replica_set, migration_complete, creation_time) VALUES(?, ?, ?, ?, ?)";
                preparedStatement = connection.prepareStatement(sql);
                preparedStatement.setString(1, new String(command.getObjectId()));
                preparedStatement.setString(2, command.getOldReplicaSetAsCsv());
                preparedStatement.setString(3, command.getNewReplicaSetAsCsv());
                preparedStatement.setBoolean(4, command.isMigrationComplete());
                preparedStatement.setTimestamp(5, Timestamp.valueOf(DateTime.now().toString(DateTimeFormat.forPattern("yyyy-MM-dd kk:mm:ss"))));
                preparedStatement.executeUpdate();
            } catch (SQLException e) {
                e.printStackTrace();
                return null;
            }

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
        } else {
            ByteArrayOutputStream byteArrayOutput = new ByteArrayOutputStream();
            DataOutputStream dataOutput = new DataOutputStream(byteArrayOutput);

            Connection connection = null;
            PreparedStatement preparedStatement = null;

            String url = "jdbc:postgresql://" + configuration.getProperty("db" + ProcessDescriptor.getInstance().localId);
            String user = "postgres";
            String password = "password";

            try {
                connection = DriverManager.getConnection(url, user, password);
                String sql = "INSERT INTO directories(ip, port, time_stamp) VALUES(?, ?, ?)";
                preparedStatement = connection.prepareStatement(sql);
                preparedStatement.setString(1, new String(command.getDirectoryNodeIP()));
                preparedStatement.setInt(2, command.getDirectoryNodePort());
                preparedStatement.setTimestamp(3, Timestamp.valueOf(DateTime.now().toString(DateTimeFormat.forPattern("yyyy-MM-dd kk:mm:ss"))));
                preparedStatement.executeUpdate();
            } catch (SQLException e) {
                e.printStackTrace();
                return null;
            }

            try {
                dataOutput.write("received directory command".getBytes());
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }

            return byteArrayOutput.toByteArray();
        }
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
        DataOutputStream output = new DataOutputStream(socket.getOutputStream());
        DataInputStream input = new DataInputStream(socket.getInputStream());

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
