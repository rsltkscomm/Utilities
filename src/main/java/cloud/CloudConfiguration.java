package cloud;

import config.ConfigurationManager;
import reporting.TestLogManager;

import java.util.HashMap;
import java.util.Map;

/**
 * Cloud Configuration Management System
 * Provides centralized configuration for cloud testing infrastructure
 */
public class CloudConfiguration {
    
    private final ConfigurationManager configManager;
    private final Map<String, CloudProvider> providers;
    private final Map<String, CloudCapability> capabilities;
    private String activeProvider;
    private boolean cloudEnabled;
    
    public CloudConfiguration() {
        this.configManager = ConfigurationManager.getInstance();
        this.providers = new HashMap<>();
        this.capabilities = new HashMap<>();
        this.cloudEnabled = configManager.getBoolean("cloud.enabled", false);
        this.activeProvider = configManager.getString("cloud.provider", "browserstack");
        
        initializeProviders();
        initializeCapabilities();
    }
    
    /**
     * Initialize supported cloud providers
     */
    private void initializeProviders() {
        // BrowserStack Configuration
        CloudProvider browserstack = new CloudProvider.Builder()
            .name("browserstack")
            .displayName("BrowserStack")
            .hubUrl("https://hub-cloud.browserstack.com/wd/hub")
            .apiUrl("https://api.browserstack.com")
            .build();
        providers.put("browserstack", browserstack);
        
        // SauceLabs Configuration
        CloudProvider saucelabs = new CloudProvider.Builder()
            .name("saucelabs")
            .displayName("SauceLabs")
            .hubUrl("https://ondemand.us-west-1.saucelabs.com:443/wd/hub")
            .apiUrl("https://api.us-west-1.saucelabs.com")
            .build();
        providers.put("saucelabs", saucelabs);
        
        // LambdaTest Configuration
        CloudProvider lambdatest = new CloudProvider.Builder()
            .name("lambdatest")
            .displayName("LambdaTest")
            .hubUrl("https://hub.lambdatest.com/wd/hub")
            .apiUrl("https://api.lambdatest.com")
            .build();
        providers.put("lambdatest", lambdatest);
        
        // CrossBrowserTesting Configuration
        CloudProvider crossbrowsertesting = new CloudProvider.Builder()
            .name("crossbrowsertesting")
            .displayName("CrossBrowserTesting")
            .hubUrl("https://hub.crossbrowsertesting.com:80/wd/hub")
            .apiUrl("https://crossbrowsertesting.com/api/v3")
            .build();
        providers.put("crossbrowsertesting", crossbrowsertesting);
        
        TestLogManager.info("Initialized " + providers.size() + " cloud providers");
    }
    
    /**
     * Initialize cloud capabilities
     */
    private void initializeCapabilities() {
        // Browser capabilities
        capabilities.put("chrome", new CloudCapability.Builder()
            .browserName("chrome")
            .platforms("Windows 10", "Windows 11", "macOS Big Sur", "macOS Monterey", "Ubuntu 20.04")
            .versions("latest", "latest-1", "latest-2")
            .mobile(false)
            .build());
            
        capabilities.put("firefox", new CloudCapability.Builder()
            .browserName("firefox")
            .platforms("Windows 10", "Windows 11", "macOS Big Sur", "macOS Monterey", "Ubuntu 20.04")
            .versions("latest", "latest-1", "latest-2")
            .mobile(false)
            .build());
            
        capabilities.put("edge", new CloudCapability.Builder()
            .browserName("edge")
            .platforms("Windows 10", "Windows 11")
            .versions("latest", "latest-1")
            .mobile(false)
            .build());
            
        capabilities.put("safari", new CloudCapability.Builder()
            .browserName("safari")
            .platforms("macOS Big Sur", "macOS Monterey", "macOS Ventura")
            .versions("14", "15", "16")
            .mobile(false)
            .build());
            
        // Mobile capabilities
        capabilities.put("android", new CloudCapability.Builder()
            .browserName("chrome")
            .platforms("Android")
            .versions("11", "12", "13", "14")
            .mobile(true)
            .build());
            
        capabilities.put("ios", new CloudCapability.Builder()
            .browserName("safari")
            .platforms("iOS")
            .versions("14", "15", "16", "17")
            .mobile(true)
            .build());
        
        TestLogManager.info("Initialized " + capabilities.size() + " cloud capabilities");
    }
    
    // ===========================================
    // CONFIGURATION GETTERS
    // ===========================================
    
    /**
     * Check if cloud testing is enabled
     */
    public boolean isCloudEnabled() {
        return cloudEnabled;
    }
    
    /**
     * Get active cloud provider
     */
    public String getActiveProvider() {
        return activeProvider;
    }
    
    /**
     * Get cloud provider configuration
     */
    public CloudProvider getProvider(String providerName) {
        return providers.get(providerName.toLowerCase());
    }
    
    /**
     * Get active cloud provider configuration
     */
    public CloudProvider getActiveProviderConfig() {
        return getProvider(activeProvider);
    }
    
    /**
     * Get cloud hub URL
     */
    public String getHubUrl() {
        CloudProvider provider = getActiveProviderConfig();
        return provider != null ? provider.getHubUrl() : "";
    }
    
    /**
     * Get cloud API URL
     */
    public String getApiUrl() {
        CloudProvider provider = getActiveProviderConfig();
        return provider != null ? provider.getApiUrl() : "";
    }
    
    /**
     * Get cloud username
     */
    public String getUsername() {
        return configManager.getString("cloud.username", "");
    }
    
    /**
     * Get cloud access key
     */
    public String getAccessKey() {
        return configManager.getString("cloud.accessKey", "");
    }
    
    /**
     * Get cloud project name
     */
    public String getProjectName() {
        return configManager.getString("cloud.projectName", configManager.getString("project.name", "DefaultProject"));
    }
    
    /**
     * Get cloud build name
     */
    public String getBuildName() {
        return configManager.getString("cloud.buildName", "Build-" + System.currentTimeMillis());
    }
    
    /**
     * Get string property
     */
    public String getString(String key, String defaultValue) {
        return configManager.getString(key, defaultValue);
    }
    
    /**
     * Set property (internal use only) - currently not implemented
     * This is a placeholder for future enhancement when ConfigurationManager supports runtime property setting
     */
    @SuppressWarnings("unused")
    private void setProperty(String key, String value) {
        // Note: ConfigurationManager doesn't support setting properties at runtime
        // This is a placeholder for future enhancement
        TestLogManager.warning("Property setting not supported in current ConfigurationManager: " + key + "=" + value);
    }
    
    /**
     * Get cloud session name
     */
    public String getSessionName() {
        return configManager.getString("cloud.sessionName", "Test-Session-" + System.currentTimeMillis());
    }
    
    /**
     * Get cloud timeout
     */
    public int getCloudTimeout() {
        return configManager.getInt("cloud.timeout", 300); // 5 minutes default
    }
    
    /**
     * Get parallel session count
     */
    public int getParallelSessions() {
        return configManager.getInt("cloud.parallelSessions", configManager.getInt("threadCount", 1));
    }
    
    /**
     * Get cloud region
     */
    public String getRegion() {
        return configManager.getString("cloud.region", "us-west-1");
    }
    
    /**
     * Check if video recording is enabled
     */
    public boolean isVideoEnabled() {
        return configManager.getBoolean("cloud.videoEnabled", configManager.isVideoRecording());
    }
    
    /**
     * Check if screenshots are enabled
     */
    public boolean isScreenshotEnabled() {
        return configManager.getBoolean("cloud.screenshotEnabled", configManager.isScreenshotOnFailure());
    }
    
    /**
     * Get cloud tunnel identifier
     */
    public String getTunnelIdentifier() {
        return configManager.getString("cloud.tunnelIdentifier", "");
    }
    
    /**
     * Check if local testing is enabled
     */
    public boolean isLocalTestingEnabled() {
        return configManager.getBoolean("cloud.localTesting", false);
    }
    
    /**
     * Get cloud capability configuration
     */
    public CloudCapability getCapability(String capabilityName) {
        return capabilities.get(capabilityName.toLowerCase());
    }
    
    /**
     * Get all supported capabilities
     */
    public Map<String, CloudCapability> getAllCapabilities() {
        return new HashMap<>(capabilities);
    }
    
    /**
     * Get all supported providers
     */
    public Map<String, CloudProvider> getAllProviders() {
        return new HashMap<>(providers);
    }
    
    /**
     * Check if provider is supported
     */
    public boolean isProviderSupported(String providerName) {
        return providers.containsKey(providerName.toLowerCase());
    }
    
    /**
     * Check if capability is supported
     */
    public boolean isCapabilitySupported(String capabilityName) {
        return capabilities.containsKey(capabilityName.toLowerCase());
    }
    
    // ===========================================
    // CONFIGURATION SETTERS
    // ===========================================
    
    /**
     * Set cloud enabled status
     */
    public void setCloudEnabled(boolean enabled) {
        this.cloudEnabled = enabled;
    }
    
    /**
     * Set active cloud provider
     */
    public void setActiveProvider(String providerName) {
        if (isProviderSupported(providerName)) {
            this.activeProvider = providerName.toLowerCase();
            TestLogManager.info("Active cloud provider set to: " + providerName);
        } else {
            TestLogManager.warning("Unsupported cloud provider: " + providerName);
        }
    }
    
    /**
     * Enable cloud testing
     */
    public void enableCloud() {
        setCloudEnabled(true);
        TestLogManager.info("Cloud testing enabled");
    }
    
    /**
     * Disable cloud testing
     */
    public void disableCloud() {
        setCloudEnabled(false);
        TestLogManager.info("Cloud testing disabled");
    }
    
    // ===========================================
    // VALIDATION METHODS
    // ===========================================
    
    /**
     * Validate cloud configuration
     */
    public boolean validateConfiguration() {
        TestLogManager.info("Validating cloud configuration...");
        
        boolean isValid = true;
        
        if (cloudEnabled) {
            // Validate provider
            if (!isProviderSupported(activeProvider)) {
                TestLogManager.error("Unsupported cloud provider: " + activeProvider);
                isValid = false;
            }
            
            // Validate credentials
            if (getUsername().isEmpty()) {
                TestLogManager.error("Cloud username is required when cloud testing is enabled");
                isValid = false;
            }
            
            if (getAccessKey().isEmpty()) {
                TestLogManager.error("Cloud access key is required when cloud testing is enabled");
                isValid = false;
            }
            
            // Validate parallel sessions
            if (getParallelSessions() <= 0 || getParallelSessions() > 10) {
                TestLogManager.error("Parallel sessions must be between 1 and 10");
                isValid = false;
            }
            
            // Validate timeout
            if (getCloudTimeout() <= 0 || getCloudTimeout() > 1800) {
                TestLogManager.error("Cloud timeout must be between 1 and 1800 seconds");
                isValid = false;
            }
        }
        
        if (isValid) {
            TestLogManager.success("Cloud configuration validation passed");
        } else {
            TestLogManager.error("Cloud configuration validation failed");
        }
        
        return isValid;
    }
    
    /**
     * Print cloud configuration
     */
    public void printConfiguration() {
        TestLogManager.info("=== Cloud Configuration ===");
        TestLogManager.info("Cloud Enabled: " + cloudEnabled);
        
        if (cloudEnabled) {
            TestLogManager.info("Active Provider: " + activeProvider);
            TestLogManager.info("Hub URL: " + getHubUrl());
            TestLogManager.info("API URL: " + getApiUrl());
            TestLogManager.info("Username: " + (getUsername().isEmpty() ? "Not Set" : "Set"));
            TestLogManager.info("Access Key: " + (getAccessKey().isEmpty() ? "Not Set" : "Set"));
            TestLogManager.info("Project Name: " + getProjectName());
            TestLogManager.info("Build Name: " + getBuildName());
            TestLogManager.info("Session Name: " + getSessionName());
            TestLogManager.info("Parallel Sessions: " + getParallelSessions());
            TestLogManager.info("Cloud Timeout: " + getCloudTimeout() + "s");
            TestLogManager.info("Region: " + getRegion());
            TestLogManager.info("Video Enabled: " + isVideoEnabled());
            TestLogManager.info("Screenshot Enabled: " + isScreenshotEnabled());
            TestLogManager.info("Local Testing: " + isLocalTestingEnabled());
            
            if (!getTunnelIdentifier().isEmpty()) {
                TestLogManager.info("Tunnel Identifier: " + getTunnelIdentifier());
            }
        }
        
        TestLogManager.info("Supported Providers: " + providers.keySet());
        TestLogManager.info("Supported Capabilities: " + capabilities.keySet());
        TestLogManager.info("========================");
    }
    
    // ===========================================
    // INNER CLASSES
    // ===========================================
    
    /**
     * Cloud Provider Configuration
     */
    public static class CloudProvider {
        private final String name;
        private final String displayName;
        private final String hubUrl;
        private final String apiUrl;
        
        private CloudProvider(Builder builder) {
            this.name = builder.name;
            this.displayName = builder.displayName;
            this.hubUrl = builder.hubUrl;
            this.apiUrl = builder.apiUrl;
        }
        
        public String getName() { return name; }
        public String getDisplayName() { return displayName; }
        public String getHubUrl() { return hubUrl; }
        public String getApiUrl() { return apiUrl; }
        
        public static class Builder {
            private String name;
            private String displayName;
            private String hubUrl;
            private String apiUrl;
            
            public Builder name(String name) {
                this.name = name;
                return this;
            }
            
            public Builder displayName(String displayName) {
                this.displayName = displayName;
                return this;
            }
            
            public Builder hubUrl(String hubUrl) {
                this.hubUrl = hubUrl;
                return this;
            }
            
            public Builder apiUrl(String apiUrl) {
                this.apiUrl = apiUrl;
                return this;
            }
            
            public CloudProvider build() {
                return new CloudProvider(this);
            }
        }
    }
    
    /**
     * Cloud Capability Configuration
     */
    public static class CloudCapability {
        private final String browserName;
        private final String[] platforms;
        private final String[] versions;
        private final boolean mobile;
        
        private CloudCapability(Builder builder) {
            this.browserName = builder.browserName;
            this.platforms = builder.platforms;
            this.versions = builder.versions;
            this.mobile = builder.mobile;
        }
        
        public String getBrowserName() { return browserName; }
        public String[] getPlatforms() { return platforms; }
        public String[] getVersions() { return versions; }
        public boolean isMobile() { return mobile; }
        
        public static class Builder {
            private String browserName;
            private String[] platforms;
            private String[] versions;
            private boolean mobile = false;
            
            public Builder browserName(String browserName) {
                this.browserName = browserName;
                return this;
            }
            
            public Builder platforms(String... platforms) {
                this.platforms = platforms;
                return this;
            }
            
            public Builder versions(String... versions) {
                this.versions = versions;
                return this;
            }
            
            public Builder mobile(boolean mobile) {
                this.mobile = mobile;
                return this;
            }
            
            public CloudCapability build() {
                return new CloudCapability(this);
            }
        }
    }
}
