package lsr.paxos.test;

import lsr.paxos.ReplicationException;
import lsr.paxos.client.Client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

public class DirectoryClient {
    private Client client;

    public void run() throws IOException, ReplicationException, InterruptedException {
        client = new Client();
        client.connect();
        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
        for (;;){
            String line;

            line = reader.readLine();

            if (line == null) {
                break;
            }

            String[] args = line.trim().split(" ");

            if (args[0].equals("bye")) {
                System.exit(0);
            }

            if (args.length < 2) {
                instructions();
                continue;
            }

            List<Integer> oldReplicaSet = new ArrayList<Integer>();
            List<Integer> newReplicaSet = new ArrayList<Integer>();
            DirectoryServiceCommand.DirectoryCommandType operation;
            String objectId = args[0];
            if (args[1].equalsIgnoreCase("DELETE") || args[1].equalsIgnoreCase("READ")) {
                operation = DirectoryServiceCommand.DirectoryCommandType.valueOf(args[1].toUpperCase());
            } else {
                String csvOldReplicaSet = args[1];
                String csvNewReplicaSet = args[2];

                String[] oldReplicas = csvOldReplicaSet.trim().split(",");
                String[] newReplicas = csvNewReplicaSet.trim().split(",");

                for (String oldReplica : oldReplicas) {
                    oldReplicaSet.add(Integer.valueOf(oldReplica));
                }
                for (String newReplica : newReplicas) {
                    newReplicaSet.add(Integer.valueOf(newReplica));
                }

                operation = DirectoryServiceCommand.DirectoryCommandType.valueOf(args[3].toUpperCase());
            }

            DirectoryServiceCommand command = new DirectoryServiceCommand(oldReplicaSet, newReplicaSet, operation, objectId);
            long start = System.currentTimeMillis();
            byte[] response = client.execute(command.toByteArray());
            long finish = System.currentTimeMillis();
            System.out.println("Run took: " + (finish - start) + "ms");
            String status = new String(response);
            System.out.println("Done! Response: " + status);
        }
    }

    private static void instructions() {
        System.out.println("Provide objId <comma-spaced old replica config> <comma-spaced new replica config> <INSERT/UPDATE/DELETE/READ>");
    }

    public static void main(String[] args) throws IOException, ReplicationException, InterruptedException {
        instructions();
        DirectoryClient client = new DirectoryClient();
        client.run();
    }
}
