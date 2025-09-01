package seleniumUtils;

import java.util.List;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import pages.PageFactory;
import reporting.ExtentManager;

/**
 * Utility class for handling dropdown selections with logging.
 */
public class DropdownUtil extends ElementUtil {

	WebDriver driver;
    public DropdownUtil(WebDriver driver, PageFactory pageFactory) {
        super(driver, pageFactory);
        this.driver = driver;
    }

    /**
     * Selects an element from a list of dropdown values.
     *
     * @param elementspath - Locator path for dropdown list elements
     * @param input        - Value to match and select
     * @return true if selection was successful, false otherwise
     */
    public boolean selectListElements(String elementspath, String input) {
        try {
            List<WebElement> allElements = findElements(elementspath);
            boolean elementFound = false;

            if (!allElements.isEmpty()) {
                ExtentManager.infoTest("Dropdown elements found -> Count: " + allElements.size());

                for (int i = 0; i < allElements.size(); i++) {
                    allElements = findElements(elementspath); // refresh list
                    String eleText = allElements.get(i).getText().trim();
                    String inputVal = input.trim();

                    ExtentManager.infoTest("Checking option : " + eleText );

                    if (eleText.equalsIgnoreCase(inputVal) || eleText.toLowerCase().contains(inputVal.toLowerCase())) {
                        allElements.get(i).click();
                        ExtentManager.passTest("Dropdown selection successful -> Selected: " + eleText);
                        elementFound = true;
                        break;
                    }
                }

                if (!elementFound) {
                    ExtentManager.failTest("Dropdown selection failed -> Value not found: " + input);
                }
                return elementFound;

            } else {
                ExtentManager.failTest("Dropdown selection failed -> No elements found for locator: " + elementspath);
                return false;
            }

        } catch (Exception e) {
            ExtentManager.failTest("Dropdown selection failed -> Locator: " + elementspath);
            ExtentManager.failTest("Reason: " + e.getMessage());
            return false;
        }
    }
}
