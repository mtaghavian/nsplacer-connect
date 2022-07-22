package pack.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.util.Random;

@Getter
@Setter
@ToString
@NoArgsConstructor
public class Resource {

    private int amount;
    private int initialAmount;

    public Resource(Integer amount, Integer max) {
        this.amount = amount;
        this.initialAmount = max;
    }

    public void addAmount(int v) {
        amount += v;
    }

    public Resource clone(boolean reset) {
        return new Resource(reset ? initialAmount : amount, initialAmount);
    }

    public void setRandomValues(Random random) {
        setAmount(Math.abs(random.nextInt()) % (initialAmount + 1));
    }

    @JsonIgnore
    public boolean isFeasible() {
        return amount >= 0 && amount <= initialAmount;
    }

    @JsonIgnore
    public double getRemainingPercent() {
        return (double) amount / initialAmount * 100.0;
    }
}
