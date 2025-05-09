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
			if (System.getProperty("ProdRun").equalsIgnoreCase("no") && System.getProperty("Environment").equalsIgnoreCase("run"))
			{
				throw new SkipException("This method is disabled in the production environment.");
			}
		}
	}

	@Override
	public void afterInvocation(IInvokedMethod method, ITestResult testResult)
	{
		// no-op
	}

}
