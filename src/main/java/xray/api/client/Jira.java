package xray.api.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import feign.HeaderMap;
import feign.Headers;
import feign.Param;
import feign.RequestLine;
import xray.json.model.api.CreateIssueResp;

import java.io.File;
import java.util.Map;

public interface Jira {

    @RequestLine("GET /rest/api/3/project/{projectKey}")
    JsonObject getProjectId(@HeaderMap Map<String, Object> headers, @Param("projectKey") String projectKey);

    @RequestLine("POST /rest/api/3/issue")
    @Headers("Content-Type: application/json")
    CreateIssueResp createIssue(@HeaderMap Map<String, Object> headers, @Param("file") byte[] file);

    @RequestLine("POST /rest/api/3/issue/{issueKey}/attachments")
    @Headers("Content-Type: multipart/form-data")
    JsonArray addAttachment(@HeaderMap Map<String, Object> headers, @Param("issueKey") String projectKey, @Param("file") File attachment);
}