package core.playwright;

import com.microsoft.playwright.Dialog;
import com.microsoft.playwright.Page;

import core.interfaces.AlertInterface;
import reporting.ExtentManager;

public class PlaywrightAlertUtil extends PlaywrightDragAndDropUtil
        implements AlertInterface {

    private final Page page;
    private Dialog activeDialog;

    public PlaywrightAlertUtil(Page page) {
        super(page);
        this.page = page;

        // Register dialog listener ONCE
        page.onDialog(dialog -> {
            this.activeDialog = dialog;
            ExtentManager.infoTest(
                    "Alert appeared -> Type: " + dialog.type() +
                    ", Message: " + dialog.message()
            );
        });
    }

    /* -------------------- ACCEPT ALERT -------------------- */

    @Override
    public boolean acceptAlert() {
        try {
            if (activeDialog == null) {
                throw new IllegalStateException("No active alert present");
            }

            String text = activeDialog.message();
            activeDialog.accept();
            ExtentManager.infoTest("Accepted alert with text: " + text);

            activeDialog = null;
            return true;
        } catch (Exception e) {
            ExtentManager.failTest("Accept alert failed");
            ExtentManager.failTest("Reason: " + e.getMessage());
            return false;
        }
    }

    /* -------------------- DISMISS ALERT -------------------- */

    @Override
    public boolean dismissAlert() {
        try {
            if (activeDialog == null) {
                throw new IllegalStateException("No active alert present");
            }

            String text = activeDialog.message();
            activeDialog.dismiss();
            ExtentManager.infoTest("Dismissed alert with text: " + text);

            activeDialog = null;
            return true;
        } catch (Exception e) {
            ExtentManager.failTest("Dismiss alert failed");
            ExtentManager.failTest("Reason: " + e.getMessage());
            return false;
        }
    }

    /* -------------------- GET ALERT TEXT -------------------- */

    @Override
    public String getAlertText() {
        try {
            if (activeDialog == null) {
                throw new IllegalStateException("No active alert present");
            }

            String text = activeDialog.message();
            ExtentManager.infoTest("Alert text: " + text);
            return text;
        } catch (Exception e) {
            ExtentManager.failTest("Get alert text failed");
            ExtentManager.failTest("Reason: " + e.getMessage());
            return null;
        }
    }

    /* -------------------- SEND KEYS TO ALERT (PROMPT) -------------------- */

    @Override
    public boolean sendKeysToAlert(String keys) {
        try {
            if (activeDialog == null) {
                throw new IllegalStateException("No active alert present");
            }

            activeDialog.accept(keys);
            ExtentManager.infoTest("Sent keys to alert: " + keys);

            activeDialog = null;
            return true;
        } catch (Exception e) {
            ExtentManager.failTest("Send keys to alert failed");
            ExtentManager.failTest("Reason: " + e.getMessage());
            return false;
        }
    }
}
