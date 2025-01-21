package xray.json.model.jira;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
public class ProjectField {
    private int id;
    private String key;

    public ProjectField(String key) {
        this.key = key;
    }

    public ProjectField(int id) {
        this.id = id;
    }
}