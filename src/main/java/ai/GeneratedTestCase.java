package ai;

import patterns.repository.TestData;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a generated test case with all its components
 */
public class GeneratedTestCase {
    private String testName;
    private String description;
    private List<TestStep> steps;
    private TestData testData;
    private List<String> expectedResults;
    private String priority;
    private String category;
    private List<String> tags;
    
    public GeneratedTestCase() {
        this.steps = new ArrayList<>();
        this.expectedResults = new ArrayList<>();
        this.tags = new ArrayList<>();
    }
    
    public String getTestName() {
        return testName;
    }
    
    public void setTestName(String testName) {
        this.testName = testName;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public List<TestStep> getSteps() {
        return steps;
    }
    
    public void setSteps(List<TestStep> steps) {
        this.steps = steps;
    }
    
    public TestData getTestData() {
        return testData;
    }
    
    public void setTestData(TestData testData) {
        this.testData = testData;
    }
    
    public List<String> getExpectedResults() {
        return expectedResults;
    }
    
    public void setExpectedResults(List<String> expectedResults) {
        this.expectedResults = expectedResults;
    }
    
    public String getPriority() {
        return priority;
    }
    
    public void setPriority(String priority) {
        this.priority = priority;
    }
    
    public String getCategory() {
        return category;
    }
    
    public void setCategory(String category) {
        this.category = category;
    }
    
    public List<String> getTags() {
        return tags;
    }
    
    public void setTags(List<String> tags) {
        this.tags = tags;
    }
}


