package core.interfaces;

import java.util.List;

public interface WindowInterface {

    String getCurrentWindowHandle();

    public List<String> getAllWindowHandles();

    boolean switchToWindow(String windowHandle);

    boolean switchToWindow(int index);

    boolean switchToParentWindow();

    boolean closeCurrentWindow();

    boolean closeAllOtherWindows();

    boolean openNewTab();
    
    boolean childWindowCloseIndex(int index);

    void switchWindow();
}
