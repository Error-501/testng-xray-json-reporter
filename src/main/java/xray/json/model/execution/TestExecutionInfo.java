package xray.json.model.execution;

import static xray.Constants.DATE_TIME_FORMAT;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Date;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TestExecutionInfo {
    @JsonProperty(required = true, value = "project")
    private String projectKey;
    @JsonProperty(required = true)
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = DATE_TIME_FORMAT)
    private Date startDate;
    @JsonProperty(required = true, value = "finishDate")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = DATE_TIME_FORMAT)
    private Date endDate;
    private String summary;
    private String description;
    private String version;
    private String revision;
    private String user;
    private String testPlanKey;
    private String testEnvironments;
    //custom fields

    public TestExecutionInfo (String projectKey, Date startDate, Date endDate ) {
        this.projectKey = projectKey;
        this.startDate = startDate;
        this.endDate = endDate;
    }
}