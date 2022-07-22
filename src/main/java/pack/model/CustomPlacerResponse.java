package pack.model;

import lombok.Getter;
import lombok.Setter;
import pack.placement.RoutingPath;

import java.util.List;

@Getter
@Setter
public class CustomPlacerResponse {

    private boolean succeeded;
    private List<ServiceNode> placedServiceNodes;
    private List<String> placedNetworkNodes;
    private List<ServiceLink> placedServiceLinks;
    private List<RoutingPath> placedPaths;
}
