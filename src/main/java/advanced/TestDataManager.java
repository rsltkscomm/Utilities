package advanced;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import reporting.TestLogManager;
import utils.CrossPlatformUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Pattern;

/**
 * Advanced test data management utilities for synthetic data generation, anonymization, and validation.
 */
public class TestDataManager {
    
    private final ObjectMapper objectMapper;
    private final String dataDirectory;
    private final Random random;
    
    public TestDataManager() {
        this.objectMapper = new ObjectMapper();
        this.dataDirectory = CrossPlatformUtils.getProjectDataDirectory()
                .resolve("test_data").toString();
        this.random = new Random();
        createDataDirectory();
    }
    
    /**
     * Generates synthetic test data for a given data type.
     * @param dataType Type of data to generate (user, product, order, etc.)
     * @param count Number of records to generate
     * @return List of generated test data
     */
    public List<Map<String, Object>> generateSyntheticData(String dataType, int count) {
        TestLogManager.info("Generating " + count + " synthetic " + dataType + " records");
        
        List<Map<String, Object>> syntheticData = new ArrayList<>();
        
        for (int i = 0; i < count; i++) {
            Map<String, Object> record = generateDataRecord(dataType);
            syntheticData.add(record);
        }
        
        // Save generated data
        saveGeneratedData(syntheticData, dataType);
        
        TestLogManager.success("Generated " + syntheticData.size() + " synthetic " + dataType + " records");
        return syntheticData;
    }
    
    /**
     * Anonymizes personal data in test data.
     * @param data Original test data
     * @return Anonymized test data
     */
    public Map<String, Object> anonymizePersonalData(Map<String, Object> data) {
        TestLogManager.info("Anonymizing personal data");
        
        Map<String, Object> anonymizedData = new HashMap<>(data);
        
        // Anonymize email addresses
        anonymizeEmails(anonymizedData);
        
        // Anonymize phone numbers
        anonymizePhoneNumbers(anonymizedData);
        
        // Anonymize names
        anonymizeNames(anonymizedData);
        
        // Anonymize addresses
        anonymizeAddresses(anonymizedData);
        
        // Anonymize credit card numbers
        anonymizeCreditCards(anonymizedData);
        
        // Anonymize SSNs
        anonymizeSSNs(anonymizedData);
        
        TestLogManager.success("Personal data anonymized successfully");
        return anonymizedData;
    }
    
    /**
     * Validates data quality of test data.
     * @param data Test data to validate
     * @return DataQualityReport with validation results
     */
    public DataQualityReport validateDataQuality(Map<String, Object> data) {
        TestLogManager.info("Validating data quality");
        
        DataQualityReport report = new DataQualityReport();
        report.setValidationTime(LocalDateTime.now());
        report.setTotalFields(data.size());
        
        // Check for null values
        checkNullValues(data, report);
        
        // Check for empty strings
        checkEmptyStrings(data, report);
        
        // Validate email formats
        validateEmailFormats(data, report);
        
        // Validate phone number formats
        validatePhoneFormats(data, report);
        
        // Validate date formats
        validateDateFormats(data, report);
        
        // Check data consistency
        checkDataConsistency(data, report);
        
        // Calculate quality score
        calculateQualityScore(report);
        
        TestLogManager.success("Data quality validation completed. Score: " + report.getQualityScore());
        return report;
    }
    
    /**
     * Generates test data from JSON schema.
     * @param schemaPath Path to JSON schema file
     * @param count Number of records to generate
     * @return List of generated test data
     */
    public List<Map<String, Object>> generateDataFromSchema(String schemaPath, int count) {
        TestLogManager.info("Generating test data from schema: " + schemaPath);
        
        try {
            JsonNode schema = objectMapper.readTree(new File(schemaPath));
            List<Map<String, Object>> generatedData = new ArrayList<>();
            
            for (int i = 0; i < count; i++) {
                Map<String, Object> record = generateRecordFromSchema(schema);
                generatedData.add(record);
            }
            
            saveGeneratedData(generatedData, "schema_generated");
            TestLogManager.success("Generated " + count + " records from schema");
            
            return generatedData;
            
        } catch (IOException e) {
            TestLogManager.error("Failed to generate data from schema", e);
            return new ArrayList<>();
        }
    }
    
    /**
     * Creates test data variations for boundary testing.
     * @param baseData Base test data
     * @return List of test data variations
     */
    public List<Map<String, Object>> createBoundaryTestData(Map<String, Object> baseData) {
        TestLogManager.info("Creating boundary test data variations");
        
        List<Map<String, Object>> variations = new ArrayList<>();
        
        // Add original data
        variations.add(new HashMap<>(baseData));
        
        // Create boundary variations
        for (String key : baseData.keySet()) {
            Object value = baseData.get(key);
            
            if (value instanceof String) {
                String strValue = (String) value;
                
                // Empty string
                Map<String, Object> emptyVariation = new HashMap<>(baseData);
                emptyVariation.put(key, "");
                variations.add(emptyVariation);
                
                // Very long string
                Map<String, Object> longVariation = new HashMap<>(baseData);
                longVariation.put(key, generateLongString(1000));
                variations.add(longVariation);
                
                // String with special characters
                Map<String, Object> specialVariation = new HashMap<>(baseData);
                specialVariation.put(key, "!@#$%^&*()_+-=[]{}|;':\",./<>?");
                variations.add(specialVariation);
                
            } else if (value instanceof Integer) {
                Integer intValue = (Integer) value;
                
                // Zero value
                Map<String, Object> zeroVariation = new HashMap<>(baseData);
                zeroVariation.put(key, 0);
                variations.add(zeroVariation);
                
                // Negative value
                Map<String, Object> negativeVariation = new HashMap<>(baseData);
                negativeVariation.put(key, -1);
                variations.add(negativeVariation);
                
                // Large value
                Map<String, Object> largeVariation = new HashMap<>(baseData);
                largeVariation.put(key, Integer.MAX_VALUE);
                variations.add(largeVariation);
                
            } else if (value instanceof Double) {
                Double doubleValue = (Double) value;
                
                // Zero value
                Map<String, Object> zeroVariation = new HashMap<>(baseData);
                zeroVariation.put(key, 0.0);
                variations.add(zeroVariation);
                
                // Negative value
                Map<String, Object> negativeVariation = new HashMap<>(baseData);
                negativeVariation.put(key, -1.0);
                variations.add(negativeVariation);
                
                // Very small value
                Map<String, Object> smallVariation = new HashMap<>(baseData);
                smallVariation.put(key, 0.000001);
                variations.add(smallVariation);
            }
        }
        
        saveGeneratedData(variations, "boundary_variations");
        TestLogManager.success("Created " + variations.size() + " boundary test data variations");
        
        return variations;
    }
    
    /**
     * Generates test data for different user personas.
     * @param personaType Type of user persona
     * @param count Number of records to generate
     * @return List of persona-based test data
     */
    public List<Map<String, Object>> generatePersonaBasedData(String personaType, int count) {
        TestLogManager.info("Generating " + count + " records for persona: " + personaType);
        
        List<Map<String, Object>> personaData = new ArrayList<>();
        
        for (int i = 0; i < count; i++) {
            Map<String, Object> record = generatePersonaRecord(personaType);
            personaData.add(record);
        }
        
        saveGeneratedData(personaData, "persona_" + personaType);
        TestLogManager.success("Generated " + personaData.size() + " persona-based records");
        
        return personaData;
    }
    
    /**
     * Creates test data for different environments.
     * @param environment Target environment (dev, test, staging, prod)
     * @param baseData Base test data
     * @return Environment-specific test data
     */
    public Map<String, Object> createEnvironmentSpecificData(String environment, Map<String, Object> baseData) {
        TestLogManager.info("Creating environment-specific data for: " + environment);
        
        Map<String, Object> envData = new HashMap<>(baseData);
        
        switch (environment.toLowerCase()) {
            case "dev":
                envData.put("environment", "development");
                envData.put("debug_mode", true);
                envData.put("log_level", "DEBUG");
                break;
                
            case "test":
                envData.put("environment", "testing");
                envData.put("debug_mode", true);
                envData.put("log_level", "INFO");
                break;
                
            case "staging":
                envData.put("environment", "staging");
                envData.put("debug_mode", false);
                envData.put("log_level", "WARN");
                break;
                
            case "prod":
                envData.put("environment", "production");
                envData.put("debug_mode", false);
                envData.put("log_level", "ERROR");
                break;
        }
        
        TestLogManager.success("Created environment-specific data for: " + environment);
        return envData;
    }
    
    /**
     * Generates test data with specific patterns.
     * @param pattern Pattern type (sequential, random, weighted)
     * @param dataType Data type to generate
     * @param count Number of records
     * @return List of pattern-based test data
     */
    public List<Map<String, Object>> generatePatternBasedData(String pattern, String dataType, int count) {
        TestLogManager.info("Generating " + count + " " + pattern + " pattern records for " + dataType);
        
        List<Map<String, Object>> patternData = new ArrayList<>();
        
        for (int i = 0; i < count; i++) {
            Map<String, Object> record = generatePatternRecord(pattern, dataType, i);
            patternData.add(record);
        }
        
        saveGeneratedData(patternData, pattern + "_" + dataType);
        TestLogManager.success("Generated " + patternData.size() + " pattern-based records");
        
        return patternData;
    }
    
    private Map<String, Object> generateDataRecord(String dataType) {
        Map<String, Object> record = new HashMap<>();
        
        switch (dataType.toLowerCase()) {
            case "user":
                record = generateUserRecord();
                break;
            case "product":
                record = generateProductRecord();
                break;
            case "order":
                record = generateOrderRecord();
                break;
            case "customer":
                record = generateCustomerRecord();
                break;
            case "employee":
                record = generateEmployeeRecord();
                break;
            default:
                record = generateGenericRecord();
        }
        
        return record;
    }
    
    private Map<String, Object> generateUserRecord() {
        Map<String, Object> user = new HashMap<>();
        user.put("id", generateId());
        user.put("username", generateUsername());
        user.put("email", generateEmail());
        user.put("firstName", generateFirstName());
        user.put("lastName", generateLastName());
        user.put("phone", generatePhoneNumber());
        user.put("dateOfBirth", generateDateOfBirth());
        user.put("address", generateAddress());
        user.put("city", generateCity());
        user.put("state", generateState());
        user.put("zipCode", generateZipCode());
        user.put("country", generateCountry());
        user.put("createdAt", LocalDateTime.now().toString());
        return user;
    }
    
    private Map<String, Object> generateProductRecord() {
        Map<String, Object> product = new HashMap<>();
        product.put("id", generateId());
        product.put("name", generateProductName());
        product.put("description", generateProductDescription());
        product.put("price", generatePrice());
        product.put("category", generateCategory());
        product.put("sku", generateSKU());
        product.put("stock", random.nextInt(1000));
        product.put("weight", generateWeight());
        product.put("dimensions", generateDimensions());
        product.put("createdAt", LocalDateTime.now().toString());
        return product;
    }
    
    private Map<String, Object> generateOrderRecord() {
        Map<String, Object> order = new HashMap<>();
        order.put("id", generateId());
        order.put("customerId", generateId());
        order.put("orderDate", LocalDate.now().toString());
        order.put("totalAmount", generatePrice());
        order.put("status", generateOrderStatus());
        order.put("shippingAddress", generateAddress());
        order.put("billingAddress", generateAddress());
        order.put("paymentMethod", generatePaymentMethod());
        order.put("items", generateOrderItems());
        return order;
    }
    
    private Map<String, Object> generateCustomerRecord() {
        Map<String, Object> customer = new HashMap<>();
        customer.put("id", generateId());
        customer.put("companyName", generateCompanyName());
        customer.put("contactPerson", generateFirstName() + " " + generateLastName());
        customer.put("email", generateEmail());
        customer.put("phone", generatePhoneNumber());
        customer.put("address", generateAddress());
        customer.put("industry", generateIndustry());
        customer.put("customerType", generateCustomerType());
        customer.put("creditLimit", generateCreditLimit());
        customer.put("createdAt", LocalDateTime.now().toString());
        return customer;
    }
    
    private Map<String, Object> generateEmployeeRecord() {
        Map<String, Object> employee = new HashMap<>();
        employee.put("id", generateId());
        employee.put("employeeId", "EMP" + String.format("%06d", random.nextInt(999999)));
        employee.put("firstName", generateFirstName());
        employee.put("lastName", generateLastName());
        employee.put("email", generateEmail());
        employee.put("phone", generatePhoneNumber());
        employee.put("department", generateDepartment());
        employee.put("position", generatePosition());
        employee.put("salary", generateSalary());
        employee.put("hireDate", generateHireDate());
        employee.put("managerId", generateId());
        return employee;
    }
    
    private Map<String, Object> generateGenericRecord() {
        Map<String, Object> record = new HashMap<>();
        record.put("id", generateId());
        record.put("name", generateRandomString(10));
        record.put("value", random.nextInt(1000));
        record.put("description", generateRandomString(50));
        record.put("active", random.nextBoolean());
        record.put("createdAt", LocalDateTime.now().toString());
        return record;
    }
    
    private Map<String, Object> generateRecordFromSchema(JsonNode schema) {
        Map<String, Object> record = new HashMap<>();
        
        JsonNode properties = schema.get("properties");
        if (properties != null) {
            properties.fieldNames().forEachRemaining(fieldName -> {
                JsonNode fieldSchema = properties.get(fieldName);
                Object value = generateValueFromFieldSchema(fieldSchema);
                record.put(fieldName, value);
            });
        }
        
        return record;
    }
    
    private Object generateValueFromFieldSchema(JsonNode fieldSchema) {
        String type = fieldSchema.get("type").asText();
        
        switch (type) {
            case "string":
                return generateStringFromSchema(fieldSchema);
            case "integer":
                return generateIntegerFromSchema(fieldSchema);
            case "number":
                return generateNumberFromSchema(fieldSchema);
            case "boolean":
                return random.nextBoolean();
            case "array":
                return generateArrayFromSchema(fieldSchema);
            default:
                return generateRandomString(10);
        }
    }
    
    private String generateStringFromSchema(JsonNode fieldSchema) {
        if (fieldSchema.has("enum")) {
            JsonNode enumValues = fieldSchema.get("enum");
            return enumValues.get(random.nextInt(enumValues.size())).asText();
        }
        
        int minLength = fieldSchema.has("minLength") ? fieldSchema.get("minLength").asInt() : 1;
        int maxLength = fieldSchema.has("maxLength") ? fieldSchema.get("maxLength").asInt() : 50;
        int length = minLength + random.nextInt(maxLength - minLength + 1);
        
        return generateRandomString(length);
    }
    
    private Integer generateIntegerFromSchema(JsonNode fieldSchema) {
        int minimum = fieldSchema.has("minimum") ? fieldSchema.get("minimum").asInt() : 0;
        int maximum = fieldSchema.has("maximum") ? fieldSchema.get("maximum").asInt() : 1000;
        return minimum + random.nextInt(maximum - minimum + 1);
    }
    
    private Double generateNumberFromSchema(JsonNode fieldSchema) {
        double minimum = fieldSchema.has("minimum") ? fieldSchema.get("minimum").asDouble() : 0.0;
        double maximum = fieldSchema.has("maximum") ? fieldSchema.get("maximum").asDouble() : 1000.0;
        return minimum + random.nextDouble() * (maximum - minimum);
    }
    
    private List<Object> generateArrayFromSchema(JsonNode fieldSchema) {
        List<Object> array = new ArrayList<>();
        int minItems = fieldSchema.has("minItems") ? fieldSchema.get("minItems").asInt() : 1;
        int maxItems = fieldSchema.has("maxItems") ? fieldSchema.get("maxItems").asInt() : 5;
        int itemCount = minItems + random.nextInt(maxItems - minItems + 1);
        
        JsonNode itemsSchema = fieldSchema.get("items");
        for (int i = 0; i < itemCount; i++) {
            array.add(generateValueFromFieldSchema(itemsSchema));
        }
        
        return array;
    }
    
    private Map<String, Object> generatePersonaRecord(String personaType) {
        Map<String, Object> record = new HashMap<>();
        
        switch (personaType.toLowerCase()) {
            case "premium_customer":
                record = generatePremiumCustomerRecord();
                break;
            case "budget_customer":
                record = generateBudgetCustomerRecord();
                break;
            case "business_customer":
                record = generateBusinessCustomerRecord();
                break;
            case "new_user":
                record = generateNewUserRecord();
                break;
            case "power_user":
                record = generatePowerUserRecord();
                break;
            default:
                record = generateUserRecord();
        }
        
        return record;
    }
    
    private Map<String, Object> generatePremiumCustomerRecord() {
        Map<String, Object> customer = generateUserRecord();
        customer.put("customerTier", "Premium");
        customer.put("loyaltyPoints", random.nextInt(10000));
        customer.put("totalSpent", 1000 + random.nextInt(9000));
        customer.put("preferredPayment", "Credit Card");
        return customer;
    }
    
    private Map<String, Object> generateBudgetCustomerRecord() {
        Map<String, Object> customer = generateUserRecord();
        customer.put("customerTier", "Budget");
        customer.put("loyaltyPoints", random.nextInt(1000));
        customer.put("totalSpent", random.nextInt(500));
        customer.put("preferredPayment", "Debit Card");
        return customer;
    }
    
    private Map<String, Object> generateBusinessCustomerRecord() {
        Map<String, Object> customer = generateCustomerRecord();
        customer.put("customerTier", "Business");
        customer.put("companySize", generateCompanySize());
        customer.put("annualRevenue", generateAnnualRevenue());
        customer.put("contractType", generateContractType());
        return customer;
    }
    
    private Map<String, Object> generateNewUserRecord() {
        Map<String, Object> user = generateUserRecord();
        user.put("registrationDate", LocalDate.now().toString());
        user.put("lastLogin", null);
        user.put("profileComplete", false);
        user.put("emailVerified", false);
        return user;
    }
    
    private Map<String, Object> generatePowerUserRecord() {
        Map<String, Object> user = generateUserRecord();
        user.put("registrationDate", LocalDate.now().minusDays(random.nextInt(365)).toString());
        user.put("lastLogin", LocalDateTime.now().minusHours(random.nextInt(24)).toString());
        user.put("profileComplete", true);
        user.put("emailVerified", true);
        user.put("loginCount", 100 + random.nextInt(900));
        return user;
    }
    
    private Map<String, Object> generatePatternRecord(String pattern, String dataType, int index) {
        Map<String, Object> record = generateDataRecord(dataType);
        
        switch (pattern.toLowerCase()) {
            case "sequential":
                record.put("sequenceNumber", index + 1);
                record.put("id", "SEQ" + String.format("%06d", index + 1));
                break;
                
            case "random":
                record.put("randomSeed", random.nextInt());
                break;
                
            case "weighted":
                record.put("weight", random.nextDouble());
                break;
        }
        
        return record;
    }
    
    private void anonymizeEmails(Map<String, Object> data) {
        for (String key : data.keySet()) {
            Object value = data.get(key);
            if (value instanceof String && isEmail((String) value)) {
                data.put(key, "user" + random.nextInt(10000) + "@example.com");
            }
        }
    }
    
    private void anonymizePhoneNumbers(Map<String, Object> data) {
        for (String key : data.keySet()) {
            Object value = data.get(key);
            if (value instanceof String && isPhoneNumber((String) value)) {
                data.put(key, "555-" + String.format("%03d", random.nextInt(1000)) + "-" + String.format("%04d", random.nextInt(10000)));
            }
        }
    }
    
    private void anonymizeNames(Map<String, Object> data) {
        String[] firstNames = {"John", "Jane", "Mike", "Sarah", "David", "Lisa"};
        String[] lastNames = {"Smith", "Johnson", "Williams", "Brown", "Jones", "Garcia"};
        
        for (String key : data.keySet()) {
            if (key.toLowerCase().contains("name") || key.toLowerCase().contains("first") || key.toLowerCase().contains("last")) {
                if (key.toLowerCase().contains("first")) {
                    data.put(key, firstNames[random.nextInt(firstNames.length)]);
                } else if (key.toLowerCase().contains("last")) {
                    data.put(key, lastNames[random.nextInt(lastNames.length)]);
                } else {
                    data.put(key, firstNames[random.nextInt(firstNames.length)] + " " + lastNames[random.nextInt(lastNames.length)]);
                }
            }
        }
    }
    
    private void anonymizeAddresses(Map<String, Object> data) {
        for (String key : data.keySet()) {
            if (key.toLowerCase().contains("address")) {
                data.put(key, "123 Anonymized Street, Anonymized City, AC 12345");
            }
        }
    }
    
    private void anonymizeCreditCards(Map<String, Object> data) {
        for (String key : data.keySet()) {
            Object value = data.get(key);
            if (value instanceof String && isCreditCard((String) value)) {
                data.put(key, "****-****-****-" + String.format("%04d", random.nextInt(10000)));
            }
        }
    }
    
    private void anonymizeSSNs(Map<String, Object> data) {
        for (String key : data.keySet()) {
            Object value = data.get(key);
            if (value instanceof String && isSSN((String) value)) {
                data.put(key, "***-**-" + String.format("%04d", random.nextInt(10000)));
            }
        }
    }
    
    private void checkNullValues(Map<String, Object> data, DataQualityReport report) {
        int nullCount = 0;
        for (Object value : data.values()) {
            if (value == null) {
                nullCount++;
            }
        }
        report.setNullValues(nullCount);
    }
    
    private void checkEmptyStrings(Map<String, Object> data, DataQualityReport report) {
        int emptyCount = 0;
        for (Object value : data.values()) {
            if (value instanceof String && ((String) value).trim().isEmpty()) {
                emptyCount++;
            }
        }
        report.setEmptyStrings(emptyCount);
    }
    
    private void validateEmailFormats(Map<String, Object> data, DataQualityReport report) {
        int invalidEmails = 0;
        for (Object value : data.values()) {
            if (value instanceof String && isEmail((String) value) && !isValidEmail((String) value)) {
                invalidEmails++;
            }
        }
        report.setInvalidEmails(invalidEmails);
    }
    
    private void validatePhoneFormats(Map<String, Object> data, DataQualityReport report) {
        int invalidPhones = 0;
        for (Object value : data.values()) {
            if (value instanceof String && isPhoneNumber((String) value) && !isValidPhoneNumber((String) value)) {
                invalidPhones++;
            }
        }
        report.setInvalidPhones(invalidPhones);
    }
    
    private void validateDateFormats(Map<String, Object> data, DataQualityReport report) {
        int invalidDates = 0;
        for (Object value : data.values()) {
            if (value instanceof String && isDate((String) value) && !isValidDate((String) value)) {
                invalidDates++;
            }
        }
        report.setInvalidDates(invalidDates);
    }
    
    private void checkDataConsistency(Map<String, Object> data, DataQualityReport report) {
        // Check for data consistency issues
        int consistencyIssues = 0;
        
        // Example: Check if email and username are consistent
        if (data.containsKey("email") && data.containsKey("username")) {
            String email = (String) data.get("email");
            String username = (String) data.get("username");
            if (email != null && username != null && !email.contains(username)) {
                consistencyIssues++;
            }
        }
        
        report.setConsistencyIssues(consistencyIssues);
    }
    
    private void calculateQualityScore(DataQualityReport report) {
        int totalIssues = report.getNullValues() + report.getEmptyStrings() + 
                         report.getInvalidEmails() + report.getInvalidPhones() + 
                         report.getInvalidDates() + report.getConsistencyIssues();
        
        double qualityScore = Math.max(0, 100 - (totalIssues * 10));
        report.setQualityScore(qualityScore);
    }
    
    private void saveGeneratedData(List<Map<String, Object>> data, String dataType) {
        try {
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            String fileName = dataType + "_" + timestamp + ".json";
            Path filePath = Paths.get(dataDirectory, fileName);
            
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(filePath.toFile(), data);
            TestLogManager.info("Generated data saved to: " + filePath);
            
        } catch (IOException e) {
            TestLogManager.error("Failed to save generated data", e);
        }
    }
    
    private void createDataDirectory() {
        try {
            Path dir = Paths.get(dataDirectory);
            if (!Files.exists(dir)) {
                Files.createDirectories(dir);
                TestLogManager.info("Created test data directory: " + dataDirectory);
            }
        } catch (IOException e) {
            TestLogManager.error("Failed to create test data directory", e);
        }
    }
    
    // Helper methods for data generation
    private String generateId() {
        return "ID" + String.format("%08d", random.nextInt(99999999));
    }
    
    private String generateUsername() {
        return "user" + random.nextInt(10000);
    }
    
    private String generateEmail() {
        return "user" + random.nextInt(10000) + "@example.com";
    }
    
    private String generateFirstName() {
        String[] names = {"John", "Jane", "Mike", "Sarah", "David", "Lisa", "Chris", "Amy", "Tom", "Emma"};
        return names[random.nextInt(names.length)];
    }
    
    private String generateLastName() {
        String[] names = {"Smith", "Johnson", "Williams", "Brown", "Jones", "Garcia", "Miller", "Davis", "Rodriguez", "Martinez"};
        return names[random.nextInt(names.length)];
    }
    
    private String generatePhoneNumber() {
        return "555-" + String.format("%03d", random.nextInt(1000)) + "-" + String.format("%04d", random.nextInt(10000));
    }
    
    private String generateDateOfBirth() {
        return LocalDate.now().minusYears(18 + random.nextInt(50)).toString();
    }
    
    private String generateAddress() {
        return (random.nextInt(9999) + 1) + " " + generateRandomString(10) + " Street";
    }
    
    private String generateCity() {
        String[] cities = {"New York", "Los Angeles", "Chicago", "Houston", "Phoenix", "Philadelphia", "San Antonio", "San Diego", "Dallas", "San Jose"};
        return cities[random.nextInt(cities.length)];
    }
    
    private String generateState() {
        String[] states = {"NY", "CA", "TX", "FL", "IL", "PA", "OH", "GA", "NC", "MI"};
        return states[random.nextInt(states.length)];
    }
    
    private String generateZipCode() {
        return String.format("%05d", random.nextInt(99999));
    }
    
    private String generateCountry() {
        return "United States";
    }
    
    private String generateProductName() {
        String[] products = {"Laptop", "Smartphone", "Tablet", "Headphones", "Camera", "Watch", "Speaker", "Keyboard", "Mouse", "Monitor"};
        return products[random.nextInt(products.length)] + " " + (random.nextInt(999) + 1);
    }
    
    private String generateProductDescription() {
        return "High-quality " + generateRandomString(20) + " product with excellent features";
    }
    
    private Double generatePrice() {
        return Math.round((10 + random.nextDouble() * 990) * 100.0) / 100.0;
    }
    
    private String generateCategory() {
        String[] categories = {"Electronics", "Clothing", "Books", "Home", "Sports", "Beauty", "Toys", "Automotive", "Health", "Garden"};
        return categories[random.nextInt(categories.length)];
    }
    
    private String generateSKU() {
        return "SKU" + String.format("%06d", random.nextInt(999999));
    }
    
    private Double generateWeight() {
        return Math.round((0.1 + random.nextDouble() * 10) * 100.0) / 100.0;
    }
    
    private String generateDimensions() {
        return (random.nextInt(50) + 1) + "x" + (random.nextInt(50) + 1) + "x" + (random.nextInt(20) + 1);
    }
    
    private String generateOrderStatus() {
        String[] statuses = {"Pending", "Processing", "Shipped", "Delivered", "Cancelled"};
        return statuses[random.nextInt(statuses.length)];
    }
    
    private String generatePaymentMethod() {
        String[] methods = {"Credit Card", "Debit Card", "PayPal", "Bank Transfer", "Cash"};
        return methods[random.nextInt(methods.length)];
    }
    
    private List<Map<String, Object>> generateOrderItems() {
        List<Map<String, Object>> items = new ArrayList<>();
        int itemCount = 1 + random.nextInt(5);
        
        for (int i = 0; i < itemCount; i++) {
            Map<String, Object> item = new HashMap<>();
            item.put("productId", generateId());
            item.put("quantity", 1 + random.nextInt(5));
            item.put("price", generatePrice());
            items.add(item);
        }
        
        return items;
    }
    
    private String generateCompanyName() {
        String[] companies = {"TechCorp", "InnovateLabs", "DataSystems", "CloudTech", "FutureSoft", "NextGen", "SmartSolutions", "ProTech", "EliteCorp", "PrimeTech"};
        return companies[random.nextInt(companies.length)];
    }
    
    private String generateIndustry() {
        String[] industries = {"Technology", "Healthcare", "Finance", "Education", "Retail", "Manufacturing", "Real Estate", "Transportation", "Energy", "Media"};
        return industries[random.nextInt(industries.length)];
    }
    
    private String generateCustomerType() {
        String[] types = {"Individual", "Small Business", "Enterprise", "Government", "Non-Profit"};
        return types[random.nextInt(types.length)];
    }
    
    private Double generateCreditLimit() {
        return Math.round((1000 + random.nextDouble() * 9000) * 100.0) / 100.0;
    }
    
    private String generateDepartment() {
        String[] departments = {"Engineering", "Sales", "Marketing", "HR", "Finance", "Operations", "Support", "Product", "Design", "Legal"};
        return departments[random.nextInt(departments.length)];
    }
    
    private String generatePosition() {
        String[] positions = {"Manager", "Developer", "Analyst", "Specialist", "Coordinator", "Director", "Consultant", "Engineer", "Designer", "Administrator"};
        return positions[random.nextInt(positions.length)];
    }
    
    private Double generateSalary() {
        return Math.round((30000 + random.nextDouble() * 70000) * 100.0) / 100.0;
    }
    
    private String generateHireDate() {
        return LocalDate.now().minusDays(random.nextInt(365 * 5)).toString();
    }
    
    private String generateCompanySize() {
        String[] sizes = {"1-10", "11-50", "51-200", "201-500", "501-1000", "1000+"};
        return sizes[random.nextInt(sizes.length)];
    }
    
    private Double generateAnnualRevenue() {
        return Math.round((100000 + random.nextDouble() * 9000000) * 100.0) / 100.0;
    }
    
    private String generateContractType() {
        String[] types = {"Monthly", "Quarterly", "Annual", "Multi-year"};
        return types[random.nextInt(types.length)];
    }
    
    private String generateLongString(int length) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length; i++) {
            sb.append((char) ('a' + random.nextInt(26)));
        }
        return sb.toString();
    }
    
    private String generateRandomString(int length) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length; i++) {
            sb.append((char) ('a' + random.nextInt(26)));
        }
        return sb.toString();
    }
    
    // Validation helper methods
    private boolean isEmail(String value) {
        return value.contains("@");
    }
    
    private boolean isValidEmail(String email) {
        String emailRegex = "^[a-zA-Z0-9_+&*-]+(?:\\.[a-zA-Z0-9_+&*-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,7}$";
        return Pattern.matches(emailRegex, email);
    }
    
    private boolean isPhoneNumber(String value) {
        return value.matches(".*\\d{3}.*\\d{3}.*\\d{4}.*");
    }
    
    private boolean isValidPhoneNumber(String phone) {
        String phoneRegex = "^\\(?([0-9]{3})\\)?[-. ]?([0-9]{3})[-. ]?([0-9]{4})$";
        return Pattern.matches(phoneRegex, phone);
    }
    
    private boolean isCreditCard(String value) {
        return value.matches(".*\\d{4}.*\\d{4}.*\\d{4}.*\\d{4}.*");
    }
    
    private boolean isSSN(String value) {
        return value.matches(".*\\d{3}.*\\d{2}.*\\d{4}.*");
    }
    
    private boolean isDate(String value) {
        return value.matches(".*\\d{4}-\\d{2}-\\d{2}.*");
    }
    
    private boolean isValidDate(String date) {
        try {
            LocalDate.parse(date);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Data quality report data model.
     */
    public static class DataQualityReport {
        private LocalDateTime validationTime;
        private int totalFields;
        private int nullValues;
        private int emptyStrings;
        private int invalidEmails;
        private int invalidPhones;
        private int invalidDates;
        private int consistencyIssues;
        private double qualityScore;
        
        // Getters and setters
        public LocalDateTime getValidationTime() { return validationTime; }
        public void setValidationTime(LocalDateTime validationTime) { this.validationTime = validationTime; }
        
        public int getTotalFields() { return totalFields; }
        public void setTotalFields(int totalFields) { this.totalFields = totalFields; }
        
        public int getNullValues() { return nullValues; }
        public void setNullValues(int nullValues) { this.nullValues = nullValues; }
        
        public int getEmptyStrings() { return emptyStrings; }
        public void setEmptyStrings(int emptyStrings) { this.emptyStrings = emptyStrings; }
        
        public int getInvalidEmails() { return invalidEmails; }
        public void setInvalidEmails(int invalidEmails) { this.invalidEmails = invalidEmails; }
        
        public int getInvalidPhones() { return invalidPhones; }
        public void setInvalidPhones(int invalidPhones) { this.invalidPhones = invalidPhones; }
        
        public int getInvalidDates() { return invalidDates; }
        public void setInvalidDates(int invalidDates) { this.invalidDates = invalidDates; }
        
        public int getConsistencyIssues() { return consistencyIssues; }
        public void setConsistencyIssues(int consistencyIssues) { this.consistencyIssues = consistencyIssues; }
        
        public double getQualityScore() { return qualityScore; }
        public void setQualityScore(double qualityScore) { this.qualityScore = qualityScore; }
    }
}

