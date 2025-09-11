package patterns.command;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.openqa.selenium.support.ui.ExpectedConditions;
import patterns.repository.TestResult;

import java.time.Duration;
import java.time.LocalDateTime;

/**
 * Command implementation for clicking on web elements.
 */
public class ClickCommand implements UICommand {
    
    private final WebDriver driver;
    private final WebElement element;
    private final String elementDescription;
    private final int timeoutSeconds;
    
    private TestResult result;
    private long executionTime;
    private long executionDuration;
    // Removed unused field
    private boolean undoable = false;
    
    public ClickCommand(WebDriver driver, WebElement element, String elementDescription) {
        this(driver, element, elementDescription, 30);
    }
    
    public ClickCommand(WebDriver driver, WebElement element, String elementDescription, int timeoutSeconds) {
        this.driver = driver;
        this.element = element;
        this.elementDescription = elementDescription;
        this.timeoutSeconds = timeoutSeconds;
    }
    
    @Override
    public boolean execute() {
        executionTime = System.currentTimeMillis();
        LocalDateTime startTime = LocalDateTime.now();
        
        try {
            // Wait for element to be clickable
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(timeoutSeconds));
            WebElement clickableElement = wait.until(ExpectedConditions.elementToBeClickable(element));
            
            // Perform the click
            clickableElement.click();
            
            executionDuration = System.currentTimeMillis() - executionTime;
            undoable = false; // Clicks are generally not undoable
            
            result = new TestResult.Builder()
                    .testName("ClickCommand: " + elementDescription)
                    .status(TestResult.Status.PASS)
                    .startTime(startTime)
                    .endTime(LocalDateTime.now())
                    .additionalInfo("element", elementDescription)
                    .additionalInfo("timeout", String.valueOf(timeoutSeconds))
                    .build();
            
            return true;
            
        } catch (Exception e) {
            executionDuration = System.currentTimeMillis() - executionTime;
            
            result = new TestResult.Builder()
                    .testName("ClickCommand: " + elementDescription)
                    .status(TestResult.Status.FAIL)
                    .startTime(startTime)
                    .endTime(LocalDateTime.now())
                    .errorMessage("Failed to click element: " + e.getMessage())
                    .stackTrace(getStackTrace(e))
                    .additionalInfo("element", elementDescription)
                    .additionalInfo("timeout", String.valueOf(timeoutSeconds))
                    .build();
            
            return false;
        }
    }
    
    @Override
    public boolean undo() {
        // Clicks are generally not undoable
        return false;
    }
    
    @Override
    public String getDescription() {
        return "Click on element: " + elementDescription;
    }
    
    @Override
    public TestResult getResult() {
        return result;
    }
    
    @Override
    public boolean isUndoable() {
        return undoable;
    }
    
    @Override
    public long getExecutionTime() {
        return executionTime;
    }
    
    @Override
    public long getExecutionDuration() {
        return executionDuration;
    }
    
    private String getStackTrace(Exception e) {
        StringBuilder sb = new StringBuilder();
        for (StackTraceElement element : e.getStackTrace()) {
            sb.append(element.toString()).append("\n");
        }
        return sb.toString();
    }
}
