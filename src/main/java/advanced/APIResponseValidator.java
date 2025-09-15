package advanced;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;
import com.networknt.schema.ValidationMessage;
import io.restassured.response.Response;
import reporting.TestLogManager;
import utils.CrossPlatformUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Pattern;

/**
 * Advanced API response validation utilities with comprehensive validation capabilities.
 */
public class APIResponseValidator {
    
    private final ObjectMapper objectMapper;
    private final JsonSchemaFactory schemaFactory;
    private final String reportDirectory;
    
    public APIResponseValidator() {
        this.objectMapper = new ObjectMapper();
        this.schemaFactory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V7);
        this.reportDirectory = CrossPlatformUtils.getProjectDataDirectory()
                .resolve("api_validation_reports").toString();
        createReportDirectory();
    }
    
    /**
     * Validates API response structure and content.
     * @param response API response to validate
     * @param validationRules Validation rules to apply
     * @return ResponseValidationResult with validation details
     */
    public ResponseValidationResult validateResponse(Response response, ResponseValidationRules validationRules) {
        TestLogManager.info("Validating API response with comprehensive rules");
        
        ResponseValidationResult result = new ResponseValidationResult();
        result.setResponseCode(response.getStatusCode());
        result.setValidationTime(LocalDateTime.now());
        result.setValidationRules(validationRules);
        
        try {
            // Parse response body
            JsonNode responseNode = objectMapper.readTree(response.getBody().asString());
            result.setResponseBody(responseNode);
            
            // Perform various validations
            validateStatusCode(response, validationRules, result);
            validateResponseStructure(responseNode, validationRules, result);
            validateDataTypes(responseNode, validationRules, result);
            validateFieldPresence(responseNode, validationRules, result);
            validateFieldValues(responseNode, validationRules, result);
            validateResponseTime(response, validationRules, result);
            validateHeaders(response, validationRules, result);
            validateContentType(response, validationRules, result);
            
            // Calculate overall validation result
            result.setValid(result.getValidationErrors().isEmpty());
            
            if (result.isValid()) {
                TestLogManager.success("API response validation passed");
            } else {
                TestLogManager.warning("API response validation failed with " + result.getValidationErrors().size() + " errors");
            }
            
        } catch (IOException e) {
            TestLogManager.error("Failed to parse API response", e);
            result.setValid(false);
            result.addValidationError("Response parsing error: " + e.getMessage());
        }
        
        return result;
    }
    
    /**
     * Validates API response against JSON schema with detailed error reporting.
     * @param response API response to validate
     * @param schemaPath Path to JSON schema file
     * @return SchemaValidationResult with detailed schema validation
     */
    public SchemaValidationResult validateResponseSchema(Response response, String schemaPath) {
        TestLogManager.info("Validating API response against JSON schema: " + schemaPath);
        
        SchemaValidationResult result = new SchemaValidationResult();
        result.setResponseCode(response.getStatusCode());
        result.setSchemaPath(schemaPath);
        result.setValidationTime(LocalDateTime.now());
        
        try {
            // Load JSON schema
            JsonSchema schema = schemaFactory.getSchema(new FileInputStream(schemaPath));
            
            // Parse response body
            JsonNode responseNode = objectMapper.readTree(response.getBody().asString());
            
            // Validate response against schema
            Set<ValidationMessage> validationMessages = schema.validate(responseNode);
            
            result.setValid(validationMessages.isEmpty());
            result.setValidationMessages(new ArrayList<>(validationMessages));
            
            // Categorize validation messages
            categorizeValidationMessages(validationMessages, result);
            
            if (result.isValid()) {
                TestLogManager.success("Schema validation passed");
            } else {
                TestLogManager.warning("Schema validation failed with " + validationMessages.size() + " errors");
            }
            
        } catch (IOException e) {
            TestLogManager.error("Failed to validate response schema", e);
            result.setValid(false);
            result.setErrorMessage("Schema validation error: " + e.getMessage());
        }
        
        return result;
    }
    
    /**
     * Validates API response data integrity and consistency.
     * @param response API response to validate
     * @param integrityRules Data integrity rules
     * @return DataIntegrityResult with integrity validation details
     */
    public DataIntegrityResult validateDataIntegrity(Response response, DataIntegrityRules integrityRules) {
        TestLogManager.info("Validating API response data integrity");
        
        DataIntegrityResult result = new DataIntegrityResult();
        result.setResponseCode(response.getStatusCode());
        result.setValidationTime(LocalDateTime.now());
        result.setIntegrityRules(integrityRules);
        
        try {
            JsonNode responseNode = objectMapper.readTree(response.getBody().asString());
            
            // Validate data consistency
            validateDataConsistency(responseNode, integrityRules, result);
            
            // Validate business rules
            validateBusinessRules(responseNode, integrityRules, result);
            
            // Validate referential integrity
            validateReferentialIntegrity(responseNode, integrityRules, result);
            
            // Validate data format consistency
            validateDataFormatConsistency(responseNode, integrityRules, result);
            
            result.setValid(result.getIntegrityIssues().isEmpty());
            
        } catch (IOException e) {
            TestLogManager.error("Failed to validate data integrity", e);
            result.setValid(false);
            result.addIntegrityIssue("Data integrity validation error: " + e.getMessage());
        }
        
        return result;
    }
    
    /**
     * Validates API response performance characteristics.
     * @param response API response to validate
     * @param performanceRules Performance validation rules
     * @return PerformanceValidationResult with performance validation details
     */
    public PerformanceValidationResult validateResponsePerformance(Response response, PerformanceValidationRules performanceRules) {
        TestLogManager.info("Validating API response performance");
        
        PerformanceValidationResult result = new PerformanceValidationResult();
        result.setResponseCode(response.getStatusCode());
        result.setValidationTime(LocalDateTime.now());
        result.setPerformanceRules(performanceRules);
        
        // Validate response time
        long responseTime = response.getTime();
        result.setResponseTime(responseTime);
        
        if (responseTime > performanceRules.getMaxResponseTime()) {
            result.addPerformanceIssue("Response time " + responseTime + "ms exceeds maximum allowed " + performanceRules.getMaxResponseTime() + "ms");
        }
        
        // Validate response size
        int responseSize = response.getBody().asString().length();
        result.setResponseSize(responseSize);
        
        if (responseSize > performanceRules.getMaxResponseSize()) {
            result.addPerformanceIssue("Response size " + responseSize + " bytes exceeds maximum allowed " + performanceRules.getMaxResponseSize() + " bytes");
        }
        
        // Validate header count
        int headerCount = response.getHeaders().size();
        result.setHeaderCount(headerCount);
        
        if (headerCount > performanceRules.getMaxHeaderCount()) {
            result.addPerformanceIssue("Header count " + headerCount + " exceeds maximum allowed " + performanceRules.getMaxHeaderCount());
        }
        
        result.setValid(result.getPerformanceIssues().isEmpty());
        
        return result;
    }
    
    /**
     * Validates API response security characteristics.
     * @param response API response to validate
     * @param securityRules Security validation rules
     * @return SecurityValidationResult with security validation details
     */
    public SecurityValidationResult validateResponseSecurity(Response response, SecurityValidationRules securityRules) {
        TestLogManager.info("Validating API response security");
        
        SecurityValidationResult result = new SecurityValidationResult();
        result.setResponseCode(response.getStatusCode());
        result.setValidationTime(LocalDateTime.now());
        result.setSecurityRules(securityRules);
        
        // Validate security headers
        validateSecurityHeaders(response, securityRules, result);
        
        // Validate sensitive data exposure
        validateSensitiveDataExposure(response, securityRules, result);
        
        // Validate CORS configuration
        validateCORSConfiguration(response, securityRules, result);
        
        // Validate content security
        validateContentSecurity(response, securityRules, result);
        
        result.setValid(result.getSecurityIssues().isEmpty());
        
        return result;
    }
    
    private void validateStatusCode(Response response, ResponseValidationRules rules, ResponseValidationResult result) {
        if (rules.getExpectedStatusCode() != null && response.getStatusCode() != rules.getExpectedStatusCode()) {
            result.addValidationError("Expected status code " + rules.getExpectedStatusCode() + 
                " but got " + response.getStatusCode());
        }
        
        if (rules.getValidStatusCodes() != null && !rules.getValidStatusCodes().contains(response.getStatusCode())) {
            result.addValidationError("Status code " + response.getStatusCode() + " is not in valid range: " + rules.getValidStatusCodes());
        }
    }
    
    private void validateResponseStructure(JsonNode responseNode, ResponseValidationRules rules, ResponseValidationResult result) {
        if (rules.getRequiredFields() != null) {
            for (String field : rules.getRequiredFields()) {
                if (!responseNode.has(field)) {
                    result.addValidationError("Required field '" + field + "' is missing");
                }
            }
        }
        
        if (rules.getForbiddenFields() != null) {
            for (String field : rules.getForbiddenFields()) {
                if (responseNode.has(field)) {
                    result.addValidationError("Forbidden field '" + field + "' is present");
                }
            }
        }
    }
    
    private void validateDataTypes(JsonNode responseNode, ResponseValidationRules rules, ResponseValidationResult result) {
        if (rules.getFieldTypes() != null) {
            for (Map.Entry<String, String> entry : rules.getFieldTypes().entrySet()) {
                String fieldName = entry.getKey();
                String expectedType = entry.getValue();
                
                if (responseNode.has(fieldName)) {
                    JsonNode fieldNode = responseNode.get(fieldName);
                    String actualType = getJsonNodeType(fieldNode);
                    
                    if (!actualType.equals(expectedType)) {
                        result.addValidationError("Field '" + fieldName + "' expected type " + expectedType + 
                            " but got " + actualType);
                    }
                }
            }
        }
    }
    
    private void validateFieldPresence(JsonNode responseNode, ResponseValidationRules rules, ResponseValidationResult result) {
        if (rules.getOptionalFields() != null) {
            for (String field : rules.getOptionalFields()) {
                if (responseNode.has(field)) {
                    result.addFieldPresenceInfo("Optional field '" + field + "' is present");
                }
            }
        }
    }
    
    private void validateFieldValues(JsonNode responseNode, ResponseValidationRules rules, ResponseValidationResult result) {
        if (rules.getFieldValueRules() != null) {
            for (Map.Entry<String, FieldValueRule> entry : rules.getFieldValueRules().entrySet()) {
                String fieldName = entry.getKey();
                FieldValueRule rule = entry.getValue();
                
                if (responseNode.has(fieldName)) {
                    JsonNode fieldNode = responseNode.get(fieldName);
                    validateFieldValue(fieldNode, fieldName, rule, result);
                }
            }
        }
    }
    
    private void validateFieldValue(JsonNode fieldNode, String fieldName, FieldValueRule rule, ResponseValidationResult result) {
        if (rule.getMinLength() != null && fieldNode.isTextual()) {
            String value = fieldNode.asText();
            if (value.length() < rule.getMinLength()) {
                result.addValidationError("Field '" + fieldName + "' length " + value.length() + 
                    " is less than minimum " + rule.getMinLength());
            }
        }
        
        if (rule.getMaxLength() != null && fieldNode.isTextual()) {
            String value = fieldNode.asText();
            if (value.length() > rule.getMaxLength()) {
                result.addValidationError("Field '" + fieldName + "' length " + value.length() + 
                    " is greater than maximum " + rule.getMaxLength());
            }
        }
        
        if (rule.getMinValue() != null && fieldNode.isNumber()) {
            double value = fieldNode.asDouble();
            if (value < rule.getMinValue()) {
                result.addValidationError("Field '" + fieldName + "' value " + value + 
                    " is less than minimum " + rule.getMinValue());
            }
        }
        
        if (rule.getMaxValue() != null && fieldNode.isNumber()) {
            double value = fieldNode.asDouble();
            if (value > rule.getMaxValue()) {
                result.addValidationError("Field '" + fieldName + "' value " + value + 
                    " is greater than maximum " + rule.getMaxValue());
            }
        }
        
        if (rule.getPattern() != null && fieldNode.isTextual()) {
            String value = fieldNode.asText();
            if (!Pattern.matches(rule.getPattern(), value)) {
                result.addValidationError("Field '" + fieldName + "' value '" + value + 
                    "' does not match pattern " + rule.getPattern());
            }
        }
        
        if (rule.getAllowedValues() != null && fieldNode.isTextual()) {
            String value = fieldNode.asText();
            if (!rule.getAllowedValues().contains(value)) {
                result.addValidationError("Field '" + fieldName + "' value '" + value + 
                    "' is not in allowed values " + rule.getAllowedValues());
            }
        }
    }
    
    private void validateResponseTime(Response response, ResponseValidationRules rules, ResponseValidationResult result) {
        if (rules.getMaxResponseTime() != null) {
            long responseTime = response.getTime();
            if (responseTime > rules.getMaxResponseTime()) {
                result.addValidationError("Response time " + responseTime + "ms exceeds maximum " + rules.getMaxResponseTime() + "ms");
            }
        }
    }
    
    private void validateHeaders(Response response, ResponseValidationRules rules, ResponseValidationResult result) {
        if (rules.getRequiredHeaders() != null) {
            for (String header : rules.getRequiredHeaders()) {
                if (!response.getHeaders().hasHeaderWithName(header)) {
                    result.addValidationError("Required header '" + header + "' is missing");
                }
            }
        }
        
        if (rules.getForbiddenHeaders() != null) {
            for (String header : rules.getForbiddenHeaders()) {
                if (response.getHeaders().hasHeaderWithName(header)) {
                    result.addValidationError("Forbidden header '" + header + "' is present");
                }
            }
        }
    }
    
    private void validateContentType(Response response, ResponseValidationRules rules, ResponseValidationResult result) {
        if (rules.getExpectedContentType() != null) {
            String contentType = response.getContentType();
            if (!contentType.contains(rules.getExpectedContentType())) {
                result.addValidationError("Expected content type '" + rules.getExpectedContentType() + 
                    "' but got '" + contentType + "'");
            }
        }
    }
    
    private void categorizeValidationMessages(Set<ValidationMessage> messages, SchemaValidationResult result) {
        for (ValidationMessage message : messages) {
            String messageType = message.getType();
            switch (messageType) {
                case "required":
                    result.addRequiredFieldError(message.getMessage());
                    break;
                case "type":
                    result.addTypeError(message.getMessage());
                    break;
                case "format":
                    result.addFormatError(message.getMessage());
                    break;
                case "pattern":
                    result.addPatternError(message.getMessage());
                    break;
                case "minimum":
                case "maximum":
                    result.addRangeError(message.getMessage());
                    break;
                default:
                    result.addOtherError(message.getMessage());
                    break;
            }
        }
    }
    
    private void validateDataConsistency(JsonNode responseNode, DataIntegrityRules rules, DataIntegrityResult result) {
        if (rules.getConsistencyRules() != null) {
            for (ConsistencyRule rule : rules.getConsistencyRules()) {
                if (!validateConsistencyRule(responseNode, rule)) {
                    result.addIntegrityIssue("Data consistency rule violated: " + rule.getDescription());
                }
            }
        }
    }
    
    private void validateBusinessRules(JsonNode responseNode, DataIntegrityRules rules, DataIntegrityResult result) {
        if (rules.getBusinessRules() != null) {
            for (BusinessRule rule : rules.getBusinessRules()) {
                if (!validateBusinessRule(responseNode, rule)) {
                    result.addIntegrityIssue("Business rule violated: " + rule.getDescription());
                }
            }
        }
    }
    
    private void validateReferentialIntegrity(JsonNode responseNode, DataIntegrityRules rules, DataIntegrityResult result) {
        if (rules.getReferentialIntegrityRules() != null) {
            for (ReferentialIntegrityRule rule : rules.getReferentialIntegrityRules()) {
                if (!validateReferentialIntegrityRule(responseNode, rule)) {
                    result.addIntegrityIssue("Referential integrity rule violated: " + rule.getDescription());
                }
            }
        }
    }
    
    private void validateDataFormatConsistency(JsonNode responseNode, DataIntegrityRules rules, DataIntegrityResult result) {
        if (rules.getFormatConsistencyRules() != null) {
            for (FormatConsistencyRule rule : rules.getFormatConsistencyRules()) {
                if (!validateFormatConsistencyRule(responseNode, rule)) {
                    result.addIntegrityIssue("Format consistency rule violated: " + rule.getDescription());
                }
            }
        }
    }
    
    private void validateSecurityHeaders(Response response, SecurityValidationRules rules, SecurityValidationResult result) {
        if (rules.getRequiredSecurityHeaders() != null) {
            for (String header : rules.getRequiredSecurityHeaders()) {
                if (!response.getHeaders().hasHeaderWithName(header)) {
                    result.addSecurityIssue("Required security header '" + header + "' is missing");
                }
            }
        }
    }
    
    private void validateSensitiveDataExposure(Response response, SecurityValidationRules rules, SecurityValidationResult result) {
        String responseBody = response.getBody().asString();
        
        if (rules.getSensitiveDataPatterns() != null) {
            for (String pattern : rules.getSensitiveDataPatterns()) {
                if (Pattern.matches(pattern, responseBody)) {
                    result.addSecurityIssue("Sensitive data pattern '" + pattern + "' found in response");
                }
            }
        }
    }
    
    private void validateCORSConfiguration(Response response, SecurityValidationRules rules, SecurityValidationResult result) {
        if (rules.isValidateCORS()) {
            String corsHeader = response.getHeader("Access-Control-Allow-Origin");
            if (corsHeader == null) {
                result.addSecurityIssue("CORS header 'Access-Control-Allow-Origin' is missing");
            } else if ("*".equals(corsHeader)) {
                result.addSecurityIssue("CORS header allows all origins (*)");
            }
        }
    }
    
    private void validateContentSecurity(Response response, SecurityValidationRules rules, SecurityValidationResult result) {
        if (rules.isValidateContentSecurity()) {
            String cspHeader = response.getHeader("Content-Security-Policy");
            if (cspHeader == null) {
                result.addSecurityIssue("Content Security Policy header is missing");
            }
        }
    }
    
    private boolean validateConsistencyRule(JsonNode responseNode, ConsistencyRule rule) {
        // Implement consistency rule validation logic
        return true; // Placeholder
    }
    
    private boolean validateBusinessRule(JsonNode responseNode, BusinessRule rule) {
        // Implement business rule validation logic
        return true; // Placeholder
    }
    
    private boolean validateReferentialIntegrityRule(JsonNode responseNode, ReferentialIntegrityRule rule) {
        // Implement referential integrity rule validation logic
        return true; // Placeholder
    }
    
    private boolean validateFormatConsistencyRule(JsonNode responseNode, FormatConsistencyRule rule) {
        // Implement format consistency rule validation logic
        return true; // Placeholder
    }
    
    private String getJsonNodeType(JsonNode node) {
        if (node.isTextual()) return "string";
        if (node.isNumber()) return "number";
        if (node.isBoolean()) return "boolean";
        if (node.isArray()) return "array";
        if (node.isObject()) return "object";
        if (node.isNull()) return "null";
        return "unknown";
    }
    
    private void createReportDirectory() {
        try {
            Path dir = Paths.get(reportDirectory);
            if (!java.nio.file.Files.exists(dir)) {
                java.nio.file.Files.createDirectories(dir);
                TestLogManager.info("Created API validation report directory: " + reportDirectory);
            }
        } catch (IOException e) {
            TestLogManager.error("Failed to create API validation report directory", e);
        }
    }
    
    /**
     * Response validation rules data model.
     */
    public static class ResponseValidationRules {
        private Integer expectedStatusCode;
        private List<Integer> validStatusCodes;
        private List<String> requiredFields;
        private List<String> forbiddenFields;
        private List<String> optionalFields;
        private Map<String, String> fieldTypes;
        private Map<String, FieldValueRule> fieldValueRules;
        private Long maxResponseTime;
        private List<String> requiredHeaders;
        private List<String> forbiddenHeaders;
        private String expectedContentType;
        
        // Getters and setters
        public Integer getExpectedStatusCode() { return expectedStatusCode; }
        public void setExpectedStatusCode(Integer expectedStatusCode) { this.expectedStatusCode = expectedStatusCode; }
        
        public List<Integer> getValidStatusCodes() { return validStatusCodes; }
        public void setValidStatusCodes(List<Integer> validStatusCodes) { this.validStatusCodes = validStatusCodes; }
        
        public List<String> getRequiredFields() { return requiredFields; }
        public void setRequiredFields(List<String> requiredFields) { this.requiredFields = requiredFields; }
        
        public List<String> getForbiddenFields() { return forbiddenFields; }
        public void setForbiddenFields(List<String> forbiddenFields) { this.forbiddenFields = forbiddenFields; }
        
        public List<String> getOptionalFields() { return optionalFields; }
        public void setOptionalFields(List<String> optionalFields) { this.optionalFields = optionalFields; }
        
        public Map<String, String> getFieldTypes() { return fieldTypes; }
        public void setFieldTypes(Map<String, String> fieldTypes) { this.fieldTypes = fieldTypes; }
        
        public Map<String, FieldValueRule> getFieldValueRules() { return fieldValueRules; }
        public void setFieldValueRules(Map<String, FieldValueRule> fieldValueRules) { this.fieldValueRules = fieldValueRules; }
        
        public Long getMaxResponseTime() { return maxResponseTime; }
        public void setMaxResponseTime(Long maxResponseTime) { this.maxResponseTime = maxResponseTime; }
        
        public List<String> getRequiredHeaders() { return requiredHeaders; }
        public void setRequiredHeaders(List<String> requiredHeaders) { this.requiredHeaders = requiredHeaders; }
        
        public List<String> getForbiddenHeaders() { return forbiddenHeaders; }
        public void setForbiddenHeaders(List<String> forbiddenHeaders) { this.forbiddenHeaders = forbiddenHeaders; }
        
        public String getExpectedContentType() { return expectedContentType; }
        public void setExpectedContentType(String expectedContentType) { this.expectedContentType = expectedContentType; }
    }
    
    /**
     * Field value rule data model.
     */
    public static class FieldValueRule {
        private Integer minLength;
        private Integer maxLength;
        private Double minValue;
        private Double maxValue;
        private String pattern;
        private List<String> allowedValues;
        
        // Getters and setters
        public Integer getMinLength() { return minLength; }
        public void setMinLength(Integer minLength) { this.minLength = minLength; }
        
        public Integer getMaxLength() { return maxLength; }
        public void setMaxLength(Integer maxLength) { this.maxLength = maxLength; }
        
        public Double getMinValue() { return minValue; }
        public void setMinValue(Double minValue) { this.minValue = minValue; }
        
        public Double getMaxValue() { return maxValue; }
        public void setMaxValue(Double maxValue) { this.maxValue = maxValue; }
        
        public String getPattern() { return pattern; }
        public void setPattern(String pattern) { this.pattern = pattern; }
        
        public List<String> getAllowedValues() { return allowedValues; }
        public void setAllowedValues(List<String> allowedValues) { this.allowedValues = allowedValues; }
    }
    
    /**
     * Response validation result data model.
     */
    public static class ResponseValidationResult {
        private int responseCode;
        private LocalDateTime validationTime;
        private ResponseValidationRules validationRules;
        private JsonNode responseBody;
        private boolean valid;
        private List<String> validationErrors;
        private List<String> fieldPresenceInfo;
        
        public ResponseValidationResult() {
            this.validationErrors = new ArrayList<>();
            this.fieldPresenceInfo = new ArrayList<>();
        }
        
        public void addValidationError(String error) {
            this.validationErrors.add(error);
        }
        
        public void addFieldPresenceInfo(String info) {
            this.fieldPresenceInfo.add(info);
        }
        
        // Getters and setters
        public int getResponseCode() { return responseCode; }
        public void setResponseCode(int responseCode) { this.responseCode = responseCode; }
        
        public LocalDateTime getValidationTime() { return validationTime; }
        public void setValidationTime(LocalDateTime validationTime) { this.validationTime = validationTime; }
        
        public ResponseValidationRules getValidationRules() { return validationRules; }
        public void setValidationRules(ResponseValidationRules validationRules) { this.validationRules = validationRules; }
        
        public JsonNode getResponseBody() { return responseBody; }
        public void setResponseBody(JsonNode responseBody) { this.responseBody = responseBody; }
        
        public boolean isValid() { return valid; }
        public void setValid(boolean valid) { this.valid = valid; }
        
        public List<String> getValidationErrors() { return validationErrors; }
        public void setValidationErrors(List<String> validationErrors) { this.validationErrors = validationErrors; }
        
        public List<String> getFieldPresenceInfo() { return fieldPresenceInfo; }
        public void setFieldPresenceInfo(List<String> fieldPresenceInfo) { this.fieldPresenceInfo = fieldPresenceInfo; }
    }
    
    /**
     * Schema validation result data model.
     */
    public static class SchemaValidationResult {
        private int responseCode;
        private String schemaPath;
        private LocalDateTime validationTime;
        private boolean valid;
        private List<ValidationMessage> validationMessages;
        private String errorMessage;
        private List<String> requiredFieldErrors;
        private List<String> typeErrors;
        private List<String> formatErrors;
        private List<String> patternErrors;
        private List<String> rangeErrors;
        private List<String> otherErrors;
        
        public SchemaValidationResult() {
            this.requiredFieldErrors = new ArrayList<>();
            this.typeErrors = new ArrayList<>();
            this.formatErrors = new ArrayList<>();
            this.patternErrors = new ArrayList<>();
            this.rangeErrors = new ArrayList<>();
            this.otherErrors = new ArrayList<>();
        }
        
        public void addRequiredFieldError(String error) {
            this.requiredFieldErrors.add(error);
        }
        
        public void addTypeError(String error) {
            this.typeErrors.add(error);
        }
        
        public void addFormatError(String error) {
            this.formatErrors.add(error);
        }
        
        public void addPatternError(String error) {
            this.patternErrors.add(error);
        }
        
        public void addRangeError(String error) {
            this.rangeErrors.add(error);
        }
        
        public void addOtherError(String error) {
            this.otherErrors.add(error);
        }
        
        // Getters and setters
        public int getResponseCode() { return responseCode; }
        public void setResponseCode(int responseCode) { this.responseCode = responseCode; }
        
        public String getSchemaPath() { return schemaPath; }
        public void setSchemaPath(String schemaPath) { this.schemaPath = schemaPath; }
        
        public LocalDateTime getValidationTime() { return validationTime; }
        public void setValidationTime(LocalDateTime validationTime) { this.validationTime = validationTime; }
        
        public boolean isValid() { return valid; }
        public void setValid(boolean valid) { this.valid = valid; }
        
        public List<ValidationMessage> getValidationMessages() { return validationMessages; }
        public void setValidationMessages(List<ValidationMessage> validationMessages) { this.validationMessages = validationMessages; }
        
        public String getErrorMessage() { return errorMessage; }
        public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
        
        public List<String> getRequiredFieldErrors() { return requiredFieldErrors; }
        public void setRequiredFieldErrors(List<String> requiredFieldErrors) { this.requiredFieldErrors = requiredFieldErrors; }
        
        public List<String> getTypeErrors() { return typeErrors; }
        public void setTypeErrors(List<String> typeErrors) { this.typeErrors = typeErrors; }
        
        public List<String> getFormatErrors() { return formatErrors; }
        public void setFormatErrors(List<String> formatErrors) { this.formatErrors = formatErrors; }
        
        public List<String> getPatternErrors() { return patternErrors; }
        public void setPatternErrors(List<String> patternErrors) { this.patternErrors = patternErrors; }
        
        public List<String> getRangeErrors() { return rangeErrors; }
        public void setRangeErrors(List<String> rangeErrors) { this.rangeErrors = rangeErrors; }
        
        public List<String> getOtherErrors() { return otherErrors; }
        public void setOtherErrors(List<String> otherErrors) { this.otherErrors = otherErrors; }
    }
    
    /**
     * Data integrity rules data model.
     */
    public static class DataIntegrityRules {
        private List<ConsistencyRule> consistencyRules;
        private List<BusinessRule> businessRules;
        private List<ReferentialIntegrityRule> referentialIntegrityRules;
        private List<FormatConsistencyRule> formatConsistencyRules;
        
        // Getters and setters
        public List<ConsistencyRule> getConsistencyRules() { return consistencyRules; }
        public void setConsistencyRules(List<ConsistencyRule> consistencyRules) { this.consistencyRules = consistencyRules; }
        
        public List<BusinessRule> getBusinessRules() { return businessRules; }
        public void setBusinessRules(List<BusinessRule> businessRules) { this.businessRules = businessRules; }
        
        public List<ReferentialIntegrityRule> getReferentialIntegrityRules() { return referentialIntegrityRules; }
        public void setReferentialIntegrityRules(List<ReferentialIntegrityRule> referentialIntegrityRules) { this.referentialIntegrityRules = referentialIntegrityRules; }
        
        public List<FormatConsistencyRule> getFormatConsistencyRules() { return formatConsistencyRules; }
        public void setFormatConsistencyRules(List<FormatConsistencyRule> formatConsistencyRules) { this.formatConsistencyRules = formatConsistencyRules; }
    }
    
    /**
     * Data integrity result data model.
     */
    public static class DataIntegrityResult {
        private int responseCode;
        private LocalDateTime validationTime;
        private DataIntegrityRules integrityRules;
        private boolean valid;
        private List<String> integrityIssues;
        
        public DataIntegrityResult() {
            this.integrityIssues = new ArrayList<>();
        }
        
        public void addIntegrityIssue(String issue) {
            this.integrityIssues.add(issue);
        }
        
        // Getters and setters
        public int getResponseCode() { return responseCode; }
        public void setResponseCode(int responseCode) { this.responseCode = responseCode; }
        
        public LocalDateTime getValidationTime() { return validationTime; }
        public void setValidationTime(LocalDateTime validationTime) { this.validationTime = validationTime; }
        
        public DataIntegrityRules getIntegrityRules() { return integrityRules; }
        public void setIntegrityRules(DataIntegrityRules integrityRules) { this.integrityRules = integrityRules; }
        
        public boolean isValid() { return valid; }
        public void setValid(boolean valid) { this.valid = valid; }
        
        public List<String> getIntegrityIssues() { return integrityIssues; }
        public void setIntegrityIssues(List<String> integrityIssues) { this.integrityIssues = integrityIssues; }
    }
    
    /**
     * Performance validation rules data model.
     */
    public static class PerformanceValidationRules {
        private long maxResponseTime;
        private int maxResponseSize;
        private int maxHeaderCount;
        
        // Getters and setters
        public long getMaxResponseTime() { return maxResponseTime; }
        public void setMaxResponseTime(long maxResponseTime) { this.maxResponseTime = maxResponseTime; }
        
        public int getMaxResponseSize() { return maxResponseSize; }
        public void setMaxResponseSize(int maxResponseSize) { this.maxResponseSize = maxResponseSize; }
        
        public int getMaxHeaderCount() { return maxHeaderCount; }
        public void setMaxHeaderCount(int maxHeaderCount) { this.maxHeaderCount = maxHeaderCount; }
    }
    
    /**
     * Performance validation result data model.
     */
    public static class PerformanceValidationResult {
        private int responseCode;
        private LocalDateTime validationTime;
        private PerformanceValidationRules performanceRules;
        private long responseTime;
        private int responseSize;
        private int headerCount;
        private boolean valid;
        private List<String> performanceIssues;
        
        public PerformanceValidationResult() {
            this.performanceIssues = new ArrayList<>();
        }
        
        public void addPerformanceIssue(String issue) {
            this.performanceIssues.add(issue);
        }
        
        // Getters and setters
        public int getResponseCode() { return responseCode; }
        public void setResponseCode(int responseCode) { this.responseCode = responseCode; }
        
        public LocalDateTime getValidationTime() { return validationTime; }
        public void setValidationTime(LocalDateTime validationTime) { this.validationTime = validationTime; }
        
        public PerformanceValidationRules getPerformanceRules() { return performanceRules; }
        public void setPerformanceRules(PerformanceValidationRules performanceRules) { this.performanceRules = performanceRules; }
        
        public long getResponseTime() { return responseTime; }
        public void setResponseTime(long responseTime) { this.responseTime = responseTime; }
        
        public int getResponseSize() { return responseSize; }
        public void setResponseSize(int responseSize) { this.responseSize = responseSize; }
        
        public int getHeaderCount() { return headerCount; }
        public void setHeaderCount(int headerCount) { this.headerCount = headerCount; }
        
        public boolean isValid() { return valid; }
        public void setValid(boolean valid) { this.valid = valid; }
        
        public List<String> getPerformanceIssues() { return performanceIssues; }
        public void setPerformanceIssues(List<String> performanceIssues) { this.performanceIssues = performanceIssues; }
    }
    
    /**
     * Security validation rules data model.
     */
    public static class SecurityValidationRules {
        private List<String> requiredSecurityHeaders;
        private List<String> sensitiveDataPatterns;
        private boolean validateCORS;
        private boolean validateContentSecurity;
        
        // Getters and setters
        public List<String> getRequiredSecurityHeaders() { return requiredSecurityHeaders; }
        public void setRequiredSecurityHeaders(List<String> requiredSecurityHeaders) { this.requiredSecurityHeaders = requiredSecurityHeaders; }
        
        public List<String> getSensitiveDataPatterns() { return sensitiveDataPatterns; }
        public void setSensitiveDataPatterns(List<String> sensitiveDataPatterns) { this.sensitiveDataPatterns = sensitiveDataPatterns; }
        
        public boolean isValidateCORS() { return validateCORS; }
        public void setValidateCORS(boolean validateCORS) { this.validateCORS = validateCORS; }
        
        public boolean isValidateContentSecurity() { return validateContentSecurity; }
        public void setValidateContentSecurity(boolean validateContentSecurity) { this.validateContentSecurity = validateContentSecurity; }
    }
    
    /**
     * Security validation result data model.
     */
    public static class SecurityValidationResult {
        private int responseCode;
        private LocalDateTime validationTime;
        private SecurityValidationRules securityRules;
        private boolean valid;
        private List<String> securityIssues;
        
        public SecurityValidationResult() {
            this.securityIssues = new ArrayList<>();
        }
        
        public void addSecurityIssue(String issue) {
            this.securityIssues.add(issue);
        }
        
        // Getters and setters
        public int getResponseCode() { return responseCode; }
        public void setResponseCode(int responseCode) { this.responseCode = responseCode; }
        
        public LocalDateTime getValidationTime() { return validationTime; }
        public void setValidationTime(LocalDateTime validationTime) { this.validationTime = validationTime; }
        
        public SecurityValidationRules getSecurityRules() { return securityRules; }
        public void setSecurityRules(SecurityValidationRules securityRules) { this.securityRules = securityRules; }
        
        public boolean isValid() { return valid; }
        public void setValid(boolean valid) { this.valid = valid; }
        
        public List<String> getSecurityIssues() { return securityIssues; }
        public void setSecurityIssues(List<String> securityIssues) { this.securityIssues = securityIssues; }
    }
    
    // Placeholder classes for integrity rules
    public static class ConsistencyRule {
        private String description;
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
    }
    
    public static class BusinessRule {
        private String description;
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
    }
    
    public static class ReferentialIntegrityRule {
        private String description;
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
    }
    
    public static class FormatConsistencyRule {
        private String description;
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
    }
}

