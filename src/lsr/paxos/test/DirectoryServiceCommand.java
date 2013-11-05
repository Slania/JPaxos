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

    public DirectoryServiceCommand(DirectoryCommand directoryCommand, Long value, List<Integer> oldReplicaSet, List<Integer> newReplicaSet, DirectoryCommandType directoryCommandType) {
        this.directoryCommand = directoryCommand;
        this.oldReplicaSet = oldReplicaSet;
        this.newReplicaSet = newReplicaSet;
        this.directoryCommandType = directoryCommandType;
    }

    public DirectoryServiceCommand(byte[] bytes, DirectoryCommand directoryCommand, List<Integer> oldReplicaSet, List<Integer> newReplicaSet, DirectoryCommandType directoryCommandType) throws IOException {
        this.directoryCommand = directoryCommand;
        this.oldReplicaSet = oldReplicaSet;
        this.newReplicaSet = newReplicaSet;
        this.directoryCommandType = directoryCommandType;
        DataInputStream dataInput = new DataInputStream(new ByteArrayInputStream(bytes));
    }

    public byte[] toByteArray() {
        ByteBuffer buffer = ByteBuffer.allocate(16);
        return buffer.array();
    }

    public String toString() {
        return "";
    }
}
