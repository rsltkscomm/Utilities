package seleniumUtils;

import org.openqa.selenium.Alert;
import org.openqa.selenium.WebDriver;

import pages.PageFactory;
import reporting.ExtentManager;

/**
 * Utility class for handling JavaScript alerts with logging.
 */
public class AlertUtil extends DragDropUtil
{

	WebDriver driver;

	public AlertUtil(WebDriver driver, PageFactory pageFactory) {
		super(driver, pageFactory);
		this.driver = driver;
	}

	/* -------------------- SWITCH TO ALERT -------------------- */
	private Alert getAlert()
	{
		try
		{
			Alert alert = driver.switchTo().alert();
			ExtentManager.infoTest("Switched to alert successfully.");
			return alert;
		} catch (Exception e)
		{
			ExtentManager.failTest("Switch to alert failed.");
			ExtentManager.failTest("Reason: " + e.getMessage());
			return null;
		}
	}

	/* -------------------- ACCEPT ALERT -------------------- */
	public boolean acceptAlert()
	{
		try
		{
			Alert alert = getAlert();
			if (alert != null)
			{
				String text = alert.getText();
				alert.accept();
				ExtentManager.infoTest("Accepted alert with text: " + text);
				return true;
			}
		} catch (Exception e)
		{
			ExtentManager.failTest("Accept alert failed.");
			ExtentManager.failTest("Reason: " + e.getMessage());
		}
		return false;
	}

	/* -------------------- DISMISS ALERT -------------------- */
	public boolean dismissAlert()
	{
		try
		{
			Alert alert = getAlert();
			if (alert != null)
			{
				String text = alert.getText();
				alert.dismiss();
				ExtentManager.infoTest("Dismissed alert with text: " + text);
				return true;
			}
		} catch (Exception e)
		{
			ExtentManager.failTest("Dismiss alert failed.");
			ExtentManager.failTest("Reason: " + e.getMessage());
		}
		return false;
	}

	/* -------------------- GET ALERT TEXT -------------------- */
	public String getAlertText()
	{
		try
		{
			Alert alert = getAlert();
			if (alert != null)
			{
				String text = alert.getText();
				ExtentManager.infoTest("Alert text: " + text);
				return text;
			}
		} catch (Exception e)
		{
			ExtentManager.failTest("Get alert text failed.");
			ExtentManager.failTest("Reason: " + e.getMessage());
		}
		return null;
	}

	/* -------------------- SEND KEYS TO ALERT -------------------- */
	public boolean sendKeysToAlert(String keys)
	{
		try
		{
			Alert alert = getAlert();
			if (alert != null)
			{
				alert.sendKeys(keys);
				ExtentManager.infoTest("Sent keys '" + keys + "' to alert.");
				return true;
			}
		} catch (Exception e)
		{
			ExtentManager.failTest("Send keys to alert failed.");
			ExtentManager.failTest("Reason: " + e.getMessage());
		}
		return false;
	}
}
