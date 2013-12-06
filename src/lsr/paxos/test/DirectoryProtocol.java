package lsr.paxos.test;

import lsr.common.*;
import lsr.paxos.ReplicationException;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.channels.ServerSocketChannel;
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

    private Socket potentialLeader;
    private Socket directory;
    private DataOutputStream leaderOutputStream;
    private DataOutputStream directoryOutputStream;
    private DataInputStream leaderInputStream;
    private DataInputStream directoryInputStream;
    private boolean isLeader = false;

    public void start(int localId) throws IOException {
        FileInputStream fis = new FileInputStream("paxos.properties");
        configuration.load(fis);
        fis.close();

        List<PID> processes = loadProcessList();
        potentialLeader = new Socket(processes.get(localId).getHostname(), processes.get(localId).getClientPort());
        leaderOutputStream = new DataOutputStream(potentialLeader.getOutputStream());
        leaderInputStream = new DataInputStream(potentialLeader.getInputStream());

        initConnection();

        byte[] byteArray = "Dummy message".getBytes();
        ClientRequest request = new ClientRequest(nextRequestId(), byteArray);
        ClientCommand command = new ClientCommand(ClientCommand.CommandType.LEADER, request);

        ByteBuffer bb = ByteBuffer.allocate(command.byteSize());
        command.writeTo(bb);
        bb.flip();
        Connection connection = null;
        PreparedStatement preparedStatement = null;
        ResultSet rs1, rs2 = null;

        String url = "jdbc:postgresql://" + configuration.getProperty("db." + localId);
        String user = "postgres";
        String password = "password";
        String migrationsSql = "SELECT object_id, old_replica_set, new_replica_set, migration_acks FROM migrations where migration_complete = 'false' limit 10";
        String directoriesSql = "SELECT id, ip, port from directories where id not in (";
        String emptyDirectoriesSql = "SELECT id, ip, port from directories";

        while (true) {
            try {
                connection = DriverManager.getConnection(url, user, password);
                break;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        while (true) {
            rs1 = null;
            rs2 = null;

            leaderOutputStream.write(bb.array());
            leaderOutputStream.flush();

            ClientReply clientReply = new ClientReply(leaderInputStream);

            if (clientReply.getResult().equals(ClientReply.Result.OK)) {
                isLeader = clientReply.getValue()[0] == 1;
                logger.info("*******" + processes.get(localId).getHostname() + " is leader? " + isLeader);
                try {
                    preparedStatement = connection.prepareStatement(migrationsSql);
                    rs1 = preparedStatement.executeQuery();
                    while (rs1.next()) {
                        String objectId = rs1.getString(1);
                        System.out.print(objectId);
                        System.out.print(": ");
                        String oldReplicaSet = rs1.getString(2);
                        System.out.println(oldReplicaSet);
                        System.out.print("--->");
                        String newReplicaSet = rs1.getString(3);
                        System.out.println(newReplicaSet);
                        System.out.print(".Progress: ");
                        String migrationAcks = rs1.getString(4);
                        System.out.println(migrationAcks);

                        if (migrationAcks != null) {
                            StringTokenizer stringTokenizer = new StringTokenizer(newReplicaSet, ",");
                            while (stringTokenizer.hasMoreElements()) {
                                directoriesSql += "?,";
                            }
                            directoriesSql += ")";
                            preparedStatement = connection.prepareStatement(directoriesSql);
                            int index = 1;
                            stringTokenizer = new StringTokenizer(newReplicaSet, ",");
                            while (stringTokenizer.hasMoreElements()) {
                                preparedStatement.setInt(index, (Integer) stringTokenizer.nextElement());
                                index++;
                            }
                        } else {
                            preparedStatement = connection.prepareStatement(emptyDirectoriesSql);
                        }
                        rs2 = preparedStatement.executeQuery();
                        System.out.println("Yet to contact directories:");

                        boolean empty = true;
                        while (rs2.next()) {
                            empty = false;
                            int directoryId = rs2.getInt(1);
                            System.out.println(directoryId);
                            String directoryIP = rs2.getString(2);
                            System.out.println(directoryIP);
                            int directoryPort = rs2.getInt(3);
                            System.out.println(directoryPort);
                            System.out.println("*********----------------------------*********");

                            directory = new Socket(directoryIP, directoryPort);
                            directoryOutputStream = new DataOutputStream(directory.getOutputStream());
                            directoryInputStream = new DataInputStream(directory.getInputStream());

                            //messageSize + objectId.length + newReplicaSet.length + objectId + newReplicaSet
                            int messageSize = 4 + 4 + 4 + objectId.getBytes().length + newReplicaSet.getBytes().length;
                            ByteBuffer buffer = ByteBuffer.allocate(messageSize);
                            buffer.putInt(messageSize);
                            buffer.putInt(objectId.getBytes().length);
                            buffer.putInt(newReplicaSet.getBytes().length);
                            buffer.put(objectId.getBytes());
                            buffer.put(newReplicaSet.getBytes());
                            buffer.flip();

                            System.out.println("*********Buffer data*********");
                            logger.info("Object id length: " + buffer.getInt());
                            logger.info("New replica set length: " + buffer.getInt());
                            byte[] debugObjId = new byte[objectId.getBytes().length];
                            buffer.get(debugObjId);
                            logger.info("Object id: " + new String(debugObjId));
                            byte[] debugNewRepSet = new byte[newReplicaSet.getBytes().length];
                            buffer.get(debugNewRepSet);
                            logger.info("Object id: " + new String(debugNewRepSet));

                            buffer.rewind();

                            directoryOutputStream.write(buffer.array());
                            directoryOutputStream.flush();

                            int ack = directoryInputStream.readInt();

                            if (ack == 1) {
                                if (migrationAcks != null && migrationAcks.contains(",")) {
                                    migrationAcks += "," + directoryId;
                                } else {
                                    migrationAcks  += directoryId;
                                }
                                String sql = "UPDATE migrations SET migration_acks = ? where object_id = ?";
                                preparedStatement = connection.prepareStatement(sql);
                                preparedStatement.setString(1, newReplicaSet);
                                preparedStatement.setString(2, objectId);
                                preparedStatement.executeUpdate();
                            }
                        }

                        if (empty) {
                            String sql = "UPDATE migrations SET migration_complete = 'TRUE' where object_id = ?";
                            preparedStatement = connection.prepareStatement(sql);
                            preparedStatement.setString(1, objectId);
                            preparedStatement.executeUpdate();
                        }
                    }
                    rs1.close();
                    rs2.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
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
            leaderOutputStream.write('T'); // True
            leaderOutputStream.flush();
            clientId = leaderInputStream.readLong();
        } else {
            leaderOutputStream.write('F'); // False
            leaderOutputStream.writeLong(clientId);
            leaderOutputStream.flush();
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
