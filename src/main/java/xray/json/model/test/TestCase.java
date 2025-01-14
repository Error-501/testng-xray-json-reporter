package xray.json.model.test;

import java.util.List;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sun.source.doctree.SummaryTree;
import lombok.Getter;
import lombok.Setter;
import xray.json.model.misc.TestType;

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