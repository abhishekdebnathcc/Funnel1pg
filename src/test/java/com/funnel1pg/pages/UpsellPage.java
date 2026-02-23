package com.funnel1pg.pages;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import java.util.List;

public class UpsellPage extends BasePage {

    // Accept buttons — common patterns across upsell pages
    private static final String ACCEPT_BTNS =
            "button:has-text('YES'), a:has-text('YES'), " +
            "button:has-text('Add to'), a:has-text('Add to'), " +
            "button:has-text('Upgrade'), a:has-text('Upgrade'), " +
            "button:has-text('Get'), " +
            "[class*='yes'][class*='btn'], [class*='accept']";

    // Decline buttons — common patterns across upsell pages
    private static final String DECLINE_BTNS =
            "button:has-text('NO'), a:has-text('NO'), " +
            "button:has-text('No thanks'), a:has-text('No thanks'), " +
            "button:has-text('Skip'), a:has-text('Skip'), " +
            "a:has-text('No, I'), " +
            "[class*='no'][class*='btn'], [class*='decline']";

    // Thank-you page indicators
    private static final String THANKYOU_HEADING =
            "h1:has-text('Thank'), h2:has-text('Thank'), " +
            "h1:has-text('Order Confirmed'), h2:has-text('Order Confirmed'), " +
            "h1:has-text('Success'), .thank-you-title";

    public UpsellPage(Page page) { super(page); }

    // ── Page Detection ────────────────────────────────────────────────────────

    public boolean isUpsellPage() {
        String url = getCurrentUrl().toLowerCase();
        if (url.contains("upsell") || url.contains("oto") ||
            url.contains("offer")  || url.contains("upgrade")) return true;
        try {
            List<Locator> btns = page.locator(DECLINE_BTNS + ", " + ACCEPT_BTNS).all();
            return !btns.isEmpty() && btns.get(0).isVisible();
        } catch (Exception e) { return false; }
    }

    public boolean isThankYouPage() {
        String url = getCurrentUrl().toLowerCase();
        if (url.contains("thank") || url.contains("confirm") ||
            url.contains("success") || url.contains("receipt")) return true;
        try {
            return page.locator(THANKYOU_HEADING).count() > 0;
        } catch (Exception e) { return false; }
    }

    // ── Navigation ────────────────────────────────────────────────────────────

    public void declineOffer() {
        try {
            page.locator(DECLINE_BTNS).first().click();
            System.out.println("✔ Declined upsell on: " + getCurrentUrl());
        } catch (Exception e) {
            System.out.println("⚠ No decline btn — trying accept on: " + getCurrentUrl());
            try {
                page.locator(ACCEPT_BTNS).first().click();
                System.out.println("✔ Accepted (fallback) on: " + getCurrentUrl());
            } catch (Exception ex) {
                System.out.println("⚠ No upsell buttons found: " + ex.getMessage());
            }
        }
    }

    /**
     * Walk through every upsell page, declining each offer, until we reach
     * the thank-you page or run out of upsell pages (max 5).
     */
    public void navigateThroughAllUpsells() {
        int max = 5, count = 0;
        while (count < max) {
            page.waitForLoadState();
            System.out.println("→ [" + count + "] Current URL: " + getCurrentUrl());

            if (isThankYouPage()) {
                System.out.println("✔ Thank-you page reached after " + count + " upsell(s)");
                break;
            }
            if (!isUpsellPage()) {
                System.out.println("ℹ No upsell detected — exiting loop");
                break;
            }

            String before = getCurrentUrl();
            declineOffer();

            // Wait for navigation away from current upsell
            try {
                page.waitForURL(url -> !url.equals(before),
                        new Page.WaitForURLOptions().setTimeout(10_000));
            } catch (Exception e) {
                page.waitForTimeout(3000);
            }
            count++;
        }
        System.out.println("✔ Upsell navigation done. Final URL: " + getCurrentUrl());
    }
}
