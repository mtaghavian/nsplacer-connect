package pack.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.util.*;

@NoArgsConstructor
@Getter
@Setter
@ToString
public class NetworkGraph {

    private List<NetworkNode> nodes = new ArrayList<>();

    private List<NetworkLink> links = new ArrayList<>();

    public NetworkGraph clone(boolean initialize) {
        NetworkGraph graph = new NetworkGraph();
        for (int i = 0; i < nodes.size(); i++) {
            graph.getNodes().add(nodes.get(i).clone(initialize));
        }
        for (int i = 0; i < links.size(); i++) {
            graph.getLinks().add(links.get(i).clone(initialize));
        }
        return graph;
    }

}
