package patterns.command;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.openqa.selenium.support.ui.ExpectedConditions;
import patterns.repository.TestResult;

import java.time.Duration;
import java.time.LocalDateTime;

/**
 * Command implementation for inputting text into web elements.
 */
public class InputCommand implements UICommand {
    
    private final WebDriver driver;
    private final WebElement element;
    private final String text;
    private final String elementDescription;
    private final int timeoutSeconds;
    
    private TestResult result;
    private long executionTime;
    private long executionDuration;
    private boolean executed = false;
    private boolean undoable = true;
    private String previousValue = "";
    
    public InputCommand(WebDriver driver, WebElement element, String text, String elementDescription) {
        this(driver, element, text, elementDescription, 30);
    }
    
    public InputCommand(WebDriver driver, WebElement element, String text, String elementDescription, int timeoutSeconds) {
        this.driver = driver;
        this.element = element;
        this.text = text;
        this.elementDescription = elementDescription;
        this.timeoutSeconds = timeoutSeconds;
    }
    
    @Override
    public boolean execute() {
        executionTime = System.currentTimeMillis();
        LocalDateTime startTime = LocalDateTime.now();
        
        try {
            // Wait for element to be visible and enabled
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(timeoutSeconds));
            WebElement visibleElement = wait.until(ExpectedConditions.visibilityOf(element));
            
            // Store previous value for undo
            previousValue = visibleElement.getAttribute("value");
            if (previousValue == null) {
                previousValue = "";
            }
            
            // Clear and input text
            visibleElement.clear();
            visibleElement.sendKeys(text);
            
            executionDuration = System.currentTimeMillis() - executionTime;
            executed = true;
            
            result = new TestResult.Builder()
                    .testName("InputCommand: " + elementDescription)
                    .status(TestResult.Status.PASS)
                    .startTime(startTime)
                    .endTime(LocalDateTime.now())
                    .additionalInfo("element", elementDescription)
                    .additionalInfo("text", text)
                    .additionalInfo("previousValue", previousValue)
                    .additionalInfo("timeout", String.valueOf(timeoutSeconds))
                    .build();
            
            return true;
            
        } catch (Exception e) {
            executionDuration = System.currentTimeMillis() - executionTime;
            executed = true;
            
            result = new TestResult.Builder()
                    .testName("InputCommand: " + elementDescription)
                    .status(TestResult.Status.FAIL)
                    .startTime(startTime)
                    .endTime(LocalDateTime.now())
                    .errorMessage("Failed to input text: " + e.getMessage())
                    .stackTrace(getStackTrace(e))
                    .additionalInfo("element", elementDescription)
                    .additionalInfo("text", text)
                    .additionalInfo("timeout", String.valueOf(timeoutSeconds))
                    .build();
            
            return false;
        }
    }
    
    @Override
    public boolean undo() {
        if (!executed || !undoable || previousValue == null) {
            return false;
        }
        
        try {
            // Restore previous value
            element.clear();
            if (!previousValue.isEmpty()) {
                element.sendKeys(previousValue);
            }
            return true;
            
        } catch (Exception e) {
            return false;
        }
    }
    
    @Override
    public String getDescription() {
        return "Input text '" + text + "' into element: " + elementDescription;
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
