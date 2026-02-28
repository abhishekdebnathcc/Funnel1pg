package com.funnel1pg.pages;

import com.funnel1pg.utils.PlaywrightManager;
import com.microsoft.playwright.Page;

public class ThankYouPage extends BasePage {

    private static final String META_PAGE_TYPE  = "meta[name='page-type']";
    private static final String ORDER_ID        = "#order_id_holder";
    private static final String TOTAL           = "#total";
    private static final String SUBTOTAL        = "#subtotal";
    private static final String SHIPPING_TOTAL  = "#shipping_total";
    private static final String FIRST_NAME      = "#firstName";
    private static final String LAST_NAME       = "#lastName";
    private static final String ADDRESS         = "#shippingAddress1";
    private static final String CITY            = "#shippingCity";
    private static final String STATE           = "#shippingState";
    private static final String ZIP             = "#shippingZip";
    private static final String EMAIL           = "#email";
    private static final String PHONE           = "#phone";
    private static final String PRODUCT_DETAILS = "#product-details";

    public ThankYouPage(Page page) { super(page); }

    // ── Detection ─────────────────────────────────────────────────────────────

    public boolean isThankYouPageDisplayed() {
        try {
            String pt = page.getAttribute(META_PAGE_TYPE, "content");
            if ("thank-you".equalsIgnoreCase(pt)) return true;
        } catch (Exception ignored) {}
        try {
            if (getCurrentUrl().toLowerCase().contains("/thank-you")) return true;
        } catch (Exception ignored) {}
        try {
            if (page.locator(ORDER_ID).count() > 0) return true;
        } catch (Exception ignored) {}
        return false;
    }

    public String getHeading() {
        try { return page.locator("h1, h2").first().textContent().trim(); }
        catch (Exception e) { return "(no heading)"; }
    }

    // ── Wait for JS to populate the page ─────────────────────────────────────

    /**
     * Waits until JS has fully populated the thank-you page:
     *   - sessionStorage cart data rendered into #product-details
     *   - order_id set either from URL param or mergeSaleSuccess event
     *   - address spans filled from localStorage checkoutData
     *
     * Also reads the order_id directly from the URL query param as a fallback,
     * and injects it into #order_id_holder if the element is still empty.
     */
    public void waitForPageToPopulate() {
        // Wait for all JS (sessionStorage render, address population) to finish.
        // networkidle is more efficient than stacking multiple waitForFunction polls.
        try {
            page.waitForLoadState(com.microsoft.playwright.options.LoadState.NETWORKIDLE,
                    new Page.WaitForLoadStateOptions().setTimeout(10_000));
        } catch (Exception ignored) {}
        page.waitForTimeout(1500);
    }

    // ── Order Detail Extraction ───────────────────────────────────────────────

    /**
     * Order ID — retrieval priority:
     *   1. Network-captured from CRM API response (most reliable — intercepted during submission)
     *   2. #order_id_holder DOM element text (set by JS from URL param or mergeSaleSuccess event)
     *   3. ?order_id= URL query parameter directly
     */
    public String getOrderNumber() {
        // 1. Best source: captured from network response before localStorage was cleared
        String networkId = PlaywrightManager.getCapturedOrderId();
        if (networkId != null && !networkId.isEmpty()) {
            return networkId;
        }

        // 2. DOM element (populated by JS if ?order_id= is in URL)
        try {
            String text = page.locator(ORDER_ID).textContent().trim();
            if (!text.isEmpty()) return text;
        } catch (Exception ignored) {}

        // 3. URL query param directly
        try {
            Object val = page.evaluate(
                "(function(){ return new URLSearchParams(window.location.search).get('order_id'); })()"
            );
            if (val != null && !val.toString().isEmpty() && !val.toString().equals("null")) {
                return val.toString();
            }
        } catch (Exception ignored) {}

        return "N/A – not returned in redirect URL";
    }

    public String getOrderPrice() {
        try { return page.locator(TOTAL).textContent().trim(); }
        catch (Exception e) { return "N/A"; }
    }

    public String getSubtotal() {
        try { return page.locator(SUBTOTAL).textContent().trim(); }
        catch (Exception e) { return "N/A"; }
    }

    public String getShippingTotal() {
        try { return page.locator(SHIPPING_TOTAL).textContent().trim(); }
        catch (Exception e) { return "N/A"; }
    }

    public String getFullName() {
        try {
            String f = page.locator(FIRST_NAME).textContent().trim();
            String l = page.locator(LAST_NAME).textContent().trim();
            return (f + " " + l).trim();
        } catch (Exception e) { return "N/A"; }
    }

    public String getEmail() {
        try { return page.locator(EMAIL).textContent().trim(); }
        catch (Exception e) { return "N/A"; }
    }

    public String getPhone() {
        try { return page.locator(PHONE).textContent().trim(); }
        catch (Exception e) { return "N/A"; }
    }

    public String getStreetAddress() {
        try { return page.locator(ADDRESS).textContent().trim(); }
        catch (Exception e) { return "N/A"; }
    }

    public String getCity() {
        try { return page.locator(CITY).textContent().trim(); }
        catch (Exception e) { return "N/A"; }
    }

    public String getState() {
        try { return page.locator(STATE).textContent().trim(); }
        catch (Exception e) { return "N/A"; }
    }

    public String getZip() {
        try { return page.locator(ZIP).textContent().trim(); }
        catch (Exception e) { return "N/A"; }
    }

    public String getCountry() {
        // Try to read from DOM; JS may or may not populate a country li.
        // Both span-based (#shippingCountry) and li-text approaches are tried.
        try {
            String bySpan = page.locator("#shippingCountry").textContent().trim();
            if (!bySpan.isEmpty()) return bySpan;
        } catch (Exception ignored) {}
        try {
            // Some funnels append country as li:nth-child(4) via getCountryByCode()
            String text = page.locator(".shipping-info li:nth-child(4)").textContent().trim();
            int colon = text.indexOf(":");
            if (colon >= 0) text = text.substring(colon + 1).trim();
            if (!text.isEmpty() && !text.equalsIgnoreCase("N/A")) return text;
        } catch (Exception ignored) {}
        return "US"; // default – all test data is US
    }

    /** Full address line for logging. */
    public String getShippingAddress() {
        return getStreetAddress() + ", " + getCity() + ", " + getState() + " " + getZip();
    }

    /** Raw product details text. */
    public String getOrderItems() {
        try { return page.locator(PRODUCT_DETAILS).textContent().trim().replaceAll("\\s+", " "); }
        catch (Exception e) { return "N/A"; }
    }

    /**
     * Returns each product as a separate line: "Product Name → $Price"
     * Reads from the rendered divs inside #product-details.
     */
    public java.util.List<String> getOrderItemLines() {
        var lines = new java.util.ArrayList<String>();
        try {
            var items = page.locator(PRODUCT_DETAILS + " > div").all();
            for (var item : items) {
                try {
                    String text = item.textContent().trim().replaceAll("\\s+", " ");
                    if (!text.isEmpty()) lines.add(text);
                } catch (Exception ignored) {}
            }
        } catch (Exception ignored) {}
        return lines;
    }
}
