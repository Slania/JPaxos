package lsr.paxos.statistics;

public class FlowPointData {

    //points where we want to color/track the request flow
    public enum FlowPoint {
        NioClientProxy_Execute, RequestBatcher_Run, RequestBatcher_SendBatch, ClientBatchManager_SendToAll, ClientBatchManager_BatchSent,
        ClientBatchManager_OnForwardClientBatch, Paxos_Propose, Paxos_EnqueueRequest, ActiveBatcher_DispatchRequests, ProposerImpl_EnqueueProposal,
        ProposerImpl_Propose, Paxos_OnMessageReceived, Paxos_MessageEvent_Run, Learner_OnAccept, Acceptor_OnPropose, Paxos_Decide,
        ClientBatchManager_InnerOnBatchOrdered, ClientBatchManager_ExecuteRequests, Replica_ExecuteClientBatch, SimplifiedMapService_Execute,
        ClientRequestManager_OnRequestExecuted, NioClientProxy_Send, ClientRequestBatcher_SendBatch
    }

    private FlowPoint flowPoint;
    private Long timestamp;

    public FlowPointData(FlowPoint flowPoint, Long timestamp) {
        this.flowPoint = flowPoint;
        this.timestamp = timestamp;
    }

    public FlowPoint getFlowPoint() {
        return flowPoint;
    }

    public void setFlowPoint(FlowPoint flowPoint) {
        this.flowPoint = flowPoint;
    }

    public Long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Long timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    public String toString() {
        return flowPoint.toString() + ": " + timestamp.toString() + "\n";
    }
}
