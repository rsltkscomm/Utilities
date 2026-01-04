package ai;

/**
 * Represents a single step in a test case
 */
public class TestStep {
    private int stepNumber;
    private String description;
    private String code;
    private String expectedResult;
    
    public TestStep() {
    }
    
    public TestStep(int stepNumber, String description, String code, String expectedResult) {
        this.stepNumber = stepNumber;
        this.description = description;
        this.code = code;
        this.expectedResult = expectedResult;
    }
    
    public int getStepNumber() {
        return stepNumber;
    }
    
    public void setStepNumber(int stepNumber) {
        this.stepNumber = stepNumber;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public String getCode() {
        return code;
    }
    
    public void setCode(String code) {
        this.code = code;
    }
    
    public String getExpectedResult() {
        return expectedResult;
    }
    
    public void setExpectedResult(String expectedResult) {
        this.expectedResult = expectedResult;
    }
}


