package com.funnel1pg.pages;

import com.microsoft.playwright.Page;

public class ThankYouPage extends BasePage {

    private static final String HEADING_SELECTOR =
            "h1:has-text('Thank'), h2:has-text('Thank'), " +
            "h1:has-text('Order Confirmed'), h2:has-text('Order Confirmed'), " +
            "h1:has-text('Success'), h2:has-text('Success'), " +
            ".thank-you-title, [class*='thank']";

    public ThankYouPage(Page page) { super(page); }

    public boolean isThankYouPageDisplayed() {
        String url = getCurrentUrl().toLowerCase();
        if (url.contains("thank") || url.contains("confirm") ||
            url.contains("success") || url.contains("receipt")) return true;
        try {
            return page.locator(HEADING_SELECTOR).count() > 0;
        } catch (Exception e) { return false; }
    }

    public String getHeading() {
        try {
            return page.locator("h1, h2").first().textContent().trim();
        } catch (Exception e) { return "(no heading found)"; }
    }

    public String getOrderConfirmationText() {
        try {
            return page.locator("body").textContent()
                    .replaceAll("\\s+", " ").trim().substring(0, 300);
        } catch (Exception e) { return "(body not available)"; }
    }
}
