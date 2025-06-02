package org.utility;

import java.lang.reflect.Method;

import org.testng.IInvokedMethod;
import org.testng.IInvokedMethodListener;
import org.testng.ITestResult;
import org.testng.SkipException;

public class NoProdMethodSkipper implements IInvokedMethodListener
{

	@Override
	public void beforeInvocation(IInvokedMethod method, ITestResult testResult)
	{
		Method actualMethod = method.getTestMethod().getConstructorOrMethod().getMethod();

		if (actualMethod.isAnnotationPresent(NoProd.class))
		{
			String restrictRun = System.getProperty("restrictRun", "no");
			String prodRun = System.getProperty("ProdRun", "yes");
			String environment = System.getProperty("Environment", "").toLowerCase();

			if ("yes".equalsIgnoreCase(restrictRun))
			{
				throw new SkipException("This script is restricted.");
			}

			if ("no".equalsIgnoreCase(prodRun))
			{
				switch (environment)
				{
				case "run":
					throw new SkipException("This method is disabled in the production environment.");
				case "run19":
					throw new SkipException("This method is disabled in the pre-production environment.");
				}
			}
		}
	}

}
