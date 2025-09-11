package patterns.repository;

import java.util.HashMap;
import java.util.Map;

/**
 * Data class representing test data for a specific test case.
 */
public class TestData {
    
    private final String testName;
    private final Map<String, String> data;
    private final Map<String, Object> complexData;
    
    public TestData(String testName) {
        this.testName = testName;
        this.data = new HashMap<>();
        this.complexData = new HashMap<>();
    }
    
    public TestData(String testName, Map<String, String> data) {
        this.testName = testName;
        this.data = new HashMap<>(data);
        this.complexData = new HashMap<>();
    }
    
    public String getTestName() {
        return testName;
    }
    
    public String getData(String key) {
        return data.get(key);
    }
    
    public String getData(String key, String defaultValue) {
        return data.getOrDefault(key, defaultValue);
    }
    
    public void setData(String key, String value) {
        data.put(key, value);
    }
    
    public void setData(Map<String, String> data) {
        this.data.putAll(data);
    }
    
    public Object getComplexData(String key) {
        return complexData.get(key);
    }
    
    public void setComplexData(String key, Object value) {
        complexData.put(key, value);
    }
    
    public Map<String, String> getAllData() {
        return new HashMap<>(data);
    }
    
    public Map<String, Object> getAllComplexData() {
        return new HashMap<>(complexData);
    }
    
    public boolean containsKey(String key) {
        return data.containsKey(key);
    }
    
    public boolean isEmpty() {
        return data.isEmpty();
    }
    
    public int size() {
        return data.size();
    }
    
    @Override
    public String toString() {
        return "TestData{" +
                "testName='" + testName + '\'' +
                ", data=" + data +
                '}';
    }
}
