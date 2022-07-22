package pack.placement;

import lombok.Getter;
import lombok.Setter;

import java.util.*;

@Getter
@Setter
public class MyStack {

    private List<SearchState> list = new ArrayList<>();
    private int maxReachedSize = 0;

    public MyStack() {
    }

    public void put(List<SearchState> states) {
        for (int i = 0; i < states.size(); i++) {
            list.add(states.get(i));
        }
        maxReachedSize = Math.max(maxReachedSize, list.size());
    }
    public boolean isEmpty() {
        return list.isEmpty();
    }

    public int size() {
        return list.size();
    }

    public SearchState take() {
        if (isEmpty()) {
            return null;
        }
        return list.remove(list.size() - 1);
    }

}

