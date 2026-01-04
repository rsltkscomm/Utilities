package ai.openai;

import config.ConfigurationManager;
import reporting.TestLogManager;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * OpenAI API Client
 * 
 * Handles communication with OpenAI API
 * Supports GPT models for test generation, self-healing, and analysis
 */
public class OpenAIClient {
    
    private static volatile OpenAIClient instance;
    private final ConfigurationManager config;
    private final HttpClient httpClient;
    private final String apiKey;
    private final String baseUrl;
    private final String model;
    private final boolean enabled;
    
    private OpenAIClient() {
        this.config = ConfigurationManager.getInstance();
        this.httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(30))
            .build();
        
        // Get API key from configuration
        this.apiKey = config.getString("openai.api.key", "");
        this.baseUrl = config.getString("openai.api.url", "https://api.openai.com/v1");
        this.model = config.getString("openai.model", "gpt-4o-mini");
        this.enabled = config.getBoolean("ai.enabled", false) && 
                      !apiKey.isEmpty() && 
                      apiKey.startsWith("sk-");
        
        if (enabled) {
            TestLogManager.info("OpenAI client initialized with model: " + model);
            validateAPIKey();
        } else {
            TestLogManager.warning("OpenAI client disabled or API key not configured");
        }
    }
    
    public static OpenAIClient getInstance() {
        if (instance == null) {
            synchronized (OpenAIClient.class) {
                if (instance == null) {
                    instance = new OpenAIClient();
                }
            }
        }
        return instance;
    }
    
    /**
     * Check if OpenAI is enabled
     */
    public boolean isEnabled() {
        return enabled;
    }
    
    /**
     * Generate text using OpenAI
     */
    public String generateText(String prompt, Map<String, Object> parameters) {
        if (!enabled) {
            throw new IllegalStateException("OpenAI is not enabled or API key is not configured");
        }
        
        try {
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", model);
            requestBody.put("messages", new Object[]{
                Map.of("role", "system", "content", "You are a helpful test automation assistant."),
                Map.of("role", "user", "content", prompt)
            });
            
            // Add parameters
            if (parameters != null) {
                if (parameters.containsKey("temperature")) {
                    requestBody.put("temperature", parameters.get("temperature"));
                } else {
                    requestBody.put("temperature", 0.7);
                }
                
                if (parameters.containsKey("max_tokens")) {
                    requestBody.put("max_tokens", parameters.get("max_tokens"));
                } else {
                    requestBody.put("max_tokens", 2000);
                }
            }
            
            String response = makeRequest("chat/completions", requestBody);
            return extractContent(response);
            
        } catch (Exception e) {
            TestLogManager.error("OpenAI API call failed", e);
            throw new OpenAIException("Failed to generate text: " + e.getMessage(), e);
        }
    }
    
    /**
     * Generate test cases from user story
     */
    public String generateTestCases(String userStory) {
        String prompt = String.format(
            "Generate test cases for the following user story:\n\n" +
            "User Story: %s\n\n" +
            "Please provide:\n" +
            "1. Test case name\n" +
            "2. Test steps\n" +
            "3. Expected results\n" +
            "4. Test data requirements\n\n" +
            "Format the response as a structured test case.",
            userStory
        );
        
        Map<String, Object> params = new HashMap<>();
        params.put("temperature", 0.3); // Lower temperature for more consistent results
        params.put("max_tokens", 3000);
        
        return generateText(prompt, params);
    }
    
    /**
     * Find alternative locator using AI
     */
    public String findAlternativeLocator(String elementName, String description, String originalLocator) {
        String prompt = String.format(
            "Given the following element information:\n" +
            "Element Name: %s\n" +
            "Description: %s\n" +
            "Original Locator (broken): %s\n\n" +
            "Suggest alternative locator strategies. Provide:\n" +
            "1. Alternative locator using different attributes\n" +
            "2. XPath or CSS selector based on element description\n" +
            "3. Text-based locator if applicable\n\n" +
            "Format: Provide locator in format 'ElementName,locatorType,value'",
            elementName, description, originalLocator
        );
        
        Map<String, Object> params = new HashMap<>();
        params.put("temperature", 0.2);
        params.put("max_tokens", 1000);
        
        return generateText(prompt, params);
    }
    
    /**
     * Analyze performance data
     */
    public String analyzePerformance(Map<String, Object> performanceData) {
        String prompt = String.format(
            "Analyze the following performance metrics and provide insights:\n\n" +
            "Performance Data: %s\n\n" +
            "Please provide:\n" +
            "1. Key findings\n" +
            "2. Potential issues\n" +
            "3. Optimization recommendations",
            performanceData.toString()
        );
        
        Map<String, Object> params = new HashMap<>();
        params.put("temperature", 0.4);
        params.put("max_tokens", 2000);
        
        return generateText(prompt, params);
    }
    
    /**
     * Make HTTP request to OpenAI API
     */
    private String makeRequest(String endpoint, Map<String, Object> requestBody) throws IOException, InterruptedException {
        String url = baseUrl + "/" + endpoint;
        
        // Convert request body to JSON
        String jsonBody = convertToJson(requestBody);
        
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .header("Content-Type", "application/json")
            .header("Authorization", "Bearer " + apiKey)
            .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
            .timeout(Duration.ofSeconds(60))
            .build();
        
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        
        if (response.statusCode() != 200) {
            throw new OpenAIException("OpenAI API error: " + response.statusCode() + " - " + response.body());
        }
        
        return response.body();
    }
    
    /**
     * Extract content from OpenAI response
     */
    private String extractContent(String responseJson) {
        // Simple JSON parsing - in production, use a proper JSON library
        try {
            // Look for "content" field in the response
            int contentStart = responseJson.indexOf("\"content\":\"");
            if (contentStart == -1) {
                return responseJson; // Return full response if parsing fails
            }
            
            contentStart += 11; // Skip "content":"
            int contentEnd = responseJson.indexOf("\"", contentStart);
            if (contentEnd == -1) {
                contentEnd = responseJson.length() - 1;
            }
            
            String content = responseJson.substring(contentStart, contentEnd);
            // Unescape JSON strings
            content = content.replace("\\n", "\n")
                            .replace("\\\"", "\"")
                            .replace("\\\\", "\\");
            
            return content;
        } catch (Exception e) {
            TestLogManager.warning("Failed to parse OpenAI response, returning raw response");
            return responseJson;
        }
    }
    
    /**
     * Convert Map to JSON string (simple implementation)
     */
    private String convertToJson(Map<String, Object> map) {
        StringBuilder json = new StringBuilder("{");
        boolean first = true;
        
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            if (!first) json.append(",");
            first = false;
            
            json.append("\"").append(entry.getKey()).append("\":");
            
            Object value = entry.getValue();
            if (value instanceof String) {
                json.append("\"").append(escapeJson((String) value)).append("\"");
            } else if (value instanceof Number || value instanceof Boolean) {
                json.append(value);
            } else if (value instanceof Object[]) {
                json.append("[");
                Object[] array = (Object[]) value;
                for (int i = 0; i < array.length; i++) {
                    if (i > 0) json.append(",");
                    if (array[i] instanceof Map) {
                        json.append(convertToJson((Map<String, Object>) array[i]));
                    } else {
                        json.append("\"").append(array[i]).append("\"");
                    }
                }
                json.append("]");
            } else if (value instanceof Map) {
                json.append(convertToJson((Map<String, Object>) value));
            } else {
                json.append("\"").append(value).append("\"");
            }
        }
        
        json.append("}");
        return json.toString();
    }
    
    /**
     * Escape JSON string
     */
    private String escapeJson(String str) {
        return str.replace("\\", "\\\\")
                 .replace("\"", "\\\"")
                 .replace("\n", "\\n")
                 .replace("\r", "\\r")
                 .replace("\t", "\\t");
    }
    
    /**
     * Validate API key by making a test request
     */
    private void validateAPIKey() {
        try {
            Map<String, Object> testRequest = new HashMap<>();
            testRequest.put("model", model);
            testRequest.put("messages", new Object[]{
                Map.of("role", "user", "content", "test")
            });
            testRequest.put("max_tokens", 5);
            
            makeRequest("chat/completions", testRequest);
            TestLogManager.success("OpenAI API key validated successfully");
            
        } catch (Exception e) {
            TestLogManager.error("OpenAI API key validation failed: " + e.getMessage());
            throw new OpenAIException("Invalid OpenAI API key", e);
        }
    }
    
    /**
     * Custom exception for OpenAI errors
     */
    public static class OpenAIException extends RuntimeException {
        public OpenAIException(String message) {
            super(message);
        }
        
        public OpenAIException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}

