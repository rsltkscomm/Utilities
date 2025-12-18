package core.interfaces;

import org.openqa.selenium.Alert;
import org.openqa.selenium.WebElement;

public interface WaitInterface {

    void setImplicitWait(int sec);

    WebElement waitForClickable(Object pr, int sec);

    WebElement waitForVisible(Object pr, int sec);

    WebElement waitForPresence(Object pr, int sec);

    boolean explicitWaitTextToBePresent(String text, Object pr, int sec);

    boolean waitForInvisibility(Object pr, int sec);

    boolean waitForText(Object pr, String text, int sec);

    boolean waitForTitle(String title, int sec);

    boolean waitForTitleContains(String partialTitle, int sec);

    boolean waitForUrl(String url, int sec);

    boolean waitForUrlContains(String partialUrl, int sec);

    Alert waitForAlert(int sec);

    boolean waitForStaleness(WebElement element, int sec);

    boolean waitForFrame(Object pr, int sec);

    WebElement fluentWait(Object pr, int timeoutSec, int pollingSec);

    boolean waitForPageLoad(int sec);

    boolean waitForJQueryLoad(int sec);

    boolean waitForJSReady(int sec);

    void turnOnImplicityWait();

    void turnOffImplicityWait();

    void wait(int seconds);

    void wait_Milli_Seconds(int milliSeconds);
}
