package config;

import reporting.TestLogManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Configuration Validation System
 * Provides comprehensive validation for framework configuration
 */
public class ConfigurationValidator {
    
    private final ConfigurationManager configManager;
    private final List<ValidationRule> validationRules;
    private final Map<String, List<String>> validationErrors;
    
    public ConfigurationValidator() {
        this.configManager = ConfigurationManager.getInstance();
        this.validationRules = new ArrayList<>();
        this.validationErrors = new HashMap<>();
        initializeValidationRules();
    }
    
    /**
     * Initialize validation rules
     */
    private void initializeValidationRules() {
        // Browser validation
        validationRules.add(new ValidationRule("browser", 
            value -> value != null && !value.trim().isEmpty(),
            "Browser configuration is required"));
        
        validationRules.add(new ValidationRule("browser",
            value -> isValidBrowser(value),
            "Invalid browser type. Supported: chrome, firefox, edge, chromeheadless"));
        
        // Timeout validation
        validationRules.add(new ValidationRule("timeout",
            value -> {
                try {
                    int timeout = Integer.parseInt(value);
                    return timeout > 0 && timeout <= 300; // Max 5 minutes
                } catch (NumberFormatException e) {
                    return false;
                }
            },
            "Timeout must be a positive integer between 1 and 300 seconds"));
        
        // Thread count validation
        validationRules.add(new ValidationRule("threadCount",
            value -> {
                try {
                    int threads = Integer.parseInt(value);
                    return threads > 0 && threads <= 10; // Max 10 threads
                } catch (NumberFormatException e) {
                    return false;
                }
            },
            "Thread count must be between 1 and 10"));
        
        // URL validation
        validationRules.add(new ValidationRule("baseUrl",
            value -> value == null || value.trim().isEmpty() || isValidUrl(value),
            "Invalid base URL format"));
        
        // Email validation
        validationRules.add(new ValidationRule("cloud.username",
            value -> value == null || value.trim().isEmpty() || isValidEmail(value),
            "Invalid email format for cloud username"));
        
        // Environment validation
        validationRules.add(new ValidationRule("environment",
            value -> value != null && isValidEnvironment(value),
            "Invalid environment. Supported: dev, test, staging, prod"));
        
        // Logging level validation
        validationRules.add(new ValidationRule("logging.level",
            value -> value == null || isValidLoggingLevel(value),
            "Invalid logging level. Supported: ALL, DEBUG, INFO, WARN, ERROR, SUCCESS"));
        
        // Performance threshold validation
        validationRules.add(new ValidationRule("performance.threshold.ms",
            value -> {
                try {
                    long threshold = Long.parseLong(value);
                    return threshold > 0 && threshold <= 60000; // Max 1 minute
                } catch (NumberFormatException e) {
                    return false;
                }
            },
            "Performance threshold must be between 1 and 60000 milliseconds"));
        
        // Retry count validation
        validationRules.add(new ValidationRule("maxRetries",
            value -> {
                try {
                    int retries = Integer.parseInt(value);
                    return retries >= 0 && retries <= 5; // Max 5 retries
                } catch (NumberFormatException e) {
                    return false;
                }
            },
            "Max retries must be between 0 and 5"));
        
        // Grid hub URL validation
        validationRules.add(new ValidationRule("grid.hubUrl",
            value -> value == null || value.trim().isEmpty() || isValidUrl(value),
            "Invalid grid hub URL format"));
    }
    
    /**
     * Validate all configuration
     */
    public ValidationResult validateAll() {
        TestLogManager.info("Starting configuration validation...");
        
        validationErrors.clear();
        
        for (ValidationRule rule : validationRules) {
            validateRule(rule);
        }
        
        // Custom validations
        validateDependencies();
        validatePaths();
        validateCloudConfiguration();
        
        boolean isValid = validationErrors.isEmpty();
        ValidationResult result = new ValidationResult(isValid, validationErrors);
        
        if (isValid) {
            TestLogManager.success("Configuration validation passed successfully");
        } else {
            TestLogManager.error("Configuration validation failed with " + validationErrors.size() + " errors");
            printValidationErrors();
        }
        
        return result;
    }
    
    /**
     * Validate a specific rule
     */
    private void validateRule(ValidationRule rule) {
        String value = configManager.getString(rule.getKey());
        if (!rule.getValidator().test(value)) {
            addValidationError(rule.getKey(), rule.getErrorMessage());
        }
    }
    
    /**
     * Validate configuration dependencies
     */
    private void validateDependencies() {
        // If grid is enabled, hub URL must be provided
        if (configManager.getBoolean("grid.enabled", false)) {
            String hubUrl = configManager.getString("grid.hubUrl");
            if (hubUrl == null || hubUrl.trim().isEmpty()) {
                addValidationError("grid.hubUrl", "Grid hub URL is required when grid is enabled");
            }
        }
        
        // If cloud testing is enabled, credentials must be provided
        if (configManager.getBoolean("cloud.enabled", false)) {
            String username = configManager.getString("cloud.username");
            String accessKey = configManager.getString("cloud.accessKey");
            
            if (username == null || username.trim().isEmpty()) {
                addValidationError("cloud.username", "Cloud username is required when cloud testing is enabled");
            }
            
            if (accessKey == null || accessKey.trim().isEmpty()) {
                addValidationError("cloud.accessKey", "Cloud access key is required when cloud testing is enabled");
            }
        }
        
        // If mobile testing is enabled, device configuration must be provided
        if (configManager.getBoolean("mobile.enabled", false)) {
            String deviceName = configManager.getString("mobile.deviceName");
            String appPath = configManager.getString("mobile.appPath");
            
            if (deviceName == null || deviceName.trim().isEmpty()) {
                addValidationError("mobile.deviceName", "Mobile device name is required when mobile testing is enabled");
            }
            
            if (appPath == null || appPath.trim().isEmpty()) {
                addValidationError("mobile.appPath", "Mobile app path is required when mobile testing is enabled");
            }
        }
        
        // If Excel data source is selected, Excel path must be valid
        if ("excel".equals(configManager.getString("dataSource", "excel"))) {
            String excelPath = configManager.getString("excelPath");
            if (excelPath == null || excelPath.trim().isEmpty()) {
                addValidationError("excelPath", "Excel path is required when using Excel data source");
            }
        }
        
        // If JSON data source is selected, JSON path must be valid
        if ("json".equals(configManager.getString("dataSource", "excel"))) {
            String jsonPath = configManager.getString("jsonPath");
            if (jsonPath == null || jsonPath.trim().isEmpty()) {
                addValidationError("jsonPath", "JSON path is required when using JSON data source");
            }
        }
    }
    
    /**
     * Validate file paths
     */
    private void validatePaths() {
        // Validate Excel path if provided
        String excelPath = configManager.getString("excelPath");
        if (excelPath != null && !excelPath.trim().isEmpty()) {
            if (!excelPath.endsWith(".xlsx") && !excelPath.endsWith(".xls")) {
                addValidationError("excelPath", "Excel file must have .xlsx or .xls extension");
            }
        }
        
        // Validate JSON path if provided
        String jsonPath = configManager.getString("jsonPath");
        if (jsonPath != null && !jsonPath.trim().isEmpty()) {
            if (!jsonPath.endsWith(".json")) {
                addValidationError("jsonPath", "JSON file must have .json extension");
            }
        }
    }
    
    /**
     * Validate cloud configuration
     */
    private void validateCloudConfiguration() {
        String provider = configManager.getString("cloud.provider", "browserstack");
        
        if (configManager.getBoolean("cloud.enabled", false)) {
            if (!isValidCloudProvider(provider)) {
                addValidationError("cloud.provider", 
                    "Invalid cloud provider. Supported: browserstack, saucelabs, crossbrowsertesting, lambdatest");
            }
        }
    }
    
    /**
     * Add validation error
     */
    private void addValidationError(String key, String message) {
        validationErrors.computeIfAbsent(key, k -> new ArrayList<>()).add(message);
    }
    
    /**
     * Print validation errors
     */
    private void printValidationErrors() {
        TestLogManager.error("Configuration Validation Errors:");
        for (Map.Entry<String, List<String>> entry : validationErrors.entrySet()) {
            String key = entry.getKey();
            List<String> errors = entry.getValue();
            
            TestLogManager.error("  " + key + ":");
            for (String error : errors) {
                TestLogManager.error("    - " + error);
            }
        }
    }
    
    // ===========================================
    // VALIDATION HELPER METHODS
    // ===========================================
    
    private boolean isValidBrowser(String browser) {
        if (browser == null) return false;
        String lowerBrowser = browser.toLowerCase();
        return lowerBrowser.equals("chrome") || 
               lowerBrowser.equals("firefox") || 
               lowerBrowser.equals("edge") || 
               lowerBrowser.equals("chromeheadless") ||
               lowerBrowser.equals("firefoxheadless") ||
               lowerBrowser.equals("edgeheadless");
    }
    
    private boolean isValidUrl(String url) {
        if (url == null || url.trim().isEmpty()) return true;
        try {
            java.net.URI.create(url).toURL();
            return true;
        } catch (Exception e) {
            return false;
        }
    }
    
    private boolean isValidEmail(String email) {
        if (email == null || email.trim().isEmpty()) return true;
        String emailRegex = "^[a-zA-Z0-9_+&*-]+(?:\\.[a-zA-Z0-9_+&*-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,7}$";
        return Pattern.matches(emailRegex, email);
    }
    
    private boolean isValidEnvironment(String environment) {
        if (environment == null) return false;
        String lowerEnv = environment.toLowerCase();
        return lowerEnv.equals("dev") || 
               lowerEnv.equals("development") ||
               lowerEnv.equals("test") || 
               lowerEnv.equals("staging") || 
               lowerEnv.equals("prod") || 
               lowerEnv.equals("production");
    }
    
    private boolean isValidLoggingLevel(String level) {
        if (level == null || level.trim().isEmpty()) return true;
        String upperLevel = level.toUpperCase();
        return upperLevel.equals("ALL") || 
               upperLevel.equals("DEBUG") || 
               upperLevel.equals("INFO") || 
               upperLevel.equals("WARN") || 
               upperLevel.equals("WARNING") || 
               upperLevel.equals("ERROR") || 
               upperLevel.equals("SUCCESS");
    }
    
    private boolean isValidCloudProvider(String provider) {
        if (provider == null) return false;
        String lowerProvider = provider.toLowerCase();
        return lowerProvider.equals("browserstack") || 
               lowerProvider.equals("saucelabs") || 
               lowerProvider.equals("crossbrowsertesting") || 
               lowerProvider.equals("lambdatest");
    }
    
    // ===========================================
    // INNER CLASSES
    // ===========================================
    
    /**
     * Validation rule definition
     */
    private static class ValidationRule {
        private final String key;
        private final java.util.function.Predicate<String> validator;
        private final String errorMessage;
        
        public ValidationRule(String key, java.util.function.Predicate<String> validator, String errorMessage) {
            this.key = key;
            this.validator = validator;
            this.errorMessage = errorMessage;
        }
        
        public String getKey() { return key; }
        public java.util.function.Predicate<String> getValidator() { return validator; }
        public String getErrorMessage() { return errorMessage; }
    }
    
    /**
     * Validation result
     */
    public static class ValidationResult {
        private final boolean isValid;
        private final Map<String, List<String>> errors;
        
        public ValidationResult(boolean isValid, Map<String, List<String>> errors) {
            this.isValid = isValid;
            this.errors = new HashMap<>(errors);
        }
        
        public boolean isValid() { return isValid; }
        public Map<String, List<String>> getErrors() { return new HashMap<>(errors); }
        
        public int getErrorCount() {
            return errors.values().stream().mapToInt(List::size).sum();
        }
        
        public List<String> getAllErrorMessages() {
            List<String> allErrors = new ArrayList<>();
            for (List<String> errorList : errors.values()) {
                allErrors.addAll(errorList);
            }
            return allErrors;
        }
    }
}
