package core.interfaces;

public interface FrameInterface {

    boolean switchToFrame(int index);

    boolean switchToFrame(String nameOrId);

    boolean switchToFrame(Object frameElement);

    boolean switchToParentFrame();

    boolean switchToDefaultContent();
}
