package core.playwright;

import java.util.ArrayList;
import java.util.List;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.SelectOption;

import base.DriverContext;
import core.interfaces.SelectInterface;
import reporting.ExtentManager;

public class PlaywrightSelectUtil extends PlaywrightDropdownUtil
        implements SelectInterface {

    protected final DriverContext driverContext;

    public PlaywrightSelectUtil(DriverContext driverContext) {
        super(driverContext); // ✅ FIXED
        this.driverContext = driverContext;
    }

    /**
     * Always resolve the CURRENT active page
     */
    protected Page page() {
        return driverContext.getPage();
    }

    // ---------------------------------------------------------
    // SELECT METHODS
    // ---------------------------------------------------------

    @Override
    public boolean selectByVisibleText(String locator, String text) {
        try {
            resolveLocator(locator)
                    .selectOption(new SelectOption().setLabel(text));

            ExtentManager.passTest(
                    "Selected by visible text -> <b>" + text + "</b>");
            return true;
        } catch (Exception e) {
            ExtentManager.failTest(
                    "Select by visible text failed -> <b>" + text + "</b>");
            ExtentManager.failTest("Reason: " + e.getMessage());
            return false;
        }
    }

    @Override
    public boolean selectByValue(String locator, String value) {
        try {
            resolveLocator(locator).selectOption(value);
            ExtentManager.passTest(
                    "Selected by value -> <b>" + value + "</b>");
            return true;
        } catch (Exception e) {
            ExtentManager.failTest(
                    "Select by value failed -> <b>" + value + "</b>");
            ExtentManager.failTest("Reason: " + e.getMessage());
            return false;
        }
    }

    @Override
    public boolean selectByIndex(String locator, int index) {
        try {
            Locator select = resolveLocator(locator);
            Locator option = select.locator("option").nth(index);
            select.selectOption(option.getAttribute("value"));

            ExtentManager.passTest(
                    "Selected by index -> <b>" + index + "</b>");
            return true;
        } catch (Exception e) {
            ExtentManager.failTest(
                    "Select by index failed -> <b>" + index + "</b>");
            ExtentManager.failTest("Reason: " + e.getMessage());
            return false;
        }
    }

    // ---------------------------------------------------------
    // DESELECT METHODS (MULTI-SELECT ONLY)
    // ---------------------------------------------------------

    @Override
    public boolean deselectByVisibleText(String locator, String text) {
        try {
            resolveLocator(locator)
                    .evaluate(
                        "(el, txt) => [...el.options]" +
                        ".forEach(o => { if(o.label===txt) o.selected=false })",
                        text
                    );

            ExtentManager.passTest(
                    "Deselected by visible text -> <b>" + text + "</b>");
            return true;
        } catch (Exception e) {
            ExtentManager.failTest(
                    "Deselect by visible text failed -> <b>" + text + "</b>");
            ExtentManager.failTest("Reason: " + e.getMessage());
            return false;
        }
    }

    @Override
    public boolean deselectByValue(String locator, String value) {
        try {
            resolveLocator(locator)
                    .evaluate(
                        "(el, val) => [...el.options]" +
                        ".forEach(o => { if(o.value===val) o.selected=false })",
                        value
                    );

            ExtentManager.passTest(
                    "Deselected by value -> <b>" + value + "</b>");
            return true;
        } catch (Exception e) {
            ExtentManager.failTest(
                    "Deselect by value failed -> <b>" + value + "</b>");
            ExtentManager.failTest("Reason: " + e.getMessage());
            return false;
        }
    }

    @Override
    public boolean deselectByIndex(String locator, int index) {
        try {
            resolveLocator(locator)
                    .evaluate("(el, i) => el.options[i].selected=false", index);

            ExtentManager.passTest(
                    "Deselected by index -> <b>" + index + "</b>");
            return true;
        } catch (Exception e) {
            ExtentManager.failTest(
                    "Deselect by index failed -> <b>" + index + "</b>");
            ExtentManager.failTest("Reason: " + e.getMessage());
            return false;
        }
    }

    @Override
    public boolean deselectAll(String locator) {
        try {
            resolveLocator(locator)
                    .evaluate(
                        "el => [...el.options].forEach(o => o.selected=false)"
                    );

            ExtentManager.passTest("Deselected all options");
            return true;
        } catch (Exception e) {
            ExtentManager.failTest("Deselect all failed.");
            ExtentManager.failTest("Reason: " + e.getMessage());
            return false;
        }
    }

    // ---------------------------------------------------------
    // GET METHODS
    // ---------------------------------------------------------

    @Override
    public String getSelectedOption(String locator) {
        try {
            String value = resolveLocator(locator)
                    .evaluate("el => el.selectedOptions[0]?.textContent")
                    .toString()
                    .trim();

            ExtentManager.passTest(
                    "Currently selected option -> <b>" + value + "</b>");
            return value;
        } catch (Exception e) {
            ExtentManager.failTest(
                    "Get selected option failed for locator: " + locator);
            ExtentManager.failTest("Reason: " + e.getMessage());
            return null;
        }
    }

    @Override
    public List<String> getAllSelectedOptions(String locator) {
        List<String> list = new ArrayList<>();
        try {
            List<?> values = (List<?>) resolveLocator(locator)
                    .evaluate(
                        "el => [...el.selectedOptions]" +
                        ".map(o => o.textContent.trim())"
                    );

            values.forEach(v -> list.add(v.toString()));
            ExtentManager.passTest("All selected options -> " + list);
        } catch (Exception e) {
            ExtentManager.failTest(
                    "Get all selected options failed for locator: " + locator);
        }
        return list;
    }

    @Override
    public List<String> getAllOptions(String locator) {
        List<String> list = new ArrayList<>();
        try {
            List<?> values = (List<?>) resolveLocator(locator)
                    .evaluate(
                        "el => [...el.options]" +
                        ".map(o => o.textContent.trim())"
                    );

            values.forEach(v -> list.add(v.toString()));
            ExtentManager.passTest("All available options -> " + list);
        } catch (Exception e) {
            ExtentManager.failTest(
                    "Get all options failed for locator: " + locator);
        }
        return list;
    }

    @Override
    public boolean isMultiple(String locator) {
        try {
            boolean multiple = (Boolean) resolveLocator(locator)
                    .evaluate("el => el.multiple");

            ExtentManager.passTest(
                    "Is multiple select? -> " + multiple);
            return multiple;
        } catch (Exception e) {
            ExtentManager.failTest(
                    "isMultiple check failed for locator: " + locator);
            return false;
        }
    }

    // ---------------------------------------------------------
    // JS SELECT ALL TEXT
    // ---------------------------------------------------------

    @Override
    public boolean jsSelectAllText(String locator) {
        try {
            resolveLocator(locator).click();
            resolveLocator(locator)
                    .evaluate(
                        "el => { const r=document.createRange();" +
                        "r.selectNodeContents(el);" +
                        "const s=window.getSelection();" +
                        "s.removeAllRanges();" +
                        "s.addRange(r); }"
                    );

            ExtentManager.infoTest(
                    "All text selected inside content area.");
            return true;
        } catch (Exception e) {
            ExtentManager.failTest(
                    "Error selecting all text in content area");
            return false;
        }
    }
}
