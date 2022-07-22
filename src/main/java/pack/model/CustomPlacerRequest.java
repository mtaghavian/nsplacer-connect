package pack.model;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CustomPlacerRequest {

    private EvaluationParams params;
    private ServiceGraph service;
    private NetworkGraph network;
}
