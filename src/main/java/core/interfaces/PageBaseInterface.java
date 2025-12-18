package core.interfaces;

public interface PageBaseInterface {

    String getLoginURL();

    String getEnvironment();

    void getDeviceSpecs();

    void ensureScreenshotFolderExists();
}
