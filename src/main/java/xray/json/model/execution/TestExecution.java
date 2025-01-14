package xray.json.model.execution;

import static com.fasterxml.jackson.annotation.JsonInclude.Include;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter @JsonInclude(Include.NON_NULL)
public class TestExecution {
    private String textExecutionKey;
    @JsonProperty("info")
    private TestExecutionInfo testExecutionInfo;
    private List<TestRun> tests;
}