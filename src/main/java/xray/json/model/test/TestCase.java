package xray.json.model.test;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;
import xray.json.model.misc.TestType;

import java.util.List;

@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TestCase {
    @JsonProperty(required = true)
    private String projectKey;
    private String summary;
    @JsonProperty(required = true)
    private TestType type;
    private String requirementKeys;
    private List<String> labels;
    private List<TestStep> steps;
    private String definition;
}