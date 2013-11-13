package lsr.paxos.statistics;

import lsr.common.ProcessDescriptor;
import lsr.paxos.replica.ClientBatchID;

import java.util.HashMap;
import java.util.List;

public class ReplicaRequestTimelines {

    public static HashMap<ClientBatchID, List<FlowPointData>> requestFlowMap;

    public static final int processId = ProcessDescriptor.getInstance().localId;

    public static Long skew = (long) 0;

    public static void addFlowPoint(ClientBatchID clientBatchID, FlowPointData data){
        data.setTimestamp(data.getTimestamp() - skew);
        List<FlowPointData> fdp = requestFlowMap.get(clientBatchID);
        fdp.add(data);
        requestFlowMap.put(clientBatchID, fdp);
    }
}
