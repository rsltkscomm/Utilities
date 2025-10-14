package config;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

import reporting.TestLogManager;

/**
 * Centralized Configuration Management System
 * Provides backward compatibility with existing framework while adding modern configuration features
 */
public class ConfigurationManager {
    
    private static final String DEFAULT_CONFIG_FILE = "src/main/resources/config.properties";
    private static final String ENV_CONFIG_FILE = "src/main/resources/config-%s.properties";
    
    private static volatile ConfigurationManager instance;
    private final Map<String, Object> configCache = new ConcurrentHashMap<>();
    private final Properties properties = new Properties();
    private final Map<String, String> systemProperties = new HashMap<>();
    private String environment;
    private boolean isInitialized = false;
    
    // Private constructor for singleton
    private ConfigurationManager() {
        initializeConfiguration();
    }
    
    /**
     * Get singleton instance
     */
    public static ConfigurationManager getInstance() {
        if (instance == null) {
            synchronized (ConfigurationManager.class) {
                if (instance == null) {
                    instance = new ConfigurationManager();
                }
            }
        }
        return instance;
    }
    
    /**
     * Initialize configuration system
     */
    private void initializeConfiguration() {
        try {
            // Load system properties first (highest priority)
            loadSystemProperties();
            
            // Determine environment
            environment = getSystemProperty("Environment", "test");
            
            // Load default configuration
            loadPropertiesFile(DEFAULT_CONFIG_FILE);
            
            // Load environment-specific configuration if exists
            String envConfigFile = String.format(ENV_CONFIG_FILE, environment.toLowerCase());
            if (Files.exists(Paths.get(envConfigFile))) {
                loadPropertiesFile(envConfigFile);
                TestLogManager.info("Loaded environment-specific config: " + envConfigFile);
            }
            
            // Load user-specific configuration if exists
            loadUserSpecificConfig();
            
            isInitialized = true;
            TestLogManager.info("Configuration Manager initialized successfully for environment: " + environment);
            
        } catch (Exception e) {
            TestLogManager.error("Failed to initialize Configuration Manager", e);
            throw new RuntimeException("Configuration initialization failed", e);
        }
    }
    
    /**
     * Load system properties
     */
    private void loadSystemProperties() {
        Properties sysProps = System.getProperties();
        for (String key : sysProps.stringPropertyNames()) {
            systemProperties.put(key, sysProps.getProperty(key));
        }
        TestLogManager.info("Loaded " + systemProperties.size() + " system properties");
    }
    
    /**
     * Load properties from file
     */
    private void loadPropertiesFile(String filePath) {
        Path path = Paths.get(filePath);
        if (!Files.exists(path)) {
            TestLogManager.warning("Configuration file not found: " + filePath);
            return;
        }
        
        try (InputStream input = new FileInputStream(path.toFile())) {
            Properties fileProps = new Properties();
            fileProps.load(input);
            
            // Merge with existing properties (file properties override existing ones)
            for (String key : fileProps.stringPropertyNames()) {
                properties.setProperty(key, fileProps.getProperty(key));
            }
            
            TestLogManager.info("Loaded " + fileProps.size() + " properties from " + filePath);
        } catch (IOException e) {
            TestLogManager.error("Failed to load properties from " + filePath, e);
        }
    }
    
    /**
     * Load user-specific configuration
     */
    private void loadUserSpecificConfig() {
        String userName = getSystemProperty("UserName", System.getProperty("user.name", "default"));
        String userConfigFile = String.format("src/main/resources/config-%s.properties", userName.toLowerCase());
        
        Path path = Paths.get(userConfigFile);
        if (Files.exists(path)) {
            loadPropertiesFile(userConfigFile);
            TestLogManager.info("Loaded user-specific config: " + userConfigFile);
        }
    }
    
    // ===========================================
    // CONFIGURATION GETTERS WITH BACKWARD COMPATIBILITY
    // ===========================================
    
    /**
     * Get string configuration value with fallback
     * Priority: System Property > Environment-specific > Default
     */
    public String getString(String key, String defaultValue) {
        if (!isInitialized) {
            initializeConfiguration();
        }
        
        // Check cache first
        if (configCache.containsKey(key)) {
            return (String) configCache.get(key);
        }
        
        // Check system properties first (highest priority)
        String value = systemProperties.get(key);
        if (value != null && !value.trim().isEmpty()) {
            configCache.put(key, value);
            return value;
        }
        
        // Check properties file
        value = properties.getProperty(key);
        if (value != null && !value.trim().isEmpty()) {
            configCache.put(key, value);
            return value;
        }
        
        // Return default value
        configCache.put(key, defaultValue);
        return defaultValue;
    }
    
    /**
     * Get string configuration value (no default)
     */
    public String getString(String key) {
        return getString(key, null);
    }
    
    /**
     * Get boolean configuration value
     */
    public boolean getBoolean(String key, boolean defaultValue) {
        String value = getString(key, String.valueOf(defaultValue));
        return Boolean.parseBoolean(value);
    }
    
    /**
     * Get integer configuration value
     */
    public int getInt(String key, int defaultValue) {
        String value = getString(key, String.valueOf(defaultValue));
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            TestLogManager.warning("Invalid integer value for " + key + ": " + value + ", using default: " + defaultValue);
            return defaultValue;
        }
    }
    
    /**
     * Get long configuration value
     */
    public long getLong(String key, long defaultValue) {
        String value = getString(key, String.valueOf(defaultValue));
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            TestLogManager.warning("Invalid long value for " + key + ": " + value + ", using default: " + defaultValue);
            return defaultValue;
        }
    }
    
    /**
     * Get Duration configuration value
     */
    public Duration getDuration(String key, Duration defaultValue) {
        String value = getString(key);
        if (value == null) {
            return defaultValue;
        }
        
        try {
            // Support formats: "30s", "2m", "1h", "5000" (milliseconds)
            value = value.trim().toLowerCase();
            if (value.endsWith("s")) {
                return Duration.ofSeconds(Long.parseLong(value.substring(0, value.length() - 1)));
            } else if (value.endsWith("m")) {
                return Duration.ofMinutes(Long.parseLong(value.substring(0, value.length() - 1)));
            } else if (value.endsWith("h")) {
                return Duration.ofHours(Long.parseLong(value.substring(0, value.length() - 1)));
            } else {
                // Assume milliseconds
                return Duration.ofMillis(Long.parseLong(value));
            }
        } catch (NumberFormatException e) {
            TestLogManager.warning("Invalid duration value for " + key + ": " + value + ", using default: " + defaultValue);
            return defaultValue;
        }
    }
    
    /**
     * Get system property with fallback
     */
    public String getSystemProperty(String key, String defaultValue) {
        return System.getProperty(key, defaultValue);
    }
    
    // ===========================================
    // FRAMEWORK-SPECIFIC CONFIGURATION GETTERS
    // ===========================================
    
    /**
     * Get browser configuration
     */
    public String getBrowser() {
        return getString("browser", "chrome");
    }
    
    /**
     * Get headless mode configuration
     */
    public boolean isHeadless() {
        return getBoolean("headless", false);
    }
    
    /**
     * Get environment
     */
    public String getEnvironment() {
        return getString("environment", environment != null ? environment : "test");
    }
    
    /**
     * Get timeout configuration
     */
    public int getTimeout() {
        return getInt("timeout", 30);
    }
    
    /**
     * Get base URL
     */
    public String getBaseUrl() {
        return getString("baseUrl", "");
    }
    
    /**
     * Get thread count for parallel execution
     */
    public int getThreadCount() {
        return getInt("threadCount", 1);
    }
    
    /**
     * Check if retry is enabled
     */
    public boolean isRetryEnabled() {
        return getBoolean("retryEnabled", true);
    }
    
    /**
     * Get max retry count
     */
    public int getMaxRetries() {
        return getInt("maxRetries", 1);
    }
    
    /**
     * Check if screenshot on failure is enabled
     */
    public boolean isScreenshotOnFailure() {
        return getBoolean("screenshotOnFailure", true);
    }
    
    /**
     * Check if video recording is enabled
     */
    public boolean isVideoRecording() {
        return getBoolean("videoRecording", false);
    }
    
    /**
     * Get data source type
     */
    public String getDataSource() {
        return getString("dataSource", "excel");
    }
    
    /**
     * Get Excel path
     */
    public String getExcelPath() {
        return getString("excelPath", "src/test/resources/testdata.xlsx");
    }
    
    /**
     * Get sheet name
     */
    public String getSheetName() {
        return getString("sheetName", "TestData");
    }
    
    /**
     * Get JSON path
     */
    public String getJsonPath() {
        return getString("jsonPath", "src/test/resources/testdata.json");
    }
    
    /**
     * Check if reporting is enabled
     */
    public boolean isReportingEnabled() {
        return getBoolean("reporting.enabled", true);
    }
    
    /**
     * Get reporting format
     */
    public String getReportingFormat() {
        return getString("reporting.format", "html");
    }
    
    /**
     * Check if grid is enabled
     */
    public boolean isGridEnabled() {
        return getBoolean("grid.enabled", false);
    }
    
    /**
     * Get grid hub URL
     */
    public String getGridHubUrl() {
        return getString("grid.hubUrl", "http://localhost:4444/wd/hub");
    }
    
    /**
     * Check if cloud testing is enabled
     */
    public boolean isCloudEnabled() {
        return getBoolean("cloud.enabled", false);
    }
    
    /**
     * Get cloud provider
     */
    public String getCloudProvider() {
        return getString("cloud.provider", "browserstack");
    }
    
    /**
     * Get cloud username
     */
    public String getCloudUsername() {
        return getString("cloud.username", "");
    }
    
    /**
     * Get cloud access key
     */
    public String getCloudAccessKey() {
        return getString("cloud.accessKey", "");
    }
    
    /**
     * Check if mobile testing is enabled
     */
    public boolean isMobileEnabled() {
        return getBoolean("mobile.enabled", false);
    }
    
    /**
     * Get mobile platform
     */
    public String getMobilePlatform() {
        return getString("mobile.platform", "android");
    }
    
    /**
     * Get mobile device name
     */
    public String getMobileDeviceName() {
        return getString("mobile.deviceName", "");
    }
    
    /**
     * Get mobile app path
     */
    public String getMobileAppPath() {
        return getString("mobile.appPath", "");
    }
    
    /**
     * Check if performance testing is enabled
     */
    public boolean isPerformanceEnabled() {
        return getBoolean("performance.enabled", true);
    }
    
    /**
     * Get performance threshold
     */
    public long getPerformanceThreshold() {
        return getLong("performance.threshold.ms", 5000);
    }
    
    /**
     * Check if performance monitoring is enabled
     */
    public boolean isPerformanceMonitoring() {
        return getBoolean("performance.monitoring", true);
    }
    
    /**
     * Check if security testing is enabled
     */
    public boolean isSecurityEnabled() {
        return getBoolean("security.enabled", true);
    }
    
    /**
     * Check if SSL validation is enabled
     */
    public boolean isSSLValidation() {
        return getBoolean("security.sslValidation", true);
    }
    
    /**
     * Check if vulnerability scan is enabled
     */
    public boolean isVulnerabilityScan() {
        return getBoolean("security.vulnerabilityScan", true);
    }
    
    // ===========================================
    // LOGGING CONFIGURATION GETTERS
    // ===========================================
    
    /**
     * Get logging level
     */
    public String getLoggingLevel() {
        return getString("logging.level", "ALL");
    }
    
    /**
     * Check if success logging is enabled
     */
    public boolean isSuccessLoggingEnabled() {
        return getBoolean("logging.success.enabled", true);
    }
    
    /**
     * Check if info logging is enabled
     */
    public boolean isInfoLoggingEnabled() {
        return getBoolean("logging.info.enabled", true);
    }
    
    /**
     * Check if warning logging is enabled
     */
    public boolean isWarningLoggingEnabled() {
        return getBoolean("logging.warning.enabled", true);
    }
    
    /**
     * Check if error logging is enabled
     */
    public boolean isErrorLoggingEnabled() {
        return getBoolean("logging.error.enabled", true);
    }
    
    /**
     * Check if test execution logging is enabled
     */
    public boolean isTestExecutionLoggingEnabled() {
        return getBoolean("logging.test.execution.enabled", true);
    }
    
    /**
     * Check if performance logging is enabled
     */
    public boolean isPerformanceLoggingEnabled() {
        return getBoolean("logging.performance.enabled", true);
    }
    
    /**
     * Get performance logging threshold
     */
    public long getPerformanceLoggingThreshold() {
        return getLong("logging.performance.threshold.ms", 1000);
    }
    
    /**
     * Check if browser logging is enabled
     */
    public boolean isBrowserLoggingEnabled() {
        return getBoolean("logging.browser.enabled", true);
    }
    
    /**
     * Check if API logging is enabled
     */
    public boolean isApiLoggingEnabled() {
        return getBoolean("logging.api.enabled", true);
    }
    
    // ===========================================
    // UTILITY METHODS
    // ===========================================
    
    /**
     * Reload configuration (useful for testing or dynamic updates)
     */
    public void reloadConfiguration() {
        synchronized (this) {
            configCache.clear();
            properties.clear();
            systemProperties.clear();
            isInitialized = false;
            initializeConfiguration();
            TestLogManager.info("Configuration reloaded successfully");
        }
    }
    
    /**
     * Get all configuration as a map (for debugging)
     */
    public Map<String, String> getAllConfiguration() {
        Map<String, String> allConfig = new HashMap<>();
        
        // Add system properties
        allConfig.putAll(systemProperties);
        
        // Add file properties (will override system properties with same key)
        for (String key : properties.stringPropertyNames()) {
            allConfig.put(key, properties.getProperty(key));
        }
        
        return allConfig;
    }
    
    /**
     * Validate configuration
     */
    public void validateConfiguration() {
        TestLogManager.info("Validating configuration...");
        
        // Validate required configurations
        if (getBrowser().isEmpty()) {
            throw new IllegalStateException("Browser configuration is required");
        }
        
        if (getTimeout() <= 0) {
            throw new IllegalStateException("Timeout must be greater than 0");
        }
        
        if (getThreadCount() <= 0) {
            throw new IllegalStateException("Thread count must be greater than 0");
        }
        
        TestLogManager.success("Configuration validation completed successfully");
    }
    
    /**
     * Print current configuration (for debugging)
     */
    public void printConfiguration() {
        TestLogManager.info("Current Configuration:");
        TestLogManager.info("Environment: " + getEnvironment());
        TestLogManager.info("Browser: " + getBrowser());
        TestLogManager.info("Headless: " + isHeadless());
        TestLogManager.info("Timeout: " + getTimeout());
        TestLogManager.info("Thread Count: " + getThreadCount());
        TestLogManager.info("Base URL: " + getBaseUrl());
        TestLogManager.info("Reporting Enabled: " + isReportingEnabled());
        TestLogManager.info("Grid Enabled: " + isGridEnabled());
        TestLogManager.info("Performance Monitoring: " + isPerformanceMonitoring());
    }
}
