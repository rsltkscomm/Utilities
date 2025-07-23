package org.utility;

import java.lang.reflect.Method;
import org.testng.IInvokedMethod;
import org.testng.IInvokedMethodListener;
import org.testng.ITestResult;
import org.testng.SkipException;

public class NoProdMethodSkipper implements IInvokedMethodListener {

    @Override
    public void beforeInvocation(IInvokedMethod method, ITestResult testResult) {
        Method actualMethod = method.getTestMethod().getConstructorOrMethod().getMethod();

        // Proceed only if the method is marked with @NoProd
        if (!actualMethod.isAnnotationPresent(NoProd.class)) {
            return;
        }

        // Fetch and normalize system properties
        String restrictRun = System.getProperty("restrictRun", "yes").toLowerCase(); // Default to "yes"
        String environment = System.getProperty("Environment", "").toLowerCase();

        // Always restrict "run" environment regardless of restrictRun setting
        if ("run".equals(environment)) {
            throw new SkipException("Execution is always restricted in 'run' environment for @NoProd methods");
        }

        // Additional restriction based on flag (if not in run environment)
        if ("yes".equals(restrictRun)) {
            throw new SkipException("Execution restricted by restrictRun=yes configuration");
        }
        
        // If we get here, the test will execute
    }
}
