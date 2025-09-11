package patterns.command;

import org.openqa.selenium.WebDriver;
import patterns.repository.TestResult;

import java.time.LocalDateTime;

/**
 * Command implementation for browser navigation actions.
 */
public class NavigationCommand implements UICommand {
    
    public enum NavigationType {
        GET, BACK, FORWARD, REFRESH
    }
    
    private final WebDriver driver;
    private final NavigationType navigationType;
    private final String url;
    private final String description;
    
    private TestResult result;
    private long executionTime;
    private long executionDuration;
    private boolean executed = false;
    private boolean undoable = false;
    private String previousUrl = "";
    
    public NavigationCommand(WebDriver driver, NavigationType navigationType, String url, String description) {
        this.driver = driver;
        this.navigationType = navigationType;
        this.url = url;
        this.description = description;
    }
    
    public NavigationCommand(WebDriver driver, NavigationType navigationType, String description) {
        this(driver, navigationType, null, description);
    }
    
    @Override
    public boolean execute() {
        executionTime = System.currentTimeMillis();
        LocalDateTime startTime = LocalDateTime.now();
        
        try {
            // Store current URL for potential undo
            previousUrl = driver.getCurrentUrl();
            
            // Perform navigation based on type
            switch (navigationType) {
                case GET:
                    if (url == null || url.trim().isEmpty()) {
                        throw new IllegalArgumentException("URL is required for GET navigation");
                    }
                    driver.get(url);
                    break;
                case BACK:
                    driver.navigate().back();
                    break;
                case FORWARD:
                    driver.navigate().forward();
                    break;
                case REFRESH:
                    driver.navigate().refresh();
                    break;
                default:
                    throw new UnsupportedOperationException("Unsupported navigation type: " + navigationType);
            }
            
            executionDuration = System.currentTimeMillis() - executionTime;
            executed = true;
            
            result = new TestResult.Builder()
                    .testName("NavigationCommand: " + description)
                    .status(TestResult.Status.PASS)
                    .startTime(startTime)
                    .endTime(LocalDateTime.now())
                    .additionalInfo("navigationType", navigationType.toString())
                    .additionalInfo("url", url != null ? url : "N/A")
                    .additionalInfo("previousUrl", previousUrl)
                    .additionalInfo("currentUrl", driver.getCurrentUrl())
                    .build();
            
            return true;
            
        } catch (Exception e) {
            executionDuration = System.currentTimeMillis() - executionTime;
            executed = true;
            
            result = new TestResult.Builder()
                    .testName("NavigationCommand: " + description)
                    .status(TestResult.Status.FAIL)
                    .startTime(startTime)
                    .endTime(LocalDateTime.now())
                    .errorMessage("Navigation failed: " + e.getMessage())
                    .stackTrace(getStackTrace(e))
                    .additionalInfo("navigationType", navigationType.toString())
                    .additionalInfo("url", url != null ? url : "N/A")
                    .additionalInfo("previousUrl", previousUrl)
                    .build();
            
            return false;
        }
    }
    
    @Override
    public boolean undo() {
        if (!executed || !undoable || previousUrl == null || previousUrl.trim().isEmpty()) {
            return false;
        }
        
        try {
            // Navigate back to previous URL
            driver.get(previousUrl);
            return true;
            
        } catch (Exception e) {
            return false;
        }
    }
    
    @Override
    public String getDescription() {
        return "Navigate " + navigationType.toString().toLowerCase() + 
               (url != null ? " to " + url : "") + ": " + description;
    }
    
    @Override
    public TestResult getResult() {
        return result;
    }
    
    @Override
    public boolean isUndoable() {
        return undoable && navigationType == NavigationType.GET;
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
