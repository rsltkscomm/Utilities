package cloud.execution;

import cloud.CloudConfiguration;
import cloud.providers.CloudProvider;
import cloud.providers.CloudProviderFactory;
import cloud.session.CloudSession;
import cloud.session.CloudSessionInfo;
import reporting.TestLogManager;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Cloud Execution Engine
 * Manages parallel execution of tests on cloud infrastructure
 */
public class CloudExecutionEngine {
    
    private final CloudConfiguration config;
    private final CloudProvider provider;
    private final ExecutorService executorService;
    private final Map<String, CloudSession> activeSessions;
    private final Map<String, Future<ExecutionResult>> executionFutures;
    private final AtomicInteger sessionCounter;
    private volatile boolean isRunning;
    
    public CloudExecutionEngine(CloudConfiguration config) {
        this.config = config;
        this.provider = CloudProviderFactory.createActiveProvider(config);
        this.executorService = createExecutorService();
        this.activeSessions = new ConcurrentHashMap<>();
        this.executionFutures = new ConcurrentHashMap<>();
        this.sessionCounter = new AtomicInteger(0);
        this.isRunning = false;
        
        TestLogManager.info("CloudExecutionEngine initialized with provider: " + provider.getDisplayName());
    }
    
    /**
     * Create executor service based on configuration
     */
    private ExecutorService createExecutorService() {
        int maxThreads = config.getParallelSessions();
        return new ThreadPoolExecutor(
            maxThreads,
            maxThreads,
            60L, TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(),
            new CloudExecutionThreadFactory(),
            new ThreadPoolExecutor.CallerRunsPolicy()
        );
    }
    
    /**
     * Start cloud execution engine
     */
    public synchronized void start() {
        if (isRunning) {
            TestLogManager.warning("Cloud execution engine is already running");
            return;
        }
        
        if (!config.isCloudEnabled()) {
            throw new IllegalStateException("Cloud testing is not enabled");
        }
        
        if (!provider.isConfigured()) {
            throw new IllegalStateException("Cloud provider is not properly configured");
        }
        
        isRunning = true;
        TestLogManager.info("Cloud execution engine started with " + config.getParallelSessions() + " parallel sessions");
    }
    
    /**
     * Stop cloud execution engine
     */
    public synchronized void stop() {
        if (!isRunning) {
            TestLogManager.warning("Cloud execution engine is not running");
            return;
        }
        
        TestLogManager.info("Stopping cloud execution engine...");
        isRunning = false;
        
        // Cancel all pending executions
        for (Future<ExecutionResult> future : executionFutures.values()) {
            if (!future.isDone()) {
                future.cancel(true);
            }
        }
        
        // Stop all active sessions
        for (CloudSession session : activeSessions.values()) {
            try {
                session.stop();
            } catch (Exception e) {
                TestLogManager.warning("Failed to stop session during shutdown: " + session.getSessionId(), e);
            }
        }
        
        // Shutdown executor service
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(30, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
                if (!executorService.awaitTermination(10, TimeUnit.SECONDS)) {
                    TestLogManager.warning("Executor service did not terminate gracefully");
                }
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }
        
        activeSessions.clear();
        executionFutures.clear();
        
        TestLogManager.info("Cloud execution engine stopped");
    }
    
    /**
     * Execute test task on cloud infrastructure
     */
    public Future<ExecutionResult> executeTask(CloudExecutionTask task) {
        if (!isRunning) {
            throw new IllegalStateException("Cloud execution engine is not running");
        }
        
        if (activeSessions.size() >= config.getParallelSessions()) {
            throw new IllegalStateException("Maximum parallel sessions reached: " + config.getParallelSessions());
        }
        
        String sessionId = generateSessionId();
        TestLogManager.info("Submitting cloud execution task: " + sessionId);
        
        Future<ExecutionResult> future = executorService.submit(() -> {
            return executeTaskInternal(sessionId, task);
        });
        
        executionFutures.put(sessionId, future);
        return future;
    }
    
    /**
     * Execute multiple tasks in parallel
     */
    public Map<String, Future<ExecutionResult>> executeTasks(List<CloudExecutionTask> tasks) {
        Map<String, Future<ExecutionResult>> futures = new HashMap<>();
        
        for (CloudExecutionTask task : tasks) {
            try {
                Future<ExecutionResult> future = executeTask(task);
                futures.put(task.getTaskId(), future);
            } catch (Exception e) {
                TestLogManager.error("Failed to submit task: " + task.getTaskId(), e);
                // Create a failed result
                ExecutionResult failedResult = ExecutionResult.failed(task.getTaskId(), e);
                futures.put(task.getTaskId(), CompletableFuture.completedFuture(failedResult));
            }
        }
        
        return futures;
    }
    
    /**
     * Wait for all executions to complete
     */
    public Map<String, ExecutionResult> waitForCompletion(Map<String, Future<ExecutionResult>> futures, long timeout, TimeUnit unit) {
        Map<String, ExecutionResult> results = new HashMap<>();
        
        for (Map.Entry<String, Future<ExecutionResult>> entry : futures.entrySet()) {
            String taskId = entry.getKey();
            Future<ExecutionResult> future = entry.getValue();
            
            try {
                ExecutionResult result = future.get(timeout, unit);
                results.put(taskId, result);
            } catch (TimeoutException e) {
                TestLogManager.error("Task execution timeout: " + taskId);
                results.put(taskId, ExecutionResult.timeout(taskId));
                future.cancel(true);
            } catch (Exception e) {
                TestLogManager.error("Task execution failed: " + taskId, e);
                results.put(taskId, ExecutionResult.failed(taskId, e));
            }
        }
        
        return results;
    }
    
    /**
     * Get execution statistics
     */
    public ExecutionStatistics getStatistics() {
        ExecutionStatistics stats = new ExecutionStatistics();
        
        stats.setTotalSessions(activeSessions.size());
        stats.setActiveExecutions(executionFutures.size());
        stats.setMaxParallelSessions(config.getParallelSessions());
        stats.setProviderName(provider.getDisplayName());
        
        // Count completed executions
        int completed = 0;
        int failed = 0;
        int running = 0;
        
        for (Future<ExecutionResult> future : executionFutures.values()) {
            if (future.isDone()) {
                try {
                    ExecutionResult result = future.get();
                    if (result.isSuccess()) {
                        completed++;
                    } else {
                        failed++;
                    }
                } catch (Exception e) {
                    failed++;
                }
            } else {
                running++;
            }
        }
        
        stats.setCompletedExecutions(completed);
        stats.setFailedExecutions(failed);
        stats.setRunningExecutions(running);
        
        return stats;
    }
    
    /**
     * Get active sessions
     */
    public Map<String, CloudSession> getActiveSessions() {
        return new HashMap<>(activeSessions);
    }
    
    /**
     * Get session information
     */
    public CloudSessionInfo getSessionInfo(String sessionId) {
        CloudSession session = activeSessions.get(sessionId);
        return session != null ? session.getSessionInfo() : null;
    }
    
    /**
     * Stop specific session
     */
    public boolean stopSession(String sessionId) {
        CloudSession session = activeSessions.get(sessionId);
        if (session != null) {
            boolean stopped = session.stop();
            if (stopped) {
                activeSessions.remove(sessionId);
                executionFutures.remove(sessionId);
            }
            return stopped;
        }
        return false;
    }
    
    /**
     * Check if engine is running
     */
    public boolean isRunning() {
        return isRunning;
    }
    
    /**
     * Internal task execution
     */
    private ExecutionResult executeTaskInternal(String sessionId, CloudExecutionTask task) {
        CloudSession session = null;
        
        try {
            TestLogManager.info("Starting cloud session: " + sessionId);
            
            // Create cloud session
            session = provider.createSession(task.getSessionName(), task.getCapabilities());
            activeSessions.put(sessionId, session);
            
            // Execute the task
            TestLogManager.info("Executing task: " + task.getTaskId());
            Object result = task.execute(session.getWebDriver());
            
            // Update session status
            session.updateStatus("passed", "Task completed successfully");
            
            TestLogManager.success("Task completed successfully: " + task.getTaskId());
            return ExecutionResult.success(task.getTaskId(), result);
            
        } catch (Exception e) {
            TestLogManager.error("Task execution failed: " + task.getTaskId(), e);
            
            if (session != null) {
                try {
                    session.updateStatus("failed", "Task failed: " + e.getMessage());
                } catch (Exception updateError) {
                    TestLogManager.warning("Failed to update session status", updateError);
                }
            }
            
            return ExecutionResult.failed(task.getTaskId(), e);
            
        } finally {
            // Clean up session
            if (session != null) {
                try {
                    activeSessions.remove(sessionId);
                    executionFutures.remove(sessionId);
                } catch (Exception e) {
                    TestLogManager.warning("Failed to clean up session: " + sessionId, e);
                }
            }
        }
    }
    
    /**
     * Generate unique session ID
     */
    private String generateSessionId() {
        return "session-" + sessionCounter.incrementAndGet() + "-" + System.currentTimeMillis();
    }
    
    /**
     * Custom thread factory for cloud execution
     */
    private static class CloudExecutionThreadFactory implements ThreadFactory {
        private final AtomicInteger threadNumber = new AtomicInteger(1);
        private final String namePrefix = "CloudExecution-";
        
        @Override
        public Thread newThread(Runnable r) {
            Thread thread = new Thread(r, namePrefix + threadNumber.getAndIncrement());
            thread.setDaemon(false);
            thread.setPriority(Thread.NORM_PRIORITY);
            return thread;
        }
    }
    
    /**
     * Execution result class
     */
    public static class ExecutionResult {
        private final String taskId;
        private final boolean success;
        private final Object result;
        private final Exception exception;
        private final long executionTime;
        private final String status;
        
        private ExecutionResult(String taskId, boolean success, Object result, Exception exception, String status) {
            this.taskId = taskId;
            this.success = success;
            this.result = result;
            this.exception = exception;
            this.executionTime = System.currentTimeMillis();
            this.status = status;
        }
        
        public static ExecutionResult success(String taskId, Object result) {
            return new ExecutionResult(taskId, true, result, null, "SUCCESS");
        }
        
        public static ExecutionResult failed(String taskId, Exception exception) {
            return new ExecutionResult(taskId, false, null, exception, "FAILED");
        }
        
        public static ExecutionResult timeout(String taskId) {
            return new ExecutionResult(taskId, false, null, null, "TIMEOUT");
        }
        
        public String getTaskId() { return taskId; }
        public boolean isSuccess() { return success; }
        public Object getResult() { return result; }
        public Exception getException() { return exception; }
        public long getExecutionTime() { return executionTime; }
        public String getStatus() { return status; }
    }
    
    /**
     * Execution statistics class
     */
    public static class ExecutionStatistics {
        private int totalSessions;
        private int activeExecutions;
        private int completedExecutions;
        private int failedExecutions;
        private int runningExecutions;
        private int maxParallelSessions;
        private String providerName;
        
        // Getters and setters
        public int getTotalSessions() { return totalSessions; }
        public void setTotalSessions(int totalSessions) { this.totalSessions = totalSessions; }
        
        public int getActiveExecutions() { return activeExecutions; }
        public void setActiveExecutions(int activeExecutions) { this.activeExecutions = activeExecutions; }
        
        public int getCompletedExecutions() { return completedExecutions; }
        public void setCompletedExecutions(int completedExecutions) { this.completedExecutions = completedExecutions; }
        
        public int getFailedExecutions() { return failedExecutions; }
        public void setFailedExecutions(int failedExecutions) { this.failedExecutions = failedExecutions; }
        
        public int getRunningExecutions() { return runningExecutions; }
        public void setRunningExecutions(int runningExecutions) { this.runningExecutions = runningExecutions; }
        
        public int getMaxParallelSessions() { return maxParallelSessions; }
        public void setMaxParallelSessions(int maxParallelSessions) { this.maxParallelSessions = maxParallelSessions; }
        
        public String getProviderName() { return providerName; }
        public void setProviderName(String providerName) { this.providerName = providerName; }
        
        public double getSuccessRate() {
            int total = completedExecutions + failedExecutions;
            return total > 0 ? (double) completedExecutions / total * 100 : 0.0;
        }
    }
}
