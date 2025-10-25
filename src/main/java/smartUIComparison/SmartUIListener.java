package smartUIComparison;

import org.testng.ITestContext;
import org.testng.ITestListener;
import org.testng.ITestResult;
import reporting.SmartUIReporting;
import reporting.ExtentManager;
import reporting.SmartUICustomReport;

/**
 * SmartUI TestNG listener (simplified) re-enabled for TestNG integration
 */
public class SmartUIListener implements ITestListener
{
	private static final ThreadLocal<Object> extentTestThreadLocal = new ThreadLocal<>();

	@Override
	public void onFinish(ITestContext context)
	{
		ExtentManager.flushReports();
		System.out.println("SmartUI Test Suite Finished: " + context.getName());
		try
		{
			String path = SmartUICustomReport.writeHtmlReport(context.getName());
			if (path != null)
			{
				System.out.println("SmartUI Custom Report generated: " + path);
			} else
			{
				System.out.println("SmartUI Custom Report generation failed");
			}
		} catch (Exception ignored)
		{
		}
	}

	@Override
	public void onTestSuccess(ITestResult result)
	{
		handleVisualComparison(result);
		cleanupThreadLocals(result);
		System.out.println("Test finished successfully: " + result.getMethod().getMethodName());
	}

	@Override
	public void onTestFailure(ITestResult result)
	{
		handleVisualComparison(result);
		cleanupThreadLocals(result);
		System.out.println("Test finished with failure: " + result.getMethod().getMethodName());
	}

	@Override
	public void onTestSkipped(ITestResult result)
	{
		cleanupThreadLocals(result);
		System.out.println("Test skipped: " + result.getMethod().getMethodName());
	}

	private void cleanupThreadLocals(ITestResult result)
	{
		extentTestThreadLocal.remove();
	}

	private void handleVisualComparison(ITestResult result)
	{
		String testName = result.getMethod().getMethodName();
		System.out.println("Starting visual comparison for test: " + testName);
		
		boolean isSmartUIEnabled = Boolean.parseBoolean(System.getProperty("smartui.enabled"));
		try
		{
			if (!isSmartUIEnabled)
			{
				System.out.println("SmartUI disabled, skipping visual comparison for: " + testName);
				return;
			}
			BaselineManager.ensureDirectoriesExist();

			VisualComparisonResult comparisonResult = performVisualComparison(testName);
			handleComparisonResult(comparisonResult, result, testName);

			// Create Jira defect if visual mismatch detected
			if (comparisonResult != null && !comparisonResult.isPassed())
			{
//				createJiraDefectForMismatch(comparisonResult, testName, result);
			}

		} catch (Exception e)
		{
			System.err.println("Error during visual comparison for test: " + testName);
			e.printStackTrace();
			reportVisualComparisonError(result, testName, e);
		}
	}

	private VisualComparisonResult performVisualComparison(String testName)
	{
		
		double tolerance = Double.parseDouble(System.getProperty("smartui.tolerance"));
		UIComparisonScreenshotUtil.waitForPageStability();

		String baselinePath = BaselineManager.getBaselinePath(testName);
		String actualPath = BaselineManager.getActualPath(testName);
		String diffPath = BaselineManager.getDiffPath(testName);

		java.awt.image.BufferedImage screenshot = UIComparisonScreenshotUtil.captureScreenshot(testName);
		UIComparisonScreenshotUtil.saveScreenshot(screenshot, actualPath);

		VisualComparisonResult comparisonResult = new VisualComparisonResult();
		comparisonResult.testName = testName;
		comparisonResult.baselinePath =baselinePath;
		comparisonResult.actualPath = actualPath;
		comparisonResult.diffPath = diffPath;
		comparisonResult.screenshotStrategy = System.getProperty("smartui.screenshot.strategy");
		comparisonResult.comparisonMethod = System.getProperty("smartui.comparison.method");

		if (!BaselineManager.baselineExists(testName))
		{
			System.out.println("Baseline not found. Creating baseline: " + baselinePath);
			BaselineManager.createBaseline(testName, actualPath);
			comparisonResult.baselineCreated = true;
			comparisonResult.diffPercent = 0.0;
			comparisonResult.tolerance = tolerance;
			comparisonResult.passed = true;
			return comparisonResult;
		}

		ComparisonMethod method = ComparisonMethod.fromString(System.getProperty("smartui.comparison.method"));
		double diffPercent = compareImages(baselinePath, actualPath, diffPath, method);

		comparisonResult.diffPercent = diffPercent;
		comparisonResult.tolerance = tolerance;
		comparisonResult.passed = diffPercent <= tolerance;
		System.out.println(String.format("Visual comparison for '%s': %s (diff: %.2f%%, tolerance: %.2f%%)", testName, comparisonResult.passed ? "PASSED" : "FAILED", diffPercent, tolerance));
		return comparisonResult;
	}

	private double compareImages(String baselinePath, String actualPath, String diffPath, ComparisonMethod method)
	{
		try
		{
			switch (method)
			{
			case TEMPLATE_MATCHING:
			case FEATURE_DETECTION:
			case STRUCTURAL_SIMILARITY:
				if (OpenCVUtils.isOpenCVAvailable())
				{
					return OpenCVUtils.compareImagesAdvanced(baselinePath, actualPath, diffPath, method);
				} else
				{
					System.out.println("OpenCV not available, falling back to basic comparison");
					return ImageComparisonUtils.compareImagesBasic(baselinePath, actualPath, diffPath);
				}
			case PIXEL_BY_PIXEL:
			default:
				return ImageComparisonUtils.compareImagesBasic(baselinePath, actualPath, diffPath);
			}
		} catch (Exception e)
		{
			System.err.println("Error in image comparison, using basic method: " + e.getMessage());
			return ImageComparisonUtils.compareImagesBasic(baselinePath, actualPath, diffPath);
		}
	}

	private void handleComparisonResult(VisualComparisonResult result, ITestResult testResult, String testName)
	{
		
		double tolerance = Double.parseDouble(System.getProperty("smartui.tolerance"));
		boolean updateBaseline = Boolean.parseBoolean(System.getProperty("smartui.update.baseline"));
		boolean updateFailOnDifference = Boolean.parseBoolean(System.getProperty("smartui.fail.on.difference"));
		try
		{
			Object extentTest = extentTestThreadLocal.get();
			if (extentTest != null)
			{
				SmartUIReporting.createDetailedReport(extentTest, testName, result.baselinePath, result.actualPath, result.diffPath, result.diffPercent, tolerance, result.passed, result.screenshotStrategy, result.comparisonMethod);
			}

			// Record entry for custom report (do this before any early returns)
			try
			{
				SmartUICustomReport.add(testResult.getTestContext().getSuite().getName(), testName, result.baselinePath, result.actualPath, result.diffPath, result.screenshotStrategy, result.comparisonMethod, result.diffPercent,
						tolerance, result.passed);
			} catch (Exception ignored)
			{
			}

			if (result.baselineCreated)
			{
				if (extentTest != null)
				{
					SmartUIReporting.reportBaselineCreation(extentTest, result.baselinePath);
				}
				return;
			}

			if (!result.passed)
			{
				if (updateBaseline)
				{
					System.out.println("Updating baseline for '" + testName + "' as differences exceed tolerance");
					BaselineManager.updateBaselineWithHistory(testName, result.actualPath);
					if (extentTest != null)
					{
						String historyPath = BaselineManager.getLatestHistoryBackup(testName);
						SmartUIReporting.reportBaselineUpdate(extentTest, result.baselinePath, historyPath);
					}
				} else
				{
					System.err.println(String.format("Visual difference (%.2f%%) exceeds tolerance (%.2f%%) for test: %s", result.diffPercent, tolerance, testName));
					if (updateFailOnDifference)
					{
						testResult.setStatus(ITestResult.FAILURE);
						testResult.setThrowable(new AssertionError(String.format("Visual difference (%.2f%%) exceeds tolerance (%.2f%%)", result.diffPercent, tolerance)));
					}
				}
			}

		} catch (Exception e)
		{
			System.err.println("Error handling comparison result for '" + testName + "': " + e.getMessage());
		}
	}

	private void reportVisualComparisonError(ITestResult result, String testName, Exception error)
	{
		try
		{
			Object extentTest = extentTestThreadLocal.get();
			if (extentTest != null)
			{
				SmartUIReporting.reportError(extentTest, testName, error);
			}
		} catch (Exception e)
		{
			System.err.println("Error reporting visual comparison error: " + e.getMessage());
		}
	}

//	/**
//	 * Create Jira defect for visual mismatch
//	 */
//	private void createJiraDefectForMismatch(VisualComparisonResult comparisonResult, String testName, ITestResult result)
//	{
//		try
//		{
//			System.out.println("Creating Jira defect for visual mismatch: " + testName);
//
//			String suiteName = result.getTestContext().getName();
//			String description = String.format("Visual regression detected in test '%s' during suite '%s'. " + "Difference: %.2f%%, Tolerance: %.2f%%", testName, suiteName, comparisonResult.getDiffPercent(), comparisonResult.getTolerance());
//
//			String issueKey = utils.JiraUtils.createVisualMismatchDefect(testName, suiteName, comparisonResult.getDiffPercent(), comparisonResult.getTolerance(), comparisonResult.getBaselinePath(), comparisonResult.getActualPath(),
//					comparisonResult.getDiffPath(), description);
//
//			if (issueKey != null)
//			{
//				// Attach screenshots to the Jira issue
//				utils.JiraUtils.attachScreenshots(issueKey, comparisonResult.getBaselinePath(), comparisonResult.getActualPath(), comparisonResult.getDiffPath());
//
//				System.out.println("Jira defect created and screenshots attached: " + issueKey);
//
//				// Add Jira issue information to test result
//				result.setAttribute("jira_issue_key", issueKey);
//				result.setAttribute("jira_issue_url", config.JiraConfig.getInstance().getUrl() + "/browse/" + issueKey);
//			}
//
//		} catch (Exception e)
//		{
//			System.err.println("Error creating Jira defect for visual mismatch: " + e.getMessage());
//			e.printStackTrace();
//		}
//	}

	private static class VisualComparisonResult
	{
		@SuppressWarnings("unused")
		String testName;
		String baselinePath;
		String actualPath;
		String diffPath;
		String screenshotStrategy;
		String comparisonMethod;
		double diffPercent;
		double tolerance;
		boolean passed;
		boolean baselineCreated;

		// Getters
		public String getBaselinePath()
		{
			return baselinePath;
		}

		public String getActualPath()
		{
			return actualPath;
		}

		public String getDiffPath()
		{
			return diffPath;
		}

		public double getDiffPercent()
		{
			return diffPercent;
		}

		public double getTolerance()
		{
			return tolerance;
		}

		public boolean isPassed()
		{
			return passed;
		}
	}
}
