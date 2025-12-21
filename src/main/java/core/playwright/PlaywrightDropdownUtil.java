package core.playwright;

import java.util.ArrayList;
import java.util.List;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;

import base.DriverContext;
import core.interfaces.DropdownInterface;
import reporting.ExtentManager;

public class PlaywrightDropdownUtil extends PlaywrightAssertUtil
        implements DropdownInterface {

    protected final DriverContext driverContext;

    public PlaywrightDropdownUtil(DriverContext driverContext) {
        super(driverContext); // ✅ parent now also uses DriverContext
        this.driverContext = driverContext;
    }

    /**
     * Always returns the CURRENT active page
     */
    protected Page page() {
        return driverContext.getPage();
    }

    // ------------------------------------------------------------
    // SELECT BY TEXT (contains / equals ignore case)
    // ------------------------------------------------------------
    @Override
    public boolean selectListElements(String elementsPath, String input) {
        try {
            Locator locator = resolveLocator(elementsPath);
            int count = locator.count();

            if (count == 0) {
                ExtentManager.failTest(
                        "Dropdown selection failed -> No elements found for locator: " + elementsPath);
                return false;
            }

            for (int i = 0; i < count; i++) {
                Locator option = locator.nth(i);
                String text = option.innerText().trim();

                if (text.equalsIgnoreCase(input)
                        || text.toLowerCase().contains(input.toLowerCase())) {

                    option.scrollIntoViewIfNeeded();
                    option.click();

                    ExtentManager.passTest(
                            "Dropdown selection successful -> Selected: <b>" + text + "</b>");
                    takeScreenshot();
                    return true;
                }
            }

            ExtentManager.failTest(
                    "Dropdown selection failed -> Value not found: <b>" + input + "</b>");
            return false;

        } catch (Exception e) {
            ExtentManager.failTest(
                    "Dropdown selection failed -> Locator: " + elementsPath);
            ExtentManager.failTest("Reason: " + e.getMessage());
            return false;
        }
    }

    // ------------------------------------------------------------
    // SELECT BY EXACT TEXT
    // ------------------------------------------------------------
    @Override
    public boolean selectExactListElements(String elementsPath, String input) {
        try {
            Locator locator = resolveLocator(elementsPath);
            int count = locator.count();

            if (count == 0) {
                ExtentManager.failTest(
                        "Dropdown selection failed -> No elements found for locator: " + elementsPath);
                return false;
            }

            for (int i = 0; i < count; i++) {
                Locator option = locator.nth(i);
                String text = option.innerText().trim();

                if (text.equalsIgnoreCase(input)) {
                    option.scrollIntoViewIfNeeded();
                    option.click();

                    ExtentManager.passTest(
                            "Dropdown selection successful -> Selected: <b>" + text + "</b>");
                    takeScreenshot();
                    return true;
                }
            }

            ExtentManager.failTest(
                    "Dropdown selection failed -> Value not found: <b>" + input + "</b>");
            return false;

        } catch (Exception e) {
            ExtentManager.failTest(
                    "Dropdown selection failed -> Locator: " + elementsPath);
            ExtentManager.failTest("Reason: " + e.getMessage());
            return false;
        }
    }

    // ------------------------------------------------------------
    // SELECT BY INDEX
    // ------------------------------------------------------------
    @Override
    public boolean selectListElementByIndex(String elementsPath, int index) {
        Locator locator = resolveLocator(elementsPath);
        int count = locator.count();

        if (index < 0 || index >= count) {
            throw new IllegalArgumentException(
                    "Invalid index: " + index + ". List size: " + count);
        }

        locator.nth(index).scrollIntoViewIfNeeded();
        locator.nth(index).click();
        return true;
    }

    // ------------------------------------------------------------
    // GET DROPDOWN VALUES
    // ------------------------------------------------------------
    @Override
    public List<String> getDropdownValuesasList(
            String dropdownLocator,
            String dropdownListLocator) {

        List<String> values = new ArrayList<>();

        resolveLocator(dropdownLocator).click();
        Locator options = resolveLocator(dropdownListLocator);

        for (int i = 0; i < options.count(); i++) {
            values.add(options.nth(i).innerText().trim());
        }
        return values;
    }

    // ------------------------------------------------------------
    // SELECT BY ATTRIBUTE
    // ------------------------------------------------------------
    @Override
    public boolean selectListElementByAttribute(Object pr, String attribute, String value) {
        Locator locator = resolveLocator(pr);
        int count = locator.count();

        for (int i = 0; i < count; i++) {
            Locator option = locator.nth(i);
            String attrVal = option.getAttribute(attribute);

            if (attrVal != null && attrVal.contains(value)) {
                option.scrollIntoViewIfNeeded();
                option.click();
                ExtentManager.infoTest(
                        "Clicked on element with " + attribute + ": " + value);
                return true;
            }
        }
        return false;
    }
}
