package xray;

import api.XrayApiHandler;
import commons.PropertyHandler;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.util.Properties;

import static xray.Constants.REPORT_FILENAME;

public class XrayImportTest {

    Properties props;

    @Test
    public void validateJsonSchema() throws IOException {
        props = PropertyHandler.loadConfigPropertiesFile();
       PropertyHandler.validateXrayJsonSchema("C:\\Users\\Dinesh\\PlayGround\\00_Java\\00_MavenProjects\\01_testng_xray_integration\\testng-xray-json-reporter\\target\\surefire-reports\\" + props.getProperty(REPORT_FILENAME));
    }


    @Test
    public void importXrayResults() throws IOException {
        XrayApiHandler xrayApi = XrayApiHandler.getXrayApiHandler();
        xrayApi.importTestExecutions("C:\\Users\\Dinesh\\PlayGround\\00_Java\\00_MavenProjects\\01_testng_xray_integration\\testng-xray-json-reporter\\target\\surefire-reports\\");
    }
}