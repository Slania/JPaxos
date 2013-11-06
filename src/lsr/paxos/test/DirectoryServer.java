package lsr.paxos.test;

import lsr.common.Configuration;
import lsr.paxos.replica.Replica;

import java.io.IOException;
import java.util.concurrent.ExecutionException;

public class DirectoryServer {
    public static void main(String[] args) throws IOException, InterruptedException,
            ExecutionException {
        if (args.length > 2) {
            usage();
            System.exit(1);
        }
        int localId = Integer.parseInt(args[0]);
        Configuration process = new Configuration();

        Replica replica = new Replica(process, localId, new DirectoryService());

        replica.start();
        System.in.read();
        System.exit(-1);
    }

    private static void usage() {
        System.out.println("Invalid arguments. Usage:\n"
                           + "   java lsr.paxos.Replica <replicaID> [echo]");
    }
}
