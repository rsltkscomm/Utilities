package cloud.execution.impl;

import org.openqa.selenium.WebDriver;
import reporting.TestLogManager;

import java.util.function.Consumer;

/**
 * Mobile Test Execution Task
 * Executes mobile tests on cloud infrastructure
 */
public class MobileTestExecutionTask extends BaseCloudExecutionTask {
    
    private final Consumer<WebDriver> testAction;
    private final String testDescription;
    private final String platform;
    private final String device;
    private final String version;
    
    protected MobileTestExecutionTask(Builder builder) {
        super(builder);
        this.testAction = builder.testAction;
        this.testDescription = builder.testDescription;
        this.platform = builder.platform;
        this.device = builder.device;
        this.version = builder.version;
    }
    
    @Override
    public Object execute(WebDriver driver) throws Exception {
        logTaskStart();
        
        try {
            TestLogManager.info("Executing mobile test: " + testDescription);
            TestLogManager.info("Platform: " + platform + ", Device: " + device + ", Version: " + version);
            
            // Execute the test action
            testAction.accept(driver);
            
            logTaskCompletion("Mobile test executed successfully");
            return "SUCCESS";
            
        } catch (Exception e) {
            logTaskFailure(e);
            throw e;
        }
    }
    
    @Override
    public String getDescription() {
        return String.format("Mobile test execution: %s - %s (%s %s %s)", 
            taskId, testDescription, platform, device, version);
    }
    
    /**
     * Builder for MobileTestExecutionTask
     */
    public static class Builder extends BaseCloudExecutionTask.Builder<MobileTestExecutionTask, Builder> {
        private Consumer<WebDriver> testAction;
        private String testDescription;
        private String platform;
        private String device;
        private String version;
        
        public Builder testAction(Consumer<WebDriver> testAction) {
            this.testAction = testAction;
            return this;
        }
        
        public Builder testDescription(String testDescription) {
            this.testDescription = testDescription;
            return this;
        }
        
        public Builder platform(String platform) {
            this.platform = platform;
            return this;
        }
        
        public Builder device(String device) {
            this.device = device;
            return this;
        }
        
        public Builder version(String version) {
            this.version = version;
            return this;
        }
        
        public Builder appPath(String appPath) {
            return addCapability("app", appPath);
        }
        
        public Builder appPackage(String appPackage) {
            return addCapability("appPackage", appPackage);
        }
        
        public Builder appActivity(String appActivity) {
            return addCapability("appActivity", appActivity);
        }
        
        public Builder bundleId(String bundleId) {
            return addCapability("bundleId", bundleId);
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
        
        public Builder deviceOrientation(String orientation) {
            return addCapability("deviceOrientation", orientation);
        }
        
        public Builder networkProfile(String profile) {
            return addCapability("networkProfile", profile);
        }
        
        public Builder geoLocation(String country, String state, String city) {
            return addCapability("geoLocation", String.format("%s,%s,%s", country, state, city));
        }
        
        public Builder customCapability(String key, Object value) {
            return addCapability(key, value);
        }
        
        @Override
        public MobileTestExecutionTask build() {
            if (taskId == null || taskId.trim().isEmpty()) {
                throw new IllegalArgumentException("Task ID is required");
            }
            
            if (sessionName == null || sessionName.trim().isEmpty()) {
                this.sessionName = "MobileTest-" + taskId;
            }
            
            if (testAction == null) {
                throw new IllegalArgumentException("Test action is required");
            }
            
            if (testDescription == null || testDescription.trim().isEmpty()) {
                this.testDescription = "Mobile test execution";
            }
            
            if (platform == null || platform.trim().isEmpty()) {
                throw new IllegalArgumentException("Platform is required for mobile tests");
            }
            
            if (device == null || device.trim().isEmpty()) {
                throw new IllegalArgumentException("Device is required for mobile tests");
            }
            
            if (version == null || version.trim().isEmpty()) {
                throw new IllegalArgumentException("Version is required for mobile tests");
            }
            
            // Add mobile-specific capabilities
            addCapability("platformName", platform);
            addCapability("deviceName", device);
            addCapability("platformVersion", version);
            
            if ("android".equalsIgnoreCase(platform)) {
                addCapability("browserName", "Chrome");
            } else if ("ios".equalsIgnoreCase(platform)) {
                addCapability("browserName", "Safari");
            }
            
            return new MobileTestExecutionTask(this);
        }
    }
    
    /**
     * Create an Android test task
     */
    public static MobileTestExecutionTask createAndroidTest(String taskId, String testDescription, String device, String version, Consumer<WebDriver> testAction) {
        return new Builder()
            .taskId(taskId)
            .testDescription(testDescription)
            .testAction(testAction)
            .platform("Android")
            .device(device)
            .version(version)
            .build();
    }
    
    /**
     * Create an iOS test task
     */
    public static MobileTestExecutionTask createiOSTest(String taskId, String testDescription, String device, String version, Consumer<WebDriver> testAction) {
        return new Builder()
            .taskId(taskId)
            .testDescription(testDescription)
            .testAction(testAction)
            .platform("iOS")
            .device(device)
            .version(version)
            .build();
    }
    
    /**
     * Create a mobile app test task
     */
    public static MobileTestExecutionTask createAppTest(String taskId, String testDescription, String platform, String device, String version, String appPath, Consumer<WebDriver> testAction) {
        return new Builder()
            .taskId(taskId)
            .testDescription(testDescription)
            .testAction(testAction)
            .platform(platform)
            .device(device)
            .version(version)
            .appPath(appPath)
            .build();
    }
}
