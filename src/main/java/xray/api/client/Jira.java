package xray.api.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import feign.*;
import xray.json.model.api.CreateIssueResp;

import java.io.File;
import java.util.Map;

public interface Jira {

    @RequestLine("GET /rest/api/latest/project/{projectKey}")
    JsonObject getProjectId(@HeaderMap Map<String, Object> headers, @Param("projectKey") String projectKey);

    @RequestLine("POST /rest/api/latest/issue")
    @Headers("Content-Type: application/json")
    CreateIssueResp createIssue(@HeaderMap Map<String, Object> headers, @Param("file") byte[] file);

    @RequestLine("GET /rest/api/latest/issue/{issueKey}")
    @Headers("Content-Type: application/json")
    Response getIssue(@HeaderMap Map<String, Object> headers, @Param("issueKey") String issueKey);

    @RequestLine("POST /rest/api/latest/issue/{issueKey}/attachments")
    @Headers("Content-Type: multipart/form-data")
    JsonArray addAttachment(@HeaderMap Map<String, Object> headers, @Param("issueKey") String issueKey, @Param("file") File attachment);
}