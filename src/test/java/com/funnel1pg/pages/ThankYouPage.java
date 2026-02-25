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
        // 1. Wait for DOMContentLoaded + JS execution
        page.waitForLoadState(com.microsoft.playwright.options.LoadState.DOMCONTENTLOADED);
        page.waitForTimeout(2000);

        // 2. Read order_id from URL ?order_id= param and inject into element if empty
        try {
            page.evaluate(
                "(function() {" +
                "  var params = new URLSearchParams(window.location.search);" +
                "  var orderId = params.get('order_id');" +
                "  var holder = document.getElementById('order_id_holder');" +
                "  if (holder && (!holder.textContent || !holder.textContent.trim()) && orderId) {" +
                "    holder.textContent = orderId;" +
                "  }" +
                "})();"
            );
        } catch (Exception ignored) {}

        // 3. Wait up to 5s for #order_id_holder to be non-empty
        try {
            page.waitForFunction(
                "document.getElementById('order_id_holder') && " +
                "document.getElementById('order_id_holder').textContent.trim().length > 0",
                null,
                new Page.WaitForFunctionOptions().setTimeout(5000)
            );
        } catch (Exception ignored) {
            // Not every funnel returns an order_id in the URL — that's OK
        }

        // 4. Wait for product-details to be rendered (populated from sessionStorage)
        try {
            page.waitForFunction(
                "document.getElementById('product-details') && " +
                "document.getElementById('product-details').children.length > 0",
                null,
                new Page.WaitForFunctionOptions().setTimeout(5000)
            );
        } catch (Exception ignored) {}

        // 5. Extra buffer for address spans (filled from localStorage)
        page.waitForTimeout(500);
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
