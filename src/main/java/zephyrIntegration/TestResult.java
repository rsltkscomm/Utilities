package zephyrIntegration;

import java.io.File;
import java.util.List;

public class TestResult {
    public String testCaseKey;             // e.g., RS-T524
    public boolean isPass;                 // overall test status
    public List<TestStep> stepResults;     // individual step results
    public File screenshotFile;            // screenshot path
    public File logFile;                   // log file path

    public TestResult(String testCaseKey, boolean isPass,
                      List<TestStep> stepResults) {
        this.testCaseKey = testCaseKey;
        this.isPass = isPass;
        this.stepResults = stepResults;
  
    }

    public static class TestStep {
        public String stepName;
        public boolean isPass;
        public String actualResultDescription;

        public TestStep(String stepName, boolean isPass,String actualResultDescription) {
            this.stepName = stepName;
            this.isPass = isPass;
            this.actualResultDescription = actualResultDescription;
        }
    }
}
