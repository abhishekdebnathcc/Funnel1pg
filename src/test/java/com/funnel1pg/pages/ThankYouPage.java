package com.funnel1pg.pages;

import com.microsoft.playwright.Page;

public class ThankYouPage extends BasePage {

    public ThankYouPage(Page page) { super(page); }

    public boolean isThankYouPageDisplayed() {
        String url = getCurrentUrl().toLowerCase();
        if (url.contains("thank") || url.contains("confirm") ||
            url.contains("success") || url.contains("receipt")) return true;
        try {
            String h = page.locator("h1, h2, .thank-you-heading").first().textContent();
            return h != null && (h.toLowerCase().contains("thank") ||
                                 h.toLowerCase().contains("order") ||
                                 h.toLowerCase().contains("confirm"));
        } catch (Exception e) { return false; }
    }

    public String getHeading() {
        try { return page.locator("h1, h2").first().textContent(); }
        catch (Exception e) { return "(heading not found)"; }
    }
}
