package patterns.repository;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import reporting.TestLogManager;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * JSON-based implementation of TestDataRepository.
 * This implementation reads test data from JSON files.
 */
public class JsonTestDataRepository implements TestDataRepository {
    
    private final String filePath;
    private final ObjectMapper objectMapper;
    private final Map<String, TestData> cache;
    private final boolean enableCaching;
    private JsonNode rootNode;
    
    public JsonTestDataRepository(String filePath) {
        this(filePath, true);
    }
    
    public JsonTestDataRepository(String filePath, boolean enableCaching) {
        this.filePath = filePath;
        this.objectMapper = new ObjectMapper();
        this.enableCaching = enableCaching;
        this.cache = enableCaching ? new HashMap<>() : null;
        
        loadJsonData();
        TestLogManager.info("JsonTestDataRepository initialized with file: " + filePath);
    }
    
    private void loadJsonData() {
        try {
            File file = new File(filePath);
            if (!file.exists()) {
                TestLogManager.warning("JSON file does not exist: " + filePath);
                return;
            }
            
            this.rootNode = objectMapper.readTree(file);
        } catch (IOException e) {
            TestLogManager.error("Error loading JSON file: " + filePath, e);
        }
    }
    
    @Override
    public Optional<TestData> getTestData(String testName) {
        if (enableCaching && cache != null && cache.containsKey(testName)) {
            return Optional.of(cache.get(testName));
        }
        
        try {
            TestData testData = loadTestDataFromJson(testName);
            if (testData != null) {
                if (enableCaching && cache != null) {
                    cache.put(testName, testData);
                }
                return Optional.of(testData);
            }
        } catch (Exception e) {
            TestLogManager.error("Error loading test data for: " + testName, e);
        }
        
        return Optional.empty();
    }
    
    @Override
    public TestData getTestData(String testName, TestData defaultValue) {
        return getTestData(testName).orElse(defaultValue);
    }
    
    @Override
    public List<TestData> getAllTestData() {
        List<TestData> allTestData = new ArrayList<>();
        
        if (rootNode == null) {
            return allTestData;
        }
        
        try {
            if (rootNode.isArray()) {
                // JSON array format
                for (JsonNode testNode : rootNode) {
                    TestData testData = parseTestDataFromNode(testNode);
                    if (testData != null) {
                        allTestData.add(testData);
                        
                        if (enableCaching && cache != null) {
                            cache.put(testData.getTestName(), testData);
                        }
                    }
                }
            } else if (rootNode.isObject()) {
                // JSON object format with test names as keys
                rootNode.fieldNames().forEachRemaining(fieldName -> {
                    JsonNode testNode = rootNode.get(fieldName);
                    String testName = fieldName;
                    
                    TestData testData = parseTestDataFromNode(testNode, testName);
                    if (testData != null) {
                        allTestData.add(testData);
                        
                        if (enableCaching && cache != null) {
                            cache.put(testName, testData);
                        }
                    }
                });
            }
        } catch (Exception e) {
            TestLogManager.error("Error loading all test data from JSON", e);
        }
        
        return allTestData;
    }
    
    @Override
    public List<TestData> getTestDataBySheet(String sheetName) {
        // JSON doesn't have sheets, so return all data
        return getAllTestData();
    }
    
    @Override
    public boolean saveTestData(TestData testData) {
        try {
            // Load existing data
            List<TestData> allData = getAllTestData();
            
            // Remove existing test data with same name
            allData.removeIf(data -> data.getTestName().equals(testData.getTestName()));
            
            // Add new test data
            allData.add(testData);
            
            // Write back to file
            return writeTestDataToFile(allData);
            
        } catch (Exception e) {
            TestLogManager.error("Error saving test data: " + testData.getTestName(), e);
            return false;
        }
    }
    
    @Override
    public boolean updateTestData(TestData testData) {
        return saveTestData(testData); // Same as save for JSON
    }
    
    @Override
    public boolean deleteTestData(String testName) {
        try {
            List<TestData> allData = getAllTestData();
            boolean removed = allData.removeIf(data -> data.getTestName().equals(testName));
            
            if (removed) {
                writeTestDataToFile(allData);
                if (enableCaching && cache != null) {
                    cache.remove(testName);
                }
            }
            
            return removed;
            
        } catch (Exception e) {
            TestLogManager.error("Error deleting test data: " + testName, e);
            return false;
        }
    }
    
    @Override
    public boolean exists(String testName) {
        return getTestData(testName).isPresent();
    }
    
    @Override
    public int count() {
        return getAllTestData().size();
    }
    
    @Override
    public boolean clear() {
        try {
            boolean cleared = writeTestDataToFile(new ArrayList<>());
            if (cleared && enableCaching && cache != null) {
                cache.clear();
            }
            return cleared;
        } catch (Exception e) {
            TestLogManager.error("Error clearing test data", e);
            return false;
        }
    }
    
    private TestData loadTestDataFromJson(String testName) {
        if (rootNode == null) {
            return null;
        }
        
        try {
            if (rootNode.isArray()) {
                // Find test in array
                for (JsonNode testNode : rootNode) {
                    String nodeTestName = testNode.get("testName").asText();
                    if (testName.equals(nodeTestName)) {
                        return parseTestDataFromNode(testNode);
                    }
                }
            } else if (rootNode.isObject()) {
                // Find test in object
                JsonNode testNode = rootNode.get(testName);
                if (testNode != null) {
                    return parseTestDataFromNode(testNode, testName);
                }
            }
        } catch (Exception e) {
            TestLogManager.error("Error parsing test data from JSON for: " + testName, e);
        }
        
        return null;
    }
    
    private TestData parseTestDataFromNode(JsonNode testNode) {
        return parseTestDataFromNode(testNode, null);
    }
    
    private TestData parseTestDataFromNode(JsonNode testNode, String defaultTestName) {
        try {
            String testName = defaultTestName;
            if (testName == null && testNode.has("testName")) {
                testName = testNode.get("testName").asText();
            }
            
            if (testName == null || testName.trim().isEmpty()) {
                return null;
            }
            
            Map<String, String> data = new HashMap<>();
            
            // Parse all fields from the JSON node
            testNode.fieldNames().forEachRemaining(key -> {
                JsonNode value = testNode.get(key);
                
                if (value.isTextual()) {
                    data.put(key, value.asText());
                } else if (value.isNumber()) {
                    data.put(key, value.asText());
                } else if (value.isBoolean()) {
                    data.put(key, String.valueOf(value.asBoolean()));
                }
                // Add more type handling as needed
            });
            
            return new TestData(testName, data);
            
        } catch (Exception e) {
            TestLogManager.error("Error parsing test data from JSON node", e);
            return null;
        }
    }
    
    private boolean writeTestDataToFile(List<TestData> testDataList) {
        try {
            // Convert TestData list to JSON structure
            List<Map<String, Object>> jsonData = testDataList.stream()
                    .map(this::convertTestDataToMap)
                    .collect(Collectors.toList());
            
            objectMapper.writerWithDefaultPrettyPrinter()
                    .writeValue(new File(filePath), jsonData);
            
            // Reload the data
            loadJsonData();
            
            return true;
            
        } catch (IOException e) {
            TestLogManager.error("Error writing test data to JSON file", e);
            return false;
        }
    }
    
    private Map<String, Object> convertTestDataToMap(TestData testData) {
        Map<String, Object> map = new HashMap<>();
        map.put("testName", testData.getTestName());
        map.putAll(testData.getAllData());
        return map;
    }
}
