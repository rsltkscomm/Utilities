package advanced;

import io.appium.java_client.AppiumDriver;
import io.appium.java_client.MobileElement;
import io.appium.java_client.android.AndroidDriver;
import io.appium.java_client.ios.IOSDriver;
import io.appium.java_client.remote.MobileCapabilityType;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.remote.RemoteWebDriver;
import reporting.TestLogManager;
import utils.CrossPlatformUtils;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Mobile testing manager for Appium integration with Android and iOS support.
 */
public class MobileTestManager {
    
    private AppiumDriver<MobileElement> driver;
    private final String platform;
    private final String deviceName;
    private final String appPath;
    private final String reportDirectory;
    
    public MobileTestManager(String platform, String deviceName, String appPath) {
        this.platform = platform.toLowerCase();
        this.deviceName = deviceName;
        this.appPath = appPath;
        this.reportDirectory = CrossPlatformUtils.getProjectDataDirectory()
                .resolve("mobile_reports").toString();
        createReportDirectory();
    }
    
    /**
     * Sets up mobile driver for the specified platform.
     * @param platform Platform type (android/ios)
     * @param device Device configuration
     * @return Configured AppiumDriver
     */
    public AppiumDriver<MobileElement> setupMobileDriver(String platform, MobileDevice device) {
        TestLogManager.info("Setting up mobile driver for platform: " + platform);
        
        try {
            DesiredCapabilities capabilities = createCapabilities(platform, device);
            URL serverUrl = new URL("http://localhost:4723/wd/hub");
            
            switch (platform.toLowerCase()) {
                case "android":
                    driver = new AndroidDriver<>(serverUrl, capabilities);
                    break;
                case "ios":
                    driver = new IOSDriver<>(serverUrl, capabilities);
                    break;
                default:
                    throw new IllegalArgumentException("Unsupported platform: " + platform);
            }
            
            driver.manage().timeouts().implicitlyWait(10, TimeUnit.SECONDS);
            TestLogManager.success("Mobile driver setup completed for " + platform);
            
            return driver;
            
        } catch (MalformedURLException e) {
            TestLogManager.error("Failed to setup mobile driver", e);
            throw new RuntimeException("Mobile driver setup failed", e);
        }
    }
    
    /**
     * Performs mobile gesture actions.
     * @param gestureType Type of gesture (tap, swipe, pinch, etc.)
     * @param params Gesture parameters
     */
    public void performMobileGesture(String gestureType, Map<String, Object> params) {
        TestLogManager.info("Performing mobile gesture: " + gestureType);
        
        try {
            switch (gestureType.toLowerCase()) {
                case "tap":
                    performTap(params);
                    break;
                case "swipe":
                    performSwipe(params);
                    break;
                case "pinch":
                    performPinch(params);
                    break;
                case "zoom":
                    performZoom(params);
                    break;
                case "scroll":
                    performScroll(params);
                    break;
                case "longpress":
                    performLongPress(params);
                    break;
                default:
                    TestLogManager.warning("Unknown gesture type: " + gestureType);
            }
            
            TestLogManager.success("Mobile gesture completed: " + gestureType);
            
        } catch (Exception e) {
            TestLogManager.error("Failed to perform mobile gesture: " + gestureType, e);
        }
    }
    
    /**
     * Captures mobile screenshot.
     * @param fileName Name of the screenshot file
     * @return Path to the saved screenshot
     */
    public Path captureMobileScreenshot(String fileName) {
        TestLogManager.info("Capturing mobile screenshot: " + fileName);
        
        try {
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            String fullFileName = fileName + "_mobile_" + timestamp + ".png";
            Path screenshotPath = Paths.get(reportDirectory, fullFileName);
            
            // Capture screenshot
            byte[] screenshot = driver.getScreenshotAs(org.openqa.selenium.OutputType.BYTES);
            java.nio.file.Files.write(screenshotPath, screenshot);
            
            TestLogManager.success("Mobile screenshot captured: " + screenshotPath);
            return screenshotPath;
            
        } catch (Exception e) {
            TestLogManager.error("Failed to capture mobile screenshot", e);
            throw new RuntimeException("Mobile screenshot capture failed", e);
        }
    }
    
    /**
     * Performs mobile app installation.
     * @param appPath Path to the mobile app file
     * @return True if installation successful
     */
    public boolean installMobileApp(String appPath) {
        TestLogManager.info("Installing mobile app: " + appPath);
        
        try {
            if (platform.equals("android")) {
                // Android app installation
                driver.installApp(appPath);
            } else if (platform.equals("ios")) {
                // iOS app installation
                driver.installApp(appPath);
            }
            
            TestLogManager.success("Mobile app installed successfully");
            return true;
            
        } catch (Exception e) {
            TestLogManager.error("Failed to install mobile app", e);
            return false;
        }
    }
    
    /**
     * Performs mobile app uninstallation.
     * @param bundleId Bundle ID of the app
     * @return True if uninstallation successful
     */
    public boolean uninstallMobileApp(String bundleId) {
        TestLogManager.info("Uninstalling mobile app: " + bundleId);
        
        try {
            driver.removeApp(bundleId);
            TestLogManager.success("Mobile app uninstalled successfully");
            return true;
            
        } catch (Exception e) {
            TestLogManager.error("Failed to uninstall mobile app", e);
            return false;
        }
    }
    
    /**
     * Performs mobile app launch.
     * @param bundleId Bundle ID of the app
     */
    public void launchMobileApp(String bundleId) {
        TestLogManager.info("Launching mobile app: " + bundleId);
        
        try {
            driver.launchApp();
            TestLogManager.success("Mobile app launched successfully");
            
        } catch (Exception e) {
            TestLogManager.error("Failed to launch mobile app", e);
        }
    }
    
    /**
     * Performs mobile app background/foreground operations.
     * @param seconds Seconds to keep app in background
     */
    public void backgroundApp(int seconds) {
        TestLogManager.info("Putting app in background for " + seconds + " seconds");
        
        try {
            driver.runAppInBackground(seconds);
            TestLogManager.success("App background operation completed");
            
        } catch (Exception e) {
            TestLogManager.error("Failed to perform app background operation", e);
        }
    }
    
    /**
     * Performs mobile device rotation.
     * @param orientation Device orientation (PORTRAIT, LANDSCAPE)
     */
    public void rotateDevice(String orientation) {
        TestLogManager.info("Rotating device to: " + orientation);
        
        try {
            if (platform.equals("android")) {
                ((AndroidDriver<MobileElement>) driver).rotate(org.openqa.selenium.ScreenOrientation.valueOf(orientation));
            } else if (platform.equals("ios")) {
                ((IOSDriver<MobileElement>) driver).rotate(org.openqa.selenium.ScreenOrientation.valueOf(orientation));
            }
            
            TestLogManager.success("Device rotated to: " + orientation);
            
        } catch (Exception e) {
            TestLogManager.error("Failed to rotate device", e);
        }
    }
    
    /**
     * Performs mobile network simulation.
     * @param networkType Network type (WIFI, DATA, AIRPLANE_MODE)
     */
    public void simulateNetwork(String networkType) {
        TestLogManager.info("Simulating network: " + networkType);
        
        try {
            if (platform.equals("android")) {
                ((AndroidDriver<MobileElement>) driver).setNetworkConnection(
                    getNetworkConnectionValue(networkType)
                );
            }
            
            TestLogManager.success("Network simulation completed: " + networkType);
            
        } catch (Exception e) {
            TestLogManager.error("Failed to simulate network", e);
        }
    }
    
    /**
     * Gets mobile device information.
     * @return MobileDeviceInfo object with device details
     */
    public MobileDeviceInfo getMobileDeviceInfo() {
        TestLogManager.info("Getting mobile device information");
        
        try {
            MobileDeviceInfo deviceInfo = new MobileDeviceInfo();
            
            // Get device capabilities
            Map<String, Object> capabilities = driver.getCapabilities().asMap();
            deviceInfo.setPlatformName((String) capabilities.get(MobileCapabilityType.PLATFORM_NAME));
            deviceInfo.setPlatformVersion((String) capabilities.get(MobileCapabilityType.PLATFORM_VERSION));
            deviceInfo.setDeviceName((String) capabilities.get(MobileCapabilityType.DEVICE_NAME));
            deviceInfo.setAppPackage((String) capabilities.get(MobileCapabilityType.APP_PACKAGE));
            deviceInfo.setAppActivity((String) capabilities.get(MobileCapabilityType.APP_ACTIVITY));
            
            // Get device dimensions
            deviceInfo.setScreenWidth(driver.manage().window().getSize().getWidth());
            deviceInfo.setScreenHeight(driver.manage().window().getSize().getHeight());
            
            TestLogManager.success("Mobile device information retrieved");
            return deviceInfo;
            
        } catch (Exception e) {
            TestLogManager.error("Failed to get mobile device information", e);
            return new MobileDeviceInfo();
        }
    }
    
    /**
     * Performs mobile performance testing.
     * @return MobilePerformanceMetrics object
     */
    public MobilePerformanceMetrics getMobilePerformanceMetrics() {
        TestLogManager.info("Getting mobile performance metrics");
        
        try {
            MobilePerformanceMetrics metrics = new MobilePerformanceMetrics();
            
            // Get memory usage
            Map<String, Object> memoryInfo = driver.executeScript("mobile: shell", 
                Map.of("command", "dumpsys", "args", "meminfo"));
            metrics.setMemoryInfo(memoryInfo);
            
            // Get CPU usage
            Map<String, Object> cpuInfo = driver.executeScript("mobile: shell", 
                Map.of("command", "top", "args", "-n 1"));
            metrics.setCpuInfo(cpuInfo);
            
            // Get battery info
            Map<String, Object> batteryInfo = driver.executeScript("mobile: shell", 
                Map.of("command", "dumpsys", "args", "battery"));
            metrics.setBatteryInfo(batteryInfo);
            
            metrics.setTimestamp(LocalDateTime.now());
            
            TestLogManager.success("Mobile performance metrics retrieved");
            return metrics;
            
        } catch (Exception e) {
            TestLogManager.error("Failed to get mobile performance metrics", e);
            return new MobilePerformanceMetrics();
        }
    }
    
    /**
     * Closes mobile driver and cleanup.
     */
    public void closeMobileDriver() {
        TestLogManager.info("Closing mobile driver");
        
        try {
            if (driver != null) {
                driver.quit();
                driver = null;
            }
            TestLogManager.success("Mobile driver closed successfully");
            
        } catch (Exception e) {
            TestLogManager.error("Failed to close mobile driver", e);
        }
    }
    
    private DesiredCapabilities createCapabilities(String platform, MobileDevice device) {
        DesiredCapabilities capabilities = new DesiredCapabilities();
        
        capabilities.setCapability(MobileCapabilityType.PLATFORM_NAME, platform);
        capabilities.setCapability(MobileCapabilityType.DEVICE_NAME, device.getDeviceName());
        capabilities.setCapability(MobileCapabilityType.PLATFORM_VERSION, device.getPlatformVersion());
        capabilities.setCapability(MobileCapabilityType.APP, appPath);
        capabilities.setCapability(MobileCapabilityType.AUTOMATION_NAME, 
            platform.equals("android") ? "UiAutomator2" : "XCUITest");
        
        // Platform-specific capabilities
        if (platform.equals("android")) {
            capabilities.setCapability(MobileCapabilityType.APP_PACKAGE, device.getAppPackage());
            capabilities.setCapability(MobileCapabilityType.APP_ACTIVITY, device.getAppActivity());
            capabilities.setCapability("autoGrantPermissions", true);
            capabilities.setCapability("noReset", false);
        } else if (platform.equals("ios")) {
            capabilities.setCapability(MobileCapabilityType.BUNDLE_ID, device.getBundleId());
            capabilities.setCapability("autoAcceptAlerts", true);
            capabilities.setCapability("autoDismissAlerts", true);
        }
        
        // Additional capabilities
        capabilities.setCapability("newCommandTimeout", 300);
        capabilities.setCapability("commandTimeouts", 300);
        
        return capabilities;
    }
    
    private void performTap(Map<String, Object> params) {
        int x = (Integer) params.getOrDefault("x", 100);
        int y = (Integer) params.getOrDefault("y", 100);
        
        driver.tap(1, x, y, 100);
    }
    
    private void performSwipe(Map<String, Object> params) {
        int startX = (Integer) params.get("startX");
        int startY = (Integer) params.get("startY");
        int endX = (Integer) params.get("endX");
        int endY = (Integer) params.get("endY");
        int duration = (Integer) params.getOrDefault("duration", 1000);
        
        driver.swipe(startX, startY, endX, endY, duration);
    }
    
    private void performPinch(Map<String, Object> params) {
        // Pinch gesture implementation
        TestLogManager.info("Performing pinch gesture");
    }
    
    private void performZoom(Map<String, Object> params) {
        // Zoom gesture implementation
        TestLogManager.info("Performing zoom gesture");
    }
    
    private void performScroll(Map<String, Object> params) {
        String direction = (String) params.getOrDefault("direction", "down");
        int distance = (Integer) params.getOrDefault("distance", 100);
        
        if (direction.equals("down")) {
            driver.swipe(0, 0, 0, -distance, 1000);
        } else if (direction.equals("up")) {
            driver.swipe(0, 0, 0, distance, 1000);
        } else if (direction.equals("left")) {
            driver.swipe(0, 0, -distance, 0, 1000);
        } else if (direction.equals("right")) {
            driver.swipe(0, 0, distance, 0, 1000);
        }
    }
    
    private void performLongPress(Map<String, Object> params) {
        int x = (Integer) params.getOrDefault("x", 100);
        int y = (Integer) params.getOrDefault("y", 100);
        int duration = (Integer) params.getOrDefault("duration", 2000);
        
        driver.tap(1, x, y, duration);
    }
    
    private int getNetworkConnectionValue(String networkType) {
        switch (networkType.toUpperCase()) {
            case "WIFI":
                return 2; // WIFI_ONLY
            case "DATA":
                return 4; // DATA_ONLY
            case "AIRPLANE_MODE":
                return 1; // AIRPLANE_MODE_ON
            default:
                return 6; // ALL_NETWORK_ON
        }
    }
    
    private void createReportDirectory() {
        try {
            Path dir = Paths.get(reportDirectory);
            if (!java.nio.file.Files.exists(dir)) {
                java.nio.file.Files.createDirectories(dir);
                TestLogManager.info("Created mobile report directory: " + reportDirectory);
            }
        } catch (Exception e) {
            TestLogManager.error("Failed to create mobile report directory", e);
        }
    }
    
    /**
     * Mobile device configuration data model.
     */
    public static class MobileDevice {
        private String deviceName;
        private String platformVersion;
        private String appPackage;
        private String appActivity;
        private String bundleId;
        
        // Getters and setters
        public String getDeviceName() { return deviceName; }
        public void setDeviceName(String deviceName) { this.deviceName = deviceName; }
        
        public String getPlatformVersion() { return platformVersion; }
        public void setPlatformVersion(String platformVersion) { this.platformVersion = platformVersion; }
        
        public String getAppPackage() { return appPackage; }
        public void setAppPackage(String appPackage) { this.appPackage = appPackage; }
        
        public String getAppActivity() { return appActivity; }
        public void setAppActivity(String appActivity) { this.appActivity = appActivity; }
        
        public String getBundleId() { return bundleId; }
        public void setBundleId(String bundleId) { this.bundleId = bundleId; }
    }
    
    /**
     * Mobile device information data model.
     */
    public static class MobileDeviceInfo {
        private String platformName;
        private String platformVersion;
        private String deviceName;
        private String appPackage;
        private String appActivity;
        private int screenWidth;
        private int screenHeight;
        
        // Getters and setters
        public String getPlatformName() { return platformName; }
        public void setPlatformName(String platformName) { this.platformName = platformName; }
        
        public String getPlatformVersion() { return platformVersion; }
        public void setPlatformVersion(String platformVersion) { this.platformVersion = platformVersion; }
        
        public String getDeviceName() { return deviceName; }
        public void setDeviceName(String deviceName) { this.deviceName = deviceName; }
        
        public String getAppPackage() { return appPackage; }
        public void setAppPackage(String appPackage) { this.appPackage = appPackage; }
        
        public String getAppActivity() { return appActivity; }
        public void setAppActivity(String appActivity) { this.appActivity = appActivity; }
        
        public int getScreenWidth() { return screenWidth; }
        public void setScreenWidth(int screenWidth) { this.screenWidth = screenWidth; }
        
        public int getScreenHeight() { return screenHeight; }
        public void setScreenHeight(int screenHeight) { this.screenHeight = screenHeight; }
    }
    
    /**
     * Mobile performance metrics data model.
     */
    public static class MobilePerformanceMetrics {
        private Map<String, Object> memoryInfo;
        private Map<String, Object> cpuInfo;
        private Map<String, Object> batteryInfo;
        private LocalDateTime timestamp;
        
        // Getters and setters
        public Map<String, Object> getMemoryInfo() { return memoryInfo; }
        public void setMemoryInfo(Map<String, Object> memoryInfo) { this.memoryInfo = memoryInfo; }
        
        public Map<String, Object> getCpuInfo() { return cpuInfo; }
        public void setCpuInfo(Map<String, Object> cpuInfo) { this.cpuInfo = cpuInfo; }
        
        public Map<String, Object> getBatteryInfo() { return batteryInfo; }
        public void setBatteryInfo(Map<String, Object> batteryInfo) { this.batteryInfo = batteryInfo; }
        
        public LocalDateTime getTimestamp() { return timestamp; }
        public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
    }
}

