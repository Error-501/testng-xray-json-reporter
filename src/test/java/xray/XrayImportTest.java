package xray;

import api.XrayApiHandler;
import org.testng.annotations.Test;

import java.io.IOException;

public class XrayImportTest {

    String AUTH_TOKEN = "";

    @Test
    public void importXrayResults() throws IOException {
        XrayApiHandler xrayApi = XrayApiHandler.getXrayApiHandler();
        xrayApi.importTestExecutions("C:\\Users\\Dinesh\\PlayGround\\00_Java\\00_MavenProjects\\01_testng_xray_integration\\testng-xray-json-reporter\\target\\surefire-reports\\");
    }
}