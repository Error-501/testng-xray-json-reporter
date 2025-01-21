package xray.json.model.execution;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

import static com.fasterxml.jackson.annotation.JsonInclude.Include;

@Getter
@Setter
@JsonInclude(Include.NON_NULL)
public class TestExecution {
    private String textExecutionKey;
    @JsonProperty("info")
    private TestExecutionInfo testExecutionInfo;
    private List<TestRun> tests;
}