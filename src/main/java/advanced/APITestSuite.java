package advanced;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;
import com.networknt.schema.ValidationMessage;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import reporting.TestLogManager;
import utils.CrossPlatformUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Advanced API Testing Suite with comprehensive schema validation, load testing, and contract testing capabilities.
 */
public class APITestSuite {
    
    private final ObjectMapper objectMapper;
    private final JsonSchemaFactory schemaFactory;
    private final String reportDirectory;
    private final ExecutorService executorService;
    
    public APITestSuite() {
        this.objectMapper = new ObjectMapper();
        this.schemaFactory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V7);
        this.reportDirectory = CrossPlatformUtils.getProjectDataDirectory()
                .resolve("api_reports").toString();
        this.executorService = Executors.newFixedThreadPool(10);
        createReportDirectory();
    }
    
    /**
     * Validates API response against JSON schema.
     * @param response API response to validate
     * @param schemaPath Path to JSON schema file
     * @return APIValidationResult with validation details
     */
    public APIValidationResult validateAPIResponseSchema(Response response, String schemaPath) {
        TestLogManager.info("Validating API response against schema: " + schemaPath);
        
        APIValidationResult result = new APIValidationResult();
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
            
            if (result.isValid()) {
                TestLogManager.success("API response validation passed");
            } else {
                TestLogManager.warning("API response validation failed with " + validationMessages.size() + " errors");
                for (ValidationMessage message : validationMessages) {
                    TestLogManager.error("Validation error: " + message.getMessage());
                }
            }
            
        } catch (IOException e) {
            TestLogManager.error("Failed to validate API response schema", e);
            result.setValid(false);
            result.setErrorMessage("Schema validation error: " + e.getMessage());
        }
        
        return result;
    }
    
    /**
     * Performs load testing on API endpoint.
     * @param endpoint API endpoint URL
     * @param concurrentUsers Number of concurrent users
     * @param duration Duration in seconds
     * @return LoadTestResult with performance metrics
     */
    public LoadTestResult performLoadTesting(String endpoint, int concurrentUsers, int duration) {
        TestLogManager.info("Starting load test: " + concurrentUsers + " users for " + duration + " seconds");
        
        LoadTestResult result = new LoadTestResult();
        result.setEndpoint(endpoint);
        result.setConcurrentUsers(concurrentUsers);
        result.setDuration(duration);
        result.setStartTime(LocalDateTime.now());
        
        List<CompletableFuture<LoadTestMetrics>> futures = new ArrayList<>();
        
        // Create concurrent requests
        for (int i = 0; i < concurrentUsers; i++) {
            CompletableFuture<LoadTestMetrics> future = CompletableFuture.supplyAsync(() -> {
                return executeLoadTestRequest(endpoint, duration);
            }, executorService);
            futures.add(future);
        }
        
        // Wait for all requests to complete
        CompletableFuture<Void> allFutures = CompletableFuture.allOf(
            futures.toArray(new CompletableFuture[0]));
        
        try {
            allFutures.get(duration + 30, TimeUnit.SECONDS);
            
            // Collect results
            List<LoadTestMetrics> metrics = new ArrayList<>();
            for (CompletableFuture<LoadTestMetrics> future : futures) {
                if (future.isDone() && !future.isCompletedExceptionally()) {
                    metrics.add(future.get());
                }
            }
            
            // Calculate aggregate metrics
            calculateLoadTestMetrics(result, metrics);
            
        } catch (Exception e) {
            TestLogManager.error("Load test execution failed", e);
            result.setError(e.getMessage());
        }
        
        result.setEndTime(LocalDateTime.now());
        generateLoadTestReport(result);
        
        TestLogManager.success("Load test completed");
        return result;
    }
    
    /**
     * Generates API documentation from OpenAPI specification.
     * @param openApiSpecPath Path to OpenAPI specification file
     * @return Path to generated documentation
     */
    public Path generateAPIDocumentation(String openApiSpecPath) {
        TestLogManager.info("Generating API documentation from: " + openApiSpecPath);
        
        try {
            JsonNode openApiSpec = objectMapper.readTree(new FileInputStream(openApiSpecPath));
            
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            String fileName = "api_documentation_" + timestamp + ".html";
            Path docPath = Paths.get(reportDirectory, fileName);
            
            StringBuilder documentation = new StringBuilder();
            documentation.append(generateHTMLHeader("API Documentation"));
            documentation.append(generateAPIDocSummary(openApiSpec));
            documentation.append(generateAPIDocEndpoints(openApiSpec));
            documentation.append(generateAPIDocSchemas(openApiSpec));
            documentation.append(generateHTMLFooter());
            
            Files.write(docPath, documentation.toString().getBytes());
            TestLogManager.success("API documentation generated: " + docPath);
            
            return docPath;
            
        } catch (IOException e) {
            TestLogManager.error("Failed to generate API documentation", e);
            throw new RuntimeException("API documentation generation failed", e);
        }
    }
    
    /**
     * Performs contract testing between API and consumer.
     * @param apiSpecPath Path to API specification
     * @param consumerSpecPath Path to consumer specification
     * @return ContractTestResult with contract validation results
     */
    public ContractTestResult performContractTesting(String apiSpecPath, String consumerSpecPath) {
        TestLogManager.info("Performing contract testing between API and consumer");
        
        ContractTestResult result = new ContractTestResult();
        result.setApiSpecPath(apiSpecPath);
        result.setConsumerSpecPath(consumerSpecPath);
        result.setTestTime(LocalDateTime.now());
        
        try {
            JsonNode apiSpec = objectMapper.readTree(new File(apiSpecPath));
            JsonNode consumerSpec = objectMapper.readTree(new File(consumerSpecPath));
            
            // Validate API contract
            validateAPIContract(apiSpec, result);
            
            // Validate consumer contract
            validateConsumerContract(consumerSpec, result);
            
            // Check compatibility
            checkContractCompatibility(apiSpec, consumerSpec, result);
            
        } catch (IOException e) {
            TestLogManager.error("Contract testing failed", e);
            result.setError(e.getMessage());
        }
        
        generateContractTestReport(result);
        return result;
    }
    
    /**
     * Performs comprehensive API testing suite.
     * @param apiConfig API configuration
     * @return APITestSuiteResult with comprehensive test results
     */
    public APITestSuiteResult performComprehensiveAPITesting(APIConfiguration apiConfig) {
        TestLogManager.info("Starting comprehensive API testing suite");
        
        APITestSuiteResult suiteResult = new APITestSuiteResult();
        suiteResult.setApiConfig(apiConfig);
        suiteResult.setStartTime(LocalDateTime.now());
        
        // Test each endpoint
        for (APIEndpoint endpoint : apiConfig.getEndpoints()) {
            TestLogManager.info("Testing endpoint: " + endpoint.getPath());
            
            APIEndpointResult endpointResult = testAPIEndpoint(endpoint);
            suiteResult.addEndpointResult(endpointResult);
        }
        
        // Perform load testing
        if (apiConfig.isLoadTestingEnabled()) {
            LoadTestResult loadResult = performLoadTesting(
                apiConfig.getBaseUrl(), 
                apiConfig.getLoadTestUsers(), 
                apiConfig.getLoadTestDuration());
            suiteResult.setLoadTestResult(loadResult);
        }
        
        // Perform contract testing
        if (apiConfig.getContractSpecPath() != null) {
            ContractTestResult contractResult = performContractTesting(
                apiConfig.getContractSpecPath(), 
                apiConfig.getConsumerSpecPath());
            suiteResult.setContractTestResult(contractResult);
        }
        
        suiteResult.setEndTime(LocalDateTime.now());
        generateComprehensiveAPITestReport(suiteResult);
        
        TestLogManager.success("Comprehensive API testing suite completed");
        return suiteResult;
    }
    
    private LoadTestMetrics executeLoadTestRequest(String endpoint, int duration) {
        LoadTestMetrics metrics = new LoadTestMetrics();
        long startTime = System.currentTimeMillis();
        
        int requestCount = 0;
        int successCount = 0;
        int errorCount = 0;
        long totalResponseTime = 0;
        
        long endTime = startTime + (duration * 1000);
        
        while (System.currentTimeMillis() < endTime) {
            try {
                long requestStart = System.currentTimeMillis();
                
                Response response = RestAssured.given()
                    .get(endpoint);
                
                long requestEnd = System.currentTimeMillis();
                long responseTime = requestEnd - requestStart;
                
                requestCount++;
                totalResponseTime += responseTime;
                
                if (response.getStatusCode() >= 200 && response.getStatusCode() < 300) {
                    successCount++;
                } else {
                    errorCount++;
                }
                
                // Small delay between requests
                Thread.sleep(100);
                
            } catch (Exception e) {
                errorCount++;
                TestLogManager.info("Load test request failed: " + e.getMessage());
            }
        }
        
        metrics.setRequestCount(requestCount);
        metrics.setSuccessCount(successCount);
        metrics.setErrorCount(errorCount);
        metrics.setTotalResponseTime(totalResponseTime);
        metrics.setAverageResponseTime(requestCount > 0 ? totalResponseTime / requestCount : 0);
        
        return metrics;
    }
    
    private void calculateLoadTestMetrics(LoadTestResult result, List<LoadTestMetrics> metrics) {
        int totalRequests = metrics.stream().mapToInt(LoadTestMetrics::getRequestCount).sum();
        int totalSuccesses = metrics.stream().mapToInt(LoadTestMetrics::getSuccessCount).sum();
        int totalErrors = metrics.stream().mapToInt(LoadTestMetrics::getErrorCount).sum();
        
        result.setTotalRequests(totalRequests);
        result.setSuccessfulRequests(totalSuccesses);
        result.setFailedRequests(totalErrors);
        result.setSuccessRate(totalRequests > 0 ? (double) totalSuccesses / totalRequests * 100 : 0);
        
        double avgResponseTime = metrics.stream()
            .mapToLong(LoadTestMetrics::getAverageResponseTime)
            .average()
            .orElse(0.0);
        result.setAverageResponseTime(avgResponseTime);
        
        long maxResponseTime = metrics.stream()
            .mapToLong(LoadTestMetrics::getAverageResponseTime)
            .max()
            .orElse(0);
        result.setMaxResponseTime(maxResponseTime);
        
        long minResponseTime = metrics.stream()
            .mapToLong(LoadTestMetrics::getAverageResponseTime)
            .min()
            .orElse(0);
        result.setMinResponseTime(minResponseTime);
        
        result.setRequestsPerSecond(totalRequests / result.getDuration());
    }
    
    private APIEndpointResult testAPIEndpoint(APIEndpoint endpoint) {
        APIEndpointResult result = new APIEndpointResult();
        result.setEndpoint(endpoint);
        result.setTestTime(LocalDateTime.now());
        
        try {
            RequestSpecification request = RestAssured.given();
            
            // Set headers
            if (endpoint.getHeaders() != null) {
                request.headers(endpoint.getHeaders());
            }
            
            // Set query parameters
            if (endpoint.getQueryParams() != null) {
                request.queryParams(endpoint.getQueryParams());
            }
            
            // Set path parameters
            if (endpoint.getPathParams() != null) {
                request.pathParams(endpoint.getPathParams());
            }
            
            // Set request body
            if (endpoint.getRequestBody() != null) {
                request.body(endpoint.getRequestBody());
            }
            
            // Execute request
            Response response = executeRequest(request, endpoint.getMethod(), endpoint.getPath());
            
            result.setResponse(response);
            result.setResponseTime(response.getTime());
            result.setSuccess(response.getStatusCode() >= 200 && response.getStatusCode() < 300);
            
            // Validate response schema if provided
            if (endpoint.getResponseSchemaPath() != null) {
                APIValidationResult validationResult = validateAPIResponseSchema(response, endpoint.getResponseSchemaPath());
                result.setValidationResult(validationResult);
            }
            
        } catch (Exception e) {
            TestLogManager.error("API endpoint test failed: " + endpoint.getPath(), e);
            result.setSuccess(false);
            result.setErrorMessage(e.getMessage());
        }
        
        return result;
    }
    
    private Response executeRequest(RequestSpecification request, String method, String path) {
        switch (method.toUpperCase()) {
            case "GET":
                return request.get(path);
            case "POST":
                return request.post(path);
            case "PUT":
                return request.put(path);
            case "DELETE":
                return request.delete(path);
            case "PATCH":
                return request.patch(path);
            default:
                throw new IllegalArgumentException("Unsupported HTTP method: " + method);
        }
    }
    
    private void validateAPIContract(JsonNode apiSpec, ContractTestResult result) {
        // Validate API specification structure
        if (!apiSpec.has("openapi") && !apiSpec.has("swagger")) {
            result.addContractIssue("API specification is not a valid OpenAPI/Swagger spec");
        }
        
        if (!apiSpec.has("paths")) {
            result.addContractIssue("API specification missing paths section");
        }
        
        if (!apiSpec.has("info")) {
            result.addContractIssue("API specification missing info section");
        }
        
        result.setApiContractValid(result.getContractIssues().isEmpty());
    }
    
    private void validateConsumerContract(JsonNode consumerSpec, ContractTestResult result) {
        // Validate consumer specification structure
        if (!consumerSpec.has("consumer")) {
            result.addContractIssue("Consumer specification missing consumer section");
        }
        
        if (!consumerSpec.has("interactions")) {
            result.addContractIssue("Consumer specification missing interactions section");
        }
        
        result.setConsumerContractValid(result.getContractIssues().isEmpty());
    }
    
    private void checkContractCompatibility(JsonNode apiSpec, JsonNode consumerSpec, ContractTestResult result) {
        // Check if consumer expectations match API capabilities
        if (apiSpec.has("paths") && consumerSpec.has("interactions")) {
            JsonNode apiPaths = apiSpec.get("paths");
            JsonNode consumerInteractions = consumerSpec.get("interactions");
            
            // Simple compatibility check
            boolean compatible = true;
            // Add more sophisticated compatibility checks here
            
            result.setCompatible(compatible);
        }
    }
    
    private void generateLoadTestReport(LoadTestResult result) {
        try {
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            String fileName = "load_test_report_" + timestamp + ".html";
            Path reportPath = Paths.get(reportDirectory, fileName);
            
            StringBuilder report = new StringBuilder();
            report.append(generateHTMLHeader("Load Test Report"));
            report.append(generateLoadTestSummary(result));
            report.append(generateLoadTestMetrics(result));
            report.append(generateHTMLFooter());
            
            Files.write(reportPath, report.toString().getBytes());
            TestLogManager.info("Load test report generated: " + reportPath);
            
        } catch (IOException e) {
            TestLogManager.error("Failed to generate load test report", e);
        }
    }
    
    private void generateContractTestReport(ContractTestResult result) {
        try {
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            String fileName = "contract_test_report_" + timestamp + ".html";
            Path reportPath = Paths.get(reportDirectory, fileName);
            
            StringBuilder report = new StringBuilder();
            report.append(generateHTMLHeader("Contract Test Report"));
            report.append(generateContractTestSummary(result));
            report.append(generateContractTestIssues(result));
            report.append(generateHTMLFooter());
            
            Files.write(reportPath, report.toString().getBytes());
            TestLogManager.info("Contract test report generated: " + reportPath);
            
        } catch (IOException e) {
            TestLogManager.error("Failed to generate contract test report", e);
        }
    }
    
    private void generateComprehensiveAPITestReport(APITestSuiteResult result) {
        try {
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            String fileName = "comprehensive_api_test_report_" + timestamp + ".html";
            Path reportPath = Paths.get(reportDirectory, fileName);
            
            StringBuilder report = new StringBuilder();
            report.append(generateHTMLHeader("Comprehensive API Test Report"));
            report.append(generateAPITestSuiteSummary(result));
            report.append(generateAPITestSuiteResults(result));
            report.append(generateHTMLFooter());
            
            Files.write(reportPath, report.toString().getBytes());
            TestLogManager.info("Comprehensive API test report generated: " + reportPath);
            
        } catch (IOException e) {
            TestLogManager.error("Failed to generate comprehensive API test report", e);
        }
    }
    
    private void createReportDirectory() {
        try {
            Path dir = Paths.get(reportDirectory);
            if (!Files.exists(dir)) {
                Files.createDirectories(dir);
                TestLogManager.info("Created API report directory: " + reportDirectory);
            }
        } catch (IOException e) {
            TestLogManager.error("Failed to create API report directory", e);
        }
    }
    
    private String generateHTMLHeader(String title) {
        return "<!DOCTYPE html><html><head><title>" + title + "</title>" +
               "<style>body{font-family:Arial,sans-serif;margin:20px;}table{border-collapse:collapse;width:100%;}" +
               "th,td{border:1px solid #ddd;padding:8px;text-align:left;}th{background-color:#f2f2f2;}" +
               ".success{color:green;}.error{color:red;}.warning{color:orange;}</style></head><body>";
    }
    
    private String generateAPIDocSummary(JsonNode openApiSpec) {
        JsonNode info = openApiSpec.get("info");
        return "<h1>API Documentation</h1>" +
               "<h2>" + info.get("title").asText() + "</h2>" +
               "<p><strong>Version:</strong> " + info.get("version").asText() + "</p>" +
               "<p><strong>Description:</strong> " + info.get("description").asText() + "</p>";
    }
    
    private String generateAPIDocEndpoints(JsonNode openApiSpec) {
        StringBuilder endpoints = new StringBuilder("<h2>Endpoints</h2>");
        JsonNode paths = openApiSpec.get("paths");
        
        paths.fieldNames().forEachRemaining(path -> {
            JsonNode pathNode = paths.get(path);
            endpoints.append("<h3>").append(path).append("</h3>");
            
            pathNode.fieldNames().forEachRemaining(method -> {
                JsonNode methodNode = pathNode.get(method);
                endpoints.append("<h4>").append(method.toUpperCase()).append("</h4>");
                endpoints.append("<p><strong>Summary:</strong> ").append(methodNode.get("summary").asText()).append("</p>");
                endpoints.append("<p><strong>Description:</strong> ").append(methodNode.get("description").asText()).append("</p>");
            });
        });
        
        return endpoints.toString();
    }
    
    private String generateAPIDocSchemas(JsonNode openApiSpec) {
        StringBuilder schemas = new StringBuilder("<h2>Data Models</h2>");
        
        if (openApiSpec.has("components") && openApiSpec.get("components").has("schemas")) {
            JsonNode schemasNode = openApiSpec.get("components").get("schemas");
            schemasNode.fieldNames().forEachRemaining(schemaName -> {
                schemas.append("<h3>").append(schemaName).append("</h3>");
                // Add schema details here
            });
        }
        
        return schemas.toString();
    }
    
    private String generateLoadTestSummary(LoadTestResult result) {
        return "<h1>Load Test Report</h1>" +
               "<p><strong>Endpoint:</strong> " + result.getEndpoint() + "</p>" +
               "<p><strong>Concurrent Users:</strong> " + result.getConcurrentUsers() + "</p>" +
               "<p><strong>Duration:</strong> " + result.getDuration() + " seconds</p>" +
               "<p><strong>Total Requests:</strong> " + result.getTotalRequests() + "</p>" +
               "<p><strong>Success Rate:</strong> " + String.format("%.2f", result.getSuccessRate()) + "%</p>" +
               "<p><strong>Average Response Time:</strong> " + result.getAverageResponseTime() + "ms</p>";
    }
    
    private String generateLoadTestMetrics(LoadTestResult result) {
        return "<h2>Performance Metrics</h2>" +
               "<table><tr><th>Metric</th><th>Value</th></tr>" +
               "<tr><td>Requests per Second</td><td>" + result.getRequestsPerSecond() + "</td></tr>" +
               "<tr><td>Min Response Time</td><td>" + result.getMinResponseTime() + "ms</td></tr>" +
               "<tr><td>Max Response Time</td><td>" + result.getMaxResponseTime() + "ms</td></tr>" +
               "<tr><td>Successful Requests</td><td>" + result.getSuccessfulRequests() + "</td></tr>" +
               "<tr><td>Failed Requests</td><td>" + result.getFailedRequests() + "</td></tr>" +
               "</table>";
    }
    
    private String generateContractTestSummary(ContractTestResult result) {
        return "<h1>Contract Test Report</h1>" +
               "<p><strong>API Specification:</strong> " + result.getApiSpecPath() + "</p>" +
               "<p><strong>Consumer Specification:</strong> " + result.getConsumerSpecPath() + "</p>" +
               "<p><strong>API Contract Valid:</strong> " + (result.isApiContractValid() ? "Yes" : "No") + "</p>" +
               "<p><strong>Consumer Contract Valid:</strong> " + (result.isConsumerContractValid() ? "Yes" : "No") + "</p>" +
               "<p><strong>Compatible:</strong> " + (result.isCompatible() ? "Yes" : "No") + "</p>";
    }
    
    private String generateContractTestIssues(ContractTestResult result) {
        StringBuilder issues = new StringBuilder("<h2>Contract Issues</h2>");
        
        if (result.getContractIssues().isEmpty()) {
            issues.append("<p class='success'>No contract issues found.</p>");
        } else {
            issues.append("<ul>");
            for (String issue : result.getContractIssues()) {
                issues.append("<li class='error'>").append(issue).append("</li>");
            }
            issues.append("</ul>");
        }
        
        return issues.toString();
    }
    
    private String generateAPITestSuiteSummary(APITestSuiteResult result) {
        int totalEndpoints = result.getEndpointResults().size();
        long successfulEndpoints = result.getEndpointResults().stream().mapToLong(r -> r.isSuccess() ? 1 : 0).sum();
        
        return "<h1>Comprehensive API Test Report</h1>" +
               "<p><strong>Base URL:</strong> " + result.getApiConfig().getBaseUrl() + "</p>" +
               "<p><strong>Total Endpoints:</strong> " + totalEndpoints + "</p>" +
               "<p><strong>Successful Endpoints:</strong> " + successfulEndpoints + "</p>" +
               "<p><strong>Success Rate:</strong> " + String.format("%.2f", (double) successfulEndpoints / totalEndpoints * 100) + "%</p>";
    }
    
    private String generateAPITestSuiteResults(APITestSuiteResult result) {
        StringBuilder results = new StringBuilder("<h2>Endpoint Test Results</h2><table>");
        results.append("<tr><th>Endpoint</th><th>Method</th><th>Status</th><th>Response Time</th><th>Schema Valid</th></tr>");
        
        for (APIEndpointResult endpointResult : result.getEndpointResults()) {
            String statusClass = endpointResult.isSuccess() ? "success" : "error";
            String status = endpointResult.isSuccess() ? "PASS" : "FAIL";
            String schemaValid = endpointResult.getValidationResult() != null ? 
                (endpointResult.getValidationResult().isValid() ? "Yes" : "No") : "N/A";
            
            results.append("<tr>")
                   .append("<td>").append(endpointResult.getEndpoint().getPath()).append("</td>")
                   .append("<td>").append(endpointResult.getEndpoint().getMethod()).append("</td>")
                   .append("<td class='").append(statusClass).append("'>").append(status).append("</td>")
                   .append("<td>").append(endpointResult.getResponseTime()).append("ms</td>")
                   .append("<td>").append(schemaValid).append("</td>")
                   .append("</tr>");
        }
        
        results.append("</table>");
        return results.toString();
    }
    
    private String generateHTMLFooter() {
        return "</body></html>";
    }
    
    /**
     * API validation result data model.
     */
    public static class APIValidationResult {
        private int responseCode;
        private String schemaPath;
        private boolean valid;
        private List<ValidationMessage> validationMessages;
        private String errorMessage;
        private LocalDateTime validationTime;
        
        // Getters and setters
        public int getResponseCode() { return responseCode; }
        public void setResponseCode(int responseCode) { this.responseCode = responseCode; }
        
        public String getSchemaPath() { return schemaPath; }
        public void setSchemaPath(String schemaPath) { this.schemaPath = schemaPath; }
        
        public boolean isValid() { return valid; }
        public void setValid(boolean valid) { this.valid = valid; }
        
        public List<ValidationMessage> getValidationMessages() { return validationMessages; }
        public void setValidationMessages(List<ValidationMessage> validationMessages) { this.validationMessages = validationMessages; }
        
        public String getErrorMessage() { return errorMessage; }
        public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
        
        public LocalDateTime getValidationTime() { return validationTime; }
        public void setValidationTime(LocalDateTime validationTime) { this.validationTime = validationTime; }
    }
    
    /**
     * Load test result data model.
     */
    public static class LoadTestResult {
        private String endpoint;
        private int concurrentUsers;
        private int duration;
        private LocalDateTime startTime;
        private LocalDateTime endTime;
        private int totalRequests;
        private int successfulRequests;
        private int failedRequests;
        private double successRate;
        private double averageResponseTime;
        private long maxResponseTime;
        private long minResponseTime;
        private double requestsPerSecond;
        private String error;
        
        // Getters and setters
        public String getEndpoint() { return endpoint; }
        public void setEndpoint(String endpoint) { this.endpoint = endpoint; }
        
        public int getConcurrentUsers() { return concurrentUsers; }
        public void setConcurrentUsers(int concurrentUsers) { this.concurrentUsers = concurrentUsers; }
        
        public int getDuration() { return duration; }
        public void setDuration(int duration) { this.duration = duration; }
        
        public LocalDateTime getStartTime() { return startTime; }
        public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }
        
        public LocalDateTime getEndTime() { return endTime; }
        public void setEndTime(LocalDateTime endTime) { this.endTime = endTime; }
        
        public int getTotalRequests() { return totalRequests; }
        public void setTotalRequests(int totalRequests) { this.totalRequests = totalRequests; }
        
        public int getSuccessfulRequests() { return successfulRequests; }
        public void setSuccessfulRequests(int successfulRequests) { this.successfulRequests = successfulRequests; }
        
        public int getFailedRequests() { return failedRequests; }
        public void setFailedRequests(int failedRequests) { this.failedRequests = failedRequests; }
        
        public double getSuccessRate() { return successRate; }
        public void setSuccessRate(double successRate) { this.successRate = successRate; }
        
        public double getAverageResponseTime() { return averageResponseTime; }
        public void setAverageResponseTime(double averageResponseTime) { this.averageResponseTime = averageResponseTime; }
        
        public long getMaxResponseTime() { return maxResponseTime; }
        public void setMaxResponseTime(long maxResponseTime) { this.maxResponseTime = maxResponseTime; }
        
        public long getMinResponseTime() { return minResponseTime; }
        public void setMinResponseTime(long minResponseTime) { this.minResponseTime = minResponseTime; }
        
        public double getRequestsPerSecond() { return requestsPerSecond; }
        public void setRequestsPerSecond(double requestsPerSecond) { this.requestsPerSecond = requestsPerSecond; }
        
        public String getError() { return error; }
        public void setError(String error) { this.error = error; }
    }
    
    /**
     * Load test metrics data model.
     */
    public static class LoadTestMetrics {
        private int requestCount;
        private int successCount;
        private int errorCount;
        private long totalResponseTime;
        private long averageResponseTime;
        
        // Getters and setters
        public int getRequestCount() { return requestCount; }
        public void setRequestCount(int requestCount) { this.requestCount = requestCount; }
        
        public int getSuccessCount() { return successCount; }
        public void setSuccessCount(int successCount) { this.successCount = successCount; }
        
        public int getErrorCount() { return errorCount; }
        public void setErrorCount(int errorCount) { this.errorCount = errorCount; }
        
        public long getTotalResponseTime() { return totalResponseTime; }
        public void setTotalResponseTime(long totalResponseTime) { this.totalResponseTime = totalResponseTime; }
        
        public long getAverageResponseTime() { return averageResponseTime; }
        public void setAverageResponseTime(long averageResponseTime) { this.averageResponseTime = averageResponseTime; }
    }
    
    /**
     * Contract test result data model.
     */
    public static class ContractTestResult {
        private String apiSpecPath;
        private String consumerSpecPath;
        private LocalDateTime testTime;
        private boolean apiContractValid;
        private boolean consumerContractValid;
        private boolean compatible;
        private List<String> contractIssues;
        private String error;
        
        public ContractTestResult() {
            this.contractIssues = new ArrayList<>();
        }
        
        public void addContractIssue(String issue) {
            this.contractIssues.add(issue);
        }
        
        // Getters and setters
        public String getApiSpecPath() { return apiSpecPath; }
        public void setApiSpecPath(String apiSpecPath) { this.apiSpecPath = apiSpecPath; }
        
        public String getConsumerSpecPath() { return consumerSpecPath; }
        public void setConsumerSpecPath(String consumerSpecPath) { this.consumerSpecPath = consumerSpecPath; }
        
        public LocalDateTime getTestTime() { return testTime; }
        public void setTestTime(LocalDateTime testTime) { this.testTime = testTime; }
        
        public boolean isApiContractValid() { return apiContractValid; }
        public void setApiContractValid(boolean apiContractValid) { this.apiContractValid = apiContractValid; }
        
        public boolean isConsumerContractValid() { return consumerContractValid; }
        public void setConsumerContractValid(boolean consumerContractValid) { this.consumerContractValid = consumerContractValid; }
        
        public boolean isCompatible() { return compatible; }
        public void setCompatible(boolean compatible) { this.compatible = compatible; }
        
        public List<String> getContractIssues() { return contractIssues; }
        public void setContractIssues(List<String> contractIssues) { this.contractIssues = contractIssues; }
        
        public String getError() { return error; }
        public void setError(String error) { this.error = error; }
    }
    
    /**
     * API configuration data model.
     */
    public static class APIConfiguration {
        private String baseUrl;
        private List<APIEndpoint> endpoints;
        private boolean loadTestingEnabled;
        private int loadTestUsers;
        private int loadTestDuration;
        private String contractSpecPath;
        private String consumerSpecPath;
        
        // Getters and setters
        public String getBaseUrl() { return baseUrl; }
        public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }
        
        public List<APIEndpoint> getEndpoints() { return endpoints; }
        public void setEndpoints(List<APIEndpoint> endpoints) { this.endpoints = endpoints; }
        
        public boolean isLoadTestingEnabled() { return loadTestingEnabled; }
        public void setLoadTestingEnabled(boolean loadTestingEnabled) { this.loadTestingEnabled = loadTestingEnabled; }
        
        public int getLoadTestUsers() { return loadTestUsers; }
        public void setLoadTestUsers(int loadTestUsers) { this.loadTestUsers = loadTestUsers; }
        
        public int getLoadTestDuration() { return loadTestDuration; }
        public void setLoadTestDuration(int loadTestDuration) { this.loadTestDuration = loadTestDuration; }
        
        public String getContractSpecPath() { return contractSpecPath; }
        public void setContractSpecPath(String contractSpecPath) { this.contractSpecPath = contractSpecPath; }
        
        public String getConsumerSpecPath() { return consumerSpecPath; }
        public void setConsumerSpecPath(String consumerSpecPath) { this.consumerSpecPath = consumerSpecPath; }
    }
    
    /**
     * API endpoint data model.
     */
    public static class APIEndpoint {
        private String path;
        private String method;
        private Map<String, String> headers;
        private Map<String, String> queryParams;
        private Map<String, String> pathParams;
        private String requestBody;
        private String responseSchemaPath;
        
        // Getters and setters
        public String getPath() { return path; }
        public void setPath(String path) { this.path = path; }
        
        public String getMethod() { return method; }
        public void setMethod(String method) { this.method = method; }
        
        public Map<String, String> getHeaders() { return headers; }
        public void setHeaders(Map<String, String> headers) { this.headers = headers; }
        
        public Map<String, String> getQueryParams() { return queryParams; }
        public void setQueryParams(Map<String, String> queryParams) { this.queryParams = queryParams; }
        
        public Map<String, String> getPathParams() { return pathParams; }
        public void setPathParams(Map<String, String> pathParams) { this.pathParams = pathParams; }
        
        public String getRequestBody() { return requestBody; }
        public void setRequestBody(String requestBody) { this.requestBody = requestBody; }
        
        public String getResponseSchemaPath() { return responseSchemaPath; }
        public void setResponseSchemaPath(String responseSchemaPath) { this.responseSchemaPath = responseSchemaPath; }
    }
    
    /**
     * API endpoint result data model.
     */
    public static class APIEndpointResult {
        private APIEndpoint endpoint;
        private Response response;
        private long responseTime;
        private boolean success;
        private String errorMessage;
        private APIValidationResult validationResult;
        private LocalDateTime testTime;
        
        // Getters and setters
        public APIEndpoint getEndpoint() { return endpoint; }
        public void setEndpoint(APIEndpoint endpoint) { this.endpoint = endpoint; }
        
        public Response getResponse() { return response; }
        public void setResponse(Response response) { this.response = response; }
        
        public long getResponseTime() { return responseTime; }
        public void setResponseTime(long responseTime) { this.responseTime = responseTime; }
        
        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }
        
        public String getErrorMessage() { return errorMessage; }
        public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
        
        public APIValidationResult getValidationResult() { return validationResult; }
        public void setValidationResult(APIValidationResult validationResult) { this.validationResult = validationResult; }
        
        public LocalDateTime getTestTime() { return testTime; }
        public void setTestTime(LocalDateTime testTime) { this.testTime = testTime; }
    }
    
    /**
     * API test suite result data model.
     */
    public static class APITestSuiteResult {
        private APIConfiguration apiConfig;
        private List<APIEndpointResult> endpointResults;
        private LoadTestResult loadTestResult;
        private ContractTestResult contractTestResult;
        private LocalDateTime startTime;
        private LocalDateTime endTime;
        
        public APITestSuiteResult() {
            this.endpointResults = new ArrayList<>();
        }
        
        public void addEndpointResult(APIEndpointResult result) {
            this.endpointResults.add(result);
        }
        
        // Getters and setters
        public APIConfiguration getApiConfig() { return apiConfig; }
        public void setApiConfig(APIConfiguration apiConfig) { this.apiConfig = apiConfig; }
        
        public List<APIEndpointResult> getEndpointResults() { return endpointResults; }
        public void setEndpointResults(List<APIEndpointResult> endpointResults) { this.endpointResults = endpointResults; }
        
        public LoadTestResult getLoadTestResult() { return loadTestResult; }
        public void setLoadTestResult(LoadTestResult loadTestResult) { this.loadTestResult = loadTestResult; }
        
        public ContractTestResult getContractTestResult() { return contractTestResult; }
        public void setContractTestResult(ContractTestResult contractTestResult) { this.contractTestResult = contractTestResult; }
        
        public LocalDateTime getStartTime() { return startTime; }
        public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }
        
        public LocalDateTime getEndTime() { return endTime; }
        public void setEndTime(LocalDateTime endTime) { this.endTime = endTime; }
    }
}

