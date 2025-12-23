package core.interfaces;

import com.microsoft.playwright.Frame;

public interface FrameInterface {

    boolean switchToFrame(int index);

    boolean switchToFrame(String nameOrId);

    boolean switchToFrame(Object frameElement);

    boolean switchToParentFrame();

    boolean switchToDefaultContent();
    
    Frame getCurrentFrame();
}
