package patterns.repository;

import data.XLSReader;
import reporting.TestLogManager;

import java.util.*;

/**
 * Excel-based implementation of TestDataRepository.
 * This implementation uses the existing XLSReader for Excel file operations.
 */
public class ExcelTestDataRepository implements TestDataRepository {
    
    private final XLSReader xlsReader;
    private final String sheetName;
    private final Map<String, TestData> cache;
    private final boolean enableCaching;
    
    public ExcelTestDataRepository(String filePath, String sheetName) {
        this(filePath, sheetName, true);
    }
    
    public ExcelTestDataRepository(String filePath, String sheetName, boolean enableCaching) {
        this.xlsReader = new XLSReader(filePath);
        this.sheetName = sheetName;
        this.enableCaching = enableCaching;
        this.cache = enableCaching ? new HashMap<>() : null;
        
        TestLogManager.info("ExcelTestDataRepository initialized with file: " + filePath + ", sheet: " + sheetName);
    }
    
    @Override
    public Optional<TestData> getTestData(String testName) {
        if (enableCaching && cache != null && cache.containsKey(testName)) {
            return Optional.of(cache.get(testName));
        }
        
        try {
            TestData testData = loadTestDataFromExcel(testName);
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
        
        try {
            int rowCount = xlsReader.getRowCount(sheetName);
            
            // Start from row 2 (assuming row 1 is header)
            for (int i = 2; i <= rowCount; i++) {
                String testMethodName = xlsReader.getCellData(sheetName, "TestMethodName", i);
                if (testMethodName != null && !testMethodName.trim().isEmpty()) {
                    TestData testData = loadTestDataFromRow(i);
                    if (testData != null) {
                        allTestData.add(testData);
                        
                        if (enableCaching && cache != null) {
                            cache.put(testMethodName, testData);
                        }
                    }
                }
            }
        } catch (Exception e) {
            TestLogManager.error("Error loading all test data", e);
        }
        
        return allTestData;
    }
    
    @Override
    public List<TestData> getTestDataBySheet(String sheetName) {
        // For Excel implementation, this is the same as getAllTestData
        // since we're already working with a specific sheet
        if (this.sheetName.equals(sheetName)) {
            return getAllTestData();
        }
        return new ArrayList<>();
    }
    
    @Override
    public boolean saveTestData(TestData testData) {
        // Excel files are typically read-only in test automation
        // This would require implementing write functionality
        TestLogManager.warning("Save operation not supported for Excel files in read-only mode");
        return false;
    }
    
    @Override
    public boolean updateTestData(TestData testData) {
        // Excel files are typically read-only in test automation
        TestLogManager.warning("Update operation not supported for Excel files in read-only mode");
        return false;
    }
    
    @Override
    public boolean deleteTestData(String testName) {
        // Excel files are typically read-only in test automation
        TestLogManager.warning("Delete operation not supported for Excel files in read-only mode");
        return false;
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
        if (enableCaching && cache != null) {
            cache.clear();
            return true;
        }
        return false;
    }
    
    private TestData loadTestDataFromExcel(String testName) {
        try {
            int rowCount = xlsReader.getRowCount(sheetName);
            
            for (int i = 2; i <= rowCount; i++) {
                String testMethodName = xlsReader.getCellData(sheetName, "TestMethodName", i);
                if (testName.equals(testMethodName)) {
                    return loadTestDataFromRow(i);
                }
            }
        } catch (Exception e) {
            TestLogManager.error("Error loading test data from Excel for: " + testName, e);
        }
        
        return null;
    }
    
    private TestData loadTestDataFromRow(int rowNumber) {
        try {
            // Get all column headers (assuming row 1 is header)
            Map<String, String> data = new HashMap<>();
            
            // Get all available columns and their data
            // This is a simplified implementation - you might want to make it more robust
            String testMethodName = xlsReader.getCellData(sheetName, "TestMethodName", rowNumber);
            if (testMethodName == null || testMethodName.trim().isEmpty()) {
                return null;
            }
            
            // Add common test data fields
            addDataIfExists(data, "TestMethodName", rowNumber);
            addDataIfExists(data, "TestDescription", rowNumber);
            addDataIfExists(data, "ExpectedResult", rowNumber);
            addDataIfExists(data, "TestData", rowNumber);
            addDataIfExists(data, "Environment", rowNumber);
            addDataIfExists(data, "Priority", rowNumber);
            addDataIfExists(data, "Category", rowNumber);
            
            // Add any other columns that might exist
            // You can extend this list based on your Excel structure
            
            return new TestData(testMethodName, data);
            
        } catch (Exception e) {
            TestLogManager.error("Error loading test data from row: " + rowNumber, e);
            return null;
        }
    }
    
    private void addDataIfExists(Map<String, String> data, String columnName, int rowNumber) {
        try {
            String value = xlsReader.getCellData(sheetName, columnName, rowNumber);
            if (value != null && !value.trim().isEmpty()) {
                data.put(columnName, value);
            }
        } catch (Exception e) {
            // Column might not exist, ignore
        }
    }
}
