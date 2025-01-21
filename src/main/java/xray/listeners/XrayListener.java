package xray.listeners;

import org.testng.IInvokedMethod;
import org.testng.IInvokedMethodListener;
import org.testng.ITestListener;
import org.testng.ITestResult;
import org.testng.annotations.Test;
import xray.annotations.XrayTest;

/**
 * The listener interface for receiving events related to execution of tests, and process Xray related annotations.
 * The listener can be automatically invoked when TestNG tests are run by using ServiceLoader mechanism.
 * You can also add this listener to a TestNG Test class by adding
 * <code>@Listeners({app.getxray.testng.XrayListener.class})</code>
 * before the test class
 *
 * @see XrayTest
 */
public class XrayListener implements IInvokedMethodListener, ITestListener {

    boolean testSuccess = true;

    public void beforeInvocation(IInvokedMethod method, ITestResult testResult) {
        String summary = null;
        String description = null;
        String testDescription;
        String xrayTestDescription = null;
        String xrayTestSummary = null;

        if (method.isTestMethod()) {
            testDescription = method.getTestMethod().getConstructorOrMethod().getMethod()
                    .getAnnotation(Test.class).description();
            if (annotationPresent(method, XrayTest.class)) {
                xrayTestDescription = method.getTestMethod().getConstructorOrMethod().getMethod()
                        .getAnnotation(XrayTest.class).description();
                xrayTestSummary = method.getTestMethod().getConstructorOrMethod().getMethod()
                        .getAnnotation(XrayTest.class).summary();

                testResult.setAttribute("test",
                        method.getTestMethod().getConstructorOrMethod().getMethod()
                                .getAnnotation(XrayTest.class).key());
                testResult.setAttribute("labels",
                        method.getTestMethod().getConstructorOrMethod().getMethod()
                                .getAnnotation(XrayTest.class).labels());
            }

            if (!emptyString(xrayTestSummary)) {
                summary = xrayTestSummary;
            } else if (!emptyString(xrayTestDescription)) {
                summary = xrayTestDescription;
            } else if (!emptyString(testDescription)) {
                summary = xrayTestDescription;
            }

            if (!emptyString(xrayTestDescription)) {
                description = xrayTestDescription;
            } else if (!emptyString(testDescription)) {
                description = testDescription;
            }

            if (!emptyString(summary)) {
                testResult.setAttribute("summary", summary);
            }
            if (!emptyString(description)) {
                testResult.setAttribute("description", description);
            }
            System.out.println(testResult.getAttributeNames().toString());
        }
    }


    private boolean annotationPresent(IInvokedMethod method, Class annotationClass) {
        boolean retVal =
                method.getTestMethod().getConstructorOrMethod().getMethod()
                        .isAnnotationPresent(annotationClass);
        System.out.println("Annotation " + retVal);
        return retVal;
    }

    private boolean emptyString(String string) {
        return (string == null || string.isEmpty() || string.trim().isEmpty());
    }

    public void afterInvocation(IInvokedMethod method, ITestResult testResult) {
        if (method.isTestMethod()) {
            if (!testSuccess) {
                testResult.setStatus(ITestResult.FAILURE);
            }
        }
    }


}