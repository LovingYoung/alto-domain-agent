package org.snlab.unicorn.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
    "coefficient",
    "flow-id"
})
public class AneMatrix {

    @JsonProperty("coefficient")
    private Double coefficient;

    /**
     * (Required)
     */
    @JsonProperty("flow-id")
    private String flowId;

    @JsonProperty("coefficient")
    public Double getCoefficient() {
        return coefficient;
    }

    @JsonProperty("coefficient")
    public void setCoefficient(Double coefficient) {
        this.coefficient = coefficient;
    }

    @JsonProperty("flow-id")
    public String getFlowId() {
        return flowId;
    }

    @JsonProperty("flow-id")
    public void setFlowId(String flowId) {
        this.flowId = flowId;
    }

}
