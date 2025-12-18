package core.playwright;

import com.microsoft.playwright.Page;

import core.interfaces.BrowserInterface;
import reporting.ExtentManager;

public class PlaywrightBrowserUtil extends PlaywrightScreenshotUtil implements BrowserInterface {

    private final Page page;

    public PlaywrightBrowserUtil(Page page) {
    	super(page);
        this.page = page;
    }

    @Override
    public void openUrl(String url) {
        page.navigate(url);
        ExtentManager.infoTest("URL launched: " + url);
    }

    @Override
    public void navigateTo(String url) {
        page.navigate(url);
        ExtentManager.infoTest("Navigated to: " + url);
    }

    @Override
    public void back() {
        page.goBack();
        ExtentManager.infoTest("Navigated Back");
    }

    @Override
    public void forward() {
        page.goForward();
        ExtentManager.infoTest("Navigated Forward");
    }

    @Override
    public void refresh() {
        page.reload();
        ExtentManager.infoTest("Page Refreshed");
    }

    @Override
    public void maximizeWindow() {
        // Playwright does NOT support maximize. Use viewport workaround.
        page.context().browser().newContext(new com.microsoft.playwright.Browser.NewContextOptions()
                .setViewportSize(null)
        );
        ExtentManager.infoTest("Window Maximized (Playwright simulated)");
    }

    @Override
    public void closeWindow() {
        page.close();
        ExtentManager.infoTest("Closed current window");
    }

    @Override
    public String getCurrentUrl() {
        return page.url();
    }

    @Override
    public String getTitle() {
        return page.title();
    }

    @Override
    public String getPageSource() {
        return page.content();
    }

    @Override
    public void deleteAllCookies() {
        page.context().clearCookies();
    }
}
