package xray;

import xray.api.handler.XrayApiHandler;
import xray.commons.JsonHandler;
import org.testng.annotations.Test;
import xray.json.model.api.CreateIssueResp;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Properties;
import java.util.concurrent.*;

import static xray.api.handler.XrayApiHandler.*;
import static xray.commons.Constants.REPORT_FILENAME;

public class XrayImportUnitTest {

    Properties props;
    File testExec = new File(Objects.requireNonNull(XrayImportUnitTest.class.getResource("/testExec.json")).getFile());
    File execInfo = new File(Objects.requireNonNull(XrayImportUnitTest.class.getResource("/testExecInfo.json")).getFile());
    XrayApiHandler xrayApi = getXrayApiHandler();

    @Test
    public void validateJsonSchema() throws IOException {
        props = JsonHandler.loadConfigPropertiesFile();
       JsonHandler.validateXrayJsonSchema(testExec.getPath() + props.getProperty(REPORT_FILENAME));
    }


    @Test
    public void importXrayResults() throws IOException {
        CreateIssueResp resp = xrayApi.importTestExecutions(testExec.getPath());
        System.out.println(resp.getExecIssueKey());
    }

    @Test
    public void importMultiPartReport() throws IOException {
        CreateIssueResp resp = xrayApi.importMultiPartExecution(testExec.getPath(), execInfo.getPath());
        System.out.println(resp.getExecIssueKey());
    }

    @Test
    public void testErrorRetry() throws InterruptedException, ExecutionException, TimeoutException {
        props = JsonHandler.loadConfigPropertiesFile();
        int i = 0;
        ExecutorService executorService = Executors.newFixedThreadPool(5);
        Callable<Integer> callableTask = () -> {
            xrayApi = getXrayApiHandler();
            System.out.println(Thread.currentThread().getId() + ":" +
                    xrayApi.importTestExecutions(testExec.getPath()).getExecIssueKey());
            return (int) Thread.currentThread().getId();
        };
        List<Callable<Integer>> callableTasks = new ArrayList<>();
        while (i < 300) {
            callableTasks.add(callableTask);
            i++;
        }
        System.out.println(callableTasks.size());
        List<Future<Integer>> futures = executorService.invokeAll(callableTasks);
        int m = 1;
        for( Future<Integer> future : futures ) {
            System.out.println(
                    "Count : " + m + " Result" +  future.get(60, TimeUnit.SECONDS));
            m++;
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