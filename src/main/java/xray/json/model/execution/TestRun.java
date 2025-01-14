package xray.json.model.execution;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.LocalDate;
import java.util.ArrayList;
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
    private LocalDate start;
    private LocalDate finish;
    private String comment;
    private String executedBy;
    private String assignee;
    private TestStatus status;
    private TestCase TestInfo;
    private ArrayList<TestStepResult> stepResults;
    private ArrayList<Iteration> iterations;
    private ArrayList<String> defects;
    private ArrayList<Evidence> evidence;
    private ArrayList<CustomField> customFields;
}
