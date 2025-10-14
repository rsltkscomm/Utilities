package config;

import base.PropertyManager;
import reporting.TestLogManager;

/**
 * Configuration Initializer
 * Provides a single entry point to initialize the entire configuration system
 * Maintains backward compatibility with existing framework initialization
 */
public class ConfigurationInitializer {
    
    private static boolean isInitialized = false;
    private static ConfigurationManager configManager;
    private static ConfigurationValidator validator;
    
    /**
     * Initialize the complete configuration system
     * This method should be called early in the framework lifecycle
     */
    public static synchronized void initialize() {
        if (isInitialized) {
            TestLogManager.info("Configuration system already initialized");
            return;
        }
        
        try {
            TestLogManager.info("Initializing configuration system...");
            
            // Step 1: Initialize ConfigurationManager
            configManager = ConfigurationManager.getInstance();
            TestLogManager.info("✓ ConfigurationManager initialized");
            
            // Step 2: Initialize ConfigurationValidator
            validator = new ConfigurationValidator();
            TestLogManager.info("✓ ConfigurationValidator initialized");
            
            // Step 3: Validate configuration
            ConfigurationValidator.ValidationResult validationResult = validator.validateAll();
            if (!validationResult.isValid()) {
                TestLogManager.warning("Configuration validation found " + validationResult.getErrorCount() + " issues");
                // Don't fail initialization for warnings, but log them
            } else {
                TestLogManager.success("✓ Configuration validation passed");
            }
            
            // Step 4: Initialize PropertyManager with backward compatibility
            initializePropertyManager();
            TestLogManager.info("✓ PropertyManager initialized with backward compatibility");
            
            // Step 5: Initialize ConfigurationFactory
            ConfigurationFactory.initialize();
            TestLogManager.info("✓ ConfigurationFactory initialized");
            
            // Step 6: Print configuration summary
            printConfigurationSummary();
            
            isInitialized = true;
            TestLogManager.success("Configuration system initialization completed successfully");
            
        } catch (Exception e) {
            TestLogManager.error("Failed to initialize configuration system", e);
            throw new RuntimeException("Configuration initialization failed", e);
        }
    }
    
    /**
     * Initialize PropertyManager with backward compatibility
     */
    private static void initializePropertyManager() {
        try {
            // Get properties path from configuration
            String propertiesPath = configManager.getString("properties.path", "src/main/resources");
            
            // Initialize PropertyManager (this will integrate with ConfigurationManager)
            PropertyManager.init(propertiesPath);
            
        } catch (Exception e) {
            TestLogManager.warning("Failed to initialize PropertyManager with custom path, using defaults", e);
            // Fallback to default initialization
            PropertyManager.init(null);
        }
    }
    
    /**
     * Print configuration summary
     */
    private static void printConfigurationSummary() {
        TestLogManager.info("=== Configuration Summary ===");
        TestLogManager.info("Environment: " + configManager.getEnvironment());
        TestLogManager.info("Browser: " + configManager.getBrowser());
        TestLogManager.info("Headless: " + configManager.isHeadless());
        TestLogManager.info("Timeout: " + configManager.getTimeout() + "s");
        TestLogManager.info("Thread Count: " + configManager.getThreadCount());
        TestLogManager.info("Retry Enabled: " + configManager.isRetryEnabled());
        TestLogManager.info("Max Retries: " + configManager.getMaxRetries());
        TestLogManager.info("Screenshot on Failure: " + configManager.isScreenshotOnFailure());
        TestLogManager.info("Reporting Enabled: " + configManager.isReportingEnabled());
        TestLogManager.info("Grid Enabled: " + configManager.isGridEnabled());
        TestLogManager.info("Cloud Enabled: " + configManager.isCloudEnabled());
        TestLogManager.info("Performance Monitoring: " + configManager.isPerformanceMonitoring());
        TestLogManager.info("=============================");
    }
    
    /**
     * Get ConfigurationManager instance
     */
    public static ConfigurationManager getConfigurationManager() {
        ensureInitialized();
        return configManager;
    }
    
    /**
     * Get ConfigurationValidator instance
     */
    public static ConfigurationValidator getValidator() {
        ensureInitialized();
        return validator;
    }
    
    /**
     * Validate current configuration
     */
    public static ConfigurationValidator.ValidationResult validateConfiguration() {
        ensureInitialized();
        return validator.validateAll();
    }
    
    /**
     * Reload configuration
     */
    public static void reloadConfiguration() {
        ensureInitialized();
        
        TestLogManager.info("Reloading configuration...");
        
        try {
            // Reload ConfigurationManager
            configManager.reloadConfiguration();
            
            // Clear ConfigurationFactory cache
            ConfigurationFactory.clearCache();
            
            // Re-validate configuration
            ConfigurationValidator.ValidationResult validationResult = validator.validateAll();
            if (!validationResult.isValid()) {
                TestLogManager.warning("Configuration validation found " + validationResult.getErrorCount() + " issues after reload");
            }
            
            TestLogManager.success("Configuration reloaded successfully");
            
        } catch (Exception e) {
            TestLogManager.error("Failed to reload configuration", e);
            throw new RuntimeException("Configuration reload failed", e);
        }
    }
    
    /**
     * Check if configuration system is initialized
     */
    public static boolean isInitialized() {
        return isInitialized;
    }
    
    /**
     * Ensure configuration is initialized
     */
    private static void ensureInitialized() {
        if (!isInitialized) {
            TestLogManager.warning("Configuration not initialized, initializing now...");
            initialize();
        }
    }
    
    /**
     * Get configuration status
     */
    public static void printStatus() {
        ensureInitialized();
        
        TestLogManager.info("=== Configuration Status ===");
        TestLogManager.info("Initialized: " + isInitialized);
        TestLogManager.info("ConfigurationManager: " + (configManager != null ? "Available" : "Not Available"));
        TestLogManager.info("Validator: " + (validator != null ? "Available" : "Not Available"));
        
        // Print current configuration
        configManager.printConfiguration();
        
        TestLogManager.info("============================");
    }
    
    /**
     * Reset configuration system (for testing)
     */
    public static synchronized void reset() {
        TestLogManager.info("Resetting configuration system...");
        
        isInitialized = false;
        configManager = null;
        validator = null;
        
        // Clear ConfigurationFactory cache
        ConfigurationFactory.clearCache();
        
        TestLogManager.info("Configuration system reset completed");
    }
}
