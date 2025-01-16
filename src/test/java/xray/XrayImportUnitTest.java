package xray;

import api.XrayApiHandler;
import commons.PropertyHandler;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.util.Properties;

import static org.apache.commons.io.FileUtils.getFile;
import static xray.Constants.REPORT_FILENAME;

public class XrayImportUnitTest {

    Properties props;
    String userDirectory = System.getProperty("user.dir");
    String reportOutputDir = userDirectory + "\\target\\surefire-reports\\";

    @Test
    public void validateJsonSchema() throws IOException {
        props = PropertyHandler.loadConfigPropertiesFile();
       PropertyHandler.validateXrayJsonSchema(reportOutputDir + props.getProperty(REPORT_FILENAME));
    }


    @Test
    public void importXrayResults() throws IOException {
        XrayApiHandler xrayApi = XrayApiHandler.getXrayApiHandler();
        xrayApi.importTestExecutions(reportOutputDir);
    }

    @Test
    public void importMultiPartReport() throws IOException {
        XrayApiHandler xrayApi = XrayApiHandler.getXrayApiHandler();
        File testExec = new File(XrayImportUnitTest.class.getResource("/testExec.json").getFile());
        File execInfo = new File(XrayImportUnitTest.class.getResource("/testExecInfo.json").getFile());
        xrayApi.importMultiPartExecution(testExec.getPath(), execInfo.getPath());
    }
}