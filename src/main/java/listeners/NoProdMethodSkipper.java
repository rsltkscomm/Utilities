package listeners;

import org.testng.IInvokedMethod;
import org.testng.IInvokedMethodListener;
import org.testng.ITestResult;
import org.testng.SkipException;

public class NoProdMethodSkipper implements IInvokedMethodListener {

    @Override
    public void beforeInvocation(IInvokedMethod method, ITestResult testResult) {
        String restrictRun = System.getProperty("restrictRun", "yes").toLowerCase(); // default = "yes"
        String environment = System.getProperty("Environment", "").toLowerCase();
        String url = System.getProperty("Environment").toUpperCase() + "_" + System.getProperty("ReleaseVersion");

        boolean isLiveEnv = "run".equals(environment) || url.contains("liv.") || url.contains("run.");
        boolean isRun19 = url.contains("run19");

        // ✅ Block fully live environments always
        if (isLiveEnv) {
            throw new SkipException("Execution is restricted in 'live' environment for @NoProd methods");
        }

        // ✅ Block RUN19 unless explicitly allowed
        if (isRun19 && !"no".equals(restrictRun)) {
            throw new SkipException("Execution in RUN19 is restricted unless restrictRun=no is set");
        }

        // ✅ Also block all other environments if restrictRun is set to yes
        if ("yes".equals(restrictRun)) {
            throw new SkipException("Execution restricted by restrictRun=yes configuration");
        }

    }
}
