package seleniumUtils;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.Select;

import pages.PageFactory;
import reporting.ExtentManager;

import java.util.ArrayList;
import java.util.List;

/**
 * Utility class for handling <select> dropdowns with logging.
 */
public class SelectUtil extends DropdownUtil {

	WebDriver driver;
    public SelectUtil(WebDriver driver, PageFactory pageFactory) {
        super(driver, pageFactory);
        this.driver = driver;
    }

    /* -------------------- SELECT METHODS -------------------- */

    public boolean selectByVisibleText(String locator, String text) {
        try {
            WebElement dropdown = findElement(locator);
            Select select = new Select(dropdown);
            select.selectByVisibleText(text);
            ExtentManager.passTest("Selected by visible text -> <b>" + text + "</b>");
            return true;
        } catch (Exception e) {
            ExtentManager.failTest("Select by visible text failed -> <b>" + text + "</b>");
            ExtentManager.failTest("Reason: " + e.getMessage());
            return false;
        }
    }

    public boolean selectByValue(String locator, String value) {
        try {
            WebElement dropdown = findElement(locator);
            Select select = new Select(dropdown);
            select.selectByValue(value);
            ExtentManager.passTest("Selected by value -> <b>" + value + "</b>");
            return true;
        } catch (Exception e) {
            ExtentManager.failTest("Select by value failed -> <b>" + value + "</b>");
            ExtentManager.failTest("Reason: " + e.getMessage());
            return false;
        }
    }

    public boolean selectByIndex(String locator, int index) {
        try {
            WebElement dropdown = findElement(locator);
            Select select = new Select(dropdown);
            select.selectByIndex(index);
            ExtentManager.passTest("Selected by index -> <b>" + index + "</b>");
            return true;
        } catch (Exception e) {
            ExtentManager.failTest("Select by index failed -> <b>" + index + "</b>");
            ExtentManager.failTest("Reason: " + e.getMessage());
            return false;
        }
    }

    /* -------------------- DESELECT METHODS -------------------- */

    public boolean deselectByVisibleText(String locator, String text) {
        try {
            WebElement dropdown = findElement(locator);
            Select select = new Select(dropdown);
            select.deselectByVisibleText(text);
            ExtentManager.passTest("Deselected by visible text -> <b>" + text + "</b>");
            return true;
        } catch (Exception e) {
            ExtentManager.failTest("Deselect by visible text failed -> <b>" + text + "</b>");
            ExtentManager.failTest("Reason: " + e.getMessage());
            return false;
        }
    }

    public boolean deselectByValue(String locator, String value) {
        try {
            WebElement dropdown = findElement(locator);
            Select select = new Select(dropdown);
            select.deselectByValue(value);
            ExtentManager.passTest("Deselected by value -> <b>" + value + "</b>");
            return true;
        } catch (Exception e) {
            ExtentManager.failTest("Deselect by value failed -> <b>" + value + "</b>");
            ExtentManager.failTest("Reason: " + e.getMessage());
            return false;
        }
    }

    public boolean deselectByIndex(String locator, int index) {
        try {
            WebElement dropdown = findElement(locator);
            Select select = new Select(dropdown);
            select.deselectByIndex(index);
            ExtentManager.passTest("Deselected by index -> <b>" + index + "</b>");
            return true;
        } catch (Exception e) {
            ExtentManager.failTest("Deselect by index failed -> <b>" + index + "</b>");
            ExtentManager.failTest("Reason: " + e.getMessage());
            return false;
        }
    }

    public boolean deselectAll(String locator) {
        try {
            WebElement dropdown = findElement(locator);
            Select select = new Select(dropdown);
            select.deselectAll();
            ExtentManager.passTest("Deselected all options");
            return true;
        } catch (Exception e) {
            ExtentManager.failTest("Deselect all failed.");
            ExtentManager.failTest("Reason: " + e.getMessage());
            return false;
        }
    }

    /* -------------------- GET METHODS -------------------- */

    public String getSelectedOption(String locator) {
        try {
            WebElement dropdown = findElement(locator);
            Select select = new Select(dropdown);
            String selected = select.getFirstSelectedOption().getText().trim();
            ExtentManager.passTest("Currently selected option -> <b>" + selected + "</b>");
            return selected;
        } catch (Exception e) {
            ExtentManager.failTest("Get selected option failed for locator: " + locator);
            ExtentManager.failTest("Reason: " + e.getMessage());
            return null;
        }
    }

    public List<String> getAllSelectedOptions(String locator) {
        List<String> selectedTexts = new ArrayList<>();
        try {
            WebElement dropdown = findElement(locator);
            Select select = new Select(dropdown);
            for (WebElement option : select.getAllSelectedOptions()) {
                selectedTexts.add(option.getText().trim());
            }
            ExtentManager.passTest("All selected options -> " + selectedTexts);
        } catch (Exception e) {
            ExtentManager.failTest("Get all selected options failed for locator: " + locator);
            ExtentManager.failTest("Reason: " + e.getMessage());
        }
        return selectedTexts;
    }

    public List<String> getAllOptions(String locator) {
        List<String> optionsList = new ArrayList<>();
        try {
            WebElement dropdown = findElement(locator);
            Select select = new Select(dropdown);
            for (WebElement option : select.getOptions()) {
                optionsList.add(option.getText().trim());
            }
            ExtentManager.passTest("All available options -> " + optionsList);
        } catch (Exception e) {
            ExtentManager.failTest("Get all options failed for locator: " + locator);
            ExtentManager.failTest("Reason: " + e.getMessage());
        }
        return optionsList;
    }

    public boolean isMultiple(String locator) {
        try {
            WebElement dropdown = findElement(locator);
            Select select = new Select(dropdown);
            boolean multiple = select.isMultiple();
            ExtentManager.passTest("Is multiple select? -> " + multiple);
            return multiple;
        } catch (Exception e) {
            ExtentManager.failTest("isMultiple check failed for locator: " + locator);
            ExtentManager.failTest("Reason: " + e.getMessage());
            return false;
        }
    }
}
