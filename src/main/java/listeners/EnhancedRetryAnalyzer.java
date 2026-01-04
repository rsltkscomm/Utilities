package listeners;

import listeners.retry.IntelligentRetryAnalyzer;
import org.testng.IAnnotationTransformer;
import org.testng.IRetryAnalyzer;
import org.testng.ITestResult;
import org.testng.annotations.ITestAnnotation;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

/**
 * Enhanced retry analyzer that uses IntelligentRetryAnalyzer
 * Maintains backward compatibility with existing RetryAnalyzer
 */
public class EnhancedRetryAnalyzer implements IRetryAnalyzer, IAnnotationTransformer {
    
    private final IntelligentRetryAnalyzer intelligentRetry;
    
    public EnhancedRetryAnalyzer() {
        // Default: 3 retries, 1s initial delay, 2x backoff, retry on all exceptions
        this.intelligentRetry = new IntelligentRetryAnalyzer();
    }
    
    public EnhancedRetryAnalyzer(int maxRetries, long initialDelayMs, 
                                double backoffMultiplier, 
                                boolean retryOnAllExceptions,
                                Class<? extends Throwable>[] retryableExceptions) {
        this.intelligentRetry = new IntelligentRetryAnalyzer(
            maxRetries, initialDelayMs, backoffMultiplier, 
            retryOnAllExceptions, retryableExceptions
        );
    }
    
    @Override
    public boolean retry(ITestResult result) {
        return intelligentRetry.retry(result);
    }
    
    @Override
    @SuppressWarnings("rawtypes")
    public void transform(ITestAnnotation annotation, Class testClass, 
                        Constructor testConstructor, Method testMethod) {
        annotation.setRetryAnalyzer(EnhancedRetryAnalyzer.class);
    }
    
    /**
     * Reset retry count
     */
    public void reset() {
        intelligentRetry.reset();
    }
}

