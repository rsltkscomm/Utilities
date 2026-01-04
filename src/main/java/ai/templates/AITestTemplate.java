package ai.templates;

import ai.AITestCaseGenerator;
import ai.GeneratedTestCase;
import base.BaseTest;
import reporting.TestLogManager;

import java.util.*;

/**
 * AI-Powered Test Templates
 * 
 * Provides pre-built test templates that can be customized with AI
 */
public class AITestTemplate {
    
    private final AITestCaseGenerator generator;
    private final Map<String, TemplateDefinition> templates;
    
    public AITestTemplate() {
        this.generator = new AITestCaseGenerator();
        this.templates = new HashMap<>();
        registerDefaultTemplates();
    }
    
    /**
     * Generate test from template
     */
    public String generateTestFromTemplate(String templateName, Map<String, Object> parameters) {
        TemplateDefinition template = templates.get(templateName);
        if (template == null) {
            throw new IllegalArgumentException("Template not found: " + templateName);
        }
        
        TestLogManager.info("Generating test from template: " + templateName);
        
        // Generate test case based on template
        String userStory = template.generateUserStory(parameters);
        List<GeneratedTestCase> testCases = generator.generateFromUserStory(userStory);
        
        if (testCases.isEmpty()) {
            throw new RuntimeException("Failed to generate test cases from template");
        }
        
        // Apply template-specific customizations
        GeneratedTestCase testCase = testCases.get(0);
        template.customizeTestCase(testCase, parameters);
        
        // Generate test code
        return generator.generateTestNGCode(testCase);
    }
    
    /**
     * Get available templates
     */
    public Set<String> getAvailableTemplates() {
        return templates.keySet();
    }
    
    /**
     * Register default templates
     */
    private void registerDefaultTemplates() {
        registerTemplate("login", new LoginTemplate());
        registerTemplate("registration", new RegistrationTemplate());
        registerTemplate("search", new SearchTemplate());
        registerTemplate("crud", new CRUDTemplate());
        registerTemplate("api", new APITemplate());
    }
    
    /**
     * Register a custom template
     */
    public void registerTemplate(String name, TemplateDefinition template) {
        templates.put(name, template);
    }
    
    /**
     * Template definition interface
     */
    public interface TemplateDefinition {
        String generateUserStory(Map<String, Object> parameters);
        void customizeTestCase(GeneratedTestCase testCase, Map<String, Object> parameters);
        String getDescription();
    }
    
    /**
     * Login Template
     */
    public static class LoginTemplate implements TemplateDefinition {
        @Override
        public String generateUserStory(Map<String, Object> parameters) {
            String userType = (String) parameters.getOrDefault("userType", "user");
            return "As a " + userType + ", I want to login to the application so that I can access my account";
        }
        
        @Override
        public void customizeTestCase(GeneratedTestCase testCase, Map<String, Object> parameters) {
            testCase.setCategory("Authentication");
            testCase.setPriority("High");
        }
        
        @Override
        public String getDescription() {
            return "Template for login functionality tests";
        }
    }
    
    /**
     * Registration Template
     */
    public static class RegistrationTemplate implements TemplateDefinition {
        @Override
        public String generateUserStory(Map<String, Object> parameters) {
            return "As a new user, I want to register for an account so that I can use the application";
        }
        
        @Override
        public void customizeTestCase(GeneratedTestCase testCase, Map<String, Object> parameters) {
            testCase.setCategory("Registration");
            testCase.setPriority("High");
        }
        
        @Override
        public String getDescription() {
            return "Template for user registration tests";
        }
    }
    
    /**
     * Search Template
     */
    public static class SearchTemplate implements TemplateDefinition {
        @Override
        public String generateUserStory(Map<String, Object> parameters) {
            String searchType = (String) parameters.getOrDefault("searchType", "products");
            return "As a user, I want to search for " + searchType + " so that I can find what I need";
        }
        
        @Override
        public void customizeTestCase(GeneratedTestCase testCase, Map<String, Object> parameters) {
            testCase.setCategory("Search");
            testCase.setPriority("Medium");
        }
        
        @Override
        public String getDescription() {
            return "Template for search functionality tests";
        }
    }
    
    /**
     * CRUD Template
     */
    public static class CRUDTemplate implements TemplateDefinition {
        @Override
        public String generateUserStory(Map<String, Object> parameters) {
            String action = (String) parameters.getOrDefault("action", "create");
            String entity = (String) parameters.getOrDefault("entity", "item");
            return "As a user, I want to " + action + " " + entity + " so that I can manage my data";
        }
        
        @Override
        public void customizeTestCase(GeneratedTestCase testCase, Map<String, Object> parameters) {
            testCase.setCategory("CRUD");
            testCase.setPriority("High");
        }
        
        @Override
        public String getDescription() {
            return "Template for CRUD operation tests";
        }
    }
    
    /**
     * API Template
     */
    public static class APITemplate implements TemplateDefinition {
        @Override
        public String generateUserStory(Map<String, Object> parameters) {
            String endpoint = (String) parameters.getOrDefault("endpoint", "API endpoint");
            return "As a system, I want to call " + endpoint + " so that I can retrieve data";
        }
        
        @Override
        public void customizeTestCase(GeneratedTestCase testCase, Map<String, Object> parameters) {
            testCase.setCategory("API");
            testCase.setPriority("High");
        }
        
        @Override
        public String getDescription() {
            return "Template for API testing";
        }
    }
}

