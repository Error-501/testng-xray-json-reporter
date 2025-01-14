package xray.json.model.execution;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import xray.json.model.misc.Evidence;

import java.util.List;

@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TestStepResult {
    private String status;
    private String comment;
    private String actualResult;
    private Evidence evidence;
    private List<String> defects;
}