package cloud.execution.impl;

import org.openqa.selenium.WebDriver;
import reporting.TestLogManager;

import java.util.function.Consumer;

/**
 * Web Test Execution Task
 * Executes web-based tests on cloud infrastructure
 */
public class WebTestExecutionTask extends BaseCloudExecutionTask {
    
    private final Consumer<WebDriver> testAction;
    private final String testDescription;
    
    protected WebTestExecutionTask(Builder builder) {
        super(builder);
        this.testAction = builder.testAction;
        this.testDescription = builder.testDescription;
    }
    
    @Override
    public Object execute(WebDriver driver) throws Exception {
        logTaskStart();
        
        try {
            TestLogManager.info("Executing web test: " + testDescription);
            
            // Execute the test action
            testAction.accept(driver);
            
            logTaskCompletion("Test executed successfully");
            return "SUCCESS";
            
        } catch (Exception e) {
            logTaskFailure(e);
            throw e;
        }
    }
    
    @Override
    public String getDescription() {
        return String.format("Web test execution: %s - %s", taskId, testDescription);
    }
    
    /**
     * Builder for WebTestExecutionTask
     */
    public static class Builder extends BaseCloudExecutionTask.Builder<WebTestExecutionTask, Builder> {
        private Consumer<WebDriver> testAction;
        private String testDescription;
        
        public Builder testAction(Consumer<WebDriver> testAction) {
            this.testAction = testAction;
            return this;
        }
        
        public Builder testDescription(String testDescription) {
            this.testDescription = testDescription;
            return this;
        }
        
        public Builder browser(String browser) {
            return addCapability("browserName", browser);
        }
        
        public Builder platform(String platform) {
            return addCapability("platform", platform);
        }
        
        public Builder version(String version) {
            return addCapability("version", version);
        }
        
        public Builder resolution(String resolution) {
            return addCapability("screenResolution", resolution);
        }
        
        public Builder enableVideo(boolean enable) {
            return addCapability("recordVideo", enable);
        }
        
        public Builder enableScreenshots(boolean enable) {
            return addCapability("recordScreenshots", enable);
        }
        
        public Builder enableNetworkLogs(boolean enable) {
            return addCapability("recordNetwork", enable);
        }
        
        public Builder enableConsoleLogs(boolean enable) {
            return addCapability("recordConsole", enable);
        }
        
        public Builder buildName(String buildName) {
            return addCapability("build", buildName);
        }
        
        public Builder sessionName(String sessionName) {
            return addCapability("name", sessionName);
        }
        
        public Builder project(String project) {
            return addCapability("project", project);
        }
        
        public Builder tags(String... tags) {
            return addCapability("tags", tags);
        }
        
        @Override
        public WebTestExecutionTask build() {
            if (taskId == null || taskId.trim().isEmpty()) {
                throw new IllegalArgumentException("Task ID is required");
            }
            
            if (sessionName == null || sessionName.trim().isEmpty()) {
                this.sessionName = "WebTest-" + taskId;
            }
            
            if (testAction == null) {
                throw new IllegalArgumentException("Test action is required");
            }
            
            if (testDescription == null || testDescription.trim().isEmpty()) {
                this.testDescription = "Web test execution";
            }
            
            return new WebTestExecutionTask(this);
        }
    }
    
    /**
     * Create a simple web test task
     */
    public static WebTestExecutionTask create(String taskId, String testDescription, Consumer<WebDriver> testAction) {
        return new Builder()
            .taskId(taskId)
            .testDescription(testDescription)
            .testAction(testAction)
            .build();
    }
    
    /**
     * Create a web test task with browser specification
     */
    public static WebTestExecutionTask create(String taskId, String testDescription, String browser, String platform, String version, Consumer<WebDriver> testAction) {
        return new Builder()
            .taskId(taskId)
            .testDescription(testDescription)
            .testAction(testAction)
            .browser(browser)
            .platform(platform)
            .version(version)
            .build();
    }
}
