package core.playwright;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.options.BoundingBox;
import org.openqa.selenium.By;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.Point;
import org.openqa.selenium.Rectangle;
import org.openqa.selenium.WebElement;

import java.util.ArrayList;
import java.util.List;

public class PlaywrightWebElement implements WebElement {

    final Locator locator;

    public PlaywrightWebElement(Locator locator) {
        this.locator = locator;
    }

    @Override
    public void click() {
        locator.click();
    }

    @Override
    public void submit() {
        locator.press("Enter");
    }

    @Override
    public void sendKeys(CharSequence... keysToSend) {
        StringBuilder sb = new StringBuilder();
        for (CharSequence cs : keysToSend) sb.append(cs);
        locator.type(sb.toString());
    }

    @Override
    public void clear() {
        locator.fill("");
    }

    @Override
    public String getTagName() {
        Object result = locator.evaluate("el => el.tagName.toLowerCase()");
        return result == null ? null : result.toString();
    }

    @Override
    public String getAttribute(String name) {
        return locator.getAttribute(name);
    }

    @Override
    public boolean isSelected() {
        String checked = locator.getAttribute("checked");
        return checked != null && (checked.equals("true") || checked.equals("checked"));
    }

    @Override
    public boolean isEnabled() {
        String disabled = locator.getAttribute("disabled");
        return disabled == null;
    }

    @Override
    public String getText() {
        return locator.innerText();
    }

    @Override
    public boolean isDisplayed() {
        try {
            return locator.isVisible();
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public Point getLocation() {
        try {
            BoundingBox box = locator.boundingBox();
            if (box == null) return new Point(0, 0);
            return new Point((int) box.x, (int) box.y);
        } catch (Exception e) {
            return new Point(0, 0);
        }
    }

    @Override
    public Dimension getSize() {
        try {
            BoundingBox box = locator.boundingBox();
            if (box == null) return new Dimension(0, 0);
            return new Dimension((int) box.width, (int) box.height);
        } catch (Exception e) {
            return new Dimension(0, 0);
        }
    }

    @Override
    public Rectangle getRect() {
        BoundingBox box = locator.boundingBox();
        if (box == null) return new Rectangle(0, 0, 0, 0);
        return new Rectangle((int) box.x, (int) box.y, (int) box.width, (int) box.height);
    }

    @Override
    public String getCssValue(String propertyName) {
        Object val = locator.evaluate(
                "(el, prop) => window.getComputedStyle(el).getPropertyValue(prop)", propertyName
        );
        return val == null ? null : val.toString();
    }

    @Override
    public WebElement findElement(By by) {
        String selector = convertByToSelector(by);
        Locator found = locator.locator(selector);
        return new PlaywrightWebElement(found);
    }

    @Override
    public List<WebElement> findElements(By by) {
        String selector = convertByToSelector(by);
        Locator found = locator.locator(selector);
        long count = found.count();
        List<WebElement> list = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            list.add(new PlaywrightWebElement(found.nth(i)));
        }
        return list;
    }

    @Override
    public <X> X getScreenshotAs(org.openqa.selenium.OutputType<X> target) {
        throw new UnsupportedOperationException(
                "getScreenshotAs not implemented in PlaywrightWebElement wrapper."
        );
    }

    @Override
    public String getAccessibleName() {
        return locator.getAttribute("aria-label");
    }

    @Override
    public String getAriaRole() {
        return locator.getAttribute("role");
    }

    private String convertByToSelector(By by) {
        String raw = by.toString();
        int colon = raw.indexOf(":");
        if (colon < 0) return raw;

        String type = raw.substring(3, colon).trim().toLowerCase();
        String value = raw.substring(colon + 1).trim();

        switch (type) {
            case "cssselector": return value;
            case "id": return "#" + value;
            case "xpath": return "xpath=" + value;
            case "name": return "[name=\"" + value + "\"]";
            case "classname": return "." + value;
            case "tagname": return value;
            case "linktext": return "text=\"" + value + "\"";
            case "partiallinktext": return "text=" + value;
        }
        return value;
    }
}
