package examples;

import advanced.*;
import base.ModernBaseTest;
import org.testng.annotations.Test;
import reporting.TestLogManager;

import java.util.*;

/**
 * Example test class demonstrating the use of Advanced API Testing Suite capabilities.
 */
public class APITestingExample extends ModernBaseTest {
    
    @Test
    public void testAPISchemaValidation() {
        TestLogManager.info("Testing API Schema Validation capabilities");
        
        APITestSuite apiTestSuite = new APITestSuite();
        APIResponseValidator validator = new APIResponseValidator();
        
        // Example API endpoint
        String apiUrl = "https://jsonplaceholder.typicode.com/posts/1";
        
        try {
            // Make API request
            io.restassured.response.Response response = io.restassured.RestAssured.get(apiUrl);
            
            // Validate response schema (if schema file exists)
            // APITestSuite.APIValidationResult schemaResult = apiTestSuite.validateAPIResponseSchema(response, "schemas/post_schema.json");
            
            // Create validation rules
            APIResponseValidator.ResponseValidationRules rules = new APIResponseValidator.ResponseValidationRules();
            rules.setExpectedStatusCode(200);
            rules.setRequiredFields(Arrays.asList("id", "title", "body", "userId"));
            rules.setMaxResponseTime(5000L);
            rules.setExpectedContentType("application/json");
            
            // Set field type validation
            Map<String, String> fieldTypes = new HashMap<>();
            fieldTypes.put("id", "number");
            fieldTypes.put("title", "string");
            fieldTypes.put("body", "string");
            fieldTypes.put("userId", "number");
            rules.setFieldTypes(fieldTypes);
            
            // Set field value rules
            Map<String, APIResponseValidator.FieldValueRule> fieldValueRules = new HashMap<>();
            
            APIResponseValidator.FieldValueRule titleRule = new APIResponseValidator.FieldValueRule();
            titleRule.setMinLength(1);
            titleRule.setMaxLength(200);
            fieldValueRules.put("title", titleRule);
            
            APIResponseValidator.FieldValueRule bodyRule = new APIResponseValidator.FieldValueRule();
            bodyRule.setMinLength(1);
            fieldValueRules.put("body", bodyRule);
            
            rules.setFieldValueRules(fieldValueRules);
            
            // Validate response
            APIResponseValidator.ResponseValidationResult validationResult = validator.validateResponse(response, rules);
            
            if (validationResult.isValid()) {
                TestLogManager.success("API response validation passed");
            } else {
                TestLogManager.warning("API response validation failed with " + validationResult.getValidationErrors().size() + " errors");
                for (String error : validationResult.getValidationErrors()) {
                    TestLogManager.error("Validation error: " + error);
                }
            }
            
        } catch (Exception e) {
            TestLogManager.error("API schema validation test failed", e);
        }
    }
    
    @Test
    public void testAPILoadTesting() {
        TestLogManager.info("Testing API Load Testing capabilities");
        
        APITestSuite apiTestSuite = new APITestSuite();
        
        // Perform load testing
        String endpoint = "https://jsonplaceholder.typicode.com/posts";
        int concurrentUsers = 5;
        int duration = 30; // seconds
        
        APITestSuite.LoadTestResult loadResult = apiTestSuite.performLoadTesting(endpoint, concurrentUsers, duration);
        
        TestLogManager.info("Load test completed:");
        TestLogManager.info("Total requests: " + loadResult.getTotalRequests());
        TestLogManager.info("Successful requests: " + loadResult.getSuccessfulRequests());
        TestLogManager.info("Failed requests: " + loadResult.getFailedRequests());
        TestLogManager.info("Success rate: " + String.format("%.2f", loadResult.getSuccessRate()) + "%");
        TestLogManager.info("Average response time: " + String.format("%.2f", loadResult.getAverageResponseTime()) + "ms");
        TestLogManager.info("Requests per second: " + String.format("%.2f", loadResult.getRequestsPerSecond()));
        
        if (loadResult.getError() != null) {
            TestLogManager.error("Load test error: " + loadResult.getError());
        }
    }
    
    @Test
    public void testAPIDocumentationGeneration() {
        TestLogManager.info("Testing API Documentation Generation capabilities");
        
        APITestSuite apiTestSuite = new APITestSuite();
        
        try {
            // Generate API documentation from OpenAPI spec
            // Note: This would require an actual OpenAPI specification file
            // Path docPath = apiTestSuite.generateAPIDocumentation("openapi_spec.json");
            // TestLogManager.info("API documentation generated: " + docPath);
            
            TestLogManager.info("API documentation generation capability demonstrated (requires OpenAPI spec file)");
            
        } catch (Exception e) {
            TestLogManager.error("API documentation generation test failed", e);
        }
    }
    
    @Test
    public void testAPIContractTesting() {
        TestLogManager.info("Testing API Contract Testing capabilities");
        
        APITestSuite apiTestSuite = new APITestSuite();
        
        try {
            // Perform contract testing
            // Note: This would require actual API and consumer specification files
            // APITestSuite.ContractTestResult contractResult = apiTestSuite.performContractTesting(
            //     "api_spec.json", "consumer_spec.json");
            
            TestLogManager.info("API contract testing capability demonstrated (requires spec files)");
            
        } catch (Exception e) {
            TestLogManager.error("API contract testing failed", e);
        }
    }
    
    @Test
    public void testComprehensiveAPITesting() {
        TestLogManager.info("Testing Comprehensive API Testing Suite");
        
        APITestSuite apiTestSuite = new APITestSuite();
        
        // Create API configuration
        APITestSuite.APIConfiguration apiConfig = new APITestSuite.APIConfiguration();
        apiConfig.setBaseUrl("https://jsonplaceholder.typicode.com");
        
        // Create API endpoints
        List<APITestSuite.APIEndpoint> endpoints = new ArrayList<>();
        
        // GET /posts endpoint
        APITestSuite.APIEndpoint postsEndpoint = new APITestSuite.APIEndpoint();
        postsEndpoint.setPath("/posts");
        postsEndpoint.setMethod("GET");
        Map<String, String> headers = new HashMap<>();
        headers.put("Accept", "application/json");
        postsEndpoint.setHeaders(headers);
        endpoints.add(postsEndpoint);
        
        // GET /posts/{id} endpoint
        APITestSuite.APIEndpoint postByIdEndpoint = new APITestSuite.APIEndpoint();
        postByIdEndpoint.setPath("/posts/{id}");
        postByIdEndpoint.setMethod("GET");
        Map<String, String> pathParams = new HashMap<>();
        pathParams.put("id", "1");
        postByIdEndpoint.setPathParams(pathParams);
        postByIdEndpoint.setHeaders(headers);
        endpoints.add(postByIdEndpoint);
        
        // POST /posts endpoint
        APITestSuite.APIEndpoint createPostEndpoint = new APITestSuite.APIEndpoint();
        createPostEndpoint.setPath("/posts");
        createPostEndpoint.setMethod("POST");
        Map<String, String> postHeaders = new HashMap<>();
        postHeaders.put("Content-Type", "application/json");
        postHeaders.put("Accept", "application/json");
        createPostEndpoint.setHeaders(postHeaders);
        createPostEndpoint.setRequestBody("{\"title\":\"Test Post\",\"body\":\"This is a test post\",\"userId\":1}");
        endpoints.add(createPostEndpoint);
        
        apiConfig.setEndpoints(endpoints);
        
        // Configure load testing
        apiConfig.setLoadTestingEnabled(true);
        apiConfig.setLoadTestUsers(3);
        apiConfig.setLoadTestDuration(10);
        
        // Perform comprehensive API testing
        APITestSuite.APITestSuiteResult suiteResult = apiTestSuite.performComprehensiveAPITesting(apiConfig);
        
        TestLogManager.info("Comprehensive API testing completed:");
        TestLogManager.info("Total endpoints tested: " + suiteResult.getEndpointResults().size());
        
        long successfulEndpoints = suiteResult.getEndpointResults().stream()
            .mapToLong(r -> r.isSuccess() ? 1 : 0)
            .sum();
        
        TestLogManager.info("Successful endpoints: " + successfulEndpoints);
        TestLogManager.info("Success rate: " + String.format("%.2f", 
            (double) successfulEndpoints / suiteResult.getEndpointResults().size() * 100) + "%");
        
        // Log individual endpoint results
        for (APITestSuite.APIEndpointResult endpointResult : suiteResult.getEndpointResults()) {
            String status = endpointResult.isSuccess() ? "PASS" : "FAIL";
            TestLogManager.info("Endpoint " + endpointResult.getEndpoint().getMethod() + 
                " " + endpointResult.getEndpoint().getPath() + ": " + status + 
                " (Response time: " + endpointResult.getResponseTime() + "ms)");
            
            if (!endpointResult.isSuccess() && endpointResult.getErrorMessage() != null) {
                TestLogManager.error("Error: " + endpointResult.getErrorMessage());
            }
        }
        
        // Log load test results if available
        if (suiteResult.getLoadTestResult() != null) {
            APITestSuite.LoadTestResult loadResult = suiteResult.getLoadTestResult();
            TestLogManager.info("Load test results:");
            TestLogManager.info("Total requests: " + loadResult.getTotalRequests());
            TestLogManager.info("Success rate: " + String.format("%.2f", loadResult.getSuccessRate()) + "%");
            TestLogManager.info("Average response time: " + String.format("%.2f", loadResult.getAverageResponseTime()) + "ms");
        }
    }
    
    @Test
    public void testAPIResponseValidation() {
        TestLogManager.info("Testing API Response Validation capabilities");
        
        APIResponseValidator validator = new APIResponseValidator();
        
        try {
            // Make API request
            String apiUrl = "https://jsonplaceholder.typicode.com/users/1";
            io.restassured.response.Response response = io.restassured.RestAssured.get(apiUrl);
            
            // Create comprehensive validation rules
            APIResponseValidator.ResponseValidationRules rules = new APIResponseValidator.ResponseValidationRules();
            rules.setExpectedStatusCode(200);
            rules.setRequiredFields(Arrays.asList("id", "name", "username", "email", "address"));
            rules.setMaxResponseTime(3000L);
            rules.setExpectedContentType("application/json");
            
            // Set field types
            Map<String, String> fieldTypes = new HashMap<>();
            fieldTypes.put("id", "number");
            fieldTypes.put("name", "string");
            fieldTypes.put("username", "string");
            fieldTypes.put("email", "string");
            fieldTypes.put("phone", "string");
            fieldTypes.put("website", "string");
            rules.setFieldTypes(fieldTypes);
            
            // Set field value rules
            Map<String, APIResponseValidator.FieldValueRule> fieldValueRules = new HashMap<>();
            
            // Email validation
            APIResponseValidator.FieldValueRule emailRule = new APIResponseValidator.FieldValueRule();
            emailRule.setPattern("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$");
            fieldValueRules.put("email", emailRule);
            
            // Name validation
            APIResponseValidator.FieldValueRule nameRule = new APIResponseValidator.FieldValueRule();
            nameRule.setMinLength(1);
            nameRule.setMaxLength(100);
            fieldValueRules.put("name", nameRule);
            
            rules.setFieldValueRules(fieldValueRules);
            
            // Validate response
            APIResponseValidator.ResponseValidationResult validationResult = validator.validateResponse(response, rules);
            
            if (validationResult.isValid()) {
                TestLogManager.success("API response validation passed");
            } else {
                TestLogManager.warning("API response validation failed with " + validationResult.getValidationErrors().size() + " errors");
                for (String error : validationResult.getValidationErrors()) {
                    TestLogManager.error("Validation error: " + error);
                }
            }
            
            // Test performance validation
            APIResponseValidator.PerformanceValidationRules perfRules = new APIResponseValidator.PerformanceValidationRules();
            perfRules.setMaxResponseTime(5000);
            perfRules.setMaxResponseSize(10000);
            perfRules.setMaxHeaderCount(20);
            
            APIResponseValidator.PerformanceValidationResult perfResult = validator.validateResponsePerformance(response, perfRules);
            
            if (perfResult.isValid()) {
                TestLogManager.success("API performance validation passed");
            } else {
                TestLogManager.warning("API performance validation failed with " + perfResult.getPerformanceIssues().size() + " issues");
                for (String issue : perfResult.getPerformanceIssues()) {
                    TestLogManager.error("Performance issue: " + issue);
                }
            }
            
            // Test security validation
            APIResponseValidator.SecurityValidationRules securityRules = new APIResponseValidator.SecurityValidationRules();
            securityRules.setRequiredSecurityHeaders(Arrays.asList("Content-Type"));
            securityRules.setValidateCORS(true);
            securityRules.setValidateContentSecurity(false);
            
            APIResponseValidator.SecurityValidationResult securityResult = validator.validateResponseSecurity(response, securityRules);
            
            if (securityResult.isValid()) {
                TestLogManager.success("API security validation passed");
            } else {
                TestLogManager.warning("API security validation failed with " + securityResult.getSecurityIssues().size() + " issues");
                for (String issue : securityResult.getSecurityIssues()) {
                    TestLogManager.error("Security issue: " + issue);
                }
            }
            
        } catch (Exception e) {
            TestLogManager.error("API response validation test failed", e);
        }
    }
    
    @Test
    public void testAPIDataIntegrityValidation() {
        TestLogManager.info("Testing API Data Integrity Validation capabilities");
        
        APIResponseValidator validator = new APIResponseValidator();
        
        try {
            // Make API request
            String apiUrl = "https://jsonplaceholder.typicode.com/posts";
            io.restassured.response.Response response = io.restassured.RestAssured.get(apiUrl);
            
            // Create data integrity rules
            APIResponseValidator.DataIntegrityRules integrityRules = new APIResponseValidator.DataIntegrityRules();
            
            // Add consistency rules
            List<APIResponseValidator.ConsistencyRule> consistencyRules = new ArrayList<>();
            APIResponseValidator.ConsistencyRule consistencyRule = new APIResponseValidator.ConsistencyRule();
            consistencyRule.setDescription("All posts should have valid user IDs");
            consistencyRules.add(consistencyRule);
            integrityRules.setConsistencyRules(consistencyRules);
            
            // Add business rules
            List<APIResponseValidator.BusinessRule> businessRules = new ArrayList<>();
            APIResponseValidator.BusinessRule businessRule = new APIResponseValidator.BusinessRule();
            businessRule.setDescription("Post titles should not be empty");
            businessRules.add(businessRule);
            integrityRules.setBusinessRules(businessRules);
            
            // Validate data integrity
            APIResponseValidator.DataIntegrityResult integrityResult = validator.validateDataIntegrity(response, integrityRules);
            
            if (integrityResult.isValid()) {
                TestLogManager.success("API data integrity validation passed");
            } else {
                TestLogManager.warning("API data integrity validation failed with " + integrityResult.getIntegrityIssues().size() + " issues");
                for (String issue : integrityResult.getIntegrityIssues()) {
                    TestLogManager.error("Integrity issue: " + issue);
                }
            }
            
        } catch (Exception e) {
            TestLogManager.error("API data integrity validation test failed", e);
        }
    }
    
    @Test
    public void testIntegratedAPITesting() {
        TestLogManager.info("Testing Integrated API Testing capabilities");
        
        APITestSuite apiTestSuite = new APITestSuite();
        APIResponseValidator validator = new APIResponseValidator();
        
        try {
            // Create comprehensive API configuration
            APITestSuite.APIConfiguration apiConfig = new APITestSuite.APIConfiguration();
            apiConfig.setBaseUrl("https://jsonplaceholder.typicode.com");
            
            // Create multiple endpoints for comprehensive testing
            List<APITestSuite.APIEndpoint> endpoints = new ArrayList<>();
            
            // Test various HTTP methods
            String[] methods = {"GET", "POST", "PUT", "DELETE"};
            String[] paths = {"/posts", "/users", "/comments", "/albums"};
            
            for (String method : methods) {
                for (String path : paths) {
                    APITestSuite.APIEndpoint endpoint = new APITestSuite.APIEndpoint();
                    endpoint.setPath(path);
                    endpoint.setMethod(method);
                    
                    Map<String, String> headers = new HashMap<>();
                    headers.put("Accept", "application/json");
                    if (method.equals("POST") || method.equals("PUT")) {
                        headers.put("Content-Type", "application/json");
                        endpoint.setRequestBody("{\"test\":\"data\"}");
                    }
                    endpoint.setHeaders(headers);
                    
                    endpoints.add(endpoint);
                }
            }
            
            apiConfig.setEndpoints(endpoints);
            
            // Enable load testing
            apiConfig.setLoadTestingEnabled(true);
            apiConfig.setLoadTestUsers(2);
            apiConfig.setLoadTestDuration(5);
            
            // Perform comprehensive testing
            APITestSuite.APITestSuiteResult suiteResult = apiTestSuite.performComprehensiveAPITesting(apiConfig);
            
            // Analyze results
            TestLogManager.info("Integrated API testing completed:");
            TestLogManager.info("Total endpoints tested: " + suiteResult.getEndpointResults().size());
            
            long successfulEndpoints = suiteResult.getEndpointResults().stream()
                .mapToLong(r -> r.isSuccess() ? 1 : 0)
                .sum();
            
            TestLogManager.info("Successful endpoints: " + successfulEndpoints);
            TestLogManager.info("Failed endpoints: " + (suiteResult.getEndpointResults().size() - successfulEndpoints));
            
            // Group results by method
            Map<String, Long> resultsByMethod = new HashMap<>();
            for (APITestSuite.APIEndpointResult result : suiteResult.getEndpointResults()) {
                String method = result.getEndpoint().getMethod();
                resultsByMethod.merge(method, result.isSuccess() ? 1L : 0L, Long::sum);
            }
            
            TestLogManager.info("Results by HTTP method:");
            for (Map.Entry<String, Long> entry : resultsByMethod.entrySet()) {
                TestLogManager.info(entry.getKey() + ": " + entry.getValue() + " successful");
            }
            
            // Performance summary
            if (suiteResult.getLoadTestResult() != null) {
                APITestSuite.LoadTestResult loadResult = suiteResult.getLoadTestResult();
                TestLogManager.info("Load test summary:");
                TestLogManager.info("Total requests: " + loadResult.getTotalRequests());
                TestLogManager.info("Success rate: " + String.format("%.2f", loadResult.getSuccessRate()) + "%");
                TestLogManager.info("Average response time: " + String.format("%.2f", loadResult.getAverageResponseTime()) + "ms");
                TestLogManager.info("Requests per second: " + String.format("%.2f", loadResult.getRequestsPerSecond()));
            }
            
            TestLogManager.success("Integrated API testing completed successfully");
            
        } catch (Exception e) {
            TestLogManager.error("Integrated API testing failed", e);
        }
    }
}

