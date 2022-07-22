package pack.model;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.Objects;
import java.util.Random;

@Getter
@Setter
@ToString
public class NetworkNode implements Comparable<NetworkNode> {

    private String label;

    private Resource cpu, storage;

    private int x, y;

    public NetworkNode() {
        cpu = new Resource(1000, 1000);
        storage = new Resource(1000, 1000);
        label = "unknown";
    }

    public NetworkNode clone(boolean reset) {
        NetworkNode n = new NetworkNode();
        n.setLabel(getLabel());
        n.setCpu(cpu.clone(reset));
        n.setStorage(storage.clone(reset));
        return n;
    }

    public boolean checkFeasibility() {
        return (cpu.getAmount() >= 0 && storage.getAmount() >= 0);
    }

    public void setRandomValues(Random random) {
        cpu.setRandomValues(random);
        storage.setRandomValues(random);
    }

    public boolean canAccommodate(ServiceNode f) {
        return (cpu.getAmount() >= f.getCpu().getAmount() && storage.getAmount() >= f.getStorage().getAmount());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        NetworkNode that = (NetworkNode) o;
        return label.equals(that.label);
    }

    @Override
    public int hashCode() {
        return Objects.hash(label);
    }

    @Override
    public int compareTo(NetworkNode o) {
        return getLabel().compareTo(o.getLabel());
    }
}

