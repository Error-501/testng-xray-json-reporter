/*
package xray.listeners;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.testng.IExecutionListener;
import org.testng.IInvokedMethod;
import org.testng.IInvokedMethodListener;
import org.testng.IReporter;
import org.testng.IResultMap;
import org.testng.ISuite;
import org.testng.ISuiteResult;
import org.testng.ITestContext;
import org.testng.ITestResult;
import org.testng.annotations.Test;
import org.testng.xml.XmlSuite;

import java.nio.charset.StandardCharsets;
import java.io.*;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;
import org.testng.internal.Utils;

import com.beust.jcommander.internal.Sets;

import java.text.SimpleDateFormat;
import xray.annotations.XrayTest;

public class XrayJsonReporterRef implements IReporter, IExecutionListener, IInvokedMethodListener {

    private final XrayJsonReporterConfigRef config = new XrayJsonReporterConfigRef();
    private static final String DEFAULT_XRAY_PROPERTIES_FILE = "xray.properties";
    private static final String XRAY_JSON_SUMMARY_FIELD = "summary";

    private String propertiesFile = DEFAULT_XRAY_PROPERTIES_FILE;

    private long executionsStartedAt;
    private long executionsFinishedAt;


    @Override
    public void generateReport(List<XmlSuite> xmlSuites, List<ISuite> suites, String outputDirectory) {
        // hack: the onExecutionFinish() callback seems to be called but the value is not set; maybe a concurrency issue?
        this.executionsFinishedAt = System.currentTimeMillis();
        JSONObject report = new JSONObject();

        Set<ITestResult> testResults = Sets.newLinkedHashSet();
        // temporary structure to hold all test results, including data-driven ones, indexed by test FQN
        HashMap<String, ArrayList<ITestResult>> results = new HashMap<>();
        JSONObject info = new JSONObject();

        if (this.propertiesFile != null)
            loadConfigPropertiesFile();

        if (!Utils.isStringEmpty(config.getUser())) {
            info.put("user", config.getUser());  
        }
        if (!Utils.isStringEmpty(config.getSummary())) {
            info.put(XRAY_JSON_SUMMARY_FIELD, config.getSummary());  
        }
        if (!Utils.isStringEmpty(config.getDescription())) {
            info.put("description", config.getDescription());  
        }
        if (!Utils.isStringEmpty(config.getProjectKey())) {
            info.put("project", config.getProjectKey());
        }

        if (!Utils.isStringEmpty(config.getVersion())) {
            info.put("version", config.getVersion());  
        }
        if (!Utils.isStringEmpty(config.getRevision())) {
            info.put("revision", config.getRevision());  
        }
        if (!Utils.isStringEmpty(config.getTestExecutionKey())) {
            report.put("testExecutionKey", config.getTestExecutionKey());  
        }
        if (!Utils.isStringEmpty(config.getTestPlanKey())) {
            info.put("testPlanKey", config.getTestPlanKey());  
        }
        if (!Utils.isStringEmpty(config.getTestEnvironments())) {
            ArrayList<String> envs = new ArrayList<>(Arrays.asList(config.getTestEnvironments().split(",")));
            info.put("testEnvironments", envs) ; 
        }

        SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX");
        String startDate = dateFormatter.format(this.executionsStartedAt);
        String finishDate = dateFormatter.format(this.executionsFinishedAt);
        info.put("startDate", startDate);
        info.put("finishDate", finishDate);
        report.put("info", info);


        // add all test results to a temporary testResults set
        JSONArray tests = new JSONArray();
        for (ISuite s : suites) {
            Map<String, ISuiteResult> suiteResults = s.getResults();
            for (ISuiteResult sr : suiteResults.values()) {
                ITestContext testContext = sr.getTestContext();
                addAllTestResults(testResults, testContext.getPassedTests());
                addAllTestResults(testResults, testContext.getFailedTests());
                addAllTestResults(testResults, testContext.getSkippedTests());
            }
        }

        // process testResults, look for multiple results for the same test, and add it hashmap
        for (ITestResult testResult : testResults) {
            String testUid = testResult.getMethod().getQualifiedName();
            System.out.println(testUid);
            if (results.containsKey(testUid)) {
                ArrayList<ITestResult> resultsArray = results.get(testUid);
                resultsArray.add(testResult);
                results.put(testUid, resultsArray); 
            } else {
                ArrayList<ITestResult> resultsArray = new ArrayList<>();
                resultsArray.add(testResult);
                results.put(testUid, resultsArray);
            }
        }

        for (String testMethod : results.keySet()) {
            Method method = results.get(testMethod).get(0).getMethod().getConstructorOrMethod().getMethod();
            if (!this.config.isReportOnlyAnnotatedTests() || (this.config.isReportOnlyAnnotatedTests() && (method.isAnnotationPresent(XrayTest.class)))) {
                addTestResults(tests, results.get(testMethod));
            }
        }
        report.put("tests", tests);
        saveReport(outputDirectory, report);
    }

    private boolean emptyString(String string) {
        return (string == null || string.isEmpty() || string.trim().isEmpty());
    }

    private static List<String> getParameterNames(Method method) {
        Parameter[] parameters = method.getParameters();
        List<String> parameterNames = new ArrayList<>();

        for (Parameter parameter : parameters) {
            if(!parameter.isNamePresent()) {
                throw new IllegalArgumentException("Parameter names are not present!");
            }

            String parameterName = parameter.getName();
            parameterNames.add(parameterName);
        }

        return parameterNames;
    }

    private void addTestResults(JSONArray tests, ArrayList<ITestResult> results){
        String xrayTestKey = null;
        String xrayTestSummary = null;
        String xrayTestDescription = null;
        String testDescription = null;

        // auxiliary variable with summary to use, based on a criteria defined ahead
        String testSummary;

        JSONObject test = new JSONObject();
        SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX");
        ITestResult firstResult = results.get(0);
        Method method = firstResult.getMethod().getConstructorOrMethod().getMethod();

        // if no testKey was given, use autoprovision mechanism
        boolean autoprovision =  !method.isAnnotationPresent(XrayTest.class) ||  (method.isAnnotationPresent(XrayTest.class)) && (emptyString(method.getAnnotation(XrayTest.class).key()));
      
        String testKey = "";
        if (method.isAnnotationPresent(XrayTest.class)) {
            testKey = method.getAnnotation(XrayTest.class).key();
            if (!emptyString(testKey))
                test.put("testKey", testKey);
        }

        if (autoprovision) {
            JSONObject testInfo = new JSONObject();
            testInfo.put("projectKey", config.getProjectKey());

            // TODO: description (and other Test issue level custom fields) can't yet be defined for new Test issues
            // add FQN of test method as a comment for easier tracking
            test.put("comment", results.get(0).getMethod().getQualifiedName());

            if (method.isAnnotationPresent(XrayTest.class)) {
                if (!emptyString(testKey)) {
                    xrayTestKey = testKey;
                    test.put("testKey", testKey);
                }

                xrayTestSummary = method.getAnnotation(XrayTest.class).summary();
                xrayTestDescription = method.getAnnotation(XrayTest.class).description();
            }
            if  (method.isAnnotationPresent(Test.class)) {
                testDescription = method.getAnnotation(Test.class).description();
            }

            // summary should only be added if no testKey was given
            if (emptyString(xrayTestKey)) {
                // override default Test issue summary using the "summary" attribute from the XrayTest annotation
                // or else, with teh "description" of @XrayTest, or from @Test; else use test name (method name or overriden)
                if (!emptyString(xrayTestSummary)) {
                    testSummary = xrayTestSummary;
                } else if (!emptyString(xrayTestDescription)) {
                    testSummary = xrayTestDescription;
                } else if (!emptyString(testDescription)) {
                    testSummary = testDescription;
                } else {
                    testSummary = firstResult.getName();
                }
                testInfo.put(XRAY_JSON_SUMMARY_FIELD, testSummary);
            }
            
            if ( (results.size() == 1 && config.isUseManualTestsForRegularTests()) ||
                 (results.size() == 1 && results.get(0).getParameters().length > 0) || 
                 (results.size() > 1 && config.isUseManualTestsForDatadrivenTests())
                ) {
                testInfo.put("type", "Manual");
                JSONArray steps = new JSONArray();
                JSONObject dummyStep = new  JSONObject();
                dummyStep.put("action", results.get(0).getName());
                dummyStep.put("data", "");
                dummyStep.put("result", "ok");
                steps.add(dummyStep);
                testInfo.put("steps", steps);
            } else {
                testInfo.put("type", "Generic");
                testInfo.put("definition", results.get(0).getMethod().getQualifiedName());
            }
            test.put("testInfo", testInfo);
        }
        // end autoprovision logic

        // just one result.. may be DD/parameterized though
        // if there is only one result for the test and it has no parameters, then create a regular non-datadriven test, i.e. without dataset (this is discussable)
        // maybe create a DD test always?
        // if (firstResult.getParameters().length > or == 0) depending..
        if (results.size() == 1) {

            // regular test; non data-driven
            ITestResult result =  results.get(0);
            String start = dateFormatter.format(result.getStartMillis());
            String finish = dateFormatter.format(result.getEndMillis());
            test.put("start", start);
            test.put("finish", finish);
            test.put("status", getTestStatus(result.getStatus()));
            Throwable throwable = result.getThrowable();
            if (result.getStatus() == ITestResult.FAILURE && throwable != null && throwable.getMessage() != null)
                test.put("comment", throwable.getMessage());

            // process attachments
            processAttachments(result, test);
        } else {
            // mutiple results => data-driven test

            // TODO: this should be based not on the first result but on the start&endtime of all iterations
            String start = dateFormatter.format(firstResult.getStartMillis());
            String finish = dateFormatter.format(firstResult.getEndMillis());
            test.put("start", start);
            test.put("finish", finish);

            JSONArray iterations = new JSONArray();
            int counter = 1;
            int totalPassed = 0;
            int totalFailed = 0;
            for (ITestResult result: results) {
                JSONObject iteration = new JSONObject();
                iteration.put("name", "iteration " + counter++);
                Object[] params = result.getParameters();
                if (params.length > 0) {
                    JSONArray parameters = new JSONArray();
                    int count = 1;

                    List<String> parameterNames = null;
                    try {
                        parameterNames = getParameterNames(result.getMethod().getConstructorOrMethod().getMethod());
                    } catch (Exception ex ){
                        System.err.println("problem getting parameter names");
                    }                   
                    for (Object param: params) {
                        JSONObject p = new JSONObject();
                        String paramName = "param" + count;
                        if (parameterNames != null)
                            paramName = parameterNames.get(count-1);
                        p.put("name", paramName);
                        p.put("value", param.toString());
                        parameters.add(p);
                        count++;
                    }
                    iteration.put("parameters", parameters);
                }

                JSONArray steps = new JSONArray();
                JSONObject dummyStep = new  JSONObject();
                String actualResult  = "";
                if (result.getStatus() == ITestResult.FAILURE) {
                    StringWriter sw = new StringWriter();
                    PrintWriter pw = new PrintWriter(sw);
                    result.getThrowable().printStackTrace(pw);
                    actualResult = sw.toString();
                }

                [ERROR] givenNumberFromDataProvider_ifEvenCheckOK_thenCorrect[5, true](com.baeldung.ParametrizedLongRunningUnitTest)  Time elapsed: 0.009 s  <<< FAILURE!
                java.lang.AssertionError: expected [false] but found [true]
	                at com.baeldung.ParametrizedLongRunningUnitTest.givenNumberFromDataProvider_ifEvenCheckOK_thenCorrect(ParametrizedLongRunningUnitTest.java:34)

                 * result.getThrowable().getMessage():
                 *   expected [false] but found [true]
                 * result.getThrowable().toString():
                 *   java.lang.AssertionError: expected [false] but found [true]
                 * result.getThrowable().getLocalizedMessage():
                 *   expected [false] but found [true]
                 * result.getThrowable().printStackTrace(pw):
                 *    java.lang.AssertionError: expected [false] but found [true]
	             *      at com.baeldung.ParametrizedLongRunningUnitTest.givenNumberFromDataProvider
                 *      ... (full trace)...


                dummyStep.put("actualResult", actualResult);
                dummyStep.put("status", getTestStatus(result.getStatus()));


                // attachments
                processAttachments(result, dummyStep);

                steps.add(dummyStep);
                iteration.put("steps", steps);

                iteration.put("status", getTestStatus(result.getStatus()));
                iterations.add(iteration);

                if (result.getStatus() == ITestResult.SUCCESS)
                    totalPassed++;
                if (result.getStatus() == ITestResult.FAILURE)
                    totalFailed++;         
            }
            test.put("iterations", i

terations);
            if (totalFailed > 0)
             test.put("status", getTestStatus(ITestResult.FAILURE));
            else if (totalPassed == iterations.size())
                test.put("status", getTestStatus(ITestResult.SUCCESS));
            else
                test.put("status", getTestStatus(ITestResult.SKIP));
        }
        tests.add(test);
    }

    private void processAttachments(ITestResult result, JSONObject targetObject) {
        JSONArray evidence = new JSONArray();
        Base64.Encoder enc= Base64.getEncoder();
        File[] attachments = (File[])result.getAttribute("attachments");
        if (attachments != null){
            for (File file : attachments)  {
                try {
                    byte[] fileContent = Files.readAllBytes(file.toPath());
                    byte[] encoded = enc.encode(fileContent);
                    String encodedStr = new String(encoded, StandardCharsets.UTF_8);
          
                    JSONObject tmpAttach = new JSONObject();
                    tmpAttach.put("data", encodedStr);
                    tmpAttach.put("filename", file.getName());
                    tmpAttach.put("contentType", getContentTypeFor(file));
                    evidence.add(tmpAttach);
                  } catch (Exception ex) {
                    System.err.println("problem processing attachment " +  file.getAbsolutePath());
                  }
            }
            targetObject.put("evidence", evidence);
        }
    }
    
    private String getTestStatus(int status){
        boolean xrayCloud = config.isXrayCloud();
        switch (status) {
            case ITestResult.FAILURE:
                return xrayCloud ? "FAILED": "FAIL";
            case ITestResult.SUCCESS:
                return xrayCloud ? "PASSED": "PASS";
            case ITestResult.SUCCESS_PERCENTAGE_FAILURE:
                return xrayCloud ? "PASSED": "PASS";
            case ITestResult.SKIP:
                return xrayCloud ? "SKIPPED": "SKIP";
            default:
                return "EXECUTING";
            }    
    }

    private String getContentTypeFor(File file) {
        String filename = file.getName();
        String extension = filename.substring(filename.lastIndexOf('.') + 1);
        switch (extension) {
            case "png":
                return "image/png";
            case "jpeg":
                return "image/jpeg";
            case "jpg":
                return "image/jpeg";
            case "gif":
                return "image/gif";
            case "txt":
                return "text/plain";
            case "log":
                return "text/plain";
            case "zip":
                return "application/zip";
            case "json":
                return "application/json";
            default:
                return "application/octet-stream";
            }
    }


    @Override
    public XrayJsonReporterConfigRef getConfig() {
      return config;
    }

}*/