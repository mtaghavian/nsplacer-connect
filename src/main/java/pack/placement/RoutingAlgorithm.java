package pack.placement;

import lombok.Getter;
import lombok.Setter;
import pack.model.NetworkLink;
import pack.model.ServiceLink;

import java.util.List;
import java.util.Map;

@Getter
@Setter
public abstract class RoutingAlgorithm {

    private int maxNumPaths = 10;
    private boolean considerLatency = false;

    public void verifyLatencyRequirement(List<RoutingPath> foundPaths, Map<String, NetworkLink> linkMap, ServiceLink vl) {
        for (int i = 0; i < foundPaths.size(); i++) {
            if (!foundPaths.get(i).isLatencyOk(linkMap, vl)) {
                foundPaths.remove(i);
                i--;
            }
        }
    }

    public abstract List<RoutingPath> route(SearchState state, String srcNode, String dstNode, ServiceLink vl);
}
