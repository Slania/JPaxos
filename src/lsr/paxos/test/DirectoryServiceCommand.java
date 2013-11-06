package lsr.paxos.test;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.Serializable;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

public class DirectoryServiceCommand implements Serializable {
    private static final long serialVersionUID = 1L;
    private List<Integer> oldReplicaSet;
    private List<Integer> newReplicaSet;
    private DirectoryCommandType directoryCommandType;
    private byte[] objectId;
    private boolean migrationComplete;

    public enum DirectoryCommandType {
        INSERT, DELETE, READ, UPDATE
    }

    public DirectoryServiceCommand(List<Integer> oldReplicaSet, List<Integer> newReplicaSet, DirectoryCommandType directoryCommandType, String objectId, boolean migrationComplete) {
        this.oldReplicaSet = oldReplicaSet;
        this.newReplicaSet = newReplicaSet;
        this.directoryCommandType = directoryCommandType;
        this.migrationComplete = migrationComplete;
        this.objectId = objectId.getBytes();
    }

    public DirectoryServiceCommand(List<Integer> oldReplicaSet, List<Integer> newReplicaSet, DirectoryCommandType directoryCommandType, String objectId) {
        this.oldReplicaSet = oldReplicaSet;
        this.newReplicaSet = newReplicaSet;
        this.directoryCommandType = directoryCommandType;
        this.objectId = objectId.getBytes();
        //will not be read/used
        migrationComplete = false;
    }

    public DirectoryServiceCommand(byte[] bytes) throws IOException {
        DataInputStream dataInput = new DataInputStream(new ByteArrayInputStream(bytes));
        this.directoryCommandType = DirectoryCommandType.values()[dataInput.readInt()];
        int objectIdLength = dataInput.readInt();
        int oldReplicaSetSize = dataInput.readInt();
        int newReplicaSetSize = dataInput.readInt();
        objectId = new byte[objectIdLength];
        dataInput.readFully(objectId, 0, objectIdLength);
        oldReplicaSet = new ArrayList<Integer>();
        newReplicaSet = new ArrayList<Integer>();
        if (oldReplicaSetSize > 0){
            for (int i = 1; i <= oldReplicaSetSize; i++){
                oldReplicaSet.add(dataInput.readInt());
            }
        }
        if (newReplicaSetSize > 0){
            for (int i = 1; i <= newReplicaSetSize; i++){
                oldReplicaSet.add(dataInput.readInt());
            }
        }
        migrationComplete = dataInput.readByte() == 1;
    }

    public List<Integer> getOldReplicaSet() {
        return oldReplicaSet;
    }

    public List<Integer> getNewReplicaSet() {
        return newReplicaSet;
    }

    public DirectoryCommandType getDirectoryCommandType() {
        return directoryCommandType;
    }

    public byte[] getObjectId() {
        return objectId;
    }

    public boolean isMigrationComplete() {
        return migrationComplete;
    }

    public byte[] toByteArray() {
        //4 for the ordinal of the CommandType
        int numOfBytes = 4 + (oldReplicaSet.size() * (Integer.SIZE / Byte.SIZE)) + (newReplicaSet.size() * (Integer.SIZE / Byte.SIZE)) + 1 + objectId.length;
        //4 + 4 for the integer sizes of the 2 lists and the integer length of the byte array
        numOfBytes += 4 + 4 + 4;
        ByteBuffer buffer = ByteBuffer.allocate(numOfBytes);
        buffer.putInt(directoryCommandType.ordinal());
        buffer.putInt(objectId.length);
        buffer.putInt(oldReplicaSet.size());
        buffer.putInt(newReplicaSet.size());
        buffer.put(objectId);
        for (Integer integer : oldReplicaSet) {
            buffer.putInt(integer);
        }
        for (Integer integer : newReplicaSet) {
            buffer.putInt(integer);
        }
        buffer.put((byte) (migrationComplete ? 1 : 0));
        return buffer.array();
    }

    public String toString() {
        StringBuilder builder = new StringBuilder();
        String csvOldReplicas;
        String csvNewReplicas;

        for (Integer integer : oldReplicaSet) {
            builder.append(integer.toString());
            builder.append(",");
        }
        builder.deleteCharAt(builder.length() - 1);
        csvOldReplicas = builder.toString();

        builder = new StringBuilder();
        for (Integer integer : newReplicaSet) {
            builder.append(integer.toString());
            builder.append(",");
        }
        builder.deleteCharAt(builder.length() - 1);
        csvNewReplicas = builder.toString();

        return "Object " + objectId + " migrating from " + csvOldReplicas + " to " + csvNewReplicas + ". Migration status: " + migrationComplete;
    }

    @Override
    public int hashCode() {
        return super.hashCode();    //To change body of overridden methods use File | Settings | File Templates.
    }

    @Override
    public boolean equals(Object obj) {
        if (this.getClass() != obj.getClass())
            return false;
        if (this == obj)
            return true;
        DirectoryServiceCommand that = (DirectoryServiceCommand) obj;
        return new String(objectId).equals(new String(that.getObjectId()));
    }
}
