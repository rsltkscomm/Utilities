package patterns.config;

import reporting.TestLogManager;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * Configuration class for managing environment-specific settings.
 * This handles loading and managing configuration for different environments.
 */
public class EnvironmentConfig {
    
    private static final Map<String, EnvironmentConfig> configs = new HashMap<>();
    private final String environment;
    private final Properties properties;
    private final Map<String, String> urls;
    private final Map<String, String> credentials;
    
    private EnvironmentConfig(String environment) {
        this.environment = environment;
        this.properties = new Properties();
        this.urls = new HashMap<>();
        this.credentials = new HashMap<>();
        loadConfiguration();
    }
    
    /**
     * Gets the configuration for a specific environment.
     * @param environment The environment name
     * @return EnvironmentConfig instance
     */
    public static synchronized EnvironmentConfig getConfig(String environment) {
        return configs.computeIfAbsent(environment, EnvironmentConfig::new);
    }
    
    /**
     * Gets the default environment configuration.
     * @return EnvironmentConfig instance
     */
    public static EnvironmentConfig getDefaultConfig() {
        String defaultEnv = System.getProperty("environment", "test");
        return getConfig(defaultEnv);
    }
    
    /**
     * Gets the environment name.
     * @return Environment name
     */
    public String getEnvironment() {
        return environment;
    }
    
    /**
     * Gets a configuration property.
     * @param key The property key
     * @return Property value or null if not found
     */
    public String getProperty(String key) {
        return properties.getProperty(key);
    }
    
    /**
     * Gets a configuration property with default value.
     * @param key The property key
     * @param defaultValue Default value if property not found
     * @return Property value or default value
     */
    public String getProperty(String key, String defaultValue) {
        return properties.getProperty(key, defaultValue);
    }
    
    /**
     * Gets a URL for a specific service.
     * @param service The service name
     * @return URL or null if not found
     */
    public String getUrl(String service) {
        return urls.get(service);
    }
    
    /**
     * Gets a URL for a specific service with default value.
     * @param service The service name
     * @param defaultValue Default URL if not found
     * @return URL or default value
     */
    public String getUrl(String service, String defaultValue) {
        return urls.getOrDefault(service, defaultValue);
    }
    
    /**
     * Gets credentials for a specific user.
     * @param user The user name
     * @return Credentials or null if not found
     */
    public String getCredentials(String user) {
        return credentials.get(user);
    }
    
    /**
     * Gets credentials for a specific user with default value.
     * @param user The user name
     * @param defaultValue Default credentials if not found
     * @return Credentials or default value
     */
    public String getCredentials(String user, String defaultValue) {
        return credentials.getOrDefault(user, defaultValue);
    }
    
    /**
     * Gets the base URL for the environment.
     * @return Base URL
     */
    public String getBaseUrl() {
        return getUrl("base", "");
    }
    
    /**
     * Gets the API base URL for the environment.
     * @return API base URL
     */
    public String getApiBaseUrl() {
        return getUrl("api", "");
    }
    
    /**
     * Gets the database URL for the environment.
     * @return Database URL
     */
    public String getDatabaseUrl() {
        return getUrl("database", "");
    }
    
    /**
     * Gets the default username for the environment.
     * @return Default username
     */
    public String getDefaultUsername() {
        return getCredentials("default.username", "");
    }
    
    /**
     * Gets the default password for the environment.
     * @return Default password
     */
    public String getDefaultPassword() {
        return getCredentials("default.password", "");
    }
    
    /**
     * Checks if the environment is production.
     * @return true if production, false otherwise
     */
    public boolean isProduction() {
        return "production".equalsIgnoreCase(environment) || "prod".equalsIgnoreCase(environment);
    }
    
    /**
     * Checks if the environment is test/development.
     * @return true if test/dev, false otherwise
     */
    public boolean isTest() {
        return "test".equalsIgnoreCase(environment) || "dev".equalsIgnoreCase(environment) || 
               "development".equalsIgnoreCase(environment);
    }
    
    /**
     * Checks if the environment is staging.
     * @return true if staging, false otherwise
     */
    public boolean isStaging() {
        return "staging".equalsIgnoreCase(environment) || "stage".equalsIgnoreCase(environment);
    }
    
    /**
     * Gets all properties as a map.
     * @return Map of all properties
     */
    public Map<String, String> getAllProperties() {
        Map<String, String> allProperties = new HashMap<>();
        for (String key : properties.stringPropertyNames()) {
            allProperties.put(key, properties.getProperty(key));
        }
        return allProperties;
    }
    
    /**
     * Gets all URLs as a map.
     * @return Map of all URLs
     */
    public Map<String, String> getAllUrls() {
        return new HashMap<>(urls);
    }
    
    /**
     * Gets all credentials as a map.
     * @return Map of all credentials
     */
    public Map<String, String> getAllCredentials() {
        return new HashMap<>(credentials);
    }
    
    private void loadConfiguration() {
        try {
            // Load environment-specific properties file
            String configFile = "src/main/resources/config/" + environment + ".properties";
            loadPropertiesFile(configFile);
            
            // Load common properties file
            loadPropertiesFile("src/main/resources/config/common.properties");
            
            // Load URLs
            loadUrls();
            
            // Load credentials
            loadCredentials();
            
            TestLogManager.info("Environment configuration loaded for: " + environment);
            
        } catch (Exception e) {
            TestLogManager.error("Failed to load environment configuration for: " + environment, e);
            loadDefaultConfiguration();
        }
    }
    
    private void loadPropertiesFile(String filePath) {
        try (FileInputStream fis = new FileInputStream(filePath)) {
            Properties fileProperties = new Properties();
            fileProperties.load(fis);
            properties.putAll(fileProperties);
            TestLogManager.info("Loaded properties from: " + filePath);
        } catch (IOException e) {
            TestLogManager.warning("Could not load properties file: " + filePath);
        }
    }
    
    private void loadUrls() {
        // Load URLs from properties
        urls.put("base", getProperty("url.base", ""));
        urls.put("api", getProperty("url.api", ""));
        urls.put("database", getProperty("url.database", ""));
        urls.put("login", getProperty("url.login", ""));
        urls.put("dashboard", getProperty("url.dashboard", ""));
        urls.put("admin", getProperty("url.admin", ""));
    }
    
    private void loadCredentials() {
        // Load credentials from properties
        credentials.put("default.username", getProperty("credentials.default.username", ""));
        credentials.put("default.password", getProperty("credentials.default.password", ""));
        credentials.put("admin.username", getProperty("credentials.admin.username", ""));
        credentials.put("admin.password", getProperty("credentials.admin.password", ""));
        credentials.put("test.username", getProperty("credentials.test.username", ""));
        credentials.put("test.password", getProperty("credentials.test.password", ""));
    }
    
    private void loadDefaultConfiguration() {
        TestLogManager.info("Loading default configuration for environment: " + environment);
        
        // Set default URLs based on environment
        switch (environment.toLowerCase()) {
            case "production":
            case "prod":
                urls.put("base", "https://production.example.com");
                urls.put("api", "https://api.production.example.com");
                break;
            case "staging":
            case "stage":
                urls.put("base", "https://staging.example.com");
                urls.put("api", "https://api.staging.example.com");
                break;
            case "test":
            case "dev":
            case "development":
            default:
                urls.put("base", "https://test.example.com");
                urls.put("api", "https://api.test.example.com");
                break;
        }
        
        // Set default credentials
        credentials.put("default.username", "testuser");
        credentials.put("default.password", "testpass");
        credentials.put("admin.username", "admin");
        credentials.put("admin.password", "adminpass");
    }
    
    @Override
    public String toString() {
        return "EnvironmentConfig{" +
                "environment='" + environment + '\'' +
                ", propertiesCount=" + properties.size() +
                ", urlsCount=" + urls.size() +
                ", credentialsCount=" + credentials.size() +
                '}';
    }
}
