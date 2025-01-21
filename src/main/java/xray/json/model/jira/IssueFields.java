package xray.json.model.jira;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Getter;
import lombok.Setter;

//To enclose the serialisation as child of fields
@JsonTypeName("fields")
@JsonTypeInfo(include = JsonTypeInfo.As.WRAPPER_OBJECT, use = JsonTypeInfo.Id.NAME)
@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class IssueFields {
    private String summary;
    private String description;
    private ProjectField project;
    @JsonProperty(value = "issuetype")
    private IssueType issueType;
}