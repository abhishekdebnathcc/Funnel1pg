package com.funnel1pg.pages;

import com.microsoft.playwright.Page;

public class UpsellPage extends BasePage {

    private static final String DECLINE_BTN =
            "button:has-text('No'), a:has-text('No'), " +
            "button:has-text('Skip'), a:has-text('Skip'), " +
            "a:has-text('No thanks'), a:has-text('No, I'), " +
            "[class*='decline'], [class*='no-btn']";

    private static final String ACCEPT_BTN =
            "button:has-text('Yes'), a:has-text('Yes'), " +
            "button:has-text('Add'), a:has-text('Add to my order'), " +
            "button:has-text('Upgrade'), [class*='accept'], [class*='yes-btn']";

    public UpsellPage(Page page) { super(page); }

    public boolean isUpsellPage() {
        String url = getCurrentUrl().toLowerCase();
        return url.contains("upsell") || url.contains("oto") ||
               url.contains("offer")  || url.contains("upgrade") ||
               hasUpsellButtons();
    }

    public boolean isThankYouPage() {
        String url = getCurrentUrl().toLowerCase();
        if (url.contains("thank") || url.contains("confirm") ||
            url.contains("success") || url.contains("receipt")) return true;
        try {
            return page.locator("h1, h2").filter(
                    new com.microsoft.playwright.Locator.FilterOptions()
                    .setHasText("Thank")).count() > 0;
        } catch (Exception e) { return false; }
    }

    private boolean hasUpsellButtons() {
        try {
            return page.locator(DECLINE_BTN + ", " + ACCEPT_BTN).first().isVisible();
        } catch (Exception e) { return false; }
    }

    public void declineOffer() {
        try {
            page.locator(DECLINE_BTN).first().click();
            System.out.println("Declined upsell on: " + getCurrentUrl());
        } catch (Exception e) {
            System.out.println("No decline button — trying accept: " + getCurrentUrl());
            try { page.locator(ACCEPT_BTN).first().click(); }
            catch (Exception ex) { System.out.println("No upsell buttons found"); }
        }
    }

    public void navigateThroughAllUpsells() {
        int max = 5, count = 0;
        while (count < max && !isThankYouPage()) {
            if (!isUpsellPage()) break;
            page.waitForLoadState();
            String from = getCurrentUrl();
            declineOffer();
            page.waitForTimeout(2500);
            page.waitForLoadState();
            System.out.println("Upsell " + (++count) + " processed: " + from);
        }
        System.out.println("Post-upsell URL: " + getCurrentUrl());
    }
}
