package api.model.jira;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.util.Map;

@Getter @Setter @JsonInclude(JsonInclude.Include.NON_NULL)
public class IssueMeta {
    private String summary;
    private String description;
    private Map<String , String > project;
    @JsonProperty( value = "issuetype")
    private Map<String , String > issueType;

}