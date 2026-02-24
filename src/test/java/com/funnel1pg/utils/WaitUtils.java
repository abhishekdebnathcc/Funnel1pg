package com.funnel1pg.utils;

import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.LoadState;
import com.microsoft.playwright.options.WaitForSelectorState;

import java.util.function.Predicate;

public class WaitUtils {

    private WaitUtils() {}

    /** Wait for an element to be visible with a custom timeout (ms). */
    public static void waitForVisible(Page page, String selector, int timeoutMs) {
        page.waitForSelector(selector,
                new Page.WaitForSelectorOptions()
                        .setState(WaitForSelectorState.VISIBLE)
                        .setTimeout(timeoutMs));
    }

    /** Wait for URL to contain a given substring. */
    public static void waitForUrlContains(Page page, String substring, int timeoutMs) {
        page.waitForURL("**" + substring + "**",
                new Page.WaitForURLOptions().setTimeout(timeoutMs));
    }

    /**
     * Wait for URL to change away from the given URL.
     * Uses explicit Predicate<String> cast to avoid overload ambiguity.
     */
    public static void waitForUrlChange(Page page, String currentUrl, int timeoutMs) {
        Predicate<String> urlChanged = url -> !url.equals(currentUrl);
        try {
            page.waitForURL(urlChanged,
                    new Page.WaitForURLOptions().setTimeout(timeoutMs));
        } catch (Exception e) {
            // Fallback: manual polling
            long deadline = System.currentTimeMillis() + timeoutMs;
            while (System.currentTimeMillis() < deadline) {
                if (!page.url().equals(currentUrl)) return;
                page.waitForTimeout(300);
            }
        }
    }

    /** Wait for network idle. */
    public static void waitForNetworkIdle(Page page) {
        page.waitForLoadState(LoadState.NETWORKIDLE);
    }

    /**
     * Retry clicking an element up to maxAttempts times.
     * Note: throws RuntimeException if all attempts fail.
     */
    public static void retryClick(Page page, String selector, int maxAttempts) {
        Exception last = null;
        for (int i = 0; i < maxAttempts; i++) {
            try {
                page.locator(selector).first().click();
                return;
            } catch (Exception e) {
                last = e;
                page.waitForTimeout(500);
            }
        }
        throw new RuntimeException("retryClick failed after " + maxAttempts + " attempts: " + selector, last);
    }

    /** Check if an element is present in the DOM (may not be visible). */
    public static boolean isPresent(Page page, String selector) {
        try { return page.locator(selector).count() > 0; }
        catch (Exception e) { return false; }
    }

    /** Check if an element is visible on the page. */
    public static boolean isVisible(Page page, String selector) {
        try { return page.locator(selector).first().isVisible(); }
        catch (Exception e) { return false; }
    }
}
