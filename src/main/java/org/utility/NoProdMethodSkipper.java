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
		String restrictRun = System.getProperty("restrictRun", "no").toLowerCase();
		String prodRun = System.getProperty("ProdRun", "yes").toLowerCase();
		String environment = System.getProperty("Environment", "").toLowerCase();

		// Restrict all runs if explicitly configured
		if ("yes".equals(restrictRun)) {
			throw new SkipException("This script is restricted.");
		}

		// Disable execution for specific environments if ProdRun is set to 'no'
		if ("no".equals(prodRun)) {
			switch (environment) {
				case "run":
					throw new SkipException("This method is disabled in the production environment.");
				case "run19":
					throw new SkipException("This method is disabled in the pre-production environment.");
				default:
					// Allow execution for other environments
			}
		}
	}

	@Override
	public void afterInvocation(IInvokedMethod method, ITestResult testResult) {
		// No action needed after test method invocation
	}
}
