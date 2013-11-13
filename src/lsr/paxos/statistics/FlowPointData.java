package lsr.paxos.statistics;

public class FlowPointData {

    //points where we want to color/track the request flow
    public enum FlowPoint {
        PLACEHOLDER
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
}
