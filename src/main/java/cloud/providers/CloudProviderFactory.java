package cloud.providers;

import cloud.CloudConfiguration;
import cloud.providers.impl.BrowserStackProvider;
import cloud.providers.impl.SauceLabsProvider;
import cloud.providers.impl.LambdaTestProvider;
import cloud.providers.impl.CrossBrowserTestingProvider;
import reporting.TestLogManager;

import java.util.HashMap;
import java.util.Map;

/**
 * Factory for creating cloud provider instances
 * Supports multiple cloud testing platforms
 */
public class CloudProviderFactory {
    
    private static final Map<String, Class<? extends CloudProvider>> providerClasses = new HashMap<>();
    
    static {
        // Register supported cloud providers
        providerClasses.put("browserstack", BrowserStackProvider.class);
        providerClasses.put("saucelabs", SauceLabsProvider.class);
        providerClasses.put("lambdatest", LambdaTestProvider.class);
        providerClasses.put("crossbrowsertesting", CrossBrowserTestingProvider.class);
        
        TestLogManager.info("CloudProviderFactory initialized with " + providerClasses.size() + " providers");
    }
    
    /**
     * Create cloud provider instance
     */
    public static CloudProvider createProvider(String providerName, CloudConfiguration config) {
        if (providerName == null || providerName.trim().isEmpty()) {
            throw new IllegalArgumentException("Provider name cannot be null or empty");
        }
        
        String normalizedName = providerName.toLowerCase().trim();
        Class<? extends CloudProvider> providerClass = providerClasses.get(normalizedName);
        
        if (providerClass == null) {
            throw new UnsupportedOperationException("Unsupported cloud provider: " + providerName + 
                ". Supported providers: " + providerClasses.keySet());
        }
        
        try {
            CloudProvider provider = providerClass.getDeclaredConstructor(CloudConfiguration.class)
                .newInstance(config);
            
            TestLogManager.info("Created cloud provider: " + providerName);
            return provider;
            
        } catch (Exception e) {
            TestLogManager.error("Failed to create cloud provider: " + providerName, e);
            throw new RuntimeException("Failed to create cloud provider: " + providerName, e);
        }
    }
    
    /**
     * Create cloud provider for active configuration
     */
    public static CloudProvider createActiveProvider(CloudConfiguration config) {
        return createProvider(config.getActiveProvider(), config);
    }
    
    /**
     * Get supported providers
     */
    public static Map<String, Class<? extends CloudProvider>> getSupportedProviders() {
        return new HashMap<>(providerClasses);
    }
    
    /**
     * Check if provider is supported
     */
    public static boolean isProviderSupported(String providerName) {
        return providerClasses.containsKey(providerName.toLowerCase());
    }
    
    /**
     * Register custom cloud provider
     */
    public static void registerProvider(String name, Class<? extends CloudProvider> providerClass) {
        providerClasses.put(name.toLowerCase(), providerClass);
        TestLogManager.info("Registered custom cloud provider: " + name);
    }
    
    /**
     * Unregister cloud provider
     */
    public static void unregisterProvider(String name) {
        Class<? extends CloudProvider> removed = providerClasses.remove(name.toLowerCase());
        if (removed != null) {
            TestLogManager.info("Unregistered cloud provider: " + name);
        }
    }
}
