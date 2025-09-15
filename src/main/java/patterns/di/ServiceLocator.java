package patterns.di;

import patterns.builder.TestConfiguration;
import reporting.TestLogManager;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Service Locator pattern implementation for managing and retrieving services.
 * This provides a centralized way to access services throughout the application.
 */
public class ServiceLocator {
    
    private static final Map<Class<?>, Object> services = new HashMap<>();
    private static final Map<Class<?>, ServiceFactory<?>> factories = new HashMap<>();
    private static TestContext testContext;
    private static boolean initialized = false;
    
    /**
     * Initializes the service locator with a test context.
     * @param context The test context to use
     */
    public static void initialize(TestContext context) {
        if (initialized) {
            TestLogManager.warning("ServiceLocator already initialized");
            return;
        }
        
        testContext = context;
        initialized = true;
        TestLogManager.info("ServiceLocator initialized");
    }
    
    /**
     * Registers a service instance.
     * @param type The service type
     * @param instance The service instance
     */
    public static <T> void registerService(Class<T> type, T instance) {
        services.put(type, instance);
        TestLogManager.info("Registered service: " + type.getSimpleName());
    }
    
    /**
     * Registers a service factory.
     * @param type The service type
     * @param factory The service factory
     */
    public static <T> void registerFactory(Class<T> type, ServiceFactory<T> factory) {
        factories.put(type, factory);
        TestLogManager.info("Registered factory for: " + type.getSimpleName());
    }
    
    /**
     * Gets a service instance.
     * @param type The service type
     * @return Optional containing the service if found
     */
    public static <T> Optional<T> getService(Class<T> type) {
        // First check if we have a direct instance
        Object service = services.get(type);
        if (service != null && type.isInstance(service)) {
            return Optional.of(type.cast(service));
        }
        
        // Then check if we have a factory
        ServiceFactory<?> factory = factories.get(type);
        if (factory != null) {
            try {
                Object instance = factory.createService();
                if (type.isInstance(instance)) {
                    // Cache the instance for future use
                    services.put(type, instance);
                    return Optional.of(type.cast(instance));
                }
            } catch (Exception e) {
                TestLogManager.error("Error creating service from factory: " + type.getSimpleName(), e);
            }
        }
        
        // Finally, try to get from test context
        if (testContext != null) {
            return testContext.getDependency(type);
        }
        
        return Optional.empty();
    }
    
    /**
     * Gets a service instance, throwing an exception if not found.
     * @param type The service type
     * @return The service instance
     * @throws IllegalStateException if service not found
     */
    public static <T> T getRequiredService(Class<T> type) {
        return getService(type)
                .orElseThrow(() -> new IllegalStateException("Required service not found: " + type.getSimpleName()));
    }
    
    /**
     * Checks if a service is registered.
     * @param type The service type
     * @return true if registered, false otherwise
     */
    public static boolean isServiceRegistered(Class<?> type) {
        return services.containsKey(type) || factories.containsKey(type) || 
               (testContext != null && testContext.getDependency(type).isPresent());
    }
    
    /**
     * Removes a service registration.
     * @param type The service type
     */
    public static void unregisterService(Class<?> type) {
        services.remove(type);
        factories.remove(type);
        TestLogManager.info("Unregistered service: " + type.getSimpleName());
    }
    
    /**
     * Clears all service registrations.
     */
    public static void clear() {
        services.clear();
        factories.clear();
        testContext = null;
        initialized = false;
        TestLogManager.info("ServiceLocator cleared");
    }
    
    /**
     * Gets the test context.
     * @return TestContext instance or null if not initialized
     */
    public static TestContext getTestContext() {
        return testContext;
    }
    
    /**
     * Gets the test configuration.
     * @return TestConfiguration instance or null if not available
     */
    public static Optional<TestConfiguration> getConfiguration() {
        if (testContext != null) {
            return Optional.of(testContext.getConfiguration());
        }
        return Optional.empty();
    }
    
    /**
     * Checks if the service locator is initialized.
     * @return true if initialized, false otherwise
     */
    public static boolean isInitialized() {
        return initialized;
    }
    
    /**
     * Interface for service factories.
     * @param <T> The service type
     */
    @FunctionalInterface
    public interface ServiceFactory<T> {
        T createService() throws Exception;
    }
}
