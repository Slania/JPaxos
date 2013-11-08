package lsr.paxos.statistics;

import lsr.common.ProcessDescriptor;

import java.util.List;

public class ReplicaRequestTimelines {

    public static List<FlowPointData> flowPointData;

    public static final int processId = ProcessDescriptor.getInstance().localId;

    public static Long skew = (long) 0;

    public static void addFlowPoint(FlowPointData data){
        data.setTimestamp(data.getTimestamp() - skew);
        flowPointData.add(data);
    }
}
