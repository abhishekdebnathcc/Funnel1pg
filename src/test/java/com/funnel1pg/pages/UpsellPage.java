package com.funnel1pg.pages;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;

import java.util.List;
import java.util.function.Predicate;

public class UpsellPage extends BasePage {

    private static final String ACCEPT_BTNS =
            "button:has-text('YES'), a:has-text('YES'), " +
            "button:has-text('Add to'), a:has-text('Add to'), " +
            "button:has-text('Upgrade'), a:has-text('Upgrade'), " +
            "[class*='accept']";

    private static final String DECLINE_BTNS =
            "button:has-text('NO'), a:has-text('NO'), " +
            "button:has-text('No thanks'), a:has-text('No thanks'), " +
            "button:has-text('Skip'), a:has-text('Skip'), " +
            "a:has-text('No, I'), " +
            "[class*='decline']";

    private static final String THANKYOU_HEADING =
            "h1:has-text('Thank'), h2:has-text('Thank'), " +
            "h1:has-text('Order Confirmed'), h2:has-text('Order Confirmed')";

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
            System.out.println("⚠ No decline btn — trying accept: " + getCurrentUrl());
            try {
                page.locator(ACCEPT_BTNS).first().click();
                System.out.println("✔ Accepted (fallback) on: " + getCurrentUrl());
            } catch (Exception ex) {
                System.out.println("⚠ No upsell buttons found: " + ex.getMessage());
            }
        }
    }

    public void navigateThroughAllUpsells() {
        int max = 5, count = 0;
        while (count < max) {
            page.waitForLoadState();
            System.out.println("→ [" + count + "] URL: " + getCurrentUrl());

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

            // Explicit Predicate<String> to avoid overload ambiguity with waitForURL
            Predicate<String> urlChanged = url -> !url.equals(before);
            try {
                page.waitForURL(urlChanged,
                        new Page.WaitForURLOptions().setTimeout(10_000));
            } catch (Exception e) {
                page.waitForTimeout(3000);
            }
            count++;
        }
        System.out.println("✔ Upsell done. Final URL: " + getCurrentUrl());
    }
}
