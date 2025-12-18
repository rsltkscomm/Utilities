package core.interfaces;

public interface BrowserInterface {

    void openUrl(String url);

    void navigateTo(String url);

    void back();

    void forward();

    void refresh();

    void maximizeWindow();

    void closeWindow();

    String getCurrentUrl();

    String getTitle();

    String getPageSource();

    void deleteAllCookies();
}
