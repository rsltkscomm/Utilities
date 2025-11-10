package performanceTracker;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Thread-safe singleton configuration manager for centralized configuration access.
 * Eliminates duplicate config loading across multiple classes.
 * 
 * Optimizations:
 * - Singleton pattern for single config load
 * - Thread-safe with ReadWriteLock
 * - Lazy initialization
 * - Configuration validation
 * - Default values for all properties
 */
public class ConfigurationManager {
    
    private static volatile ConfigurationManager instance;
    private static final ReadWriteLock lock = new ReentrantReadWriteLock();
    
    private Properties properties;
    private boolean isLoaded = false;
    
    // Configuration constants
    private static final String CONFIG_FILE = "config.properties";
    
    // Private constructor for singleton
    private ConfigurationManager() {
    }
    
    /**
     * Get singleton instance (thread-safe double-check locking)
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
     * Reset singleton instance (for testing purposes)
     */
    public static void resetInstance() {
        synchronized (ConfigurationManager.class) {
            instance = null;
        }
    }
    
    /**
     * Get property value with thread-safe read
     */
    private String getProperty(String key, String defaultValue) {
        lock.readLock().lock();
        try {
            return properties != null ? properties.getProperty(key, defaultValue) : defaultValue;
        } finally {
            lock.readLock().unlock();
        }
    }
    
    // ============================================================================
    // Test Execution Settings
    // ============================================================================
    
    public boolean isReportDefectEnabled() {
        return "Yes".equalsIgnoreCase(System.getProperty("reportdefect", "No"));
    }
    
    public String getScreenshotPath() {
        return System.getProperty("SCREENSHOT_PATH", "./screenshots/");
    }
    
    public String getLogPath() {
        return System.getProperty("LOG_PATH", "./logs/");
    }
    
    // ============================================================================
    // Network Configuration
    // ============================================================================
    
    public int getConnectionTimeout() {
        return getIntProperty("CONNECTION_TIMEOUT_MS", 60000); // 60 seconds
    }
    
    public int getReadTimeout() {
        return getIntProperty("READ_TIMEOUT_MS", 60000); // 60 seconds
    }
    
    public int getMaxRetries() {
        return getIntProperty("MAX_API_RETRIES", 3);
    }
    
    public int getRetryDelayMs() {
        return getIntProperty("RETRY_DELAY_MS", 1000);
    }
    
    // ============================================================================
    // Browser Logs Capture Configuration
    // ============================================================================
    
    public boolean isCaptureBrowserLogsEnabled() {
        return "true".equalsIgnoreCase(System.getProperty("CAPTURE_BROWSER_LOGS", "true"));
    }
    
    public boolean isCaptureBrowserWarnings() {
        return "true".equalsIgnoreCase(System.getProperty("CAPTURE_BROWSER_WARNINGS", "true"));
    }
    
    public boolean isCaptureBrowserInfo() {
        return "true".equalsIgnoreCase(System.getProperty("CAPTURE_BROWSER_INFO", "false"));
    }
    
    public boolean isAttachBrowserLogsToDefect() {
        return "true".equalsIgnoreCase(System.getProperty("ATTACH_BROWSER_LOGS_TO_DEFECT", "true"));
    }
    
    // ============================================================================
    // Network Traffic Capture Configuration
    // ============================================================================
    
    public boolean isNetworkTrafficCaptureEnabled() {
        return "true".equalsIgnoreCase(System.getProperty("CAPTURE_NETWORK_TRAFFIC", "true"));
    }
    
    public boolean isAttachHarFileToDefect() {
        return "true".equalsIgnoreCase(System.getProperty("ATTACH_HAR_FILE_TO_DEFECT", "true"));
    }
    
    // ============================================================================
    // Utility Methods
    // ============================================================================
    
    /**
     * Get integer property with default value
     */
    private int getIntProperty(String key, int defaultValue) {
        try {
            String value = System.getProperty(key, String.valueOf(defaultValue));
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            System.err.println("⚠️  Invalid integer for " + key + ", using default: " + defaultValue);
            return defaultValue;
        }
    }
    
    /**
     * Check if configuration is loaded
     */
    public boolean isConfigurationLoaded() {
        lock.readLock().lock();
        try {
            return isLoaded;
        } finally {
            lock.readLock().unlock();
        }
    }
    
    // ================================================================================
    // SLA CONFIGURATION
    // ================================================================================
    
    public boolean isSLAReportingEnabled() {
        return "true".equalsIgnoreCase(System.getProperty("SLA_REPORTING_ENABLED", "true"));
    }
    
    public String getSLAConfigPath() {
        return System.getProperty("SLA_CONFIG_PATH", "./config/sla.properties");
    }
    
    public boolean isNetworkCaptureEnabled() {
        return "true".equalsIgnoreCase(System.getProperty("CAPTURE_NETWORK_TRAFFIC", "false"));
    }
    
    // ================================================================================
    // PERFORMANCE MONITORING CONFIGURATION
    // ================================================================================
    
    public boolean isPerformanceMonitoringEnabled() {
        return "true".equalsIgnoreCase(System.getProperty("ENABLE_PERFORMANCE_MONITORING", "false"));
    }
    
    public boolean isCapturePageLoadTimes() {
        return "true".equalsIgnoreCase(System.getProperty("CAPTURE_PAGE_LOAD_TIMES", "true"));
    }
    
    public boolean isCaptureApiResponseTimes() {
        return "true".equalsIgnoreCase(System.getProperty("CAPTURE_API_RESPONSE_TIMES", "true"));
    }
    
    public boolean isIncludePerformanceInDefect() {
        return "true".equalsIgnoreCase(System.getProperty("INCLUDE_PERFORMANCE_IN_DEFECT", "true"));
    }
    
    public boolean isGeneratePerformanceSummary() {
        return "true".equalsIgnoreCase(System.getProperty("GENERATE_PERFORMANCE_SUMMARY", "true"));
    }
    
    public int getPerformanceThresholdPageLoadMs() {
        return getIntProperty("PERFORMANCE_THRESHOLD_PAGE_LOAD_MS", 5000);
    }
    
    public int getPerformanceThresholdApiResponseMs() {
        return getIntProperty("PERFORMANCE_THRESHOLD_API_RESPONSE_MS", 1000);
    }

    // ============================================================================
    // Database & Environment (RUN19)
    // ============================================================================

    public String getEnvironmentName() {
        return System.getProperty("ENV_NAME", "LOCAL");
    }

    public String getMasterDbUrl() {
        return System.getProperty("dburl_master", "");
    }

    public String getMasterDbUser() {
        return System.getProperty("masterdb_userName", "");
    }

    public String getMasterDbPassword() {
        return System.getProperty("masterdb_password", "");
    }
    
    public boolean isCaptureWebVitals() {
        return "true".equalsIgnoreCase(System.getProperty("CAPTURE_WEB_VITALS", "true"));
    }
    
    public boolean isGenerateHtmlPerformanceReport() {
        return "true".equalsIgnoreCase(System.getProperty("GENERATE_HTML_PERFORMANCE_REPORT", "true"));
    }

    // ============================================================================
    // Hook Toggles
    // ============================================================================

    public boolean isLoginHooksEnabled() { return "true".equalsIgnoreCase(System.getProperty("LOGIN_HOOKS_ENABLED", "false")); }

    public boolean isEnterpriseSplunkEnabled() { return "true".equalsIgnoreCase(System.getProperty("ENTERPRISE_SPLUNK_ENABLED", "false")); }
    public boolean isEnterpriseDatadogEnabled() { return "true".equalsIgnoreCase(System.getProperty("ENTERPRISE_DATADOG_ENABLED", "false")); }
    public boolean isEnterpriseNewRelicEnabled() { return "true".equalsIgnoreCase(System.getProperty("ENTERPRISE_NEWRELIC_ENABLED", "false")); }
    public boolean isEnterpriseKafkaEnabled() { return "true".equalsIgnoreCase(System.getProperty("ENTERPRISE_KAFKA_ENABLED", "false")); }
    public boolean isEnterpriseJiraReleaseEnabled() { return "true".equalsIgnoreCase(System.getProperty("ENTERPRISE_JIRA_RELEASE_ENABLED", "false")); }

    public boolean isLoadHooksEnabled() { return "true".equalsIgnoreCase(System.getProperty("LOAD_HOOKS_ENABLED", "false")); }
    public boolean isCiArtifactsEnabled() { return "true".equalsIgnoreCase(System.getProperty("CI_ARTIFACTS_ENABLED", "false")); }

    public boolean isWeb3Enabled() { return "true".equalsIgnoreCase(System.getProperty("WEB3_ENABLED", "false")); }
    public boolean isEdgeEnabled() { return "true".equalsIgnoreCase(System.getProperty("EDGE_ENABLED", "false")); }
    public boolean isAdvancedAIEnabled() { return "true".equalsIgnoreCase(System.getProperty("ADVANCED_AI_ENABLED", "false")); }
    public boolean isPerformanceAsCodeEnabled() { return "true".equalsIgnoreCase(System.getProperty("PERFORMANCE_AS_CODE_ENABLED", "false")); }

    /**
     * Safe public accessor for arbitrary config keys.
     */
    public String getConfig(String key, String defaultValue) {
        return getProperty(key, defaultValue);
    }

    // ============================================================================
    // API Transaction Capture
    // ============================================================================

    public boolean isCaptureApiDetailsEnabled() { return "true".equalsIgnoreCase(System.getProperty("CAPTURE_API_DETAILS", "false")); }
    public boolean isApiCaptureSameOriginOnly() { return "true".equalsIgnoreCase(System.getProperty("API_CAPTURE_SAME_ORIGIN_ONLY", "true")); }
    public int getApiBodyMaxKb() { return getIntProperty("API_BODY_MAX_KB", 128); }
    
    public boolean isAutoScreenshotOnPerfIssue() {
        return "true".equalsIgnoreCase(System.getProperty("AUTO_SCREENSHOT_ON_PERF_ISSUE", "true"));
    }
    
    public boolean isAutoCreatePerformanceDefect() {
        return "true".equalsIgnoreCase(System.getProperty("AUTO_CREATE_PERFORMANCE_DEFECT", "false"));
    }
    
    public int getPerformanceThresholdLcpMs() {
        return getIntProperty("PERFORMANCE_THRESHOLD_LCP_MS", 2500);
    }
    
    public double getPerformanceThresholdCls() {
        try {
            return Double.parseDouble(System.getProperty("PERFORMANCE_THRESHOLD_CLS", "0.1"));
        } catch (NumberFormatException e) {
            return 0.1;
        }
    }
    
    public int getPerformanceMinWebVitalsScore() {
        return getIntProperty("PERFORMANCE_MIN_WEB_VITALS_SCORE", 60);
    }
    
    public boolean isLighthouseAuditsEnabled() {
        return "true".equalsIgnoreCase(System.getProperty("ENABLE_LIGHTHOUSE_AUDITS", "false"));
    }
    
    public int getLighthousePerformanceThreshold() {
        return getIntProperty("LIGHTHOUSE_PERFORMANCE_THRESHOLD", 50);
    }
    
    // ================================================================================
    // ADVANCED PERFORMANCE METRICS CONFIGURATION
    // ================================================================================
    
    public boolean isCaptureAdvancedMetrics() {
        return "true".equalsIgnoreCase(System.getProperty("CAPTURE_ADVANCED_METRICS", "true"));
    }
    
    public int getPerformanceThresholdINPMs() {
        return getIntProperty("PERFORMANCE_THRESHOLD_INP_MS", 200);
    }
    
    public int getPerformanceThresholdFIDMs() {
        return getIntProperty("PERFORMANCE_THRESHOLD_FID_MS", 100);
    }
    
    public int getLongTaskThresholdMs() {
        return getIntProperty("LONG_TASK_THRESHOLD_MS", 50);
    }
    
    public boolean isCaptureLongTasks() {
        return "true".equalsIgnoreCase(System.getProperty("CAPTURE_LONG_TASKS", "true"));
    }
    
    public boolean isCaptureResourceTiming() {
        return "true".equalsIgnoreCase(System.getProperty("CAPTURE_RESOURCE_TIMING", "true"));
    }
    
    public boolean isCaptureNavigationTiming() {
        return "true".equalsIgnoreCase(System.getProperty("CAPTURE_NAVIGATION_TIMING", "true"));
    }
    
    public boolean isCapturePaintTiming() {
        return "true".equalsIgnoreCase(System.getProperty("CAPTURE_PAINT_TIMING", "true"));
    }
    
    public boolean isCaptureLayoutShiftEvents() {
        return "true".equalsIgnoreCase(System.getProperty("CAPTURE_LAYOUT_SHIFT_EVENTS", "true"));
    }
    
    public boolean isCaptureEventTiming() {
        return "true".equalsIgnoreCase(System.getProperty("CAPTURE_EVENT_TIMING", "true"));
    }
    
}

