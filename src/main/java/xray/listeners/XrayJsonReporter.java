package xray.listeners;

import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static xray.Constants.*;

import com.beust.jcommander.internal.Sets;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;
import org.testng.IExecutionListener;
import org.testng.IReporter;
import org.testng.IResultMap;
import org.testng.ISuite;
import org.testng.ISuiteResult;
import org.testng.ITestContext;
import org.testng.ITestResult;
import org.testng.xml.XmlSuite;
import xray.annotations.XrayTest;
import xray.json.model.execution.TestExecution;
import xray.json.model.execution.TestExecutionInfo;

public class XrayJsonReporter implements IReporter, IExecutionListener {

    private static final String DEFAULT_XRAY_PROPERTIES_FILE = "xray.properties";
    public static final ObjectMapper objectMapper = new ObjectMapper();
    private Properties xrayProps = null;
    private Date executionStartDate;
    private Date executionEndDate;
    private TestExecutionInfo testExecutionInfo;


    @Override
    public void generateReport(List<XmlSuite> xmlSuites, List<ISuite> suites, String outputDirectory) {
        executionEndDate = Calendar.getInstance().getTime();
        loadConfigPropertiesFile();
        if(isNotBlank(get(PROJECT_KEY))) {
            testExecutionInfo =
                new TestExecutionInfo(get(PROJECT_KEY), executionStartDate, executionEndDate);
        }
        else {
            throw new NoSuchElementException("projectKey must be specified to create XRAY JSON Reports");
        }

        Set<ITestResult> testResults = Sets.newLinkedHashSet();
        HashMap<String, ArrayList<ITestResult>> results = new HashMap<>();
        for (ISuite s : suites) {
            Map<String, ISuiteResult> suiteResults = s.getResults();
            for (ISuiteResult sr : suiteResults.values()) {
                ITestContext testContext = sr.getTestContext();
                addAllTestResults(testResults, testContext.getPassedTests());
                addAllTestResults(testResults, testContext.getFailedTests());
                addAllTestResults(testResults, testContext.getSkippedTests());
            }
        }

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
            Boolean reportOnlyAnnotatedTest  =
                (Boolean) xrayProps.getOrDefault(REPORT_ONLY_ANNOTATED, false);
            if (!reportOnlyAnnotatedTest ||
                (reportOnlyAnnotatedTest && method.isAnnotationPresent(XrayTest.class))) {
//                addTestResults(tests, results.get(testMethod));
            }
        }

        try {
            serializeReport(outputDirectory);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public String get(String props) {
        return xrayProps.getProperty(props);
    }

    private void serializeReport(String outputDirectory) throws IOException {
        TestExecution testExecution = new TestExecution();
        if(isNotBlank(xrayProps.getProperty(TEST_EXECUTION_KEY))) {
            testExecution.setTextExecutionKey(xrayProps.getProperty(TEST_EXECUTION_KEY));
        }
        testExecution.setTestExecutionInfo(testExecutionInfo);
        System.out.println(testExecution.getTestExecutionInfo().getProjectKey().toString());
        new File(outputDirectory).mkdirs();
        File reportFile = new File(outputDirectory, get("report_filename"));
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(reportFile))) {
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(writer, testExecution);
        } catch (IOException e) {
            //TODO Logger
            System.err.println("Error in BufferedWriter 1 write" +  e);
        }
    }

    private void loadConfigPropertiesFile() {
        try {
            InputStream stream = getClass().getClassLoader().getResourceAsStream(DEFAULT_XRAY_PROPERTIES_FILE);
            if (stream == null) {
                throw new IOException("Could not find " + DEFAULT_XRAY_PROPERTIES_FILE + " in classpath");
            }
            if (stream != null) {
                xrayProps = new Properties();
                xrayProps.load(stream);
            }
        }
        catch (Exception e) {
            //TODO: Should be logger
                System.err.println("error loading listener configuration from properties files");
        }
    }

    private void addAllTestResults(Set<ITestResult> testResults, IResultMap resultMap) {
        if (resultMap != null) {
            testResults.addAll(
                resultMap.getAllResults().stream()
                    .sorted((o1, o2) -> (int) (o1.getStartMillis() - o2.getStartMillis()))
                    .collect(Collectors.toCollection(LinkedHashSet::new)));
        }
    }

    private void addTestResults(ArrayList<ITestResult> results){
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

                String labels = method.getAnnotation(XrayTest.class).labels();
                if (!emptyString(labels)) {
                    ArrayList<String> labelsArray = new ArrayList<>(Arrays.asList(labels.split(" ")));
                    testInfo.put("labels", labelsArray);
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

                /*
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
                 */
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
            test.put("iterations", iterations);
            if (totalFailed > 0)
                test.put("status", getTestStatus(ITestResult.FAILURE));
            else if (totalPassed == iterations.size())
                test.put("status", getTestStatus(ITestResult.SUCCESS));
            else
                test.put("status", getTestStatus(ITestResult.SKIP));
        }
        tests.add(test);
    }

    @Override
    public void onExecutionStart() {
        executionStartDate = Calendar.getInstance().getTime();
    }
}
