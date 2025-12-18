package core.interfaces;

public interface ScreenshotInterface {

    String takeScreenshot(String screenshotName, Object element);

    void takeScreenshot();

    void javaScriptHighLightwithScrnShot(Object obj);

    String takeScreenshotBase64(Object element);

    String takeScreenshot(String screenshotName);
}
