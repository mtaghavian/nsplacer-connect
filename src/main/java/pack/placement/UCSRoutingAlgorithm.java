package pack.placement;

import lombok.Getter;
import lombok.Setter;
import pack.model.NetworkGraph;
import pack.model.NetworkLink;
import pack.model.ServiceLink;

import java.util.*;

public class UCSRoutingAlgorithm extends RoutingAlgorithm {

    private Map<String, Map<String, String>> linkNodeMap;

    public UCSRoutingAlgorithm(NetworkGraph graph) {
        linkNodeMap = new HashMap<>();
        for (NetworkLink link : graph.getLinks()) {
            if (!link.isLoop()) {
                Map<String, String> map = linkNodeMap.get(link.getSrcNode());
                if (map == null) {
                    map = new HashMap<>();
                }
                map.put(link.getDstNode(), link.getLabel());
                linkNodeMap.put(link.getSrcNode(), map);
            }
        }
    }

    @Override
    public List<RoutingPath> route(SearchState state, String srcNode, String dstNode, ServiceLink vl) {
        if (srcNode.equals(dstNode)) {
            for (NetworkLink link : state.getNetworkGraph().getLinks()) {
                if ((link.getBandwidth().getAmount() >= vl.getBandwidth().getAmount())
                        && link.isLoop() && link.getSrcNode().equals(srcNode)) {
                    List<RoutingPath> paths = new ArrayList<>();
                    paths.add(new RoutingPath(CollectionUtils.concat(new ArrayList<>(), link.getLabel())));
                    return paths;
                }
            }
            return new ArrayList<>();
        }
        for (NetworkLink link : state.getNetworkGraph().getLinks()) {
            state.getLinkMap().put(link.getLabel(), link);
        }
        Set<String> visitedNodes = new HashSet<>();
        List<UCSNode> queue = new ArrayList<>();
        UCSNode root = new UCSNode();
        root.setNodeLabel(srcNode);
        queue.add(root);
        List<RoutingPath> foundPaths = new ArrayList<>();
        int foundDepth = -1;
        while (!queue.isEmpty()) {
            Collections.sort(queue);
            UCSNode node = queue.remove(queue.size() - 1);
            int nodeDepth = node.getDepth();
            if ((foundDepth != -1) && (nodeDepth > foundDepth)) {
                break;
            }
            if (node.getNodeLabel().equals(dstNode)) {
                if (foundDepth == -1) {
                    foundDepth = node.getDepth();
                }
                foundPaths.add(new RoutingPath(getLinks(node)));
            } else {
                if (visitedNodes.contains(node.nodeLabel)) {
                    continue;
                }
                visitedNodes.add(node.getNodeLabel());
                // Expansion
                if (linkNodeMap.get(node.getNodeLabel()) == null) {
                    // We have an unconnected graph
                    continue;
                }
                for (String dstNodeLabel : linkNodeMap.get(node.getNodeLabel()).keySet()) {
                    NetworkLink link = state.getLinkMap().get(linkNodeMap.get(node.getNodeLabel()).get(dstNodeLabel));
                    if (link.getBandwidth().getAmount() >= vl.getBandwidth().getAmount()) {
                        UCSNode child = new UCSNode();
                        child.setNodeLabel(link.getDstNode());
                        child.setParent(node);
                        node.children.add(child);
                        child.setCost(node.getCost() + link.getLatency().getAmount());
                        if (!child.hasLoop() && child.getCost() <= vl.getLatency().getAmount()) {
                            queue.add(child);
                        }
                    }
                }
            }
        }
        return foundPaths;
    }

    public List<String> getLinks(UCSNode node) {
        List<String> path = new ArrayList<>();
        List<UCSNode> pathNodes = node.getPathFromRoot();
        for (int i = 0; i < pathNodes.size() - 1; i++) {
            path.add(linkNodeMap.get(pathNodes.get(i).getNodeLabel()).get(pathNodes.get(i + 1).getNodeLabel()));
        }
        return path;
    }

    @Setter
    @Getter
    private class UCSNode implements Comparable<UCSNode> {
        private UCSNode parent = null;
        private List<UCSNode> children = new ArrayList<>();
        private String nodeLabel = null;
        private Integer cost = 0;

        public List<UCSNode> getPathFromRoot() {
            List<UCSNode> path = getPathToRoot();
            Collections.reverse(path);
            return path;
        }

        public List<UCSNode> getPathToRoot() {
            List<UCSNode> path = new ArrayList<>();
            UCSNode node = this;
            path.add(node);
            while (node.getParent() != null) {
                path.add(node.getParent());
                node = node.getParent();
            }
            return path;
        }

        @Override
        public int compareTo(UCSNode o) {
            return -cost.compareTo(o.cost);
        }

        public boolean hasLoop() {
            List<UCSNode> pathToRoot = getPathToRoot();
            for (int i = 1; i < pathToRoot.size(); i++) {
                if (pathToRoot.get(i).getNodeLabel().equals(nodeLabel)) {
                    return true;
                }
            }
            return false;
        }

        public int getDepth() {
            return getPathToRoot().size() - 1;
        }
    }
}
