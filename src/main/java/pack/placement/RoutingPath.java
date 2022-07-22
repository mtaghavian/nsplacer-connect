package pack.placement;

import lombok.Getter;
import lombok.Setter;
import pack.model.NetworkLink;
import pack.model.ServiceLink;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Getter
@Setter
public class RoutingPath {

    private List<String> links;

    public RoutingPath() {
        links = new ArrayList<>();
    }

    public RoutingPath(List<String> links) {
        this.links = links;
    }

    public boolean isLatencyOk(Map<String, NetworkLink> map, ServiceLink vl) {
        int sum = 0;
        for (String link : links) {
            sum += map.get(link).getLatency().getAmount();
        }
        return sum <= vl.getLatency().getAmount();
    }

    public boolean canAccommodate(Map<String, NetworkLink> map, ServiceLink serviceLink) {
        for (String linkLabel : links) {
            if (!map.get(linkLabel).canAccommodate(serviceLink)) {
                return false;
            }
        }
        return true;
    }

    public String getSrcNode(Map<String, NetworkLink> map) {
        return links.isEmpty() ? null : map.get(links.get(0)).getSrcNode();
    }

    public String getDstNode(Map<String, NetworkLink> map) {
        return links.isEmpty() ? null : map.get(links.get(links.size() - 1)).getDstNode();
    }

    public void takeBandwidth(Map<String, NetworkLink> linkMap, int v) {
        for (String link:links) {
            linkMap.get(link).getBandwidth().addAmount(-v);
        }
    }
}
