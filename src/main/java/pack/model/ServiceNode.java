package pack.model;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.Objects;

@Getter
@Setter
@ToString
public class ServiceNode {

    private String label;

    private Resource cpu, storage;

    public ServiceNode() {
        cpu = new Resource(100,100);
        storage = new Resource(100,100);
        label = "unknown";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ServiceNode that = (ServiceNode) o;
        return label.equals(that.label);
    }

    @Override
    public int hashCode() {
        return Objects.hash(label);
    }

    public ServiceNode clone() {
        ServiceNode n = new ServiceNode();
        n.setLabel(getLabel());
        n.setCpu(cpu.clone(false));
        n.setStorage(storage.clone(false));
        return n;
    }
}
