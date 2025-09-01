package assertUtils;

import reporting.ExtentManager;
import seleniumUtils.ScreenshotUtil;

public class AssertUtil
{
	public boolean writeLog(boolean expression, String passLog, String failLog)
	{
		if (expression)
		{
			ExtentManager.infoTest(passLog);
			ScreenshotUtil.takeScreenshot();
		} else
		{
			ExtentManager.warningTest(failLog);
			ScreenshotUtil.takeScreenshot();
		}
		return expression;
	}
}
