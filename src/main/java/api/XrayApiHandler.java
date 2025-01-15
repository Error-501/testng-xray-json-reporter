package api;

import api.client.FeignClientConfiguration;
import api.client.service.XrayCloud;
import api.model.xray.AuthenticateReq;
import feign.Response;
import commons.PropertyHandler;

import java.io.IOException;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import static xray.Constants.REPORT_FILENAME;

public class XrayApiHandler {

    private final String AUTH_TOKEN;
    private final XrayCloud xrayCloudApi;
    private final XrayCloud xrayCloudFileApi;
    private final Properties props;
    private static XrayApiHandler xrayApiHandler;

    private XrayApiHandler() {
        FeignClientConfiguration feign = new FeignClientConfiguration();
        xrayCloudApi = feign.getXrayClient();
        xrayCloudFileApi = feign.getXrayFileClient();
        props = PropertyHandler.loadConfigPropertiesFile();
        AuthenticateReq authenticateReq = new AuthenticateReq(props.getProperty("clientId"), props.getProperty("clientSecret"));
        AUTH_TOKEN = "Bearer " + xrayCloudApi.getAuthToken(authenticateReq);
    }

    public static XrayApiHandler getXrayApiHandler() {
        if(xrayApiHandler == null) {
            xrayApiHandler = new XrayApiHandler();
        }
        return xrayApiHandler;
    }

    public void importTestExecutions(String outputDirectory) throws IOException {
        Path report = Path.of(outputDirectory,props.getProperty(REPORT_FILENAME));
        Map<String, Object> headers = new HashMap<>();
        if(report.toFile().exists()) {
            String contentType = URLConnection.guessContentTypeFromName(String.valueOf(report.getFileName()));
            headers.put("Content-Type", contentType);
            headers.put("Authorization", AUTH_TOKEN);
            Response response = xrayCloudFileApi.importTestExecution(headers, Files.readAllBytes(report));
            System.out.println(response.status());
            System.out.println(response);
        }
        else {
            throw new IOException("XRAY Json Report: "+report+" is not found");
        }
    }

    public void importMultiPartExecution(){

    }
}