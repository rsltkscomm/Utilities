package cloud.execution;

import org.openqa.selenium.WebDriver;

import java.util.concurrent.Callable;

/**
 * Cloud Execution Task Interface
 * Defines a task that can be executed on cloud infrastructure
 */
public interface CloudExecutionTask extends Callable<Object> {
    
    /**
     * Get unique task ID
     */
    String getTaskId();
    
    /**
     * Get session name for the task
     */
    String getSessionName();
    
    /**
     * Get capabilities required for the task
     */
    java.util.Map<String, Object> getCapabilities();
    
    /**
     * Execute the task with the provided WebDriver
     */
    Object execute(WebDriver driver) throws Exception;
    
    /**
     * Get task priority (higher number = higher priority)
     */
    default int getPriority() {
        return 5; // Default priority
    }
    
    /**
     * Get task timeout in seconds
     */
    default long getTimeoutSeconds() {
        return 300; // 5 minutes default
    }
    
    /**
     * Get task description
     */
    default String getDescription() {
        return "Cloud execution task: " + getTaskId();
    }
    
    /**
     * Check if task supports retry on failure
     */
    default boolean isRetryable() {
        return true; // Default to retryable
    }
    
    /**
     * Get maximum retry attempts
     */
    default int getMaxRetries() {
        return 3; // Default max retries
    }
    
    /**
     * Callable implementation
     */
    @Override
    default Object call() throws Exception {
        // This will be called by the execution engine
        // The actual execution is handled by execute(WebDriver)
        throw new UnsupportedOperationException("Task execution is handled by CloudExecutionEngine");
    }
}
