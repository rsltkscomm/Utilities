package cloud.execution.impl;

import cloud.execution.CloudExecutionTask;
import org.openqa.selenium.WebDriver;
import reporting.TestLogManager;

import java.util.HashMap;
import java.util.Map;

/**
 * Base implementation of CloudExecutionTask
 * Provides common functionality for cloud execution tasks
 */
public abstract class BaseCloudExecutionTask implements CloudExecutionTask {
    
    protected final String taskId;
    private final String sessionName;
    private final Map<String, Object> capabilities;
    private final int priority;
    private final long timeoutSeconds;
    private final boolean retryable;
    private final int maxRetries;
    
    protected BaseCloudExecutionTask(Builder<?, ?> builder) {
        this.taskId = builder.taskId;
        this.sessionName = builder.sessionName;
        this.capabilities = new HashMap<>(builder.capabilities);
        this.priority = builder.priority;
        this.timeoutSeconds = builder.timeoutSeconds;
        this.retryable = builder.retryable;
        this.maxRetries = builder.maxRetries;
    }
    
    @Override
    public String getTaskId() {
        return taskId;
    }
    
    @Override
    public String getSessionName() {
        return sessionName;
    }
    
    @Override
    public Map<String, Object> getCapabilities() {
        return new HashMap<>(capabilities);
    }
    
    @Override
    public int getPriority() {
        return priority;
    }
    
    @Override
    public long getTimeoutSeconds() {
        return timeoutSeconds;
    }
    
    @Override
    public boolean isRetryable() {
        return retryable;
    }
    
    @Override
    public int getMaxRetries() {
        return maxRetries;
    }
    
    /**
     * Add capability to the task
     */
    protected void addCapability(String key, Object value) {
        capabilities.put(key, value);
    }
    
    /**
     * Remove capability from the task
     */
    protected void removeCapability(String key) {
        capabilities.remove(key);
    }
    
    /**
     * Get capability value
     */
    protected Object getCapability(String key) {
        return capabilities.get(key);
    }
    
    /**
     * Check if capability exists
     */
    protected boolean hasCapability(String key) {
        return capabilities.containsKey(key);
    }
    
    /**
     * Abstract method to be implemented by subclasses
     * This is where the actual test logic goes
     */
    @Override
    public abstract Object execute(WebDriver driver) throws Exception;
    
    /**
     * Get task description
     */
    @Override
    public String getDescription() {
        return String.format("Cloud execution task: %s (Priority: %d, Timeout: %ds)", 
            taskId, priority, timeoutSeconds);
    }
    
    /**
     * Log task start
     */
    protected void logTaskStart() {
        TestLogManager.info("Starting cloud execution task: " + taskId);
        TestLogManager.info("Session name: " + sessionName);
        TestLogManager.info("Capabilities: " + capabilities);
        TestLogManager.info("Priority: " + priority + ", Timeout: " + timeoutSeconds + "s");
    }
    
    /**
     * Log task completion
     */
    protected void logTaskCompletion(Object result) {
        TestLogManager.success("Cloud execution task completed: " + taskId);
        if (result != null) {
            TestLogManager.info("Task result: " + result.toString());
        }
    }
    
    /**
     * Log task failure
     */
    protected void logTaskFailure(Exception exception) {
        TestLogManager.error("Cloud execution task failed: " + taskId, exception);
    }
    
    /**
     * Builder class for creating tasks
     */
    public static abstract class Builder<T extends BaseCloudExecutionTask, B extends Builder<T, B>> {
        protected String taskId;
        protected String sessionName;
        protected Map<String, Object> capabilities;
        protected int priority;
        protected long timeoutSeconds;
        protected boolean retryable;
        protected int maxRetries;
        
        protected Builder() {
            this.capabilities = new HashMap<>();
            this.priority = 5; // Default priority
            this.timeoutSeconds = 300; // 5 minutes default
            this.retryable = true;
            this.maxRetries = 3;
        }
        
        public B taskId(String taskId) {
            this.taskId = taskId;
            return self();
        }
        
        public B sessionName(String sessionName) {
            this.sessionName = sessionName;
            return self();
        }
        
        public B addCapability(String key, Object value) {
            this.capabilities.put(key, value);
            return self();
        }
        
        public B capabilities(Map<String, Object> capabilities) {
            this.capabilities.putAll(capabilities);
            return self();
        }
        
        public B priority(int priority) {
            this.priority = priority;
            return self();
        }
        
        public B timeoutSeconds(long timeoutSeconds) {
            this.timeoutSeconds = timeoutSeconds;
            return self();
        }
        
        public B retryable(boolean retryable) {
            this.retryable = retryable;
            return self();
        }
        
        public B maxRetries(int maxRetries) {
            this.maxRetries = maxRetries;
            return self();
        }
        
        public abstract T build();
        
        @SuppressWarnings("unchecked")
        protected B self() {
            return (B) this;
        }
    }
}
