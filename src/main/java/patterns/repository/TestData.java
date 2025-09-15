package patterns.repository;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Represents test data for a specific test.
 * This class holds all the data needed for test execution.
 */
public class TestData {
    
    private final String testName;
    private final Map<String, String> data;
    private final Map<String, Object> metadata;
    
    public TestData(String testName) {
        this.testName = testName;
        this.data = new HashMap<>();
        this.metadata = new HashMap<>();
    }
    
    /**
     * Gets the test name.
     * @return The test name
     */
    public String getTestName() {
        return testName;
    }
    
    /**
     * Gets a data value by key.
     * @param key The data key
     * @return Optional containing the value if found
     */
    public Optional<String> getData(String key) {
        return Optional.ofNullable(data.get(key));
    }
    
    /**
     * Gets a data value by key with default value.
     * @param key The data key
     * @param defaultValue The default value if key not found
     * @return The data value or default value
     */
    public String getData(String key, String defaultValue) {
        return data.getOrDefault(key, defaultValue);
    }
    
    /**
     * Sets a data value.
     * @param key The data key
     * @param value The data value
     */
    public void setData(String key, String value) {
        data.put(key, value);
    }
    
    /**
     * Sets multiple data values.
     * @param data Map of key-value pairs
     */
    public void setData(Map<String, String> data) {
        this.data.putAll(data);
    }
    
    /**
     * Gets all data as a map.
     * @return Map of all data
     */
    public Map<String, String> getAllData() {
        return new HashMap<>(data);
    }
    
    /**
     * Gets metadata by key.
     * @param key The metadata key
     * @return Optional containing the metadata value if found
     */
    public Optional<Object> getMetadata(String key) {
        return Optional.ofNullable(metadata.get(key));
    }
    
    /**
     * Sets metadata.
     * @param key The metadata key
     * @param value The metadata value
     */
    public void setMetadata(String key, Object value) {
        metadata.put(key, value);
    }
    
    /**
     * Gets all metadata as a map.
     * @return Map of all metadata
     */
    public Map<String, Object> getAllMetadata() {
        return new HashMap<>(metadata);
    }
    
    /**
     * Checks if a data key exists.
     * @param key The data key
     * @return true if exists, false otherwise
     */
    public boolean hasData(String key) {
        return data.containsKey(key);
    }
    
    /**
     * Checks if a metadata key exists.
     * @param key The metadata key
     * @return true if exists, false otherwise
     */
    public boolean hasMetadata(String key) {
        return metadata.containsKey(key);
    }
    
    /**
     * Removes a data entry.
     * @param key The data key
     * @return The removed value or null if not found
     */
    public String removeData(String key) {
        return data.remove(key);
    }
    
    /**
     * Removes a metadata entry.
     * @param key The metadata key
     * @return The removed value or null if not found
     */
    public Object removeMetadata(String key) {
        return metadata.remove(key);
    }
    
    /**
     * Clears all data.
     */
    public void clearData() {
        data.clear();
    }
    
    /**
     * Clears all metadata.
     */
    public void clearMetadata() {
        metadata.clear();
    }
    
    /**
     * Clears all data and metadata.
     */
    public void clear() {
        clearData();
        clearMetadata();
    }
    
    @Override
    public String toString() {
        return "TestData{" +
                "testName='" + testName + '\'' +
                ", data=" + data +
                ", metadata=" + metadata +
                '}';
    }
}