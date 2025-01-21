package xray.api.handler;

import xray.api.client.XrayCloud;
import xray.api.config.FeignController;
import xray.commons.JsonHandler;
import xray.json.model.api.CreateIssueResp;
import xray.json.model.api.XrayAuthenticateReq;

import java.io.IOException;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import static xray.commons.Constants.XRAY_CLOUD_API_ENDPOINT;

public class XrayApiHandler {

    private final String AUTH_TOKEN;
    private final String xrayEndPoint;
    private static XrayApiHandler xrayApiHandler;

    private XrayApiHandler() {
        Properties props = JsonHandler.loadConfigPropertiesFile();
        xrayEndPoint = props.getProperty(XRAY_CLOUD_API_ENDPOINT);
        XrayCloud xrayCloudApi = FeignController.getDefaultClient(XrayCloud.class, xrayEndPoint);
        XrayAuthenticateReq xrayAuthenticateReq = new XrayAuthenticateReq(props.getProperty("clientId"), props.getProperty("clientSecret"));
        AUTH_TOKEN = "Bearer " + xrayCloudApi.getAuthToken(xrayAuthenticateReq);
    }

    public static XrayApiHandler getXrayApiHandler() {
        if (xrayApiHandler == null) {
            xrayApiHandler = new XrayApiHandler();
        }
        return xrayApiHandler;
    }

    public CreateIssueResp importTestExecutions(String testReportFilePath) throws IOException {
        XrayCloud xrayCloudFileApi = FeignController.getFileClient(XrayCloud.class, xrayEndPoint);
        CreateIssueResp response;
        Path report = Path.of(testReportFilePath);
        Map<String, Object> headers = new HashMap<>();
        if (report.toFile().exists()) {
            String contentType = URLConnection.guessContentTypeFromName(String.valueOf(report.getFileName()));
            headers.put("Content-Type", contentType);
            headers.put("Authorization", AUTH_TOKEN);
            response = xrayCloudFileApi.importTestExecution(headers, Files.readAllBytes(report));
        } else {
            throw new IOException("API request failed to start. " +
                    "Kindly check xray Json Report: " + report + " is exist or valid");
        }
        return response;
    }

    public CreateIssueResp importMultiPartExecution(String testExecFilePath, String execInfoFilePath) throws IOException {
        XrayCloud xrayCloudFormApi = FeignController.getFormClient(XrayCloud.class, xrayEndPoint);
        CreateIssueResp response;
        Path report = Path.of(testExecFilePath);
        Path execInfo = Path.of(execInfoFilePath);
        Map<String, Object> headers = new HashMap<>();
        if (report.toFile().exists() && execInfo.toFile().exists()) {
            headers.put("Authorization", AUTH_TOKEN);
            response = xrayCloudFormApi.importMultiPartExecution(
                    headers, report.toFile(), execInfo.toFile());
        } else {
            throw new IOException("API request failed to start. " +
                    "Kindly check if Xray Reports present or valid");
        }
        return response;
    }
}