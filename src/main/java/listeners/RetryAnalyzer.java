package listeners;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

import org.testng.IAnnotationTransformer;
import org.testng.IRetryAnalyzer;
import org.testng.ITestResult;
import org.testng.annotations.ITestAnnotation;

/**
 * RetryAnalyzer for retrying failed TestNG test cases once.
 */
public class RetryAnalyzer implements IRetryAnalyzer, IAnnotationTransformer {

	private int retryCount = 0;
	private static final int MAX_RETRY_LIMIT = 1;

	@Override
	public boolean retry(ITestResult result) {
		if (retryCount < MAX_RETRY_LIMIT) {
			retryCount++;
			return true;
		}
		return false;
	}

	// ✅ Must use raw types to match TestNG's interface
	@Override
	public void transform(ITestAnnotation annotation, Class testClass, Constructor testConstructor, Method testMethod) {
		annotation.setRetryAnalyzer(RetryAnalyzer.class);
	}
}
