package performanceTracker;

import org.testng.ISuite;
import org.testng.ISuiteListener;

public class PerformanceInitializer implements ISuiteListener {

    @Override
    public void onFinish(ISuite suite) {
    	PerformanceTracker.generatePerformanceReportsForSuite(suite);
    }
}
