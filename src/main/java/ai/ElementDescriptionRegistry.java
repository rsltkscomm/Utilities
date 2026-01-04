package ai;

import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

/**
 * Registry for storing element descriptions
 * Helps self-healing locator find elements using semantic information
 */
public class ElementDescriptionRegistry {
    
    private static volatile ElementDescriptionRegistry instance;
    private final Map<String, String> descriptions;
    
    private ElementDescriptionRegistry() {
        this.descriptions = new ConcurrentHashMap<>();
    }
    
    public static ElementDescriptionRegistry getInstance() {
        if (instance == null) {
            synchronized (ElementDescriptionRegistry.class) {
                if (instance == null) {
                    instance = new ElementDescriptionRegistry();
                }
            }
        }
        return instance;
    }
    
    /**
     * Register element description
     */
    public void registerDescription(String elementName, String description) {
        if (elementName != null && description != null && !description.isEmpty()) {
            descriptions.put(elementName.toLowerCase(), description);
        }
    }
    
    /**
     * Get element description
     */
    public String getDescription(String elementName) {
        if (elementName == null) return null;
        return descriptions.get(elementName.toLowerCase());
    }
    
    /**
     * Check if description exists
     */
    public boolean hasDescription(String elementName) {
        if (elementName == null) return false;
        return descriptions.containsKey(elementName.toLowerCase());
    }
    
    /**
     * Remove description
     */
    public void removeDescription(String elementName) {
        if (elementName != null) {
            descriptions.remove(elementName.toLowerCase());
        }
    }
    
    /**
     * Get all descriptions
     */
    public Map<String, String> getAllDescriptions() {
        return new ConcurrentHashMap<>(descriptions);
    }
    
    /**
     * Clear all descriptions
     */
    public void clear() {
        descriptions.clear();
    }
}

