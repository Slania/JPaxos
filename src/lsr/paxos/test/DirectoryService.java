package lsr.paxos.test;

import lsr.service.SimplifiedService;

import java.io.*;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DirectoryService extends SimplifiedService {

    private HashMap<DirectoryServiceCommand, Boolean> map = new HashMap<DirectoryServiceCommand, Boolean>();

    protected byte[] execute(byte[] value) {
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
        try {
            dataOutput.writeChars(command.toString());
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }

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

    private static final Logger logger = Logger.getLogger(DirectoryService.class.getCanonicalName());
}
