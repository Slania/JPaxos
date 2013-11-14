package lsr.paxos.statistics;

import lsr.common.ProcessDescriptor;
import lsr.paxos.replica.ClientBatchID;

import java.util.HashMap;
import java.util.List;
import java.util.logging.Logger;

public class ReplicaRequestTimelines {

    static final Logger logger = Logger.getLogger(ReplicaRequestTimelines.class.getCanonicalName());

    public static HashMap<ClientBatchID, List<FlowPointData>> requestFlowMap = new HashMap<ClientBatchID, List<FlowPointData>>();

    public static final int processId = ProcessDescriptor.getInstance().localId;

    public static Long skew = (long) 0;

    public static void addFlowPoint(ClientBatchID clientBatchID, FlowPointData data){
        data.setTimestamp(data.getTimestamp() - skew);
        List<FlowPointData> fdp = requestFlowMap.get(clientBatchID);
        fdp.add(data);
        requestFlowMap.put(clientBatchID, fdp);
    }

    public static void logFLowPoints(ClientBatchID clientBatchID){
        List<FlowPointData> flowPointData = requestFlowMap.get(clientBatchID);
        for (FlowPointData flowPoint : flowPointData) {
            logger.info("*******" + flowPoint.toString() + "*******");
        }
    }
}
