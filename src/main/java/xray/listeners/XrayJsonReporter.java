package xray.listeners;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.testng.*;
import org.testng.annotations.Test;
import org.testng.xml.XmlSuite;
import xray.annotations.XrayTest;
import xray.api.handler.XrayApiHandler;
import xray.commons.JsonHandler;
import xray.json.model.execution.TestExecution;
import xray.json.model.execution.TestExecutionInfo;
import xray.json.model.execution.TestRun;
import xray.json.model.execution.TestStepResult;
import xray.json.model.jira.IssueFields;
import xray.json.model.jira.IssueType;
import xray.json.model.jira.ProjectField;
import xray.json.model.misc.CustomField;
import xray.json.model.misc.TestStatus;
import xray.json.model.misc.TestType;
import xray.json.model.test.Iteration;
import xray.json.model.test.TestCase;
import xray.json.model.test.TestStep;

import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

import static org.apache.commons.lang3.StringUtils.*;
import static xray.api.handler.JiraApiHandler.getJiraApiHandler;
import static xray.commons.Constants.*;
import static xray.commons.JsonHandler.writeJsonFileFromObject;

public class XrayJsonReporter implements IReporter, IExecutionListener {
    private Properties xrayProps = null;
    private Date executionStartDate;
    private Date executionEndDate;
    private TestExecutionInfo testExecutionInfo;
    private TestExecution testExecution;
    SimpleDateFormat dateFormatter = new SimpleDateFormat(DATE_TIME_FORMAT);
    private Path reportFilePath;
    private Path execInfoPath;


    @Override
    public void generateReport(List<XmlSuite> xmlSuites, List<ISuite> suites, String outputDirectory) {
        executionEndDate = Calendar.getInstance().getTime();
        testExecution = new TestExecution();
        xrayProps = JsonHandler.loadConfigPropertiesFile();
        boolean reportOnlyAnnotatedTest = (boolean) xrayProps.getOrDefault(REPORT_ONLY_ANNOTATED, false);

        if (isNotBlank(get(PROJECT_KEY))) {
            testExecutionInfo =
                    new TestExecutionInfo(get(PROJECT_KEY), executionStartDate, executionEndDate);
            setTestExecutionInfoProps();
        } else {
            throw new IllegalArgumentException("JIRA Project Key must " +
                    "be specified in Xray.properties to generate XRAY JSON Reports");
        }

        HashMap<String, ArrayList<ITestResult>> testResults = getTestResultsFromSuite(suites);
        List<TestRun> tests = new ArrayList<>();
        for (String testMethod : testResults.keySet()) {
            Method method = testResults.get(testMethod).get(0).getMethod().getConstructorOrMethod().getMethod();
            if (reportOnlyAnnotatedTest && !method.isAnnotationPresent(XrayTest.class)) {
                continue;
            }
            tests.add(createTestRun(testResults.get(testMethod)));
        }

        try {
            String testExecutionKey = StringUtils.isNotBlank(get(TEST_EXECUTION_KEY)) &&
                    getJiraApiHandler(get(PROJECT_KEY)).isIssueValid(get(TEST_EXECUTION_KEY))
                    ? get(TEST_EXECUTION_KEY) : null;
            testExecutionInfo.setTestPlanKey(testExecutionKey);
            testExecution.setTestExecutionInfo(testExecutionInfo);
            testExecution.setTests(tests);
            serializeReport(outputDirectory);
        } catch (Exception e) {
            throw new RuntimeException(String.format("Could not create the JSON Report File %s %s", e.getCause(), e.getMessage()));
        }
    }

    private void setTestExecutionInfoProps() {
        testExecutionInfo.setSummary(getIfBlank(get(TEST_EXECUTION_SUMMARY), () -> null));
        testExecutionInfo.setDescription(getIfBlank(get(TEST_EXECUTION_DESCRIPTION), () -> null));
        testExecutionInfo.setRevision(getIfBlank(get(TEST_EXECUTION_REVISION), () -> null));
        testExecutionInfo.setUser(getIfBlank(get(TEST_EXECUTION_USER), () -> null));
        testExecutionInfo.setVersion(getIfBlank(get(TEST_EXECUTION_VERSION), () -> null));
        String testPlanKey = StringUtils.isNotBlank(get(TEST_PLAN_KEY)) &&
                getJiraApiHandler(get(PROJECT_KEY)).isIssueValid(get(TEST_PLAN_KEY))
                ? get(TEST_PLAN_KEY) : null;
        testExecutionInfo.setTestPlanKey(testPlanKey);
    }

    private TestRun createTestRun(List<ITestResult> results) {
        TestRun testRun = new TestRun();
        ITestResult firstResult = results.get(0);
        Method testMethod = firstResult.getMethod().getConstructorOrMethod().getMethod();
        if (testMethod.isAnnotationPresent(XrayTest.class)) {
            String testKey = testMethod.getAnnotation(XrayTest.class).key();
            if(StringUtils.isNotBlank(testKey) &&
                    getJiraApiHandler(get(PROJECT_KEY)).isIssueValid(testKey)) {
                testRun.setTestKey(testMethod.getAnnotation(XrayTest.class).key());
            }
        }
        if (isBlank(testRun.getTestKey())) {
            testRun.setTestInfo(createTestCase(results, testMethod));
        }
        long start = firstResult.getStartMillis();
        long finish = firstResult.getEndMillis();
        int totalPassed = 0;
        int totalFailed = 0;

        //if test is not parameterized / data driven
        if (results.size() == 1) {
            ITestResult result = results.get(0);
            testRun.setStatus(Objects.requireNonNull(
                    TestStatus.getStatus(result.getStatus())).name());
            Throwable throwable = result.getThrowable();
            if (result.getStatus() == ITestResult.FAILURE
                    && throwable != null && throwable.getMessage() != null) {
                testRun.setComment(throwable.getMessage());
                totalFailed += 1;
            } else {
                totalPassed += 1;
            }
        } else {
            List<Iteration> iterations = new ArrayList<>();
            int counter = 1;
            for (ITestResult result : results) {
                start = Math.min(start, result.getStartMillis());
                finish = Math.max(finish, result.getEndMillis());
                Iteration iteration = new Iteration();
                iteration.setName(getTestUniqueKey(testMethod,
                        result.getMethod().getQualifiedName())
                        + ":" + counter);
                Object[] params = result.getParameters();
                if (params.length > 0) {
                    List<Map<String, String>> parameters = new ArrayList<>();
                    int count = 1;
                    List<String> parameterNames = null;
                    try {
                        parameterNames = getParameterNames(result.getMethod().getConstructorOrMethod().getMethod());
                    } catch (Exception ex) {
                        System.err.println("problem getting parameter names" + ex);
                    }
                    for (Object param : params) {
                        Map<String, String> parameter = new HashMap<>();
                        String paramName = "param" + count;
                        if (parameterNames != null)
                            paramName = parameterNames.get(count - 1);
                        parameter.put("name", paramName);
                        parameter.put("value", param.toString());
                        parameters.add(parameter);
                        count++;
                    }
                    iteration.setParameters(parameters);
                }

                String actualResult = "";
                List<TestStepResult> stepResults = new ArrayList<>();
                if (result.getStatus() == ITestResult.FAILURE) {
                    actualResult = ExceptionUtils.getStackTrace(result.getThrowable());
                }
                TestStepResult dummyResult = new TestStepResult();
                dummyResult.setStatus(Objects.requireNonNull(
                        TestStatus.getStatus(result.getStatus())).name());
                dummyResult.setActualResult(actualResult);
                stepResults.add(dummyResult);
                iteration.setStepResults(stepResults);
                iteration.setStatus(Objects.requireNonNull(
                        TestStatus.getStatus(result.getStatus())).name());
                iterations.add(iteration);

                if (result.getStatus() == ITestResult.SUCCESS)
                    totalPassed++;
                if (result.getStatus() == ITestResult.FAILURE)
                    totalFailed++;
                counter++;
            }
            testRun.setIterations(iterations);
            if (totalFailed > 0)
                testRun.setStatus(Objects.requireNonNull(
                        TestStatus.getStatus(ITestResult.FAILURE)).name());
            else if (totalPassed == iterations.size())
                testRun.setStatus(Objects.requireNonNull(
                        TestStatus.getStatus(ITestResult.SUCCESS)).name());
            else
                testRun.setStatus(Objects.requireNonNull(
                        TestStatus.getStatus(ITestResult.SKIP)).name());
        }
        testRun.setCustomFields(getCustomIterationFields(totalPassed, totalFailed));
        testRun.setStart(dateFormatter.format(start));
        testRun.setFinish(dateFormatter.format(finish));
        return testRun;
    }

    private List<CustomField> getCustomIterationFields(int totalPassed, int totalFailed) {
        CustomField iterationsPassed = new CustomField(null, get(ITERATIONS_PASSED_FIELD_NAME), totalPassed);
        CustomField iterationsFailed = new CustomField(null, get(ITERATIONS_FAILED_FIELD_NAME), totalFailed);
        return Arrays.asList(iterationsPassed, iterationsFailed);
    }

    private static List<String> getParameterNames(Method method) {
        Parameter[] parameters = method.getParameters();
        List<String> parameterNames = new ArrayList<>();

        for (Parameter parameter : parameters) {
            if (!parameter.isNamePresent()) {
                //TODO: log warn or lower level
                throw new IllegalArgumentException("Parameter names are not present!");
            }
            String parameterName = parameter.getName();
            parameterNames.add(parameterName);
        }
        return parameterNames;
    }

    private TestCase createTestCase(List<ITestResult> results, Method testMethod) {
        TestCase testCase = new TestCase();
        testCase.setProjectKey(get(PROJECT_KEY));

        // For Generic tests - Generic Test Definition, Manual - Summary
        String testName = results.get(0).getMethod().getQualifiedName();
        String testUniqueId = getTestUniqueKey(testMethod, testName);
        if (testMethod.isAnnotationPresent(XrayTest.class)) {
            testCase.setLabels(Arrays.asList(testMethod.getAnnotation(XrayTest.class).labels()));
        }
        testCase.setSummary(testUniqueId);
        boolean isTestParameterized = results.size() > 1;
        boolean isManualTest = Boolean.parseBoolean(get(USE_MANUAL_TEST_FOR_REGULAR_TESTS));
        if (isTestParameterized || isManualTest) {
            testCase.setType(TestType.MANUAL);
            testCase.setSteps(Collections.singletonList(
                    new TestStep(testName, "", "ok")));
        } else {
            testCase.setType(TestType.GENERIC);
            testCase.setDefinition(testUniqueId);
        }
        return testCase;
    }

    /**
     * Creates a unique ID to identify the test in Xray.
     * If unique tag is present, it will always be appended as prefix.
     * If unique tag is not found, Fully Qualified Name ( FQN )
     * of the test method will be used instead.
     * <pre>
     * The Precedence is as follows:
     *        1. Unique Tag / FQN + Xray Tag Summary
     *        2. Unique Tag / FQN + Xray Tag Description
     *        3. Unique Tag / FQN + TestNg Annotation Description
     *        4. Unique Tag + FQN
     * </pre>
     *
     * @param testMethod the test method to generate unique id for.
     * @return Unique value to identify the test in xray on auto-provision.
     */
    private String getTestUniqueKey(Method testMethod, String testName) {
        String testSummary = "";
        String testNgDescription = "";
        String xrayTestSummary = "";
        String xrayTestDescription = "";
        Optional<String> uniqueTag = Optional.empty();
        if (testMethod.isAnnotationPresent(Test.class)) {
            List<String> testGroups = Arrays.asList(testMethod.getAnnotation(Test.class).groups());
            testNgDescription = testMethod.getAnnotation(Test.class).description();
            uniqueTag = testGroups.stream()
                    .filter(e -> e.endsWith("_UT"))
                    .findFirst();
        }
        if (testMethod.isAnnotationPresent(XrayTest.class)) {
            xrayTestSummary = testMethod.getAnnotation(XrayTest.class).summary();
            xrayTestDescription = testMethod.getAnnotation(XrayTest.class).description();
        }
        testSummary += isNotBlank(xrayTestSummary) ? xrayTestSummary :
                isNotBlank(xrayTestDescription) ? xrayTestDescription :
                isNotBlank(testNgDescription) ? testNgDescription : testName;
        return uniqueTag.isPresent() ?
                String.format("[%s] %s", uniqueTag.get(), testSummary) : testSummary;
    }

    public HashMap<String, ArrayList<ITestResult>> getTestResultsFromSuite(List<ISuite> suites) {
        Set<ITestResult> testResults = new LinkedHashSet<>();
        for (ISuite s : suites) {
            Map<String, ISuiteResult> suiteResults = s.getResults();
            for (ISuiteResult sr : suiteResults.values()) {
                ITestContext testContext = sr.getTestContext();
                addAllTestResults(testResults, testContext.getPassedTests());
                addAllTestResults(testResults, testContext.getFailedTests());
                addAllTestResults(testResults, testContext.getSkippedTests());
            }
        }

        HashMap<String, ArrayList<ITestResult>> results = new HashMap<>();
        for (ITestResult testResult : testResults) {
            String testUid = testResult.getMethod().getQualifiedName();
            ArrayList<ITestResult> resultsArray = results.containsKey(testUid)
                    ? results.get(testUid) : new ArrayList<>();
            resultsArray.add(testResult);
            results.put(testUid, resultsArray);
        }
        return results;
    }

    private void addAllTestResults(Set<ITestResult> testResults, IResultMap resultMap) {
        if (resultMap != null) {
            testResults.addAll(
                    resultMap.getAllResults().stream()
                            .sorted((o1, o2) -> (int) (o1.getStartMillis() - o2.getStartMillis()))
                            .collect(Collectors.toCollection(LinkedHashSet::new)));
        }
    }

    private void serializeReport(String outputDirectory) throws Exception {
        Files.createDirectories(Path.of(outputDirectory));
        reportFilePath = Path.of(outputDirectory, get(REPORT_FILENAME));
        if (get(MULTIPART_IMPORT).equalsIgnoreCase("true")) {
            execInfoPath = Path.of(outputDirectory, get(REPORT_EXEC_INFO_FILENAME));
            createMultiPartExecutionInfo(execInfoPath.toString());
        }
        writeJsonFileFromObject(reportFilePath.toString(), testExecution);
    }

    private void createMultiPartExecutionInfo(String outputFilePath) throws IOException {
        Path jsonRequest = Path.of(outputFilePath);
        Files.createDirectories(jsonRequest.getParent());
        IssueFields issue = new IssueFields();
        issue.setProject(new ProjectField(
                getJiraApiHandler(get(PROJECT_KEY)).getProjectId()));
        issue.setIssueType(new IssueType("Test Execution"));
        issue.setSummary(get(TEST_EXECUTION_SUMMARY));
        issue.setDescription(get(TEST_EXECUTION_DESCRIPTION));
        writeJsonFileFromObject(outputFilePath, issue);
    }

    public String get(String props) {
        return xrayProps.getProperty(props);
    }

    @Override
    public void onExecutionStart() {
        executionStartDate = Calendar.getInstance().getTime();
    }

    @Override
    public void onExecutionFinish() {
        XrayApiHandler xrayApi = XrayApiHandler.getXrayApiHandler();
        try {
            JsonHandler.validateXrayJsonSchema(reportFilePath.toString());
            if (get(MULTIPART_IMPORT).equalsIgnoreCase("true")) {
                xrayApi.importMultiPartExecution(reportFilePath.toString(), execInfoPath.toString());
            } else {
                xrayApi.importTestExecutions(reportFilePath.toString());
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}