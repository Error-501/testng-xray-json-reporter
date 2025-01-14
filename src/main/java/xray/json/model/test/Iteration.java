package xray.json.model.test;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;
import xray.json.model.execution.TestStepResult;

import java.util.List;
import java.util.Map;

@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Iteration {
    private String name;
    private String log;
    private String duration;
    private String status;
    @JsonProperty(value = "steps")
    private List<TestStepResult> stepResults;
    private List<Map<String, String>> parameters;
}