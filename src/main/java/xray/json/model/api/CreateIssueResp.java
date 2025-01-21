package xray.json.model.api;

import com.google.gson.annotations.SerializedName;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class CreateIssueResp {
    @SerializedName("id")
    private String execIssueId;
    @SerializedName("key")
    private String execIssueKey;
    @SerializedName("self")
    private String reqData;
}