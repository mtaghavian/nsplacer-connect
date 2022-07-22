package pack.placement;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import pack.model.*;

import java.util.*;

@NoArgsConstructor
@Getter
@Setter
@ToString
public class SearchState implements Comparable<SearchState> {

    public static Random random = new Random(System.currentTimeMillis());
    private NetworkGraph networkGraph;
    private ServiceGraph serviceGraph;
    private Double objectiveValue = 0.0;
    private Integer depth;
    private List<ServiceNode> placedServiceNodes;
    private List<String> placedNetworkNodes;
    private List<ServiceLink> placedServiceLinks;
    private List<RoutingPath> placedPaths;
    private Map<String, NetworkLink> linkMap;
    private Map<String, Map<String, String>> nodeLinkMap;
    private Map<String, NetworkNode> nodeMap;
    private RoutingAlgorithm routingAlgorithm;

    public SearchState(NetworkGraph networkGraph, ServiceGraph serviceGraph, RoutingAlgorithm routingAlgorithm, int depth) {
        this.networkGraph = networkGraph;
        this.serviceGraph = serviceGraph;
        this.routingAlgorithm = routingAlgorithm;
        this.depth = depth;
        linkMap = new HashMap<>();
        nodeLinkMap = new HashMap<>();
        for (NetworkLink link : networkGraph.getLinks()) {
            if (!link.isLoop()) {
                linkMap.put(link.getLabel(), link);

                Map<String, String> m = nodeLinkMap.get(link.getSrcNode());
                if (m == null) {
                    m = new HashMap<>();
                }
                m.put(link.getDstNode(), link.getLabel());
                nodeLinkMap.put(link.getSrcNode(), m);
            }
        }
        nodeMap = new HashMap<>();
        for (NetworkNode node : networkGraph.getNodes()) {
            nodeMap.put(node.getLabel(), node);
        }
        if (depth == 0) {
            serviceGraph.getTraversedNodes();
            //Collections.shuffle(serviceGraph.getTraversedNodes());
            placedServiceNodes = new ArrayList<>();
            placedNetworkNodes = new ArrayList<>();
            placedServiceLinks = new ArrayList<>();
            placedPaths = new ArrayList<>();
        }
    }

    private void generateIndices(List<List<Integer>> indicesList, List<List<RoutingPath>> paths, List<Integer> indices, int offset) {
        if (offset == paths.size()) {
            indicesList.add(indices);
        } else {
            for (int i = 0; i < paths.get(offset).size(); i++) {
                generateIndices(indicesList, paths, CollectionUtils.concat(new ArrayList<>(indices), i), offset + 1);
            }
        }
    }

    public void updateObjectiveValue() {
        int sum = 0;
        for (RoutingPath path : placedPaths) {
            for (String link : path.getLinks()) {
                sum += linkMap.get(link).getLatency().getAmount();
            }
        }
        objectiveValue = (double) sum;
    }

    public List<SearchState> expand() {
        List<SearchState> children = new ArrayList<>();
        ServiceNode placingServiceNode = serviceGraph.getTraversedNodes().get(depth);
        for (NetworkNode placingNode : networkGraph.getNodes()) {
            expandStateOverNode(placingNode, placingServiceNode, children);
        }
        Collections.sort(children);
        return children;
    }

    private List<RoutingPath> generatePaths(ServiceLink serviceLink, NetworkGraph networkGraph) {
        int srcNodeIndex = placedServiceNodes.indexOf(serviceGraph.getNodeMap().get(serviceLink.getSrcNode()));
        int dstNodeIndex = placedServiceNodes.indexOf(serviceGraph.getNodeMap().get(serviceLink.getDstNode()));
        boolean isSrcPlaced = srcNodeIndex >= 0;
        boolean isDstPlaced = dstNodeIndex >= 0;
        List<RoutingPath> paths = new ArrayList<>();
        if (isSrcPlaced && isDstPlaced) {
            String srcNode = placedNetworkNodes.get(srcNodeIndex);
            String dstNode = placedNetworkNodes.get(dstNodeIndex);
            paths.addAll(routingAlgorithm.route(this, srcNode, dstNode, serviceLink));
        } else if (isSrcPlaced) { // only source NF is placed
            String srcNode = placedNetworkNodes.get(srcNodeIndex);
            for (String dstNode : nodeMap.keySet()) {
                if (!dstNode.equals(srcNode)) {
                    List<RoutingPath> routes = routingAlgorithm.route(this, srcNode, dstNode, serviceLink);
                    if (!routes.isEmpty()) {
                        paths.addAll(routes);
                    }
                }
            }
        } else if (isDstPlaced) { // only destination NF is placed
            String dstNode = placedNetworkNodes.get(dstNodeIndex);
            for (String srcNode : nodeMap.keySet()) {
                if (!dstNode.equals(srcNode)) {
                    List<RoutingPath> routes = routingAlgorithm.route(this, srcNode, dstNode, serviceLink);
                    if (!routes.isEmpty()) {
                        paths.addAll(routes);
                    }
                }
            }
        } else { // Neither source nor destination is placed
            for (String srcNode : nodeMap.keySet()) {
                for (String dstNode : nodeMap.keySet()) {
                    if (!dstNode.equals(srcNode)) {
                        List<RoutingPath> routes = routingAlgorithm.route(this, srcNode, dstNode, serviceLink);
                        if (!routes.isEmpty()) {
                            paths.addAll(routes);
                        }
                    }
                }
            }
        }
        return paths;
    }

    private void expandStateOverNode(NetworkNode placingNode, ServiceNode placingServiceNode, List<SearchState> children) {
        NetworkGraph graph = networkGraph.clone(false);
        for (NetworkNode node : graph.getNodes()) {
            if (node.getLabel().equals(placingNode.getLabel())) {
                placingNode = node;
                break;
            }
        }

        if (!placingNode.canAccommodate(placingServiceNode) || placedNetworkNodes.contains(placingNode.getLabel())) {
            return;
        }

        SearchState child = new SearchState(graph, serviceGraph, routingAlgorithm, depth + 1);
        child.setPlacedNetworkNodes(CollectionUtils.concat(new ArrayList<>(placedNetworkNodes), placingNode.getLabel()));
        child.setPlacedServiceNodes(CollectionUtils.concat(new ArrayList<>(placedServiceNodes), placingServiceNode));
        child.setPlacedServiceLinks(new ArrayList<>(placedServiceLinks));
        child.setPlacedPaths(new ArrayList<>(placedPaths));

        // Perform placement for the service node
        placingNode.getCpu().addAmount(-placingServiceNode.getCpu().getAmount());
        placingNode.getStorage().addAmount(-placingServiceNode.getStorage().getAmount());

        // Perform placement for the service links
        child.performLinkPlacements();

        // Update objective value
        if (child.isFeasible()) {
            child.updateObjectiveValue();
            children.add(child);
        }
    }

    private void performLinkPlacements() {
        HashMap<String, Integer> placedSLMap = new HashMap<>();
        for (int i = 0; i < placedServiceLinks.size(); i++) {
            placedSLMap.put(placedServiceLinks.get(i).getLabel(), i);
        }
        HashMap<String, Integer> placedSNMap = new HashMap<>();
        for (int i = 0; i < placedServiceNodes.size(); i++) {
            placedSNMap.put(placedServiceNodes.get(i).getLabel(), i);
        }

        boolean sequentialPlacement = true;

        List<ServiceLink> serviceLinksToBePlaced = new ArrayList<>();
        List<List<RoutingPath>> candidatePaths = new ArrayList<>();
        boolean routingFailed = false;
        for (ServiceLink vl : serviceGraph.getLinks()) {
            if (!placedSLMap.containsKey(vl.getLabel())) {
                if (placedSNMap.containsKey(vl.getSrcNode()) && placedSNMap.containsKey(vl.getDstNode())) {
                    String srcNode = placedNetworkNodes.get(placedSNMap.get(vl.getSrcNode()));
                    String dstNode = placedNetworkNodes.get(placedSNMap.get(vl.getDstNode()));
                    List<RoutingPath> routingPaths = routingAlgorithm.route(this, srcNode, dstNode, vl);

                    routingPaths = routingAlgorithm.route(this, srcNode, dstNode, vl);

                    serviceLinksToBePlaced.add(vl);
                    candidatePaths.add(routingPaths);

                    // For performance reasons, if we could not find any path for a VL
                    // This placement is not feasible and we can skip placing other VLs
                    if (routingPaths.isEmpty()) {
                        routingFailed = true;
                        placedServiceLinks.add(vl);
                        placedPaths.add(new RoutingPath());
                        break;
                    }

                    if (sequentialPlacement) {
                        RoutingPath path = routingPaths.get(0);
                        for (String linkLabel : path.getLinks()) {
                            NetworkLink link = linkMap.get(linkLabel);
                            link.getBandwidth().addAmount(-vl.getBandwidth().getAmount());
                        }
                        placedServiceLinks.add(vl);
                        placedPaths.add(path);
                    }
                }
            }
        }

    }

    public Map<String, String> getPlacementNodeMap() {
        Map<String, String> map = new HashMap<>();
        List<ServiceNode> traversedNodes = serviceGraph.getTraversedNodes();
        for (int i = 0; i < traversedNodes.size(); i++) {
            map.put(traversedNodes.get(i).getLabel(), placedNetworkNodes.get(i));
        }
        return map;
    }

    public Map<String, String> getPlacementLinkMap() {
        Map<String, String> map = new HashMap<>();
        for (int i = 0; i < placedServiceLinks.size(); i++) {
            StringBuilder sb = new StringBuilder();
            sb.append(placedPaths.get(i).getLinks().get(0));
            for (int j = 1; j < placedPaths.get(i).getLinks().size(); j++) {
                sb.append("-" + placedPaths.get(i).getLinks().get(j));
            }
            map.put(placedServiceLinks.get(i).getLabel(), sb.toString());
        }
        return map;
    }

    public int calcPlacedLatency() {
        int sum = 0;
        for (RoutingPath path : placedPaths) {
            for (String l : path.getLinks()) {
                sum += linkMap.get(l).getLatency().getAmount();
            }
        }
        return sum;
    }

    @Override
    public int compareTo(SearchState o) {
        int cmp = objectiveValue.compareTo(o.getObjectiveValue());
        cmp = -cmp;
        if (cmp != 0) {
            return cmp;
        }
        return cmp;
    }

    public boolean isFeasible() {
        // Check infeasibility for nodes
        for (int i = 0; i < networkGraph.getNodes().size(); i++) {
            if (!networkGraph.getNodes().get(i).checkFeasibility()) {
                return false;
            }
        }
        // Check infeasibility for links
        for (RoutingPath p : placedPaths) {
            if (p.getLinks() == null || p.getLinks().isEmpty()) {
                return false;
            }
        }
        // Check to make sure that there is no two NFs placed over a single node
        HashSet<String> bag = new HashSet<>();
        for (String nodeLabel : placedNetworkNodes) {
            if (bag.contains(nodeLabel)) {
                return false;
            }
            bag.add(nodeLabel);
        }
        return true;
    }

    public boolean isTerminal() {
        return depth == serviceGraph.getTraversedNodes().size();
    }
}
