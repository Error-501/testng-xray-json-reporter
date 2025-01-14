package xray.json.model.execution;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.LocalDate;
import java.util.List;

import lombok.Getter;
import lombok.Setter;
import xray.json.model.misc.CustomField;
import xray.json.model.misc.Evidence;
import xray.json.model.misc.TestStatus;
import xray.json.model.test.Iteration;
import xray.json.model.test.TestCase;

@Getter @Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TestRun {
    private String testKey;
    private String start;
    private String finish;
    private String comment;
    private String executedBy;
    private String assignee;
    private String status;
    private TestCase TestInfo;
    private List<TestStepResult> stepResults;
    private List<Iteration> iterations;
    private List<String> defects;
    private List<Evidence> evidence;
    private List<CustomField> customFields;
}