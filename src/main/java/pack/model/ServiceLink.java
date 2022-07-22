package pack.model;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.Objects;

@Getter
@Setter
@ToString
public class ServiceLink {

    private String label;

    private String srcNode, dstNode;

    private Resource bandwidth, latency;

    public ServiceLink() {
        bandwidth = new Resource(100, 100);
        latency = new Resource(100, 100);
        dstNode = srcNode = label = "unknown";
    }

    public ServiceLink clone() {
        ServiceLink l = new ServiceLink();
        l.setLabel(getLabel());
        l.setSrcNode(getSrcNode());
        l.setDstNode(getDstNode());
        l.setBandwidth(bandwidth.clone(false));
        l.setLatency(latency.clone(false));
        return l;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ServiceLink that = (ServiceLink) o;
        return label.equals(that.label);
    }

    @Override
    public int hashCode() {
        return Objects.hash(label);
    }

}
