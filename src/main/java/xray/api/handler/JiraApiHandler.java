package xray.api.handler;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import lombok.Getter;
import xray.api.client.Jira;
import xray.api.config.FeignController;
import xray.commons.JsonHandler;
import xray.json.model.api.CreateIssueResp;

import java.io.IOException;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import static xray.commons.Constants.*;

public class JiraApiHandler {
    private final Properties props;
    private static JiraApiHandler jiraApiHandler;
    private final Jira jiraApi;
    private final Jira jiraFileApi;
    private final Jira jiraFormApi;
    @Getter
    private final int projectId;
    private final String projectKey;
    private final Map<String, Object> globalHeaders = new HashMap<>();
    private final Map<String, Object> formHeader = new HashMap<>() {{
        put("X-Atlassian-Token", "no-check");
    }};


    private JiraApiHandler(String projectKey) {
        this.projectKey = projectKey;
        props = JsonHandler.loadConfigPropertiesFile();
        jiraApi = FeignController.getDefaultClient(Jira.class, props.getProperty(JIRA_API_ENDPOINT));
        jiraFileApi = FeignController.getFileClient(Jira.class, props.getProperty(JIRA_API_ENDPOINT));
        jiraFormApi = FeignController.getFormClient(Jira.class, props.getProperty(JIRA_API_ENDPOINT));
        String encodedAuth = getEncodedAuthKey(props.getProperty(JIRA_USER), props.getProperty(JIRA_API_TOKEN));
        globalHeaders.put("Authorization", "Basic " + encodedAuth);
        JsonObject response = jiraApi.getProjectId(globalHeaders, projectKey);
        projectId = response.get("id").getAsInt();
    }

    public static JiraApiHandler getJiraApiHandler(String projectKey) {
        if (jiraApiHandler != null && jiraApiHandler.projectKey.equals(projectKey)) {
            return jiraApiHandler;
        }
        jiraApiHandler = new JiraApiHandler(projectKey);
        return jiraApiHandler;
    }

    public CreateIssueResp createIssue(String issueJsonFilePath) throws IOException {
        Path issueFieldsPath = Path.of(issueJsonFilePath);
        CreateIssueResp createIssueResp;
        Map<String, Object> headers = new HashMap<>(this.globalHeaders);
        if (issueFieldsPath.toFile().exists()) {
            String contentType = URLConnection.guessContentTypeFromName(String.valueOf(issueFieldsPath.getFileName()));
            headers.put("Content-Type", contentType);
            createIssueResp = jiraFileApi.createIssue(headers, Files.readAllBytes(issueFieldsPath));
        } else {
            throw new IOException("Jira json Report: " + issueFieldsPath + " is not found");
        }
        return createIssueResp;
    }

    /**
     * @param issueKey - Jira Issue ID to which the attachment needs to be added.
     * @param attachmentPath - The absolute filepath to the attachment file.
     * @return - Returns a generic gson JsonArray response.
     *          ( Should deserialize with model if needed in future )
     */
    public JsonArray addAttachment(String issueKey, String attachmentPath) throws IOException {
        JsonArray jsonResponse;
        Map<String, Object> headers = new HashMap<>(this.globalHeaders);
        headers.putAll(formHeader);
        Path attachment = Path.of(attachmentPath);
        if (attachment.toFile().exists()) {
            jsonResponse = jiraFormApi.addAttachment(headers, issueKey, attachment.toFile());
        } else {
            throw new IOException("Attachment: " + attachment + " is not found");
        }
        return jsonResponse;
    }

    private String getEncodedAuthKey(String user, String password) {
        return Base64.getEncoder().encodeToString((user + ":" + password).getBytes());
    }
}