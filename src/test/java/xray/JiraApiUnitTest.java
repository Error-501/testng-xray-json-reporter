package xray;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.testng.Assert;
import org.testng.annotations.Test;
import xray.api.handler.JiraApiHandler;
import xray.commons.JsonHandler;
import xray.json.model.api.CreateIssueResp;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.*;

import static xray.api.handler.JiraApiHandler.*;
import static xray.commons.Constants.*;

public class JiraApiUnitTest {

    Properties props = JsonHandler.loadConfigPropertiesFile();
    File execInfo = new File(JiraApiUnitTest.class.getResource("/testExecInfo.json").getFile());
    JiraApiHandler jiraApiHandler = getJiraApiHandler(props.getProperty(PROJECT_KEY));

    @Test
    public void testGetProjectId() {
        System.out.println(jiraApiHandler.getProjectId());
    }

    @Test
    public void testCreateTestIssue() throws IOException {
        props = JsonHandler.loadConfigPropertiesFile();
        CreateIssueResp issueResp =  jiraApiHandler.createIssue(execInfo.getPath());
        System.out.println(issueResp.getExecIssueKey());
        Assert.assertNotNull(issueResp.getExecIssueKey());
    }

    @Test
    public void testGetIssue() throws IOException {
        JsonObject issueResp = jiraApiHandler.getIssue("AUTO-30");
        Assert.assertNotNull(issueResp);
    }

    @Test
    public void testAddAttachmentToIssue() throws IOException {
        props = JsonHandler.loadConfigPropertiesFile();
        String issueKey = "AUTO-30";
        JsonArray issueResp =  jiraApiHandler.addAttachment(issueKey, execInfo.getPath());
        System.out.println(issueResp.get(0).getAsJsonObject().get("id"));
        Assert.assertNotNull(issueResp.get(0).getAsJsonObject().get("id"));
    }

    @Test
    public void testErrorRetry() throws InterruptedException, ExecutionException, TimeoutException {
        props = JsonHandler.loadConfigPropertiesFile();
        int i = 0;
        ExecutorService executorService = Executors.newFixedThreadPool(50);
        Callable<Integer> callableTask = () -> {
            jiraApiHandler = getJiraApiHandler(props.getProperty(PROJECT_KEY));
            return jiraApiHandler.getProjectId();
        };
        List<Callable<Integer>> callableTasks = new ArrayList<>();
        while (i < 400) {
            callableTasks.add(callableTask);
            i++;
        }
        List<Future<Integer>> futures = executorService.invokeAll(callableTasks);
        int j = 1;
        for(Future<Integer> id : futures) {
            System.out.println(j + ": " + id.get(60, TimeUnit.SECONDS));
            j++;
        }
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(800, TimeUnit.MILLISECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
        }
    }
}