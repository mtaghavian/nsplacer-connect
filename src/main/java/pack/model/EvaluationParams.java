package pack.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class EvaluationParams {

    private String networkTopology;
    private String snCPU;
    private String snStorage;
    private String snBandwidth;
    private String snLatency;

    private String serviceTopology;
    private Integer serviceSize;
    private String sgCPU;
    private String sgStorage;
    private String sgBandwidth;
    private String sgLatency;

    private Integer timeout;
    private String routing;
    private String strategy;
    private String approach;
    private String terminationType;
    private Boolean shuffle;

}
