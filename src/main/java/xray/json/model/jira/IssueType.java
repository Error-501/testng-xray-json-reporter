package xray.json.model.jira;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.Setter;

/**
 * A common def for required fields in create Issue call.
 * <p>
 * Applicable to IssueType, Project
 * (Should create Separate Field Definitions if needed in future)
 */
@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
public class IssueType {
    private int id;
    private String name;

    public IssueType(String name) {
        this.name = name;
    }

    public IssueType(int id) {
        this.id = id;
    }
}