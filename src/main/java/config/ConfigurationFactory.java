package config;

import reporting.TestLogManager;

import java.util.HashMap;
import java.util.Map;

/**
 * Factory class for providing backward-compatible configuration access
 * This ensures existing code continues to work while new code can use ConfigurationManager
 */
public class ConfigurationFactory {
    
    private static final Map<String, Object> legacyConfigCache = new HashMap<>();
    private static ConfigurationManager configManager;
    
    /**
     * Initialize the factory
     */
    public static void initialize() {
        if (configManager == null) {
            configManager = ConfigurationManager.getInstance();
            TestLogManager.info("ConfigurationFactory initialized");
        }
    }
    
    /**
     * Get ConfigurationManager instance
     */
    public static ConfigurationManager getConfigurationManager() {
        initialize();
        return configManager;
    }
    
    /**
     * Legacy method for getting configuration values
     * Maintains backward compatibility with existing test scripts
     */
    public static String getConfigValue(String key, String defaultValue) {
        initialize();
        
        // Check legacy cache first
        if (legacyConfigCache.containsKey(key)) {
            return (String) legacyConfigCache.get(key);
        }
        
        // Try ConfigurationManager
        String value = configManager.getString(key, defaultValue);
        
        // Cache the result
        legacyConfigCache.put(key, value);
        
        return value;
    }
    
    /**
     * Legacy method for getting configuration values without default
     */
    public static String getConfigValue(String key) {
        return getConfigValue(key, null);
    }
    
    /**
     * Get system property with fallback to configuration
     */
    public static String getSystemOrConfigProperty(String key, String defaultValue) {
        initialize();
        
        // Check system properties first
        String systemValue = System.getProperty(key);
        if (systemValue != null && !systemValue.trim().isEmpty()) {
            return systemValue;
        }
        
        // Fallback to configuration
        return getConfigValue(key, defaultValue);
    }
    
    /**
     * Get browser configuration (backward compatible)
     */
    public static String getBrowser() {
        return getSystemOrConfigProperty("Browser", getConfigValue("browser", "chrome"));
    }
    
    /**
     * Get environment configuration (backward compatible)
     */
    public static String getEnvironment() {
        return getSystemOrConfigProperty("Environment", getConfigValue("environment", "test"));
    }
    
    /**
     * Get project name (backward compatible)
     */
    public static String getProjectName() {
        return getSystemOrConfigProperty("Project", getConfigValue("project.name", "DefaultProject"));
    }
    
    /**
     * Get user name (backward compatible)
     */
    public static String getUserName() {
        return getSystemOrConfigProperty("UserName", getConfigValue("user.name", System.getProperty("user.name", "default")));
    }
    
    /**
     * Get release version (backward compatible)
     */
    public static String getReleaseVersion() {
        return getSystemOrConfigProperty("ReleaseVersion", getConfigValue("release.version", "1.0.0"));
    }
    
    /**
     * Get suite name (backward compatible)
     */
    public static String getSuiteName() {
        return getSystemOrConfigProperty("SuiteName", getConfigValue("suite.name", "DefaultSuite"));
    }
    
    /**
     * Get account (backward compatible)
     */
    public static String getAccount() {
        return getSystemOrConfigProperty("Account", getConfigValue("account", "default"));
    }
    
    /**
     * Get report type (backward compatible)
     */
    public static String getReportType() {
        return getSystemOrConfigProperty("reportType", getConfigValue("reporting.format", "html"));
    }
    
    /**
     * Get Klov property file (backward compatible)
     */
    public static String getKlovPropertyFile() {
        return getSystemOrConfigProperty("klovpropertyFile", getConfigValue("klov.propertyFile", ""));
    }
    
    /**
     * Get date wise report flag (backward compatible)
     */
    public static String getDateWiseReport() {
        return getSystemOrConfigProperty("DateWiseReport", getConfigValue("reporting.dateWise", "yes"));
    }
    
    /**
     * Get release wise report flag (backward compatible)
     */
    public static String getReleaseWiseReport() {
        return getSystemOrConfigProperty("ReleasewiseReport", getConfigValue("reporting.releaseWise", "yes"));
    }
    
    /**
     * Get account wise report flag (backward compatible)
     */
    public static String getAccountWiseReport() {
        return getSystemOrConfigProperty("AccountWiseReport", getConfigValue("reporting.accountWise", "yes"));
    }
    
    /**
     * Get report bug flag (backward compatible)
     */
    public static String getReportBug() {
        return getSystemOrConfigProperty("ReportBug", getConfigValue("bug.reporting", "No"));
    }
    
    /**
     * Get automation data sheet name (backward compatible)
     */
    public static String getAutomationDataSheetName() {
        return getSystemOrConfigProperty("AutomationDataSheetName", getConfigValue("automation.dataSheetName", "AutomationData"));
    }
    
    /**
     * Get automation data path (backward compatible)
     */
    public static String getAutomationDataPath() {
        return getSystemOrConfigProperty("AutomationDataPath", getConfigValue("automation.dataPath", "src/test/resources/"));
    }
    
    /**
     * Get regression account setup flag (backward compatible)
     */
    public static String getRegressionAccountSetup() {
        return getSystemOrConfigProperty("RegressionAccountSetup", getConfigValue("regression.accountSetup", ""));
    }
    
    /**
     * Get regression audience flag (backward compatible)
     */
    public static String getRegressionAudience() {
        return getSystemOrConfigProperty("RegressionAudience", getConfigValue("regression.audience", ""));
    }
    
    /**
     * Get regression communication flag (backward compatible)
     */
    public static String getRegressionCommunication() {
        return getSystemOrConfigProperty("RegressionCommunication", getConfigValue("regression.communication", ""));
    }
    
    /**
     * Get regression preferences flag (backward compatible)
     */
    public static String getRegressionPreferences() {
        return getSystemOrConfigProperty("RegressionPreferences", getConfigValue("regression.preferences", ""));
    }
    
    /**
     * Get regression analytics flag (backward compatible)
     */
    public static String getRegressionAnalytics() {
        return getSystemOrConfigProperty("RegressionAnalytics", getConfigValue("regression.analytics", ""));
    }
    
    /**
     * Get deployment checklist flag (backward compatible)
     */
    public static String getDeploymentChecklist() {
        return getSystemOrConfigProperty("Deploymentchecklist", getConfigValue("deployment.checklist", ""));
    }
    
    /**
     * Get page load testing flag (backward compatible)
     */
    public static String getPageLoadTesting() {
        return getSystemOrConfigProperty("PageLoadTesting", getConfigValue("performance.pageLoadTesting", ""));
    }
    
    /**
     * Get new account creation checklist flag (backward compatible)
     */
    public static String getNewAccountCreationChecklist() {
        return getSystemOrConfigProperty("NewAccountCreationChecklist", getConfigValue("account.newCreationChecklist", ""));
    }
    
    /**
     * Get feature wise checklist flag (backward compatible)
     */
    public static String getFeaturewiseChecklist() {
        return getSystemOrConfigProperty("FeaturewiseChecklist", getConfigValue("feature.checklist", ""));
    }
    
    /**
     * Get root path for Windows (backward compatible)
     */
    public static String getRootWindows() {
        return getSystemOrConfigProperty("Root_Windows", getConfigValue("paths.root.windows", "C:\\"));
    }
    
    /**
     * Get root path for Linux (backward compatible)
     */
    public static String getRootLinux() {
        return getSystemOrConfigProperty("Root_Linux", getConfigValue("paths.root.linux", "/opt/"));
    }
    
    /**
     * Clear legacy configuration cache
     */
    public static void clearCache() {
        legacyConfigCache.clear();
        TestLogManager.info("Configuration cache cleared");
    }
    
    /**
     * Reload configuration
     */
    public static void reload() {
        initialize();
        configManager.reloadConfiguration();
        clearCache();
        TestLogManager.info("Configuration reloaded");
    }
    
    /**
     * Print all configuration values (for debugging)
     */
    public static void printAllConfiguration() {
        initialize();
        configManager.printConfiguration();
        
        TestLogManager.info("Legacy Configuration Values:");
        TestLogManager.info("Browser: " + getBrowser());
        TestLogManager.info("Environment: " + getEnvironment());
        TestLogManager.info("Project: " + getProjectName());
        TestLogManager.info("User: " + getUserName());
        TestLogManager.info("Release Version: " + getReleaseVersion());
        TestLogManager.info("Suite Name: " + getSuiteName());
        TestLogManager.info("Account: " + getAccount());
        TestLogManager.info("Report Type: " + getReportType());
    }
    
    /**
     * Validate all configuration
     */
    public static void validateConfiguration() {
        initialize();
        configManager.validateConfiguration();
        
        // Validate legacy configuration
        TestLogManager.info("Validating legacy configuration...");
        
        if (getBrowser().isEmpty()) {
            TestLogManager.warning("Browser configuration is empty, using default: chrome");
        }
        
        if (getEnvironment().isEmpty()) {
            TestLogManager.warning("Environment configuration is empty, using default: test");
        }
        
        if (getProjectName().isEmpty()) {
            TestLogManager.warning("Project name configuration is empty, using default");
        }
        
        TestLogManager.success("Legacy configuration validation completed");
    }
}
