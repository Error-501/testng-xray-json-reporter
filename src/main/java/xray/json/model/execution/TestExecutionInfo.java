package xray.json.model.execution;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.text.SimpleDateFormat;
import java.util.Date;
import lombok.Getter;
import lombok.Setter;
import xray.Constants;

@Getter @Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TestExecutionInfo {
    private String projectKey;
    private String summary;
    private String description;
    private String version;
    private String revision;
    private String user;
    private String startDate;
    private String endDate;
    private String testPlanKey;
    private String testEnvironments;
    //custom fields

    public TestExecutionInfo (String projectKey, Date startDate, Date endDate ) {
        SimpleDateFormat dateFormatter = new SimpleDateFormat(Constants.DATE_TIME_FORMAT);
        this.projectKey = projectKey;
        this.startDate = dateFormatter.format(startDate);
        this.endDate = dateFormatter.format(endDate);
    }
}
