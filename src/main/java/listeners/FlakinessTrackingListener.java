package listeners;

import listeners.retry.*;
import org.testng.ITestListener;
import org.testng.ITestResult;
import reporting.TestLogManager;

/**
 * TestNG listener for tracking test flakiness and stability
 */
public class FlakinessTrackingListener implements ITestListener {
    
    private final FlakyTestDetector detector;
    private final TestStabilityScorer scorer;
    private final FlakinessReporter reporter;
    
    public FlakinessTrackingListener() {
        this.detector = new FlakyTestDetector();
        this.scorer = new TestStabilityScorer(detector);
        this.reporter = new FlakinessReporter(detector, scorer);
    }
    
    @Override
    public void onTestSuccess(ITestResult result) {
        detector.recordExecution(result);
        scorer.calculateStabilityScore(getTestKey(result));
    }
    
    @Override
    public void onTestFailure(ITestResult result) {
        detector.recordExecution(result);
        scorer.calculateStabilityScore(getTestKey(result));
        
        // Check if test is flaky
        String testKey = getTestKey(result);
        if (detector.isFlaky(testKey)) {
            TestLogManager.warning("Flaky test detected: " + testKey);
            reporter.alertOnFlakyTests();
        }
    }
    
    @Override
    public void onTestSkipped(ITestResult result) {
        detector.recordExecution(result);
    }
    
    @Override
    public void onFinish(org.testng.ITestContext context) {
        // Generate report at end of test suite
        reporter.generateReport();
    }
    
    /**
     * Get test key from result
     */
    private String getTestKey(ITestResult result) {
        String className = result.getTestClass().getName();
        String methodName = result.getMethod().getMethodName();
        return className + "." + methodName;
    }
    
    /**
     * Get flaky test detector
     */
    public FlakyTestDetector getDetector() {
        return detector;
    }
    
    /**
     * Get stability scorer
     */
    public TestStabilityScorer getScorer() {
        return scorer;
    }
    
    /**
     * Get reporter
     */
    public FlakinessReporter getReporter() {
        return reporter;
    }
}

