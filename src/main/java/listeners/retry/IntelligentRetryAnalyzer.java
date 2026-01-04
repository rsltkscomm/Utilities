package listeners.retry;

import org.testng.IRetryAnalyzer;
import org.testng.ITestResult;
import reporting.TestLogManager;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Intelligent retry analyzer with exponential backoff and exception-based retry logic
 */
public class IntelligentRetryAnalyzer implements IRetryAnalyzer {
    
    private final int maxRetries;
    private final long initialDelayMs;
    private final double backoffMultiplier;
    private final Set<Class<? extends Throwable>> retryableExceptions;
    private final boolean retryOnAllExceptions;
    
    // Thread-local to track retry count per test
    private final ThreadLocal<Integer> retryCount = ThreadLocal.withInitial(() -> 0);
    
    public IntelligentRetryAnalyzer() {
        this(3, 1000, 2.0, true, null);
    }
    
    public IntelligentRetryAnalyzer(int maxRetries, long initialDelayMs, 
                                    double backoffMultiplier, 
                                    boolean retryOnAllExceptions,
                                    Class<? extends Throwable>[] retryableExceptions) {
        this.maxRetries = maxRetries;
        this.initialDelayMs = initialDelayMs;
        this.backoffMultiplier = backoffMultiplier;
        this.retryOnAllExceptions = retryOnAllExceptions;
        this.retryableExceptions = retryableExceptions != null ? 
            new HashSet<>(Arrays.asList(retryableExceptions)) : new HashSet<>();
    }
    
    @Override
    public boolean retry(ITestResult result) {
        int currentRetry = retryCount.get();
        
        if (currentRetry >= maxRetries) {
            retryCount.remove();
            TestLogManager.warning("Max retries (" + maxRetries + ") reached for test: " + 
                result.getMethod().getMethodName());
            return false;
        }
        
        // Check if exception is retryable
        Throwable throwable = result.getThrowable();
        if (throwable != null && !shouldRetryOnException(throwable)) {
            TestLogManager.info("Exception not retryable: " + throwable.getClass().getName());
            retryCount.remove();
            return false;
        }
        
        // Calculate exponential backoff delay
        long delay = calculateBackoffDelay(currentRetry);
        
        TestLogManager.info("Retrying test: " + result.getMethod().getMethodName() + 
            " (Attempt " + (currentRetry + 1) + "/" + maxRetries + ") after " + delay + "ms");
        
        // Apply backoff delay
        try {
            Thread.sleep(delay);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            TestLogManager.warning("Retry delay interrupted");
            retryCount.remove();
            return false;
        }
        
        retryCount.set(currentRetry + 1);
        return true;
    }
    
    /**
     * Check if exception is retryable
     */
    private boolean shouldRetryOnException(Throwable throwable) {
        if (retryOnAllExceptions) {
            return true;
        }
        
        if (retryableExceptions.isEmpty()) {
            return false;
        }
        
        // Check if exception or any of its causes matches retryable exceptions
        Throwable current = throwable;
        while (current != null) {
            if (retryableExceptions.contains(current.getClass())) {
                return true;
            }
            current = current.getCause();
        }
        
        return false;
    }
    
    /**
     * Calculate exponential backoff delay
     */
    private long calculateBackoffDelay(int retryAttempt) {
        return (long) (initialDelayMs * Math.pow(backoffMultiplier, retryAttempt));
    }
    
    /**
     * Reset retry count (called after test passes)
     */
    public void reset() {
        retryCount.remove();
    }
    
    /**
     * Get current retry count
     */
    public int getCurrentRetryCount() {
        return retryCount.get();
    }
}

