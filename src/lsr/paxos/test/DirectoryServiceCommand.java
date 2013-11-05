package lsr.paxos.test;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.Serializable;
import java.nio.ByteBuffer;
import java.util.List;

public class DirectoryServiceCommand implements Serializable {
    private static final long serialVersionUID = 1L;
    private final DirectoryCommand directoryCommand;
    private final List<Integer> oldReplicaSet;
    private final List<Integer> newReplicaSet;
    private final DirectoryCommandType directoryCommandType;

    public enum DirectoryCommandType {
        INSERT, DELETE, READ, UPDATE
    }

    public DirectoryServiceCommand(DirectoryCommand directoryCommand, List<Integer> oldReplicaSet, List<Integer> newReplicaSet, DirectoryCommandType directoryCommandType) {
        this.directoryCommand = directoryCommand;
        this.oldReplicaSet = oldReplicaSet;
        this.newReplicaSet = newReplicaSet;
        this.directoryCommandType = directoryCommandType;
    }

    public DirectoryServiceCommand(byte[] bytes) throws IOException {
        DataInputStream dataInput = new DataInputStream(new ByteArrayInputStream(bytes));
        this.directoryCommandType = DirectoryCommandType.values()[dataInput.readInt()];
        this.oldReplicaSet = oldReplicaSet;
        this.newReplicaSet = newReplicaSet;
        this.directoryCommandType = directoryCommandType;
        DataInputStream dataInput = new DataInputStream(new ByteArrayInputStream(bytes));
    }

    public byte[] toByteArray() {
        //4 for the ordinal of the CommandType
        int numOfBytes = 4 + (oldReplicaSet.size() * (Integer.SIZE / Byte.SIZE)) + (newReplicaSet.size() * (Integer.SIZE / Byte.SIZE));
        //4 + 4 for the sizes of the 2 lists
        numOfBytes += 4 + 4;
        ByteBuffer buffer = ByteBuffer.allocate(numOfBytes);
        buffer.putInt(directoryCommandType.ordinal());
        buffer.putInt(oldReplicaSet.size());
        buffer.putInt(newReplicaSet.size());
        for (Integer integer : oldReplicaSet) {
            buffer.putInt(integer);
        }
        for (Integer integer : newReplicaSet) {
            buffer.putInt(integer);
        }
        return buffer.array();
    }

    public String toString() {
        return "";
    }
}
