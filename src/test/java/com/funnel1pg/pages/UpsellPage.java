package com.funnel1pg.pages;

import com.microsoft.playwright.Page;
import java.util.List;

/**
 * UpsellPage – represents an upsell/OTO page in the funnel.
 *
 * DETECTION (from real DOM inspection):
 *   Primary  → <meta name="page-type" content="upsell" />
 *   Fallback → URL path ends with /upsell or contains /upsell
 *   Fallback → form[name="is-upsell"] exists on page
 *
 * ACTIONS (exact selectors from live DOM):
 *   1. addProductToUpsell()   → clicks  a.btn-upsell  ("Add To My Order.")
 *   2. selectUpsellShipping() → clicks  div.shipping-option.shipping-method-div
 *   3. acceptAndContinue()    → clicks  button.submit-upsell-btn
 *      (decline button is #no-btn / button.decline-button – do NOT click)
 */
public class UpsellPage extends BasePage {

    // ── Exact selectors from live DOM ─────────────────────────────────────────

    /** Meta tag that identifies this page as an upsell. */
    private static final String META_PAGE_TYPE = "meta[name='page-type']";

    /** The "Add To My Order" product button on the upsell card. */
    private static final String ADD_PRODUCT_BTN = "a.btn-upsell";

    /** Shipping option rows – click to select. */
    private static final String SHIPPING_OPTION = "div.shipping-option.shipping-method-div";

    /** The primary submit button: "COMPLETE YOUR SECURE PURCHASE". */
    private static final String ACCEPT_BTN = "button.submit-upsell-btn";

    /** Decline link – "No thank you…" – used only for detection, never clicked. */
    private static final String DECLINE_BTN = "#no-btn, button.decline-button";

    /** The upsell form. */
    private static final String UPSELL_FORM = "form[name='is-upsell'], form.is-upsell";

    // ─────────────────────────────────────────────────────────────────────────

    public UpsellPage(Page page) { super(page); }

    // ── Page Detection ────────────────────────────────────────────────────────

    /**
     * Returns true ONLY when on an upsell page.
     *
     * Strategy (in priority order):
     *  1. <meta name="page-type" content="upsell"> – the most reliable signal
     *  2. URL path contains "/upsell"
     *  3. form[name="is-upsell"] present on page
     */
    /** Returns "Name  $price" for the upsell product being offered on this page. */
    public String getUpsellProductName() {
        try {
            var prod = page.locator("product").first();
            String name  = prod.getAttribute("attr-name");
            String price = prod.getAttribute("attr-sale-price");
            if (price == null || price.equals("0") || price.isEmpty())
                price = prod.getAttribute("attr-price");
            if (name != null && !name.trim().isEmpty())
                return formatItem(name.trim(), price);
        } catch (Exception ignored) {}
        try {
            String name = page.locator(".title-block__main").first().textContent().trim();
            if (!name.isEmpty() && !name.equals("PRODUCT NAME")) return name;
        } catch (Exception ignored) {}
        try {
            return page.locator("h1, h2, h3").first().textContent().trim();
        } catch (Exception ignored) {}
        return "Upsell Product";
    }

    private String formatItem(String name, String price) {
        if (price == null || price.trim().isEmpty() || price.equals("0")) return name;
        String p = price.trim();
        return p.startsWith("$") ? name + "  " + p : name + "  $" + p;
    }

        public boolean isUpsellPage() {
        try {
            // 1. Meta tag check – definitive
            String pageType = page.getAttribute(META_PAGE_TYPE, "content");
            if ("upsell".equalsIgnoreCase(pageType)) {
                return true;
            }
        } catch (Exception ignored) { }

        try {
            // 2. URL check
            if (getCurrentUrl().toLowerCase().contains("/upsell")) {
                return true;
            }
        } catch (Exception ignored) { }

        try {
            // 3. Form name check
            if (page.locator(UPSELL_FORM).count() > 0) {
                return true;
            }
        } catch (Exception ignored) { }

        return false;
    }

    // ── Upsell Actions ────────────────────────────────────────────────────────

    /**
     * Step 1: Click "Add To My Order." button (a.btn-upsell) to select the product.
     */
    public void addProductToUpsell() {
        try {
            var btn = page.locator(ADD_PRODUCT_BTN);
            if (btn.count() > 0) {
                btn.first().click();
                page.waitForTimeout(800);
                System.out.println("✓ Upsell product added (a.btn-upsell clicked)");
            } else {
                System.out.println("ℹ a.btn-upsell not found – product may auto-select");
            }
        } catch (Exception e) {
            System.out.println("⚠ addProductToUpsell: " + e.getMessage());
        }
    }

    /**
     * Step 2: Click the shipping option row to select shipping.
     * Uses div.shipping-option.shipping-method-div – clicks first available option.
     */
    public void selectUpsellShipping() {
        try {
            var options = page.locator(SHIPPING_OPTION);
            int count = options.count();
            if (count > 0) {
                options.first().click();
                page.waitForTimeout(500);
                System.out.println("✓ Upsell shipping selected (" + count + " option(s) available)");
            } else {
                System.out.println("ℹ No shipping options found on upsell page");
            }
        } catch (Exception e) {
            System.out.println("ℹ selectUpsellShipping: " + e.getMessage());
        }
    }

    /**
     * Step 3: Click "COMPLETE YOUR SECURE PURCHASE" (button.submit-upsell-btn)
     * to submit the upsell order.
     */
    public void acceptAndContinue() {
        try {
            var btn = page.locator(ACCEPT_BTN);
            if (btn.count() > 0) {
                btn.first().click();
                page.waitForTimeout(1000);
                System.out.println("✓ Upsell accepted (button.submit-upsell-btn clicked)");
            } else {
                System.out.println("⚠ button.submit-upsell-btn not found");
            }
        } catch (Exception e) {
            System.out.println("⚠ acceptAndContinue: " + e.getMessage());
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /** Returns text labels of all shipping options (for logging). */
    public List<String> getAvailableShippingMethods() {
        try {
            var labels = page.locator(SHIPPING_OPTION + " label.shipping-title").all();
            var methods = new java.util.ArrayList<String>();
            for (var l : labels) {
                try { methods.add(l.textContent().trim()); } catch (Exception ignored) { }
            }
            return methods;
        } catch (Exception e) {
            return java.util.Collections.emptyList();
        }
    }
}
